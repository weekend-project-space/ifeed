-- Baseline migration: represents the existing schema managed by ddl-auto:update.
-- On existing databases, Flyway will skip this via baseline-on-migrate.
-- On fresh databases, this creates the supplementary tables (JPA entities are
-- still created by Hibernate validate + this script).

-- Extensions
CREATE EXTENSION IF NOT EXISTS vector;

-- Article embeddings (used by vector search)
CREATE TABLE IF NOT EXISTS article_embeddings (
    id BIGINT PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1024)
);

CREATE INDEX IF NOT EXISTS idx_article_embedding_ivfflat
    ON article_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 1000);

-- Vector store (used by Spring AI PgVector)
CREATE TABLE IF NOT EXISTS vector_store (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    embedding halfvec(1024) NOT NULL
);

CREATE INDEX IF NOT EXISTS vector_store_metadata_idx
    ON vector_store USING GIN(metadata);

-- Radar feature tables
CREATE TABLE IF NOT EXISTS radar_snapshot (
    snapshot_id TEXT PRIMARY KEY,
    generated_at TIMESTAMPTZ NOT NULL,
    window_hours INT NOT NULL,
    include_global BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radar_snapshot_expires_at
    ON radar_snapshot (expires_at);

CREATE TABLE IF NOT EXISTS radar_topic (
    topic_id UUID PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES radar_snapshot(snapshot_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    article_count INT NOT NULL DEFAULT 0,
    top_keywords JSONB,
    score DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_radar_topic_snapshot_score
    ON radar_topic (snapshot_id, score DESC);

CREATE INDEX IF NOT EXISTS idx_radar_topic_snapshot_updated_at
    ON radar_topic (snapshot_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS radar_topic_article (
    snapshot_id TEXT NOT NULL REFERENCES radar_snapshot(snapshot_id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES radar_topic(topic_id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    PRIMARY KEY (snapshot_id, topic_id, article_id)
);

CREATE INDEX IF NOT EXISTS idx_radar_topic_article_page_by_rank
    ON radar_topic_article (snapshot_id, topic_id, rank);

CREATE INDEX IF NOT EXISTS idx_radar_topic_article_page_by_score
    ON radar_topic_article (snapshot_id, topic_id, score DESC);
