ALTER TABLE topic_tracks
    ADD COLUMN scan_status varchar(24) NOT NULL DEFAULT 'IDLE',
    ADD COLUMN scan_started_at timestamptz,
    ADD COLUMN scan_finished_at timestamptz,
    ADD COLUMN scan_error varchar(240),
    ADD COLUMN scan_token uuid,
    ADD COLUMN scan_lease_until timestamptz;

CREATE TABLE topic_track_evaluations (
    id bigserial PRIMARY KEY,
    topic_track_id bigint NOT NULL REFERENCES topic_tracks(id) ON DELETE CASCADE,
    config_version integer NOT NULL,
    article_id bigint NOT NULL,
    evaluated_at timestamptz NOT NULL,
    UNIQUE (topic_track_id, config_version, article_id)
);
INSERT INTO topic_track_evaluations (topic_track_id, config_version, article_id, evaluated_at)
SELECT topic_track_id, config_version, article_id, matched_at FROM topic_track_matches
ON CONFLICT DO NOTHING;

ALTER TABLE topic_track_summaries
    ADD COLUMN config_version integer NOT NULL DEFAULT 0,
    ADD COLUMN topic_name varchar(120),
    ADD COLUMN keyword varchar(240);
UPDATE topic_track_summaries s SET topic_name = t.name, keyword = t.keyword
FROM topic_tracks t WHERE s.topic_track_id = t.id;
UPDATE topic_track_summaries s SET config_version = inferred.version
FROM (
    SELECT summary.id, min(matches.config_version) AS version
    FROM topic_track_summaries summary
    CROSS JOIN LATERAL jsonb_array_elements(summary.article_previews) preview
    JOIN articles article ON article.uid::text = preview->>'articleId'
    JOIN topic_track_matches matches ON matches.topic_track_id = summary.topic_track_id
        AND matches.article_id = article.id
    GROUP BY summary.id
    HAVING count(DISTINCT matches.config_version) = 1
        AND count(DISTINCT article.id) = max(summary.article_count)
) inferred WHERE s.id = inferred.id;
ALTER TABLE topic_track_summaries ALTER COLUMN topic_name SET NOT NULL;
ALTER TABLE topic_track_summaries ALTER COLUMN keyword SET NOT NULL;
CREATE INDEX idx_topic_summaries_version ON topic_track_summaries(topic_track_id, config_version, created_at DESC, id DESC);
