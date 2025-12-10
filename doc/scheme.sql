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