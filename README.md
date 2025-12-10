# iFeed

iFeed 是一个以 AI 驱动 的智能信息流聚合与推荐系统，基于 Spring Boot、PostgreSQL、MongoDB 与 OpenAI 等技术构建。
它支持 RSS 订阅、语义检索、内容嵌入与向量化推荐，通过深度语言模型实现个性化内容聚合与智能推荐。

------------------------------------------------------------------------

## 🚀 项目简介

iFeed 旨在构建一个现代化的内容聚合与推荐平台，核心特性包括：

- **智能聚合**: 支持 RSS 订阅与自动聚合。
- **语义检索**: 基于 pgvector 的向量检索与语义匹配，超越传统关键词搜索。
- **AI 赋能**: 利用 OpenAI 进行内容摘要、自动分类与标签生成。
- **混合推荐**: 结合 TF-IDF 关键词匹配 + 向量相似度 + 新鲜度权重的混合召回算法。
- **现代架构**: 前后端分离，Spring Boot 3.x + Vue 3 技术栈。

------------------------------------------------------------------------

## 🧩 技术栈

| 组件 | 说明 |
| :--- | :--- |
| **后端框架** | Spring Boot 3.x |
| **前端框架** | Vue 3 + Vite |
| **数据库** | PostgreSQL + pgvector 插件 (结构化数据 & 向量) |
| **NoSQL** | MongoDB (非结构化数据 & 缓存) |
| **AI 模块** | OpenAI Chat / Embedding API |
| **检索算法** | TF-IDF + 向量 + 新鲜度 混合召回 |
| **连接池** | HikariCP |
| **构建工具** | Maven (Backend) / npm (Frontend) |

------------------------------------------------------------------------

## 🛠️ 部署与运行

本项目支持多种部署方式，包括 Docker Compose 一键部署和手动构建部署。

👉 **详细的部署步骤、环境变量配置及数据库初始化脚本，请参阅 [部署指南](./deployment.md)。**

### 快速开始 (Docker Compose)

如果您已安装 Docker 和 Docker Compose，可以快速启动体验：

1.  克隆项目：
    ```bash
    git clone https://github.com/weekend-project-space/ifeed.git
    cd ifeed
    ```

2.  参考 [部署指南](./deployment.md) 配置 `docker-compose.yml` 及环境变量。

3.  启动服务：
    ```bash
    docker-compose up -d
    ```

------------------------------------------------------------------------

## 🧠 推荐系统说明

iFeed 支持多层次推荐策略，确保推荐内容的准确性与时效性：

1.  **关键词检索**：基于 TF-IDF 算法的语义匹配，处理精确查找需求。
2.  **向量召回**：利用 OpenAI Embedding + pgvector 实现语义相似度检索，发现潜在相关内容。
3.  **混合策略**：融合 TF-IDF 与向量召回结果，并引入时间衰减因子（新鲜度），综合计算最终得分。

数据库表结构示例（向量表）：

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE article_embeddings (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(1024)
);
CREATE INDEX ON article_embeddings USING ivfflat (embedding vector_cosine_ops);
```

------------------------------------------------------------------------

## 📂 目录结构

```bash
ifeed/
├── doc/                # 文档资料
├── frontend/           # Vue 前端项目
├── src/                # Spring Boot 后端源码
│   ├── main/
│   │   ├── java/com/ifeed/
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   └── static/
│   └── test/
├── pom.xml             # Maven 配置
├── deployment.md       # 部署指南
└── README.md           # 项目说明
```

------------------------------------------------------------------------

## 💡 TODO

-   [ ] RAG 知识库增强
-   [ ] 更多数据源采集器支持
-   [ ] 多用户系统完善

------------------------------------------------------------------------

## 📜 License

MIT License © weekend-project-space