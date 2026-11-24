package org.bitmagic.ifeed.infrastructure.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalHandler;
import org.bitmagic.ifeed.infrastructure.text.search.FilterExpression;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@Slf4j
@RequiredArgsConstructor
public class VectorRetrievalHandler implements RetrievalHandler {

    private final VectorStore vectorStore;

    private final UserSubscriptionRepository subscriptionRepository;

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

        // ------------------ feed 筛选 ------------------
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<FilterExpressionBuilder.Op> filters = new ArrayList<>();

        // ------------------ feed 筛选 ------------------
        if (!context.isIncludeGlobal()) {
            if (Objects.nonNull(context.getUserId())) {
                List<Integer> feedIds = subscriptionRepository.findActiveFeedIdsByUserId(context.getUserId());
                if (feedIds.isEmpty()) return Collections.emptyList();
                filters.add(b.in("feedId", feedIds.toArray(Integer[]::new)));
            } else if (context.getSourceFeeds() != null && !context.getSourceFeeds().isEmpty()) {
                filters.add(b.in("feedId", context.getSourceFeeds().toArray(Integer[]::new)));
            }
        }
        // includeGlobal = true → 全局，不加 feed 条件

        // ------------------ 日期范围 ------------------
        var dateRange = context.getDateRange();
        if (Objects.nonNull(dateRange)) {
            if (dateRange.from() != null) {
                filters.add(b.gte("publishedAt", dateRange.from().getEpochSecond()));
            }
            if (dateRange.to() != null) {
                filters.add(b.lte("publishedAt", dateRange.to().getEpochSecond()));
            }
        }
//        构建最终过滤器
        if (!filters.isEmpty()) {
            FilterExpressionBuilder.Op combined = filters.getFirst();
            for (int i = 1; i < filters.size(); i++) {
                combined = b.and(combined, filters.get(i));
            }
            builder.filterExpression(combined.build());
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
