package org.bitmagic.ifeed.infrastructure.text.search.pg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitmagic.ifeed.infrastructure.TermUtils;
import org.bitmagic.ifeed.infrastructure.text.search.Document;
import org.bitmagic.ifeed.infrastructure.text.search.ScoredDocument;
import org.bitmagic.ifeed.infrastructure.text.search.SearchRequest;
import org.bitmagic.ifeed.infrastructure.text.search.TextSearchStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PostgreSQL TSVector 实现的 TextSearchStore
 */
public class PgTextSearchStore implements TextSearchStore {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String textSearchConfig;
    private final ObjectMapper objectMapper;

    public PgTextSearchStore(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String textSearchConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.tableName = tableName;
        this.textSearchConfig = textSearchConfig != null ? textSearchConfig : "simple";
        this.objectMapper = new ObjectMapper();
        initializeTable();
    }

    /**
     * 初始化数据库表和索引
     */
    private void initializeTable() {
        // 创建表
        jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGINT PRIMARY KEY,
                    content TEXT NOT NULL,
                    title VARCHAR(500),
                    category VARCHAR(100),
                    feed_id INT,
                    feed_title VARCHAR(200),
                    tags VARCHAR(200),
                    summary VARCHAR(500),
                    metadata JSONB,
                    tsv TSVECTOR,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """, tableName));

        // 创建索引
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_tsv_idx ON %s USING GIN(tsv)",
                tableName, tableName));
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_metadata_idx ON %s USING GIN(metadata)",
                tableName, tableName));
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_category_idx ON %s(category)",
                tableName, tableName));
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_feed_id_idx ON %s(feed_id)",
                tableName, tableName));

        createTrigger();
    }

    /**
     * 创建触发器自动更新 TSVector
     */
    private void createTrigger() {
        jdbcTemplate.execute(String.format("""
                        CREATE OR REPLACE FUNCTION %s_tsv_trigger() RETURNS trigger AS $$
                        BEGIN
                            NEW.tsv := 
                                setweight(to_tsvector('%s', COALESCE(NEW.title, '')), 'A') ||
                                setweight(to_tsvector('%s', COALESCE(NEW.category, '')), 'A') ||
                                setweight(to_tsvector('%s', COALESCE(NEW.feed_title, '')), 'A') ||
                                setweight(to_tsvector('%s', COALESCE(NEW.tags, '')), 'B') ||
                                setweight(to_tsvector('%s', COALESCE(NEW.summary, '')), 'C') ||
                                setweight(to_tsvector('%s', COALESCE(NEW.content, '')), 'C');
                            NEW.updated_at := CURRENT_TIMESTAMP;
                            RETURN NEW;
                        END
                        $$ LANGUAGE plpgsql;
                        
                        DROP TRIGGER IF EXISTS %s_tsv_update ON %s;
                        CREATE TRIGGER %s_tsv_update 
                        BEFORE INSERT OR UPDATE ON %s
                        FOR EACH ROW EXECUTE FUNCTION %s_tsv_trigger();
                        """, tableName, textSearchConfig, textSearchConfig, textSearchConfig,
                textSearchConfig, textSearchConfig, textSearchConfig,
                tableName, tableName, tableName, tableName, tableName));
    }

    @Override
    @Transactional
    public void add(List<Document> documents) {
        String sql = String.format("""
                INSERT INTO %s (id, content, title, category, feed_id, feed_title, tags, summary, metadata)
                VALUES (:id, :content, :title, :category, :feedId, :feedTitle, :tags, :summary, :metadata::jsonb)
                ON CONFLICT (id) DO UPDATE SET
                    content = EXCLUDED.content,
                    title = EXCLUDED.title,
                    category = EXCLUDED.category,
                    feed_id = EXCLUDED.feed_id,
                    feed_title = EXCLUDED.feed_title,
                    tags = EXCLUDED.tags,
                    summary = EXCLUDED.summary,
                    metadata = EXCLUDED.metadata
                """, tableName);

        MapSqlParameterSource[] batchParams = documents.stream()
                .map(doc -> {
                    try {
                        return new MapSqlParameterSource()
                                .addValue("id", doc.id())
                                .addValue("content", TermUtils.segmentStr(doc.content()))
                                .addValue("title", TermUtils.segmentStr(truncate(getMetadataString(doc, "title"), 500)))
                                .addValue("category", TermUtils.segmentStr(truncate(getMetadataString(doc, "category"), 100)))
                                .addValue("feedId", doc.feedId())
                                .addValue("feedTitle", TermUtils.segmentStr(truncate(getMetadataString(doc, "feedTitle"), 200)))
                                .addValue("tags", TermUtils.segmentStr(truncate(getMetadataString(doc, "tags"), 200)))
                                .addValue("summary", TermUtils.segmentStr(truncate(getMetadataString(doc, "summary"), 300)))
                                .addValue("metadata", toJson(doc.metadata()));
                    } catch (Exception e) {
                        throw new RuntimeException("Error processing document id: " + doc.id(), e);
                    }
                })
                .toArray(MapSqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(sql, batchParams);
    }


    @Override
    @Transactional
    public Optional<Boolean> delete(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Optional.of(false);
        }

        String sql = String.format("DELETE FROM %s WHERE id IN (:ids)", tableName);
        int deleted = namedJdbcTemplate.update(sql,
                new MapSqlParameterSource("ids", idList));
        return Optional.of(deleted > 0);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return similaritySearchWithScore(request).stream()
                .map(ScoredDocument::document)
                .toList();
    }

    @Override
    public List<ScoredDocument> similaritySearchWithScore(SearchRequest request) {
        String sql = String.format("""
                WITH query AS (
                    SELECT websearch_to_tsquery('%s', :query) AS q
                )
                SELECT 
                    d.id, d.content, d.feed_id, d.metadata,
                    ts_rank_cd(d.tsv, query.q, 32) AS score
                FROM %s d
                CROSS JOIN query
                WHERE query.q @@ d.tsv
                """, textSearchConfig, tableName);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", TermUtils.segmentStr(request.query()))
                .addValue("topK", request.topK());

        if (request.filterExpression() != null) {
            sql += " AND " + request.filterExpression().toFilterString();
            request.filterExpression().addParameters(params);
        }

        sql += " ORDER BY score DESC LIMIT :topK";

        return namedJdbcTemplate.query(sql, params, this::mapScoredDocument).stream()
                .filter(doc -> doc.score() >= request.similarityThreshold())
                .toList();
    }

    /**
     * 带用户订阅过滤的搜索
     */
    public List<ScoredDocument> searchWithFilter(
            String query, int topK, Integer userId, boolean includeGlobal, double threshold) {

        String sql = String.format("""
                WITH query AS (
                    SELECT websearch_to_tsquery('%s', :query) AS q
                )
                SELECT
                    d.id, d.content, d.feed_id,  d.metadata,
                    ts_rank_cd(d.tsv, query.q, 33) AS score
                FROM %s d
                CROSS JOIN query
                WHERE query.q @@ d.tsv
                  AND ts_rank_cd(d.tsv, query.q, 33) > :scoreThreshold
                  AND (:includeGlobal = TRUE
                    OR EXISTS (
                        SELECT 1 FROM user_subscriptions us
                        WHERE us.feed_id = d.feed_id AND us.user_id = :userId AND us.is_active = TRUE
                    ))
                ORDER BY score DESC
                LIMIT :topK
                """, textSearchConfig, tableName);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", TermUtils.segmentStr(query))
                .addValue("topK", topK)
                .addValue("userId", userId)
                .addValue("includeGlobal", includeGlobal)
                .addValue("scoreThreshold", threshold);

        return namedJdbcTemplate.query(sql, params, this::mapScoredDocument);
    }
//
//    /**
//     * 带高亮的搜索
//     */
//    public List<HighlightedResult> searchWithHighlight(String query, int topK) {
//        String sql = String.format("""
//                WITH query AS (
//                    SELECT websearch_to_tsquery('%s', :query) AS q
//                )
//                SELECT
//                    d.id, d.content, d.title, d.category, d.feed_title, d.tags, d.summary, d.metadata,
//                    ts_rank_cd(d.tsv, query.q, 32) AS score,
//                    ts_headline('%s', d.content, query.q, 'MaxWords=50,MinWords=15') AS headline
//                FROM %s d
//                CROSS JOIN query
//                WHERE query.q @@ d.tsv
//                ORDER BY score DESC
//                LIMIT :topK
//                """, textSearchConfig, textSearchConfig, tableName);
//
//        return namedJdbcTemplate.query(sql,
//                new MapSqlParameterSource("query", query).addValue("topK", topK),
//                this::mapHighlightedResult);
//    }

    /**
     * 手动更新 TSVector
     */
    @Transactional
    public void updateTSVector(Long id, Map<String, String> fields) {
        String sql = String.format("""
                        UPDATE %s SET tsv =
                            setweight(to_tsvector('%s', COALESCE(:title, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(:category, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(:feedTitle, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(:tags, '')), 'B') ||
                            setweight(to_tsvector('%s', COALESCE(:summary, '')), 'C') ||
                            setweight(to_tsvector('%s', COALESCE(:content, '')), 'C')
                        WHERE id = :id
                        """, tableName, textSearchConfig, textSearchConfig, textSearchConfig,
                textSearchConfig, textSearchConfig, textSearchConfig);

        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        fields.forEach(params::addValue);
        namedJdbcTemplate.update(sql, params);
    }

    /**
     * 重建所有 TSVector
     */
    @Transactional
    public int rebuildAllTSVectors() {
        return jdbcTemplate.update(String.format("""
                        UPDATE %s SET tsv =
                            setweight(to_tsvector('%s', COALESCE(title, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(category, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(feed_title, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(tags, '')), 'B') ||
                            setweight(to_tsvector('%s', COALESCE(summary, '')), 'C') ||
                            setweight(to_tsvector('%s', COALESCE(content, '')), 'C')
                        """, tableName, textSearchConfig, textSearchConfig, textSearchConfig,
                textSearchConfig, textSearchConfig, textSearchConfig));
    }

    /**
     * 获取统计信息
     */
    public SearchStats getStats() {
        String sql = String.format("""
                SELECT 
                    COUNT(*) as total,
                    AVG(LENGTH(content)) as avg_length,
                    SUM(pg_column_size(tsv)) as index_size,
                    MAX(updated_at) as last_updated
                FROM %s
                """, tableName);

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new SearchStats(
                        rs.getLong("total"),
                        rs.getDouble("avg_length"),
                        rs.getLong("index_size"),
                        rs.getTimestamp("last_updated").toLocalDateTime()
                )
        );
    }

    private String getMetadataString(Document doc, String key) {
        if (doc.metadata() == null || !doc.metadata().containsKey(key)) {
            return "";
        }
        Object value = doc.metadata().get(key);
        if (value == null) {
            return "";
        }
        return TermUtils.segmentStr(value.toString());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    // ============ RowMapper 方法 ============

    private ScoredDocument mapScoredDocument(ResultSet rs, int rowNum) throws SQLException {
        return new ScoredDocument(mapDocument(rs), rs.getDouble("score"));
    }

//    private HighlightedResult mapHighlightedResult(ResultSet rs, int rowNum) throws SQLException {
//        return new HighlightedResult(
//                mapDocument(rs),
//                rs.getDouble("score"),
//                rs.getString("headline")
//        );
//    }

    private Document mapDocument(ResultSet rs) throws SQLException {
        Map<String, Object> metadata = fromJson(rs.getString("metadata"));
        Map<String, Object> enriched = new HashMap<>(metadata);
        return new Document(rs.getLong("id"), rs.getInt("feed_id"), rs.getString("content"), enriched);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}