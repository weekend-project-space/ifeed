CREATE TABLE IF NOT EXISTS article_embeddings (
                                                  id BIGINT PRIMARY KEY,             -- Java Long 对应的主键
                                                  content TEXT,                       -- 存储内容
                                                  metadata JSONB,                     -- 存储元数据，用 JSONB 更高效
                                                  embedding VECTOR(1024)              -- 向量维度假设为 1024
    );

CREATE INDEX IF NOT EXISTS idx_article_embedding_ivfflat
    ON article_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 1000);
ALTER TABLE articles ADD COLUMN tsv TSVECTOR;
-- 为tsv字段创建GIN索引
CREATE INDEX idx_articles_tsv ON articles USING GIN(tsv);

--
-- 100万数据
-- ============================================
-- 1. 启用扩展
-- ============================================
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 2. 创建表(使用halfvec)
-- ============================================
CREATE TABLE vector_store (
                              id BIGSERIAL PRIMARY KEY,
                              content TEXT NOT NULL,
                              metadata JSONB DEFAULT '{}'::jsonb,
                              embedding halfvec(1024) NOT NULL
);

-- ============================================
-- 3. 创建辅助索引
-- ============================================

CREATE INDEX vector_store_metadata_idx
    ON vector_store USING GIN(metadata);

-- ============================================
-- 4. 创建HNSW向量索引
-- ============================================
SET maintenance_work_mem = '2GB';
SET max_parallel_maintenance_workers = 4;

CREATE INDEX vector_store_embedding_idx
    ON vector_store
    USING hnsw (embedding halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ============================================
-- 5. 雷达功能表
-- ============================================

-- 快照：每次滚动任务生成一个 snapshot，用于翻页一致性
CREATE TABLE IF NOT EXISTS radar_snapshot (
    snapshot_id TEXT PRIMARY KEY,
    generated_at TIMESTAMPTZ NOT NULL,
    window_hours INT NOT NULL,
    include_global BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radar_snapshot_expires_at
    ON radar_snapshot (expires_at);

-- 主题：一个 snapshot 下包含多个主题（AI 命名/描述 + 统计信息）
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

-- 主题关联文章：用于主题详情页分页
-- 注意：这里使用 articles.id (BIGINT) 作为外键，更容易与现有向量检索/召回的 DocScore.docId 对齐
CREATE TABLE IF NOT EXISTS radar_topic_article (
    snapshot_id TEXT NOT NULL REFERENCES radar_snapshot(snapshot_id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES radar_topic(topic_id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    PRIMARY KEY (snapshot_id, topic_id, article_id)
);

CREATE INDEX IF NOT EXISTS idx_radar_topic_article_page_by_rank
    ON radar_topic_article (snapshot_id, topic_id, rank);

CREATE INDEX IF NOT EXISTS idx_radar_topic_article_page_by_score
    ON radar_topic_article (snapshot_id, topic_id, score DESC);
