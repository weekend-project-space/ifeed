package org.bitmagic.ifeed.application.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.bitmagic.ifeed.application.retrieval.DocScore;
import org.bitmagic.ifeed.application.retrieval.RetrievalContext;
import org.bitmagic.ifeed.application.retrieval.RetrievalHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@Slf4j
@RequiredArgsConstructor
public class VectorRetrievalHandler implements RetrievalHandler {

    private final VectorStoreTurbo vectorStore;
    private final double SimilarityThreshold;

    @Override
    public boolean supports(RetrievalContext context) {
        return context.getEmbedding() != null && context.getEmbedding().length > 0;
    }

    @Override
    public List<DocScore> handle(RetrievalContext context) {
        log.debug("Executing Vector retrieval");

        var builder = SearchRequestTurbo.builder()
                .embedding(context.getEmbedding())
                .topK(context.getTopK())
                .similarityThreshold(SimilarityThreshold);

        if (!context.isIncludeGlobal() && !CollectionUtils.isEmpty(context.getFeedIds())) {
            builder.filterExpression(
                    new FilterExpressionBuilder()
                            .in("feedId", (Object) context.getFeedIds().toArray(Integer[]::new))
                            .build());
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocScore> scores = new ArrayList<>();
        for (Document doc : documents) {
            Object idValue = doc.getId();
            try {
                Long articleId = Long.parseLong(idValue.toString());
                long publishedAtSec = 0L;
                Object tsVal = doc.getMetadata().get("publishedAt");
                if (tsVal != null) {
                    if (tsVal instanceof Number n) {
                        publishedAtSec = n.longValue();
                    } else {
                        try {
                            publishedAtSec = Long.parseLong(tsVal.toString());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                scores.add(new DocScore(
                        articleId,
                        doc.getScore(),
                        publishedAtSec > 0 ? Instant.ofEpochSecond(publishedAtSec) : null,
                        String.valueOf(doc.getMetadata().get("title"))
                ));
            } catch (RuntimeException ignored) {
            }
        }

        return scores;
    }
}
