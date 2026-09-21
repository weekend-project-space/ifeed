package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ReadHistoryItemResponse;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.record.ReadHistoryArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserReadHistoryRepository {

    private static final String UPSERT = """
            INSERT INTO user_read_history (user_id, article_id, read_at)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, article_id)
            DO UPDATE SET read_at = GREATEST(user_read_history.read_at, EXCLUDED.read_at)
            """;

    private static final String ARTICLE_QUERY = """
            SELECT article.id AS article_id, article.uid, article.title, article.summary,
                   article.thumbnail, feed.title AS feed_title, article.pub_date,
                   article.tags, article.category, history.read_at
            FROM user_read_history history
            JOIN articles article ON article.id = history.article_id
            LEFT JOIN feeds feed ON feed.id = article.feed_id
            WHERE history.user_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Long userId, Long articleId, Instant readAt) {
        jdbcTemplate.update(UPSERT, userId, articleId, Timestamp.from(readAt));
    }

    public void upsertAll(Long userId, Map<Long, Instant> history) {
        jdbcTemplate.batchUpdate(UPSERT, history.entrySet(), 500, (statement, entry) -> {
            statement.setLong(1, userId);
            statement.setLong(2, entry.getKey());
            statement.setTimestamp(3, Timestamp.from(entry.getValue()));
        });
    }

    public int delete(Long userId, Long articleId) {
        return jdbcTemplate.update("DELETE FROM user_read_history WHERE user_id = ? AND article_id = ?",
                userId, articleId);
    }

    public Page<ReadHistoryItemResponse> findByUserId(Long userId, Pageable pageable) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_read_history WHERE user_id = ?", Long.class, userId);
        Sort.Order order = pageable.getSort().getOrderFor("readAt");
        String direction = order != null && order.isAscending() ? "ASC" : "DESC";
        String sql = ARTICLE_QUERY + " ORDER BY history.read_at " + direction
                + ", history.id ASC LIMIT ? OFFSET ?";
        var content = jdbcTemplate.query(sql, (resultSet, rowNumber) -> new ReadHistoryItemResponse(
                resultSet.getString("uid"),
                resultSet.getString("title"),
                resultSet.getString("feed_title"),
                resultSet.getString("thumbnail"),
                resultSet.getString("summary"),
                resultSet.getTimestamp("read_at").toInstant()),
                userId, pageable.getPageSize(), pageable.getOffset());
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    public List<ReadHistoryArticle> findRecent(Long userId, Instant since, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String sql = ARTICLE_QUERY;
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (since != null) {
            sql += " AND history.read_at >= ?";
            parameters.add(Timestamp.from(since));
        }
        sql += " ORDER BY history.read_at DESC, history.id ASC LIMIT ?";
        parameters.add(limit);
        return jdbcTemplate.query(sql, this::mapArticle, parameters.toArray());
    }

    private ReadHistoryArticle mapArticle(ResultSet resultSet, int rowNumber) throws SQLException {
        var article = new ArticleSummary(
                resultSet.getObject("uid", UUID.class),
                resultSet.getLong("article_id"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("thumbnail"),
                resultSet.getString("feed_title"),
                resultSet.getTimestamp("pub_date").toInstant(),
                resultSet.getString("tags"),
                resultSet.getString("category"));
        return new ReadHistoryArticle(article, resultSet.getTimestamp("read_at").toInstant());
    }
}
