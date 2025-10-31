package org.bitmagic.ifeed.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.vectore.SearchRequestTurbo;
import org.bitmagic.ifeed.config.vectore.VectorStoreTurbo;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
//@Service
@RequiredArgsConstructor
public class DefaultRecommendationService implements RecommendationService {

    private final VectorStoreTurbo vectorStoreTurbo;

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final Map<UUID, List<UUID>> user2Items = new ConcurrentHashMap<>();

    @Override
    public Page<ArticleSummaryView> recommend(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;

        List<UUID> cachedIds = user2Items.get(userId);
        if (safePage == 0 || cachedIds == null) {
            long start = System.currentTimeMillis();
            cachedIds = recall(userId).stream()
                    .limit(100)
                    .map(Document::getId)
                    .map(UUID::fromString)
                    .toList();
            user2Items.put(userId, cachedIds);
            log.info("recall:{}ms", System.currentTimeMillis() - start);
        }

        if (cachedIds.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), 0);
        }

        int fromIndex = safePage * safeSize;
        if (fromIndex >= cachedIds.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), cachedIds.size());
        }

        int toIndex = Math.min(fromIndex + safeSize, cachedIds.size());
        List<UUID> pageIds = cachedIds.subList(fromIndex, toIndex);

        Map<UUID, ArticleSummaryView> summariesById = articleRepository.findArticleSummariesByIds(pageIds).stream()
                .collect(Collectors.toMap(ArticleSummaryView::id, summary -> summary));

        List<ArticleSummaryView> ordered = pageIds.stream()
                .map(summariesById::get)
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(ordered, PageRequest.of(safePage, safeSize), cachedIds.size());
    }

    private List<Document> recall(UUID userId) {
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            List<String> articleIds = userBehaviorRepository.findById(userId.toString()).map(UserBehaviorDocument::getReadHistory).orElse(Collections.emptyList()).stream().map(UserBehaviorDocument.ArticleRef::getArticleId).toList();
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return vectorStoreTurbo.similaritySearch(SearchRequestTurbo.builder().embedding(userEmbedding.getEmbedding()).similarityThreshold(0.2).topK(100).filterExpression(b.nin("articleId", articleIds.toArray()).build()).build());
        }).orElse(Collections.emptyList());
    }

}
