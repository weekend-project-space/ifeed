package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.application.retrieval.DocScore;
import org.bitmagic.ifeed.application.retrieval.RetrievalContext;
import org.bitmagic.ifeed.application.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.application.retrieval.impl.Bm25RetrievalHandler;
import org.bitmagic.ifeed.application.retrieval.impl.MultiChannelRetrievalPipeline;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 JPA 的倒排索引实现，支持按类目或作者获取最新文章集合。
 */
@Component
//@RequiredArgsConstructor
public class JpaInvertedIndex implements InvertedIndex {


    private static final String BM25_SQL = """
            WITH query AS (
                SELECT websearch_to_tsquery('simple', ?) AS q
            ),
            documents AS (
                SELECT a.id,
                       a.pub_date,
                       a.title,
                       setweight(to_tsvector('simple', coalesce(a.feed_id::text, '')), 'A') ||
                       setweight(to_tsvector('simple', coalesce(a.category, '')), 'B') ||
                       setweight(to_tsvector('simple', coalesce(a.tags, '')), 'C')   AS document
                FROM articles a
                WHERE (? = TRUE) OR EXISTS (
                    SELECT 1
                    FROM user_subscriptions us
                    WHERE us.feed_id = a.feed_id
                      AND us.user_id = ?
                      AND us.is_active = TRUE
                )
            )
            SELECT d.id,
                  ts_rank_cd(d.document, query.q) * (1 - ln(1 + extract(epoch FROM (now() - d.pub_date)) / 86400)::numeric / 7) AS score,
                   d.title,
                   d.pub_date AS pubDate
            FROM documents d
            CROSS JOIN query
            WHERE query.q @@ d.document
            ORDER BY score DESC
            LIMIT ?
            """;

    private final RetrievalPipeline retrievalPipeline;

//    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public JpaInvertedIndex(VectorStoreTurbo vectorStore, JdbcTemplate jdbcTemplate, SearchRetrievalProperties properties) {
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda())
                .addHandler(new Bm25RetrievalHandler(jdbcTemplate, BM25_SQL), properties.getBm25Weight());
//        .addHandler(new VectorRetrievalHandler(vectorStore, properties.getSimilarityThreshold()), properties.getVectorWeight());
    }
    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
//         jdbcTemplate.query("select id from articles where feed_id in  (%s)".formatted(Strings.repeat("?,",attributes.size())))

//        String sql = "select id from articles where feed_id = ANY(?) limit ?";
//
//        Array arr = jdbcTemplate.execute((Connection conn) ->
//                conn.createArrayOf("BIGINT", attributes.stream().map(UserPreferenceService.AttributePreference::attributeValue).toArray())
//        );
//
//        return jdbcTemplate.queryForList(sql, Long.class, arr, k).stream().map(id -> new ScoredId(id, 1, Map.of())).toList();
        String attrs = attributes.stream().map(UserPreferenceService.AttributePreference::attributeValue).collect(Collectors.joining(" OR "));
        List<DocScore> scores = retrievalPipeline.execute(RetrievalContext.builder().includeGlobal(true).query(attrs).topK(k).build());
        return scores.stream().map(docScore -> new ScoredId(docScore.docId(), docScore.score(), Map.of())).toList();
    }

}
