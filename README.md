# iFeed

iFeed 是一个以 AI 驱动 的智能信息流聚合与推荐系统，基于 Spring Boot、PostgreSQL、MongoDB 与 OpenAI 等技术构建。
它支持 RSS 订阅、语义检索、内容嵌入与向量化推荐，通过深度语言模型实现个性化内容聚合与智能推荐。

------------------------------------------------------------------------

## 🚀 项目简介

iFeed 旨在构建一个现代化的内容聚合与推荐平台，支持：

- RSS 订阅与聚合
- 向量检索与语义匹配（pgvector）
- 基于 OpenAI 的内容摘要与分类
- 自定义推荐算法（BM25 + 向量 + 新鲜度 混合召回） - 前后端解耦架构

------------------------------------------------------------------------

## 🧩 技术栈

组件 说明
  ----------------- -----------------------------
**后端框架**      Spring Boot 3.x

**数据库**        PostgreSQL + pgvector 插件

**缓存 / 存储**   MongoDB

**AI 模块**       OpenAI Chat / Embedding API

**检索算法**      BM25 + 向量 + 新鲜度 召回

**连接池**        HikariCP

**构建工具**      Maven

------------------------------------------------------------------------

## ⚙️ 环境配置

以下环境变量在 `application.yml` 中定义，可通过系统变量或 `.env`
文件配置。

明细参考 [配置参数说明文档.md](./doc/配置参数说明文档.md)

  ----------------------------------------------------------------------------------
变量名 说明 默认值
  ----------------------------------- ------------------- --------------------------
`PG`                                PostgreSQL 主机地址 `127.0.0.1`

`PG_PORT`                           PostgreSQL 端口     `5432`

`PG_DATABASE`                       数据库名            `i_feed`

`PG_USERNAME`                       数据库用户          `i_feed`

`PG_PASSWORD`                       数据库密码          `0123.`

`VECTOR_INDEX_TYPE`                 向量索引类型        `HNSW`

`VECTOR_DISTANCE_TYPE`              向量距离度量方式    `COSINE_DISTANCE`

`MONGO_DB`                          Mongo 主机地址      `127.0.0.1`

`MONGO_DB_PORT`                     Mongo 端口          `27017`

`MONGO_DB_USERNAME`                 Mongo 用户名        `mongo_cTXZaN`

`MONGO_DB_PASSWORD`                 Mongo 密码          `mongo_MDNyW6`

`MONGO_DB_DATABASE`                 Mongo 数据库名      `i_feed`

`app.ai.provider.api-key`           OpenAI API Key      *(需手动设置)*

`app.ai.provider.endpoint`          OpenAI API Endpoint *(可选)*

`app.ai.provider.model`             Chat 模型           `gpt-4o-mini`

`app.ai.provider.embedding.model`   向量模型            `text-embedding-3-small`

----------------------------------------------------------------------------------

## 🧠 推荐系统说明

iFeed 支持多层次推荐策略：

1. **关键词检索**：基于 BM25 算法的语义匹配。
2. **向量召回**：利用 OpenAI Embedding + pgvector 实现语义相似度检索。
3. **混合策略**：BM25 与向量召回融合，提高内容相关性。

数据库表结构示例：

``` sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE article_embeddings (
    id SERIAL PRIMARY KEY,
    article_id VARCHAR(64),
    title TEXT,
    content TEXT,
    embedding vector(1024)
);
CREATE INDEX ON article_embeddings USING hnsw (embedding vector_cosine_ops);
```

------------------------------------------------------------------------

## 🧰 启动项目

### 1️⃣ 克隆项目

``` bash
git clone https://github.com/weekend-project-space/ifeed.git
cd ifeed
```

### 2️⃣ 环境依赖

确保本地安装：

- JDK 21+ - PostgreSQL 17 + 并启用 pgvector - MongoDB 6+
- Maven / Node

### 3️⃣ 配置环境变量

可以在 `.env` 文件中定义上表变量，或在系统环境中设置。

### 4️⃣ 启动项目

``` bash
./mvnw spring-boot:run
```

------------------------------------------------------------------------

## 📂 目录结构

``` bash
ifeed/
├── doc/
├── frontend/
├── src/
│   ├── main/
│   │   ├── java/com/ifeed/
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   └── static/
│   └── test/
├── pom.xml
└── README.md
```

------------------------------------------------------------------------

## 💡 TODO

-   [ ] RAG知识库
-   [ ] 收集器

------------------------------------------------------------------------

## 📜 License

MIT License © weekend-project-space