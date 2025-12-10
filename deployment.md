# iFeed 部署指南

本文档详细说明如何部署 iFeed 应用。支持 Docker Compose 一键部署（推荐）和手动构建部署。

## 前置要求

-   **Docker & Docker Compose**: 推荐使用最新版本。
-   **Java 21**: 如果需要手动构建后端。
-   **Node.js 20+**: 如果需要手动构建前端。

## 架构概览

iFeed 由以下组件组成：
-   **Frontend**: Vue 3 + Vite 单页应用。
-   **Backend**: Spring Boot 3.5 应用。
-   **Database**: PostgreSQL 16 (启用 pgvector 插件) 用于存储业务数据和向量数据。
-   **NoSQL**: MongoDB 用于存储非结构化数据。

## 环境变量配置

以下是主要的配置项，可以通过环境变量传递给 Docker 容器或应用。

| 变量名                                  | 默认值 | 说明                              |
|:-------------------------------------| :--- |:--------------------------------|
| `PG`                                 | `127.0.0.1` | PostgreSQL 主机地址                 |
| `PG_PORT`                            | `5432` | PostgreSQL 端口                   |
| `PG_DATABASE`                        | `i_feed` | 数据库名称                           |
| `PG_USERNAME`                        | `i_feed` | 数据库用户名                          |
| `PG_PASSWORD`                        | `0123.` | 数据库密码                           |
| `MONGO_DB`                           | `127.0.0.1` | MongoDB 主机地址                    |
| `MONGO_DB_PORT`                      | `27017` | MongoDB 端口                      |
| `MONGO_DB_DATABASE`                           | `i_feed` | 数据库名称                   |
| `MONGO_DB_USERNAME`                  | `mongo_cTXZaN` | MongoDB 用户名                     |
| `MONGO_DB_PASSWORD`                  | `mongo_MDNyW6` | MongoDB 密码                      |
| `app.ai.provider.api-key`            | - | **必填** OpenAI 兼容接口的 API Key     |
| `app.ai.provider.base-url`           | - | **必填** OpenAI 兼容接口的 Base URL    |
| `app.ai.provider.model`              | `gpt-4o-mini` | 聊天模型名称                          |
| `app.ai.provider.embedding.api-key`  | - | Embedding 服务的 API Key (通常同上chat)    |
| `app.ai.provider.embedding.base-url` | - | Embedding 服务的 Base URL (通常同上chat)   |
| `app.ai.provider.embedding.model`    | `Qwen/Qwen3-Embedding-4B` | Embedding 模型名称                  |
| `app.ai.provider.reranker.api-key`   | - | reranker 服务的 API Key (通常同上chat) |
| `app.ai.provider.reranker.base-url` | - | reranker 服务的 Base URL (通常同上chat)    |
| `app.ai.provider.reranker.model`    | `bge-reranker-v2-m3` | reranker 模型名称                   |

## Docker Compose 部署 (推荐)

### 1. 准备初始化脚本

在项目根目录下创建 `init` 目录，并分别创建 PostgreSQL 和 MongoDB 的初始化脚本。

**PostgreSQL 初始化脚本** (`init/postgres/init.sql`):

```sql
CREATE DATABASE i_feed;

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE article_embeddings (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(1024)
);
CREATE INDEX ON article_embeddings USING ivfflat (embedding vector_cosine_ops);
```

**MongoDB 初始化脚本** (`init/mongo/init.js`):

```javascript
// 这里的用户名密码应与 docker-compose.yml 中的环境变量一致
// 如果使用 root 用户连接，此脚本可能不是必须的，但为了权限分离建议创建
db.createUser({
    user: "mongo_cTXZaN",
    pwd: "mongo_MDNyW6",
    roles: [
        { role: "readWrite", db: "i_feed" }
    ]
});
```

### 2. 创建 docker-compose.yml

在项目根目录下创建一个 `docker-compose.yml` 文件：

