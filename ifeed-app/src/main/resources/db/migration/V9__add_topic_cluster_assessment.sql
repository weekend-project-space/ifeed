ALTER TABLE topic_track_summaries
    ADD COLUMN category VARCHAR(32),
    ADD COLUMN score DOUBLE PRECISION;

ALTER TABLE topic_track_summaries
    ADD CONSTRAINT chk_topic_track_summaries_category
        CHECK (category IN ('NEWS', 'KNOWLEDGE', 'COMMUNITY', 'ANNOUNCEMENT')),
    ADD CONSTRAINT chk_topic_track_summaries_score
        CHECK (score >= 0 AND score <= 3);
