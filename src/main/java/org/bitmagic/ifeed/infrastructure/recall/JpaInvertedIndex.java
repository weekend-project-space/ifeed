package org.bitmagic.ifeed.infrastructure.recall;

import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.application.retrieval.DocScore;
import org.bitmagic.ifeed.application.retrieval.RetrievalContext;
import org.bitmagic.ifeed.application.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.application.retrieval.impl.Bm25RetrievalHandler;
import org.bitmagic.ifeed.application.retrieval.impl.MultiChannelRetrievalPipeline;
import org.bitmagic.ifeed.application.retrieval.impl.VectorRetrievalHandler;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 JPA 的倒排索引实现，支持按类目或作者获取最新文章集合。
 */
@Component
public class JpaInvertedIndex implements InvertedIndex {


    private static final String BM25_SQL = """
            WITH query AS (
                SELECT websearch_to_tsquery('simple', ?) AS q
            ),
            documents AS (
                SELECT a.id,
                       a.pub_date,
                       a.title,
                       setweight(to_tsvector('simple', coalesce(a.category, '')), 'A') ||
                       setweight(to_tsvector('simple', coalesce(a.tags, '')), 'B') ||
                       setweight(to_tsvector('simple', coalesce(a.author, '')), 'B')AS document
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
                   ts_rank_cd(d.document, query.q) AS score,
                   d.title,
                   d.pub_date AS pubDate
            FROM documents d
            CROSS JOIN query
            WHERE query.q @@ d.document
            ORDER BY score DESC
            LIMIT ?
            """;

    private final RetrievalPipeline retrievalPipeline;


    @Autowired
    public JpaInvertedIndex(VectorStoreTurbo vectorStore, JdbcTemplate jdbcTemplate, SearchRetrievalProperties properties) {
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda()).
                addHandler(new Bm25RetrievalHandler(jdbcTemplate, BM25_SQL), properties.getBm25Weight())
                .addHandler(new VectorRetrievalHandler(vectorStore, properties.getSimilarityThreshold()), properties.getVectorWeight());
    }

    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
        String attrs = attributes.stream().map(UserPreferenceService.AttributePreference::attributeValue).collect(Collectors.joining(" OR "));
        List<DocScore> scores = retrievalPipeline.execute(RetrievalContext.builder().includeGlobal(true).query(attrs).topK(k).build());
        return scores.stream().map(docScore -> new ScoredId(docScore.docId(), docScore.score(), Map.of())).toList();
    }

}
