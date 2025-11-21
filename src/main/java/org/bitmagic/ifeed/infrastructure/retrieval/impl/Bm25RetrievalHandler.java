package org.bitmagic.ifeed.infrastructure.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.TermUtils;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
public class Bm25RetrievalHandler implements RetrievalHandler {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    // 使用中文分词配置
    private static final String CHINESE_SEARCH_SQL = """
            WITH query AS (
                SELECT websearch_to_tsquery('simple', :query) AS q
            )
            SELECT a.id,
                   ts_rank_cd(a.tsv, query.q, 33) AS score,
                   a.title,
                   a.summary,
                   a.pub_date AS pubDate
            FROM articles a
            CROSS JOIN query
            WHERE query.q @@ a.tsv
              AND (
                :includeGlobal = TRUE 
                OR EXISTS (
                    SELECT 1
                    FROM user_subscriptions us
                    WHERE us.feed_id = a.feed_id
                      AND us.user_id = :userId
                      AND us.is_active = TRUE
                )
              )
            ORDER BY score DESC
            LIMIT :topK
            """;

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
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("query", TermUtils.segmentStr(buildChineseQuery(context.getQuery())))
                    .addValue("includeGlobal", context.isIncludeGlobal())
                    .addValue("userId", context.getUserId())
                    .addValue("topK", context.getTopK());

            List<DocScore> results = namedParameterJdbcTemplate.query(
                    CHINESE_SEARCH_SQL,
                    params,
                    (rs, rowNum) -> new DocScore(
                            rs.getLong("id"),
                            rs.getDouble("score"),
                            rs.getTimestamp("pubDate").toInstant(),
                            "bm25_chinese",
                            Map.of(
                                    "title", rs.getString("title"),
                                    "summary", rs.getString("summary"),
                                    "pubDate", rs.getTimestamp("pubDate").getTime()
                            )
                    )
            );

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
