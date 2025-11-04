package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.AnnIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于向量检索的 ANN 查询实现，直接调用 PgVector 向量库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorAnnIndex implements AnnIndex {

    private final VectorStoreTurbo vectorStore;

    private final RecallVectorProperties properties;

    @Override
    public List<ScoredId> query(float[] vector, int k, Map<String, Object> filters) {
        if (vector == null || vector.length == 0 || k <= 0) {
            return List.of();
        }

        var requestBuilder = SearchRequestTurbo.builder()
                .embedding(vector)
                .topK(k)
                .similarityThreshold(properties.similarityThreshold());

        properties.buildFilter(filters).ifPresent(requestBuilder::filterExpression);

        List<Document> documents = vectorStore.similaritySearch(requestBuilder.build());
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        Map<Long, ScoredId> dedup = new LinkedHashMap<>();
        for (Document document : documents) {
            Long articleId = extractArticleId(document);
            if (articleId == null) {
                continue;
            }
            double score = document.getScore();
            Map<String, Object> meta = new LinkedHashMap<>();
            Object title = document.getMetadata().get("title");
            if (title != null) {
                meta.put("title", title);
            }
            ScoredId candidate = new ScoredId(articleId, score, meta.isEmpty() ? Map.of() : meta);
            dedup.merge(articleId, candidate, (left, right) -> left.score() >= right.score() ? left : right);
        }

        return new ArrayList<>(dedup.values());
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
