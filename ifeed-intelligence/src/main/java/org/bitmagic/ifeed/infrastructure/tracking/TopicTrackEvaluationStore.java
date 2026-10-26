package org.bitmagic.ifeed.infrastructure.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopicTrackEvaluationStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** 查询指定话题配置版本中已经处理过的文章，避免重复调用 Jev、向量和总结流程。 */
    public List<Long> evaluated(Long topicId, int version, Collection<Long> articleIds) {
        if (articleIds.isEmpty()) return List.of();
        return jdbcTemplate.queryForList("""
                select article_id from topic_track_evaluations
                where topic_track_id = :topicId and config_version = :version and article_id in (:articleIds)
                """, new MapSqlParameterSource().addValue("topicId", topicId).addValue("version", version)
                .addValue("articleIds", articleIds), Long.class);
    }

    /** 批量记录文章已处理状态；同一话题、配置版本和文章重复写入时保持幂等。 */
    public void record(Long topicId, int version, Collection<Long> articleIds, Instant now) {
        var rows = articleIds.stream().map(articleId -> new MapSqlParameterSource()
                .addValue("topicId", topicId).addValue("version", version)
                .addValue("articleId", articleId).addValue("now", Timestamp.from(now)))
                .toArray(MapSqlParameterSource[]::new);
        if (rows.length == 0) return;
        jdbcTemplate.batchUpdate("""
                insert into topic_track_evaluations(topic_track_id, config_version, article_id, evaluated_at)
                values (:topicId, :version, :articleId, :now) on conflict do nothing
                """, rows);
    }
}
