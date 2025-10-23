package org.bitmagic.ifeed.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.config.vectore.SearchRequestTurbo;
import org.bitmagic.ifeed.config.vectore.VectorStoreTurbo;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
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
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final VectorStoreTurbo vectorStoreTurbo;

    private final EmbeddingModel embeddingModel;

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;

    private final ArticleRepository articleRepository;

    private final Map<UUID, List<UUID>> user2Items = new ConcurrentHashMap<>();

    public List<Document> recall(UUID userId) {
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            List<String> articleIds = userBehaviorRepository.findById(userId.toString()).map(UserBehaviorDocument::getReadHistory).orElse(Collections.emptyList()).stream().map(UserBehaviorDocument.ArticleRef::getArticleId).toList();
            float[] embed = embeddingModel.embed(userEmbedding.getContent());
            float[] accumulator = minix(embed, userEmbedding.getEmbedding(), 0.8f, 0.2f);
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return vectorStoreTurbo.similaritySearch(SearchRequestTurbo.builder().embedding(accumulator).topK(100).filterExpression(b.nin("articleId", articleIds.toArray()).build()).build());
        }).orElse(Collections.emptyList());
    }


    public Page<ArticleSummaryView> rank(UUID userId, int page, int size) {
        List<UUID> aIds = user2Items.getOrDefault(userId, Collections.emptyList());
        if (page == 0) {
            aIds = recall(userId).stream().limit(100).map(Document::getId).map(UUID::fromString).toList();
            user2Items.put(userId, aIds);
        }
        return articleRepository.findArticleSummariesByIds(aIds, PageRequest.of(page, size));
    }

    private float[] minix(float[] embed1, float[] embed2, float weight1, float weight2) {
        float[] accumulator = new float[embed1.length];
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] = embed1[i] * weight1;
        }
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] += embed2[i] * weight2;
        }
        return accumulator;
    }
}
