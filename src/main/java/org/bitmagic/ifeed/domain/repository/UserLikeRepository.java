package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.LikeItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserLikeRepository {

    private final JdbcTemplate jdbcTemplate;

    public Instant save(Long userId, Long articleId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO user_likes (user_id, article_id) VALUES (?, ?)
                ON CONFLICT (user_id, article_id) DO UPDATE SET liked_at = user_likes.liked_at
                RETURNING liked_at
                """, Timestamp.class, userId, articleId).toInstant();
    }

    public void delete(Long userId, Long articleId) {
        jdbcTemplate.update("DELETE FROM user_likes WHERE user_id = ? AND article_id = ?", userId, articleId);
    }

    public Page<LikeItemResponse> list(Long userId, BehaviorPageQuery query) {
        var total = jdbcTemplate.queryForObject("SELECT count(*) FROM user_likes WHERE user_id = ?", Long.class, userId);
        var content = jdbcTemplate.query("""
                SELECT liked.id, liked.liked_at, article.uid, article.title, article.summary,
                       article.thumbnail, feed.title AS feed_title
                FROM user_likes liked
                JOIN articles article ON article.id = liked.article_id
                LEFT JOIN feeds feed ON feed.id = article.feed_id
                WHERE liked.user_id = ?
                """ + " ORDER BY " + query.orderBy() + " LIMIT ? OFFSET ?",
                (resultSet, rowNumber) -> new LikeItemResponse(resultSet.getObject("uid", UUID.class),
                        resultSet.getString("title"), resultSet.getString("feed_title"),
                        resultSet.getString("thumbnail"), resultSet.getString("summary"),
                        resultSet.getTimestamp("liked_at").toInstant()),
                userId, query.pageable().getPageSize(), query.pageable().getOffset());
        return new PageImpl<>(content, query.pageable(), total);
    }
}
