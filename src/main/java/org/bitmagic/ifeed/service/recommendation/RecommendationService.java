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
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final VectorStoreTurbo vectorStoreTurbo;

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final Map<UUID, List<UUID>> user2Items = new ConcurrentHashMap<>();

    public Page<ArticleSummaryView> rank(UUID userId, int page, int size) {
        Long start = System.currentTimeMillis();
        List<UUID> aIds = user2Items.getOrDefault(userId, Collections.emptyList());
        if (page == 0) {
            aIds = recall(userId).stream().limit(100).map(Document::getId).map(UUID::fromString).toList();
            user2Items.put(userId, aIds);
            log.info("recall:{}ms", System.currentTimeMillis() - start);
        }
        return new PageImpl<>(articleRepository.findArticleSummariesByIds(aIds.subList(page * size, Math.min((page + 1) * size, aIds.size()))), PageRequest.of(page, size), aIds.size());
    }

    private List<Document> recall(UUID userId) {
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            List<String> articleIds = userBehaviorRepository.findById(userId.toString()).map(UserBehaviorDocument::getReadHistory).orElse(Collections.emptyList()).stream().map(UserBehaviorDocument.ArticleRef::getArticleId).toList();
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return vectorStoreTurbo.similaritySearch(SearchRequestTurbo.builder().embedding(userEmbedding.getEmbedding()).similarityThreshold(0.2).topK(100).filterExpression(b.nin("articleId", articleIds.toArray()).build()).build());
        }).orElse(Collections.emptyList());
    }

}
