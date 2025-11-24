package org.bitmagic.ifeed.infrastructure.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalHandler;
import org.bitmagic.ifeed.infrastructure.text.search.pg.PgTextSearchStore;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 先用tf-idf
 *
 * @author yangrd
 * @date 2025/11/3
 **/
@RequiredArgsConstructor
@Slf4j
public class TextSearchRetrievalHandler implements RetrievalHandler {


    private final PgTextSearchStore pgTextSearchStore;

    @Override
    public boolean supports(RetrievalContext context) {
        return context.getQuery() != null && !context.getQuery().trim().isEmpty();
    }

    @Override
    public List<DocScore> handle(RetrievalContext context) {
        long startTime = System.currentTimeMillis();

        validateContext(context);

        log.debug("Executing Chinese BM25 retrieval: query='{}', userId={}, topK={}",
                context.getQuery(), context.getUserId(), context.getTopK());

        try {
            List<DocScore> results = Collections.emptyList();
            if (Strings.isNotBlank(context.getQuery())) {
                results = pgTextSearchStore.searchWithFilter(buildChineseQuery(context.getQuery()), context.getTopK(), context.getUserId(), context.isIncludeGlobal()).stream().map(doc -> {
                    Map<String, Object> metadata = doc.document().metadata();
                    return new DocScore(doc.document().id(), doc.score(), Instant.ofEpochSecond((Integer) metadata.get("pubDate")), "bm25_chinese", metadata);
                }).toList();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Chinese BM25 retrieval completed: {} results in {}ms",
                    results.size(), duration);

            return results;

        } catch (Exception e) {
            log.error("Chinese BM25 retrieval failed for query: {}", context.getQuery(), e);
            throw new RuntimeException("Chinese full-text search failed", e);
        }
    }

    /**
     * 构建中文查询，支持多词组合
     */
    private String buildChineseQuery(String query) {
        // 清理输入
        String cleaned = query.trim()
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .substring(0, Math.min(query.length(), 500));

        // 将空格替换为 & (AND 操作符)，支持多词搜索
        // 例如: "人工智能 机器学习" -> "人工智能 & 机器学习"
        return cleaned.replaceAll("\\s+", " & ");
    }

    private void validateContext(RetrievalContext context) {
        if (context.getTopK() <= 0 || context.getTopK() > 1000) {
            throw new IllegalArgumentException("topK must be between 1 and 1000");
        }
        if (!context.isIncludeGlobal() && context.getUserId() == null) {
            throw new IllegalArgumentException("userId is required when includeGlobal is false");
        }
    }
}
