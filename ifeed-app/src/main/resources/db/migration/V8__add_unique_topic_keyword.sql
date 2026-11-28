ALTER TABLE topic_tracks ADD COLUMN normalized_keyword VARCHAR(240);

UPDATE topic_tracks
SET normalized_keyword = translate(
    btrim(regexp_replace(keyword, E'[ \\t\\n\\r\\f\\x0B]+', ' ', 'g')),
    'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'
);

DO $$
BEGIN
    IF EXISTS (
        SELECT normalized_keyword FROM topic_tracks
        GROUP BY normalized_keyword HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate topic keywords exist. Resolve them before adding the unique constraint; no topic data has been deleted.';
    END IF;
END $$;

ALTER TABLE topic_tracks ALTER COLUMN normalized_keyword SET NOT NULL;
ALTER TABLE topic_tracks ADD CONSTRAINT uq_topic_tracks_normalized_keyword UNIQUE (normalized_keyword);
