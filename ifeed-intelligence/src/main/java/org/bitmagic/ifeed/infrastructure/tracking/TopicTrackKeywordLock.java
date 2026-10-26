package org.bitmagic.ifeed.infrastructure.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TopicTrackKeywordLock {

    private final JdbcTemplate jdbcTemplate;

    public void lockUser(Integer userId) {
        jdbcTemplate.execute((PreparedStatementCreator) connection -> {
            var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)");
            statement.setInt(1, 1);
            statement.setInt(2, userId);
            return statement;
        }, statement -> {
            statement.execute();
            return null;
        });
    }

    public void lock(String normalizedKeyword) {
        jdbcTemplate.execute((PreparedStatementCreator) connection -> {
            var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))");
            statement.setString(1, normalizedKeyword);
            return statement;
        }, statement -> {
            statement.execute();
            return null;
        });
    }
}
