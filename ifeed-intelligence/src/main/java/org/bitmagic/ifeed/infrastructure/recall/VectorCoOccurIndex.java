package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.CoOccurIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.EmbeddingStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private List<ScoredId> searchSimilarItems(Long seedId, float[] vector, int topK) {
        var request = SearchRequestTurbo.builder()
                .embedding(vector)
                .topK(topK)
                .similarityThreshold(properties.similarityThreshold())
                .build();
        return Objects.requireNonNull(vectorStore.similaritySearch(request)).stream().map(document -> new ScoredId(Long.parseLong(document.getId()), Optional.ofNullable(document.getScore()).orElse(0d), document.getMetadata())).filter(s->s.id()!=seedId).toList();
    }

}
