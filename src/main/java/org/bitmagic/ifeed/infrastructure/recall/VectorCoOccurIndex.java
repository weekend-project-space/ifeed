package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.CoOccurIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.EmbeddingStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用向量相似度实现的物品共现索引。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorCoOccurIndex implements CoOccurIndex {

    private final EmbeddingStore embeddingStore;
    private final VectorStoreTurbo vectorStore;
    private final RecallVectorProperties properties;

    @Override
    public List<ScoredId> topRelated(Long itemId, int k) {
        if (itemId == null || k <= 0) {
            return List.of();
        }

        return embeddingStore.getItemVector(itemId)
                .map(vector -> searchSimilarItems(itemId, vector, k))
                .orElse(List.of());
    }

    private List<ScoredId> searchSimilarItems(Long seedId, float[] vector, int limit) {
        var request = SearchRequestTurbo.builder()
                .embedding(vector)
                .topK(limit + 1)
                .similarityThreshold(properties.similarityThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<Long, ScoredId> candidates = new LinkedHashMap<>();
        for (Document document : documents) {
            Long articleId = extractArticleId(document);
            if (articleId == null || articleId.equals(seedId)) {
                continue;
            }
            double score = document.getScore();
            candidates.merge(articleId, new ScoredId(articleId, score, Map.of()),
                    (left, right) -> left.score() >= right.score() ? left : right);
            if (candidates.size() >= limit) {
                break;
            }
        }
        return List.copyOf(candidates.values());
    }

    private Long extractArticleId(Document document) {
        Object value = document.getMetadata().get("articleId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
