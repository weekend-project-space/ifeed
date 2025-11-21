package org.bitmagic.ifeed.infrastructure.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalHandler;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
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

    @Override
    public boolean supports(RetrievalContext context) {
        return context.getQuery() != null && !context.getQuery().isEmpty();
    }

    @Override
    public List<DocScore> handle(RetrievalContext context) {
        log.debug("Executing Vector retrieval");
        var builder = SearchRequest.builder()
                .query(context.getQuery())
                .topK(context.getTopK())
                .similarityThreshold(context.getThreshold());

        if (!context.isIncludeGlobal() && !CollectionUtils.isEmpty(context.getFeedIds())) {
            builder.filterExpression(
                    new FilterExpressionBuilder()
                            .in("feedId", context.getFeedIds().toArray(Integer[]::new))
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
                        "vector",
                        doc
                ));
            } catch (RuntimeException ignored) {
            }
        }

        return scores;
    }
}
