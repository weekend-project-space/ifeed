package org.bitmagic.ifeed.service.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.vectore.SearchRequestTurbo;
import org.bitmagic.ifeed.config.vectore.VectorStoreTurbo;
import org.bitmagic.ifeed.service.retrieval.DocScore;
import org.bitmagic.ifeed.service.retrieval.RetrievalContext;
import org.bitmagic.ifeed.service.retrieval.RetrievalHandler;
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
                            .in("feedId", context.getFeedIds().toArray(Integer[]::new))
                            .build());
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocScore> scores = new ArrayList<>();
        for (Document doc : documents) {
            Object idValue = doc.getMetadata().get("articleId");
            if (idValue == null) continue;
            try {
                scores.add(new DocScore(
                        Long.valueOf((String) idValue),
                        doc.getScore(),
                        Instant.ofEpochSecond(Long.parseLong(doc.getMetadata().get("publishedAt").toString())),
                        doc.getMetadata().get("title").toString()
                ));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return scores;
    }
}
