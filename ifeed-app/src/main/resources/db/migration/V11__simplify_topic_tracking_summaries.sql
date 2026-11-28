ALTER TABLE topic_track_matches ALTER COLUMN category DROP NOT NULL;
ALTER TABLE topic_track_summaries ADD COLUMN titles_only boolean NOT NULL DEFAULT false;

CREATE TABLE topic_track_summary_articles (
    summary_id bigint NOT NULL REFERENCES topic_track_summaries(id) ON DELETE CASCADE,
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    PRIMARY KEY (summary_id, article_id)
);
CREATE INDEX idx_topic_summary_articles_article ON topic_track_summary_articles(article_id);
INSERT INTO topic_track_summary_articles(summary_id, article_id)
SELECT summary.id, article.id
FROM topic_track_summaries summary
CROSS JOIN LATERAL jsonb_array_elements(summary.article_previews) preview
JOIN articles article ON article.uid::text = preview->>'articleId'
ON CONFLICT DO NOTHING;

UPDATE topic_tracks SET config_version = config_version + 1,
    categories = '[]'::jsonb,
    scan_status = CASE WHEN enabled THEN 'QUEUED' ELSE 'DISABLED' END,
    scan_token = NULL, scan_lease_until = NULL, scan_error = NULL;
