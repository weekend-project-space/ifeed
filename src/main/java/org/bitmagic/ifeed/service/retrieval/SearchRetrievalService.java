package org.bitmagic.ifeed.service.retrieval;

import com.rometools.utils.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.SearchRetrievalProperties;
import org.bitmagic.ifeed.config.vectore.SearchRequestTurbo;
import org.bitmagic.ifeed.config.vectore.VectorStoreTurbo;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.service.ArticleService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 混合检索服务：结合 BM25 与向量召回，完成打分融合与分页返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRetrievalService {

    private static final String BM25_SQL = """
            WITH query AS (
                SELECT websearch_to_tsquery('simple', ?) AS q
            ),
            documents AS (
                SELECT a.id,
                       setweight(to_tsvector('simple', coalesce(a.title, '')), 'A') ||
                       setweight(to_tsvector('simple', coalesce(a.category, '')), 'A') ||
                       setweight(to_tsvector('simple', coalesce(a.tags, '')), 'B') ||
                       setweight(to_tsvector('simple', coalesce(a.summary, '')), 'B') ||
                       setweight(to_tsvector('simple', coalesce(a.author, '')), 'B') ||
                       setweight(to_tsvector('simple', coalesce(a.content, '')), 'C') AS document
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
                   ts_rank_cd(d.document, query.q) AS score
            FROM documents d
            CROSS JOIN query
            WHERE query.q @@ d.document
            ORDER BY score DESC
            LIMIT ?
            """;

    private final VectorStoreTurbo vectorStore;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ArticleService articleService;
    private final JdbcTemplate jdbcTemplate;
    private final SearchRetrievalProperties properties;


    public Page<ArticleSummaryView> hybridSearch(UUID userId,
                                                 String query,
                                                 boolean includeGlobal,
                                                 int page,
                                                 int size) {

        if (!StringUtils.hasText(query)) {
            return Page.empty(PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        }
        List<UUID> artIds = hybridSearch(userId, null, query, includeGlobal, properties.getFusionTopK());
        Page<ArticleSummaryView> data = articleService.findIds2Article(artIds, page, size);

        log.debug("Hybrid search complete: user={}, query='{}', totalCandidates={}, pageIsLast={}",
                userId, query, artIds.size(), data.isLast());
        return data;
    }


    /**
     * 对查询执行混合检索：BM25 + 向量召回，并融合得分返回分页结果
     * 返回融合后的文章 ID 列表。
     */
    public List<UUID> hybridSearch(UUID userId,
                                   float[] queryEmbedding,
                                   String query,
                                   boolean includeGlobal,
                                   int maxSize) {
        int safeSize = maxSize <= 0 ? properties.getFusionTopK() : maxSize;
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        int desired = Math.max(properties.getFusionTopK(), safeSize);
        if (Objects.isNull(query) && Strings.isBlank(normalizedQuery)) {
            return Collections.emptyList();
        }

        List<UUID> feedIds = includeGlobal ? Collections.emptyList() : userSubscriptionRepository.findActiveFeedIdsByUserId(userId);
        log.debug("Hybrid search(IDs) start: user={}, includeGlobal={}, maxSize={}, query='{}'", userId, includeGlobal, safeSize, normalizedQuery);
        if (!includeGlobal && CollectionUtils.isEmpty(feedIds)) {
            return Collections.emptyList();
        }

        List<CombinedScore> combined = combineScores(normalizedQuery, queryEmbedding, userId, includeGlobal, desired, feedIds);
        if (combined.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> ids = combined.stream()
                .limit(safeSize)
                .map(CombinedScore::id)
                .toList();
        log.debug("Hybrid search(IDs) complete: user={}, query='{}', returnCount={}", userId, normalizedQuery, ids.size());
        return ids;
    }

    private Map<UUID, Double> fetchBm25Scores(String query,
                                              UUID userId,
                                              boolean includeGlobal,
                                              int limit) {
        log.trace("BM25 fetch: user={}, includeGlobal={}, limit={}, query='{}'", userId, includeGlobal, limit, query);
        List<Bm25Row> rows = jdbcTemplate.query(BM25_SQL,
                (rs, rowNum) -> new Bm25Row(readUuid(rs, "id"), rs.getDouble("score")),
                query,
                includeGlobal,
                userId,
                limit);

        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Bm25Row row : rows) {
            if (row.id() != null) {
                scores.putIfAbsent(row.id(), row.score());
            }
        }
        return scores;
    }

    private Map<UUID, Double> fetchVectorScores(float[] queryEmbedding, String query,
                                                boolean includeGlobal,
                                                int limit,
                                                List<UUID> feedIds) {
        log.trace("Vector fetch: includeGlobal={}, limit={}, feedIds={}", includeGlobal, limit, feedIds.size());
        if (queryEmbedding == null && !StringUtils.hasText(query)) {
            return Collections.emptyMap();
        }

        List<Document> documents;
        if (queryEmbedding != null) {
            var builder = SearchRequestTurbo.builder()
                    .embedding(queryEmbedding)
                    .topK(limit)
                    .similarityThreshold(properties.getSimilarityThreshold());

            if (!includeGlobal) {
                if (CollectionUtils.isEmpty(feedIds)) {
                    return Collections.emptyMap();
                }
                FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
                builder.filterExpression(filterBuilder.in("feedId", feedIds.stream().map(UUID::toString).toArray(String[]::new)).build());
            }
            documents = vectorStore.similaritySearch(builder.build());
        } else {
            var builder = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(properties.getSimilarityThreshold());

            if (!includeGlobal) {
                if (CollectionUtils.isEmpty(feedIds)) {
                    return Collections.emptyMap();
                }
                FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
                builder.filterExpression(filterBuilder.in("feedId", feedIds.stream().map(UUID::toString).toArray(String[]::new)).build());
            }
            documents = vectorStore.similaritySearch(builder.build());
        }

        if (documents == null || documents.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Document document : documents) {
            Object articleIdValue = document.getMetadata().get("articleId");
            if (articleIdValue == null) {
                continue;
            }
            try {
                UUID articleId = UUID.fromString(articleIdValue.toString());
                scores.putIfAbsent(articleId, document.getScore());
            } catch (IllegalArgumentException ignored) {
                // skip invalid IDs
            }
        }
        return scores;
    }

    private Map<UUID, Double> normalizeScores(Map<UUID, Double> scores) {
        if (scores.isEmpty()) {
            return Collections.emptyMap();
        }
        double min = scores.values().stream().min(Double::compareTo).orElse(0.0);
        double max = scores.values().stream().max(Double::compareTo).orElse(0.0);

        if (Double.compare(max, min) == 0) {
            Map<UUID, Double> normalized = new HashMap<>();
            scores.keySet().forEach(id -> normalized.put(id, 1.0));
            return normalized;
        }

        double range = max - min;
        Map<UUID, Double> normalized = new HashMap<>();
        scores.forEach((id, score) -> normalized.put(id, (score - min) / range));
        return normalized;
    }

    private List<CombinedScore> combineScores(String normalizedQuery,
                                              float[] queryEmbedding,
                                              UUID userId,
                                              boolean includeGlobal,
                                              int desired,
                                              List<UUID> feedIds) {
        int bm25Limit = Math.max(properties.getBm25TopK(), desired);
        int vectorLimit = Math.max(properties.getVectorTopK(), desired);

        Map<UUID, Double> bm25Scores = StringUtils.hasText(normalizedQuery)
                ? fetchBm25Scores(normalizedQuery, userId, includeGlobal, bm25Limit)
                : Collections.emptyMap();
        Map<UUID, Double> vectorScores = fetchVectorScores(queryEmbedding, normalizedQuery, includeGlobal, vectorLimit, feedIds);

        Map<UUID, Double> normalizedBm25 = normalizeScores(bm25Scores);
        Map<UUID, Double> normalizedVector = normalizeScores(vectorScores);

        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(normalizedBm25.keySet());
        allIds.addAll(normalizedVector.keySet());

        if (allIds.isEmpty()) {
            return Collections.emptyList();
        }

        double bm25Weight = properties.getBm25Weight();
        double vectorWeight = properties.getVectorWeight();

        List<CombinedScore> combined = allIds.stream()
                .map(id -> new CombinedScore(id,
                        normalizedBm25.getOrDefault(id, 0.0),
                        normalizedVector.getOrDefault(id, 0.0),
                        bm25Weight,
                        vectorWeight))
                .sorted((a, b) -> Double.compare(b.totalScore(), a.totalScore()))
                .collect(Collectors.toCollection(ArrayList::new));

        int fusionLimit = Math.max(properties.getFusionTopK(), desired);
        if (combined.size() > fusionLimit) {
            combined = new ArrayList<>(combined.subList(0, fusionLimit));
        }

        return combined;
    }

    private UUID readUuid(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String string) {
            try {
                return UUID.fromString(string);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private record CombinedScore(UUID id, double bm25Score, double vectorScore, double bm25Weight,
                                 double vectorWeight) {
        double totalScore() {
            return bm25Score * bm25Weight + vectorScore * vectorWeight;
        }
    }

    private record Bm25Row(UUID id, double score) {
    }
}
