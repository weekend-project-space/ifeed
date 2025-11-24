package org.bitmagic.ifeed.application.search;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.Bm25RetrievalHandler;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.MultiChannelRetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.VectorRetrievalHandler;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 混合检索服务：结合 BM25 与向量召回，完成打分融合与分页返回。
 */
@Slf4j
@Service
public class SearchRetrievalService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ArticleService articleService;
    private final SearchRetrievalProperties properties;
    private final RetrievalPipeline retrievalPipeline;

    public SearchRetrievalService(VectorStoreTurbo vectorStore, UserSubscriptionRepository userSubscriptionRepository, ArticleService articleService, Bm25RetrievalHandler bm25RetrievalHandler, SearchRetrievalProperties properties) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.articleService = articleService;
        this.properties = properties;
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda()).
                addHandler(bm25RetrievalHandler, properties.getBm25Weight())
                .addHandler(new VectorRetrievalHandler(vectorStore), properties.getVectorWeight());
    }

    public Page<ArticleSummaryView> hybridSearch(Integer userId,
                                                 String query,
                                                 boolean includeGlobal,
                                                 int page,
                                                 int size) {

        if (!StringUtils.hasText(query)) {
            return Page.empty(PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        }
        List<DocScore> artIds = hybridSearch(userId, null, query, includeGlobal, properties.getFusionTopK());
        Page<ArticleSummaryView> data = articleService.findIds2Article(artIds.stream().map(DocScore::docId).toList(), page, size);
        log.debug("Hybrid search complete: user={}, query='{}', totalCandidates={}, pageIsLast={}",
                userId, query, artIds.size(), data.isLast());
        return data;
    }


    /**
     * 对查询执行混合检索：BM25 + 向量召回，并融合得分返回分页结果
     * 返回融合后的文章 ID 列表。
     */
    public List<DocScore> hybridSearch(Integer userId,
                                       float[] queryEmbedding,
                                       String query,
                                       boolean includeGlobal,
                                       int maxSize) {
        int safeSize = maxSize <= 0 ? properties.getFusionTopK() : maxSize;
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        int desired = Math.max(properties.getBm25TopK(), safeSize);
        if (Objects.isNull(query)) {
            return Collections.emptyList();
        }

        List<Integer> feedIds = includeGlobal ? Collections.emptyList() : userSubscriptionRepository.findActiveFeedIdsByUserId(userId);
        log.debug("Hybrid search(IDs) start: user={}, includeGlobal={}, maxSize={}, query='{}'", userId, includeGlobal, safeSize, normalizedQuery);
        if (!includeGlobal && CollectionUtils.isEmpty(feedIds)) {
            return Collections.emptyList();
        }
        List<DocScore> docScores = retrievalPipeline.execute(RetrievalContext.builder().query(normalizedQuery).embedding(queryEmbedding).threshold(properties.getSimilarityThreshold()).userId(userId).topK(desired).feedIds(feedIds).build());
        log.debug("Hybrid search(IDs) complete: user={}, query='{}', returnCount={}", userId, normalizedQuery, docScores.size());
        return docScores;
    }

}