```yaml
services:
  ifeed-app:
    image: ifeed/ifeed:latest 
    ports:
      - "8080:8080"
    environment:
      - PG=postgres
      - MONGO_DB=mongo
      - app.ai.provider.api-key=your_api_key_here
      - app.ai.provider.base-url=your_base_url_here
      - app.ai.provider.embedding.api-key=your_api_key_here
      - app.ai.provider.embedding.base-url=your_base_url_here
      - app.ai.provider.reranker.api-key=your_api_key_here
      - app.ai.provider.reranker.base-url=your_base_url_here
    depends_on:
      - postgres
      - mongo
    restart: always

  postgres:
    image: pgvector/pgvector:pg17
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=postgres # 默认连接库，init.sql 中会创建 i_feed
      - POSTGRES_USER=i_feed
      - POSTGRES_PASSWORD=0123.
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
      - ./init/postgres:/docker-entrypoint-initdb.d # 挂载初始化脚本
    restart: always

  mongo:
    image: mongo:latest
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=mongo_cTXZaN
      - MONGO_INITDB_ROOT_PASSWORD=mongo_MDNyW6
      - MONGO_INITDB_DATABASE=i_feed
    volumes:
      - ./data/mongo:/data/db
      - ./init/mongo:/docker-entrypoint-initdb.d # 挂载初始化脚本
    restart: always
```

### 启动服务

```bash
docker-compose up -d
```

### 或 docker run

```
docker run -d \
    --name ifeed-app \
    -p 8080:8080 \
    -e PG=PG\
    -e PG_DATABASE=i_feed \
    -e PG_USERNAME=PG_USERNAME \
    -e PG_PASSWORD=PG_PASSWORD \
    -e MONGO_DB=MONGO_DB \
    -e MONGO_DB_DATABASE=i_feed \
    -e MONGO_DB_USERNAME=MONGO_DB_USERNAME \
    -e MONGO_DB_PASSWORD=MONGO_DB_PASSWORD \
    -e app.ai.provider.api-key=APIKEY \
    -e app.ai.provider.base-url=BASE_URL \
    -e app.ai.provider.reranker.api-key=APIKEY \
    -e app.ai.provider.reranker.base-url=BASE_URL \
    --restart always \
    ifeed/ifeed:latest
```

访问 `http://localhost:8080` 即可使用。

## 手动构建与部署

### 1. 构建前端

```bash
cd frontend
npm install
npm run build
```
构建产物位于 `frontend/dist` 目录。Spring Boot 配置会自动将 `classpath:/static` 映射到前端资源，因此需要将 `dist` 内容复制到后端的资源目录（如果不是打包在同一个 Jar 中，通常需要 Nginx 反代）。

**注意**: 当前项目的构建流程中，后端 Jar 包通常不包含前端资源，建议使用 Nginx 部署前端，反向代理 API 请求到后端。

### 2. 构建后端

```bash
# 在项目根目录
./mvnw clean package -DskipTests
```
构建产物位于 `target/ifeed-0.0.1-SNAPSHOT.jar`。

### 3. 运行

确保 PostgreSQL 和 MongoDB 已启动并配置正确。

**注意**: 对于手动安装的 PostgreSQL，必须安装并启用 `pgvector` 扩展，并初始化向量表：

1.  安装 `pgvector` (参考 [官方文档](https://github.com/pgvector/pgvector)).
2.  在数据库中执行以下 SQL 初始化语句:

```sql
CREATE DATABASE i_feed;

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE article_embeddings (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(1024)
);
CREATE INDEX ON article_embeddings USING ivfflat (embedding vector_cosine_ops);
```

**注意**: 对于手动安装的 MongoDB，需要创建数据库用户并授权：

1.  连接到 MongoDB (使用 `mongosh`)。
2.  执行以下命令：

```javascript
use admin
db.auth('mongo_cTXZaN','mongo_MDNyW6')
use i_feed
db.createUser({user:"mongo_cTXZaN",pwd:"mongo_MDNyW6",roles:[{role:"readWrite",db:"i_feed"}]})
```

```bash
java -jar target/ifeed-0.0.1-SNAPSHOT.jar \
  --PG=localhost \
  --app.ai.provider.api-key=sk-...
```

## Nginx 配置示例 (前后端分离部署)

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```
