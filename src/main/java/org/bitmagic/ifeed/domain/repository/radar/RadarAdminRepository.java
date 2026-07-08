package org.bitmagic.ifeed.domain.repository.radar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RadarAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public void deleteExpiredSnapshots() {
        int deletedTopics = jdbcTemplate.update("delete from radar_topic where snapshot_id in (select snapshot_id from radar_snapshot where expires_at <= now())");
        int deletedSnapshots = jdbcTemplate.update("delete from radar_snapshot where expires_at <= now()");
        log.info("Deleted expired radar snapshots: snapshots={}, topics={}", deletedSnapshots, deletedTopics);
    }

}

