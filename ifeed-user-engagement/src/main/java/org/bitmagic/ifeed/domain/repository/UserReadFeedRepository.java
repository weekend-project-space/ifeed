package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserReadFeedRepository {

    private static final String UPSERT = """
            INSERT INTO user_read_feeds (user_id, feed_id, read_at)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, feed_id)
            DO UPDATE SET read_at = GREATEST(user_read_feeds.read_at, EXCLUDED.read_at)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Long userId, Integer feedId, Instant readAt) {
        jdbcTemplate.update(UPSERT, userId, feedId, Timestamp.from(readAt));
    }

    public Map<String, Instant> findReadTimes(Long userId) {
        return jdbcTemplate.query("""
                SELECT feed.uid AS feed_uid, read_feed.read_at
                FROM user_read_feeds read_feed
                JOIN feeds feed ON feed.id = read_feed.feed_id
                WHERE read_feed.user_id = ?
                """, resultSet -> {
            Map<String, Instant> readTimes = new LinkedHashMap<>();
            while (resultSet.next()) {
                readTimes.put(resultSet.getObject("feed_uid", java.util.UUID.class).toString(),
                        resultSet.getTimestamp("read_at").toInstant());
            }
            return readTimes;
        }, userId);
    }
}
