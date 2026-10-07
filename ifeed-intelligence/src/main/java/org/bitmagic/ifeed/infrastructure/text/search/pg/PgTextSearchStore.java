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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

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
        this.tableName = validateIdentifier(tableName, "table name");
        this.textSearchConfig = validateIdentifier(
                textSearchConfig != null ? textSearchConfig : "simple", "text search config");
        this.objectMapper = new ObjectMapper();
        initializeTable();
    }

    private static String validateIdentifier(String identifier, String type) {
        if (identifier == null || !identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid " + type + ": " + identifier);
        }
        return identifier;
    }

    /**
     * 初始化数据库表和索引
     */
    private void initializeTable() {
        // 创建表
        jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGINT PRIMARY KEY,
                    pub_date TIMESTAMP WITH TIME ZONE NOT NULL,
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
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_pub_date_idx ON %s(pub_date DESC)",
                tableName, tableName));
        createTrigger();
        createFilteredTsvIndex();
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

    /**
     * 为 feed_title, category, tags 三个字段创建组合 GIN 索引
     */
    private void createFilteredTsvIndex() {
        // 添加一个生成列用于存储这三个字段的 tsvector
        jdbcTemplate.execute(String.format("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = '%s' AND column_name = 'catalog_tsv'
                    ) THEN
                        ALTER TABLE %s ADD COLUMN catalog_tsv TSVECTOR
                        GENERATED ALWAYS AS (
                            setweight(to_tsvector('%s', COALESCE(title, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(feed_title, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(category, '')), 'A') ||
                            setweight(to_tsvector('%s', COALESCE(tags, '')), 'B')
                        ) STORED;
                    END IF;
                END $$;
                """, tableName, tableName, textSearchConfig, textSearchConfig, textSearchConfig, textSearchConfig));

        // 为这个生成列创建 GIN 索引
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_catalog_tsv_idx ON %s USING GIN(catalog_tsv)",
                tableName, tableName));
    }

    @Override
    @Transactional
    public void add(List<Document> documents) {
        String sql = String.format(
                """
                        INSERT INTO %s (id, pub_date, content, title, category, feed_id, feed_title, tags, summary, metadata)
                        VALUES (:id, :pubDate, :content, :title, :category, :feedId, :feedTitle, :tags, :summary, :metadata::jsonb)
                        ON CONFLICT (id) DO UPDATE SET
                            content = EXCLUDED.content,
                            pub_date = EXCLUDED.pub_date,
                            title = EXCLUDED.title,
                            category = EXCLUDED.category,
                            feed_id = EXCLUDED.feed_id,
                            feed_title = EXCLUDED.feed_title,
                            tags = EXCLUDED.tags,
                            summary = EXCLUDED.summary,
                            metadata = EXCLUDED.metadata
                        """,
                tableName);

        MapSqlParameterSource[] batchParams = documents.stream()
                .map(doc -> {
                    try {
                        return new MapSqlParameterSource()
                                .addValue("id", doc.id())
                                .addValue("pubDate", Timestamp.from(getMetadataInstant(doc, "pubDate")))
                                .addValue("content", TermUtils.segmentStr(doc.content()))
                                .addValue("title", truncate(getMetadataSegmentStr(doc, "title"), 500))
                                .addValue("category", truncate(getMetadataSegmentStr(doc, "category"), 100))
                                .addValue("feedId", doc.feedId())
                                .addValue("feedTitle", truncate(getMetadataSegmentStr(doc, "feedTitle"), 200))
                                .addValue("tags", truncate(getMetadataStr(doc, "tags"), 200))
                                .addValue("summary", truncate(getMetadataSegmentStr(doc, "summary"), 300))
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
            String filterSql = request.filterExpression().toFilterString();
            if (filterSql.contains("'") && !filterSql.contains(":")) {
                throw new IllegalArgumentException("Filter must use named parameters");
            }
            sql += " AND " + filterSql;
            request.filterExpression().addParameters(params);
        }

        sql += " ORDER BY score DESC LIMIT :topK";

        return namedJdbcTemplate.query(sql, params, this::mapScoredDocument).stream()
                .filter(doc -> doc.score() >= request.similarityThreshold())
                .toList();
    }

    /**
     * 带用户订阅过滤的搜索 - 仅搜索 feedTitle, category, tags 字段
     * 使用预先构建的 catalog_tsv 索引提升性能
     * 集成时间衰减因子,优先返回较新的文档
     */
    public List<ScoredDocument> searchWithFilter(
            String query, int topK, boolean includeGlobal, Integer userId, Collection<Integer> feedIds, Instant start, Instant end, double threshold) {

//        String sql = String.format("""
//                WITH query AS (
//                    SELECT websearch_to_tsquery('%s', :query) AS q
//                ),
//                scored_articles AS (
//                    SELECT
//                        d.id,
//                        d.content,
//                        d.feed_id,
//                        d.metadata,
//                        d.pub_date,
//                        ts_rank_cd(d.catalog_tsv, query.q, 33) AS text_score,
//                        EXTRACT(EPOCH FROM (NOW() - d.pub_date)) / 86400.0 AS days_ago
//                    FROM %s d
//                    CROSS JOIN query
//                    WHERE query.q @@ d.catalog_tsv
//                      AND d.pub_date > NOW() - INTERVAL '90 days'
//                      AND d.pub_date IS NOT NULL
//                      AND (:includeGlobal = TRUE
//                        OR EXISTS (
//                            SELECT 1 FROM user_subscriptions us
//                            WHERE us.feed_id = d.feed_id AND us.user_id = :userId AND us.is_active = TRUE
//                        ))
//                )
//                SELECT
//                    id,
//                    content,
//                    feed_id,
//                    metadata,
//                    text_score * EXP(-0.02 * days_ago) AS score
//                FROM scored_articles
//                WHERE text_score > :scoreThreshold
//                ORDER BY score DESC
//                LIMIT :topK
//                """, textSearchConfig, tableName);
        String sql = String.format("""
                WITH q AS (
                    SELECT websearch_to_tsquery('simple', :query) AS q
                ),
                candidates AS (
                    SELECT
                        d.id,
                        d.content,
                        d.feed_id,
                        d.metadata,
                        d.pub_date,
                        ts_rank_cd(
                            d.catalog_tsv,
                            q.q,
                            33
                        ) AS text_score
                    FROM article_tsv_store d
                    CROSS JOIN q
                    WHERE
                        q.q @@ d.catalog_tsv
                        AND d.pub_date >= NOW() - INTERVAL '90 days'
                        AND (:start::timestamp IS NULL OR d.pub_date >= :start)
                        AND (:end::timestamp IS NULL OR d.pub_date <= :end)
                        AND (
                             :includeGlobal = TRUE
                             OR (:hasFeedIds = TRUE AND d.feed_id = ANY(:feedIds))
                            OR EXISTS (
                                SELECT 1
                                FROM user_subscriptions us
                                WHERE us.feed_id = d.feed_id
                                  AND us.user_id = :userId
                                  AND us.is_active = TRUE
                            )
                        )
                    ORDER BY text_score DESC
                    LIMIT 500 
                )
                SELECT
                    id,
                    content,
                    feed_id,
                    metadata,
                    text_score * EXP(
                        -0.02 * EXTRACT(EPOCH FROM (NOW() - pub_date)) / 86400.0
                    ) AS score
                FROM candidates
                WHERE text_score >= :scoreThreshold
                ORDER BY score DESC
                LIMIT :topK;
                """, textSearchConfig, tableName);
        boolean hasFeedIds = feedIds != null && !feedIds.isEmpty();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", TermUtils.segmentStr(query))
                .addValue("topK", topK)
                .addValue("userId", userId)
                .addValue("includeGlobal", includeGlobal)
                .addValue("hasFeedIds", hasFeedIds)
                .addValue("feedIds", feedIds)
                .addValue("start", Objects.nonNull(start)?Timestamp.from(start):null)
                .addValue("end", Objects.nonNull(end)?Timestamp.from(end):null)
                .addValue("scoreThreshold", threshold);

        return namedJdbcTemplate.query(sql, params, this::mapScoredDocument);
    }


    /**
     * 使用权重分桶（A/B/C/D 四级）进行多关键词搜索
     * 直接传入四个桶的关键词列表，高权重词放 A 桶，低权重词放 D 桶
     * <p>
     * 权重含义（PostgreSQL 内置）：
     * A: 最高权重 (1.0)
     * B: 高权重     (0.4)
     * C: 中权重     (0.2)
     * D: 低权重     (0.1)
     * <p>
     * 你可以通过自定义 weights 数组进一步微调（默认已优化为常见场景）
     */
//    public List<ScoredDocument> searchWithWeightedBuckets(
//            List<String> bucketA,   // 最高优先级关键词，例如核心主题词
//            List<String> bucketB,   // 高优先级
//            List<String> bucketC,   // 中优先级
//            List<String> bucketD,   // 低优先级（扩展词、相关词）
//            int topK,
//            Integer userId,
//            boolean includeGlobal,
//            double threshold) {
//
//        // 合并所有非空桶
//        boolean hasAny = false;
//        StringBuilder tsQueryBuilder = new StringBuilder();
//
//        // 处理 A 桶
//        appendTerms(tsQueryBuilder, bucketA, 'A', hasAny);
//        hasAny = hasAny || (bucketA != null && !bucketA.isEmpty());
//
//        // 处理 B 桶
//        appendTerms(tsQueryBuilder, bucketB, 'B', hasAny);
//        hasAny = hasAny || (bucketB != null && !bucketB.isEmpty());
//
//        // 处理 C 桶
//        appendTerms(tsQueryBuilder, bucketC, 'C', hasAny);
//        hasAny = hasAny || (bucketC != null && !bucketC.isEmpty());
//
//        // 处理 D 桶
//        appendTerms(tsQueryBuilder, bucketD, 'D', hasAny);
//        hasAny = hasAny || (bucketD != null && !bucketD.isEmpty());
//
//        if (!hasAny) {
//            return List.of();
//        }
//
//        String tsQueryStr = tsQueryBuilder.toString();
//
//        // 可自定义权重数组，进一步拉开差距（推荐值，经过实际测试效果更好）
//        // 顺序必须是 [D, C, B, A]
//        float[] customWeights = new float[]{0.15f, 0.30f, 0.60f, 1.0f};
//
//        float wD = customWeights[0];
//        float wC = customWeights[1];
//        float wB = customWeights[2];
//        float wA = customWeights[3];
//
//        String sql = """
//                WITH q AS (
//                    SELECT
//                        websearch_to_tsquery('simple', :tsQuery) AS tsq,
//                        ARRAY[%f, %f, %f, %f]::float4[] AS weights
//                ),
//                candidates AS (
//                    SELECT
//                        d.id,
//                        d.content,
//                        d.feed_id,
//                        d.metadata,
//                        d.pub_date,
//                        ts_rank_cd(d.catalog_tsv, q.tsq, 33, q.weights) AS text_score
//                    FROM article_tsv_store d
//                    CROSS JOIN q
//                    WHERE q.tsq @@ d.catalog_tsv
//                      AND d.pub_date >= NOW() - INTERVAL '90 days'
//                      AND (
//                          :includeGlobal = TRUE
//                          OR EXISTS (
//                              SELECT 1 FROM user_subscriptions us
//                              WHERE us.feed_id = d.feed_id
//                                AND us.user_id = :userId
//                                AND us.is_active = TRUE
//                          )
//                      )
//                    ORDER BY text_score DESC
//                    LIMIT 500
//                )
//                SELECT
//                    id,
//                    content,
//                    feed_id,
//                    metadata,
//                    text_score * EXP(-0.02 * EXTRACT(EPOCH FROM (NOW() - pub_date)) / 86400.0) AS score
//                FROM candidates
//                WHERE text_score >= :scoreThreshold
//                ORDER BY score DESC
//                LIMIT :topK;
//                """.formatted(wD, wC, wB, wA);
//
//        MapSqlParameterSource params = new MapSqlParameterSource()
//                .addValue("tsQuery", tsQueryStr)
//                .addValue("topK", topK)
//                .addValue("userId", userId)
//                .addValue("includeGlobal", includeGlobal)
//                .addValue("scoreThreshold", threshold);
//
//        return namedJdbcTemplate.query(sql, params, this::mapScoredDocument);
//    }
//
//    // 辅助方法：追加词到 tsquery，自动处理分词和 OR 连接
//    private boolean appendTerms(StringBuilder builder, List<String> terms, char label, boolean hasPrevious) {
//        if (terms == null || terms.isEmpty()) {
//            return hasPrevious;
//        }
//
//        // 常见停用词 + 标点过滤（可根据你的分词器结果扩展）
//        Set<String> stopWords = Set.of(
//                "的", "了", "是", "在", "有", "和", "与", "或", "于", "等", "被", "为", "对", "从", "到",
//                "这", "那", "你", "我", "他", "她", "它", "们", "之", "其", "年", "月", "日", "时",
//                 "很多", "都", "很", "也", "就", "而", "且", "但", "还", "并",
//                "-", "–", "--", "—", ":", "|", "/", "\\", "(", ")", "[", "]", "{", "}", "、", "，", "。", "！", "？", "“", "”"
//        );
//
//        for (String rawTerm : terms) {
//            String term = rawTerm.trim();
//            if (term.isEmpty()) continue;
//
//            String segmented = TermUtils.segmentStr(term);
//            if (segmented.isBlank()) continue;
//
//            // 分词后按空格拆分，过滤停用词和无效词
//            String[] words = segmented.split("\\s+");
//            List<String> validWords = new ArrayList<>();
//            for (String word : words) {
//                word = word.trim();
//                if (word.isEmpty()) continue;
//                if (stopWords.contains(word)) continue;
//                validWords.add(word);
//            }
//
//            if (validWords.isEmpty()) continue;
//
//            if (hasPrevious) {
//                builder.append(" | ");
//            }
//
//            // 如果多个有效词，用 & 连接；单个词直接输出
//            if (validWords.size() > 1) {
//                builder.append("(");
//                for (int i = 0; i < validWords.size(); i++) {
//                    if (i > 0) builder.append(" & ");
//                    builder.append(validWords.get(i));
//                }
//                builder.append(")");
//            } else {
//                builder.append(validWords.get(0));
//            }
//
//            builder.append(":").append(label);
//
//            hasPrevious = true;
//        }
//
//        return hasPrevious;
//
//    }

    // /**
    // * 带用户订阅过滤的搜索 - 仅搜索 feedTitle, category, tags 字段
    // * 使用预先构建的 catalog_tsv 索引提升性能
    // */
    // public List<ScoredDocument> searchWithFilter(
    // String query, int topK, Integer userId, boolean includeGlobal, double
    // threshold) {
    //
    // String sql = String.format("""
    // WITH query AS (
    // SELECT websearch_to_tsquery('%s', :query) AS q
    // )
    // SELECT
    // d.id,
    // d.content,
    // d.feed_id,
    // d.metadata,
    // ts_rank_cd(d.catalog_tsv, query.q, 33) AS score
    // FROM %s d
    // CROSS JOIN query
    // WHERE query.q @@ d.catalog_tsv
    // AND ts_rank_cd(d.catalog_tsv, query.q, 33) > :scoreThreshold
    // AND (:includeGlobal = TRUE
    // OR EXISTS (
    // SELECT 1 FROM user_subscriptions us
    // WHERE us.feed_id = d.feed_id AND us.user_id = :userId AND us.is_active = TRUE
    // ))
    // ORDER BY score DESC
    // LIMIT :topK
    // """, textSearchConfig, tableName);
    //
    // MapSqlParameterSource params = new MapSqlParameterSource()
    // .addValue("query", TermUtils.segmentStr(query))
    // .addValue("topK", topK)
    // .addValue("userId", userId)
    // .addValue("includeGlobal", includeGlobal)
    // .addValue("scoreThreshold", threshold);
    //
    // return namedJdbcTemplate.query(sql, params, this::mapScoredDocument);
    // }

    // /**
    // * 带用户订阅过滤的搜索
    // */
    // public List<ScoredDocument> searchWithFilter(
    // String query, int topK, Integer userId, boolean includeGlobal, double
    // threshold) {
    //
    // String sql = String.format("""
    // WITH query AS (
    // SELECT websearch_to_tsquery('%s', :query) AS q
    // )
    // SELECT
    // d.id, d.content, d.feed_id, d.metadata,
    // ts_rank_cd(d.tsv, query.q, 33) AS score
    // FROM %s d
    // CROSS JOIN query
    // WHERE query.q @@ d.tsv
    // AND ts_rank_cd(d.tsv, query.q, 33) > :scoreThreshold
    // AND (:includeGlobal = TRUE
    // OR EXISTS (
    // SELECT 1 FROM user_subscriptions us
    // WHERE us.feed_id = d.feed_id AND us.user_id = :userId AND us.is_active = TRUE
    // ))
    // ORDER BY score DESC
    // LIMIT :topK
    // """, textSearchConfig, tableName);
    //
    // MapSqlParameterSource params = new MapSqlParameterSource()
    // .addValue("query", TermUtils.segmentStr(query))
    // .addValue("topK", topK)
    // .addValue("userId", userId)
    // .addValue("includeGlobal", includeGlobal)
    // .addValue("scoreThreshold", threshold);
    //
    // return namedJdbcTemplate.query(sql, params, this::mapScoredDocument);
    // }

    //
    // /**
    // * 带高亮的搜索
    // */
    // public List<HighlightedResult> searchWithHighlight(String query, int topK) {
    // String sql = String.format("""
    // WITH query AS (
    // SELECT websearch_to_tsquery('%s', :query) AS q
    // )
    // SELECT
    // d.id, d.content, d.title, d.category, d.feed_title, d.tags, d.summary,
    // d.metadata,
    // ts_rank_cd(d.tsv, query.q, 32) AS score,
    // ts_headline('%s', d.content, query.q, 'MaxWords=50,MinWords=15') AS headline
    // FROM %s d
    // CROSS JOIN query
    // WHERE query.q @@ d.tsv
    // ORDER BY score DESC
    // LIMIT :topK
    // """, textSearchConfig, textSearchConfig, tableName);
    //
    // return namedJdbcTemplate.query(sql,
    // new MapSqlParameterSource("query", query).addValue("topK", topK),
    // this::mapHighlightedResult);
    // }

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

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SearchStats(
                rs.getLong("total"),
                rs.getDouble("avg_length"),
                rs.getLong("index_size"),
                rs.getTimestamp("last_updated").toLocalDateTime()));
    }

    private String getMetadataSegmentStr(Document doc, String key) {
        return TermUtils.segmentStr(getMetadataStr(doc, key));
    }

    private String getMetadataStr(Document doc, String key) {
        if (doc.metadata() == null || !doc.metadata().containsKey(key)) {
            return "";
        }
        Object value = doc.metadata().get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private Instant getMetadataInstant(Document doc, String key) {
        if (doc.metadata() == null || !doc.metadata().containsKey(key)) {
            return Instant.now();
        }
        Object value = doc.metadata().get(key);
        if (value == null) {
            return Instant.now();
        }
        return Instant.ofEpochSecond((Long) value);
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

    // private HighlightedResult mapHighlightedResult(ResultSet rs, int rowNum)
    // throws SQLException {
    // return new HighlightedResult(
    // mapDocument(rs),
    // rs.getDouble("score"),
    // rs.getString("headline")
    // );
    // }

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
        if (json == null || json.isBlank())
            return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}