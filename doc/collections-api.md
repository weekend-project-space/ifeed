# 收藏与文件夹接口文档

状态：后端接口和 PostgreSQL 存储已实现。目标数据库仍需执行建表和旧收藏回填，步骤见第 12 节。

## 1. 接口概览

| 功能 | 方法 | 路径 |
|---|---|---|
| 收藏文章／更新所属文件夹 | POST | `/api/user/collections/{articleId}` |
| 取消收藏 | DELETE | `/api/user/collections/{articleId}` |
| 创建文件夹 | POST | `/api/user/collection-folders` |
| 编辑文件夹 | PUT | `/api/user/collection-folders/{folderId}` |
| 删除文件夹 | DELETE | `/api/user/collection-folders/{folderId}` |
| 文件夹列表（Pageable） | GET | `/api/user/collection-folders` |
| 文件夹下收藏列表（Pageable） | GET | `/api/user/collections` |

## 2. 通用约定

- 所有接口要求 `Authorization: Bearer <token>`，用户身份由登录信息确定，请求不传 `userId`。
- 请求体和响应体使用 JSON。有请求体时传 `Content-Type: application/json`。
- 文件夹和收藏记录均使用内部数字自增主键 `id`，不对外暴露。
- 本次新增数据中只有文件夹具有对外 UUID，接口字段名为 `folderId`。收藏记录不新增对外 UUID，也不返回独立的收藏 ID。
- `articleId` 引用现有文章 API 的 UUID，用于指定被收藏的文章，不是收藏记录的 ID；本次不改变文章标识。
- 时间使用 ISO 8601 UTC 格式，例如 `2026-09-21T08:00:00Z`。
- 每个用户对同一篇文章最多有一条收藏；一条收藏最多属于一个文件夹。
- 收藏的 `folderId: null` 表示默认收藏夹。默认收藏夹是逻辑分组，没有文件夹记录，不支持创建、编辑或删除，也不计入文件夹分页列表。
- 只能查询和操作自己的收藏、文件夹。指定不存在或属于其他用户的文件夹，统一返回 `404`。
- 文件夹只包含名称，不包含备注、描述、嵌套层级或自定义排序。
- 文件夹名称去除首尾空格后长度为 1～50 个字符；同一用户下去除首尾空格后完全相同的名称不可重复。
- 文章详情 `GET /api/articles/{articleId}` 返回 `collected`、`liked` 和文件夹对外 UUID `folderId`。`folderId` 为 `null` 时，结合 `collected` 区分未收藏与默认收藏夹。前端使用详情状态初始化按钮，收藏列表支持通过 URL 中的 `folderId` 保留筛选条件。

### 内外部 ID 映射

| 数据 | 数据库内部标识 | 对外标识 |
|---|---|---|
| 文件夹 | `id`：`bigint` 自增主键 | `uid`：唯一 UUID，响应中命名为 `folderId` |
| 收藏记录 | `id`：`bigint` 自增主键 | 无；操作时通过当前用户和 `articleId` 定位 |

新表中的 `id`、`user_id`、`article_id`、`folder_id` 均使用 PostgreSQL `BIGINT`，Java 对应 `Long`。所有表间关联使用内部数字主键，`folder_id` 可为空；UUID 仅用于对外标识、请求入口查找和响应输出，不作为关联键。收藏记录另设 `(user_id, article_id)` 唯一约束，防止重复收藏。完整表结构见第 11 节。

### Pageable

两个列表接口使用 Spring Data `Pageable` 参数，返回 `Page` 风格 JSON。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `page` | integer | `0` | 从 0 开始，必须大于等于 0 |
| `size` | integer | `20` | 每页 1～100 条 |
| `sort` | string | 见各接口 | 格式为 `字段,asc` 或 `字段,desc`，可重复传入 |

分页响应保留 `content`、`totalElements`、`totalPages`、`number`、`size` 等 Spring Data `Page` 字段。下文示例仅展示这五个主要字段；页码字段为 `number`，不是 `page`。

排序字段限定为各接口列出的字段；未传方向时按 `asc`，不支持的字段或方向返回 `400`。服务端在业务排序后追加唯一 ID 升序排序，保证排序值相同时顺序稳定。空列表的 `totalElements`、`totalPages` 均为 `0`；超出末页时返回空 `content`，总数仍为实际匹配数量。

## 3. 收藏文章／更新所属文件夹

`POST /api/user/collections/{articleId}`

### 请求

路径参数 `articleId` 为文章 UUID。

```json
{
  "folderId": "cde78912-3456-4789-abcd-1234567890ab"
}
```

| 请求方式 | 未收藏时 | 已收藏时 |
|---|---|---|
| 指定 `folderId` UUID | 收藏到指定文件夹 | 移动到指定文件夹 |
| 显式传 `"folderId": null` | 收藏到默认收藏夹 | 移回默认收藏夹 |
| 不传请求体，或传 `{}` | 收藏到默认收藏夹 | 保持原文件夹 |

### 行为

- 文章必须存在；指定的文件夹必须存在且属于当前用户。校验失败时不创建或移动收藏。
- 首次收藏创建记录，`collectedAt` 为服务端当前时间。
- 已收藏时更新所属文件夹，不改变 `collectedAt`。
- 同一请求重复提交不生成重复收藏，也不改变首次收藏时间。
- 取消后再次收藏，会生成新的 `collectedAt`。

### 响应：200 OK

```json
{
  "articleId": "7c3db9d8-10a0-4a69-9f3c-2f1b1d0b7d22",
  "folderId": "cde78912-3456-4789-abcd-1234567890ab",
  "collectedAt": "2026-09-21T08:00:00Z"
}
```

该响应表示保存后的收藏状态。`folderId` 在默认收藏夹中为 `null`。此响应替代现有的 `MessageResponse`，实现时需同步使用响应内容的调用方。

## 4. 取消收藏

`DELETE /api/user/collections/{articleId}`

请求体：无。

删除当前用户对指定文章的收藏，与文章所在文件夹无关。未收藏或已经取消时也返回成功；文章已物理删除时，对应收藏由外键级联删除，取消收藏仍返回成功。`articleId` 格式不合法仍返回 `400`。

### 响应：200 OK

```json
{
  "message": "Article uncollected."
}
```

## 5. 创建文件夹

`POST /api/user/collection-folders`

### 请求

```json
{
  "name": "技术资料"
}
```

`name` 必填，校验规则见通用约定。同名文件夹已存在时返回 `409`。

### 响应：201 Created

```json
{
  "folderId": "cde78912-3456-4789-abcd-1234567890ab",
  "name": "技术资料",
  "createdAt": "2026-09-21T07:00:00Z",
  "updatedAt": "2026-09-21T07:00:00Z"
}
```

## 6. 编辑文件夹

`PUT /api/user/collection-folders/{folderId}`

### 请求

```json
{
  "name": "开发资料"
}
```

仅修改文件夹名称，`name` 必填。名称与当前用户的其他文件夹重复时返回 `409`；名称未变化时直接返回成功。

### 响应：200 OK

```json
{
  "folderId": "cde78912-3456-4789-abcd-1234567890ab",
  "name": "开发资料",
  "createdAt": "2026-09-21T07:00:00Z",
  "updatedAt": "2026-09-21T09:00:00Z"
}
```

`createdAt` 保持不变，名称实际变化时更新 `updatedAt`。文件夹中的收藏和收藏时间均不改变。

## 7. 删除文件夹

`DELETE /api/user/collection-folders/{folderId}`

请求体：无。

删除文件夹时，将其中收藏的 `folderId` 设为 `null`，移回默认收藏夹，保留原收藏时间。移动收藏与删除文件夹必须在同一事务中完成，不允许出现部分成功。文件夹不存在或不属于当前用户时返回 `404`。

### 响应：200 OK

```json
{
  "message": "Collection folder deleted."
}
```

## 8. 文件夹列表（Pageable）

`GET /api/user/collection-folders?page=0&size=20&sort=createdAt,desc`

只返回当前用户创建的文件夹，不包含默认收藏夹。支持 `page`、`size`、`sort`。

- 默认排序：`createdAt,desc`。
- 可排序字段：`name`、`createdAt`、`updatedAt`。
- `totalElements` 为当前用户自定义文件夹总数。

### 响应：200 OK

```json
{
  "content": [
    {
      "folderId": "cde78912-3456-4789-abcd-1234567890ab",
      "name": "开发资料",
      "createdAt": "2026-09-21T07:00:00Z",
      "updatedAt": "2026-09-21T09:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

## 9. 文件夹下收藏列表（Pageable）

`GET /api/user/collections?folderId=cde78912-3456-4789-abcd-1234567890ab&page=0&size=20&sort=collectedAt,desc`

在现有收藏列表接口上增加 `folderId` 筛选，不新增其他列表接口。

| `folderId` 查询参数 | 查询范围 |
|---|---|
| UUID 字符串 | 指定文件夹下的收藏 |
| `default` | 默认收藏夹，即 `folderId IS NULL` 的收藏 |
| 不传 | 当前用户的全部收藏，兼容现有调用 |

`default` 仅是此查询参数的保留值，不是实际文件夹 ID。移动到默认收藏夹使用 JSON `null`；不接受空字符串或字符串 `null`。

- 支持 `page`、`size`、`sort`。
- 默认排序：`collectedAt,desc`；可排序字段：`collectedAt`。
- `totalElements`、`totalPages` 均按当前筛选范围计算。
- 指定的文件夹存在但为空时返回空页；不存在或属于其他用户时返回 `404`。
- 每条收藏保留文章引用及收藏时间；文章存在但部分元信息缺失时，对应展示字段可为 `null`，不应导致整个列表报错。文章物理删除后，其收藏不再计入列表和总数。

### 响应：200 OK

```json
{
  "content": [
    {
      "articleId": "7c3db9d8-10a0-4a69-9f3c-2f1b1d0b7d22",
      "folderId": "cde78912-3456-4789-abcd-1234567890ab",
      "title": "PostgreSQL 索引使用指南",
      "feedTitle": "技术周刊",
      "thumbnail": "https://example.com/cover.jpg",
      "summary": "介绍常见索引的使用方式。",
      "collectedAt": "2026-09-21T08:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

## 10. 错误响应

沿用项目的 `ErrorResponse` 格式：

```json
{
  "timestamp": "2026-09-21T08:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Collection folder not found.",
  "path": "/api/user/collection-folders/cde78912-3456-4789-abcd-1234567890ab"
}
```

| 状态码 | 场景 |
|---|---|
| `400` | UUID 格式错误、文件夹名称不合法、分页或排序参数不合法 |
| `401` | 未登录或登录已失效 |
| `404` | 收藏时文章不存在，或指定文件夹不存在／不属于当前用户 |
| `409` | 创建或编辑文件夹时名称重复 |
| `500` | 服务端异常 |

重复收藏和重复取消收藏属于正常请求，不返回 `409` 或 `404`。删除不存在的文件夹仍按第 7 节返回 `404`。

## 11. PostgreSQL 表结构

新增两张表：文件夹表 `user_collection_folders` 和收藏表 `user_collections`。一条收藏最多属于一个文件夹，无需中间关联表。

### 文件夹表：user_collection_folders

| 字段 | 类型 | 可空 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | 否 | 自增主键，内部 `Long` ID |
| `uid` | `UUID` | 否 | 唯一对外标识，由应用生成，映射为接口 `folderId` |
| `user_id` | `BIGINT` | 否 | 关联 `users.id` |
| `name` | `VARCHAR(50)` | 否 | 文件夹名称，同一用户下不可重复 |
| `created_at` | `TIMESTAMPTZ` | 否 | 创建时间 |
| `updated_at` | `TIMESTAMPTZ` | 否 | 名称最后修改时间 |

### 收藏表：user_collections

| 字段 | 类型 | 可空 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | 否 | 自增主键，仅内部使用，不设置 UUID |
| `user_id` | `BIGINT` | 否 | 关联 `users.id` |
| `article_id` | `BIGINT` | 否 | 关联 `articles.id`，不保存文章 UUID |
| `folder_id` | `BIGINT` | 是 | 关联 `user_collection_folders.id`，`NULL` 表示默认收藏夹 |
| `collected_at` | `TIMESTAMPTZ` | 否 | 首次收藏时间，移动文件夹时不变 |

收藏表不增加备注、状态、删除标记或独立的对外标识。取消收藏直接删除记录；文件夹内收藏数量通过查询统计，不冗余存储。

### 建表 SQL

以下 SQL 依赖已有的 `users`、`articles` 表，已纳入 `V4__add_collections_and_likes.sql`，不代表目标数据库已经执行迁移。

```sql
CREATE TABLE user_collection_folders (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uid UUID NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_collection_folders_uid UNIQUE (uid),
    CONSTRAINT uq_collection_folders_user_name UNIQUE (user_id, name),
    CONSTRAINT uq_collection_folders_user_id UNIQUE (user_id, id),
    CONSTRAINT ck_collection_folders_name
        CHECK (name = btrim(name) AND char_length(name) > 0),
    CONSTRAINT fk_collection_folders_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE user_collections (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    folder_id BIGINT,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_collections_user_article UNIQUE (user_id, article_id),
    CONSTRAINT fk_user_collections_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_collections_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_collections_folder
        FOREIGN KEY (user_id, folder_id)
        REFERENCES user_collection_folders (user_id, id)
);

CREATE INDEX idx_collection_folders_user_created
    ON user_collection_folders (user_id, created_at DESC, id ASC);

CREATE INDEX idx_user_collections_user_collected
    ON user_collections (user_id, collected_at DESC, id ASC);

CREATE INDEX idx_user_collections_user_folder_collected
    ON user_collections (user_id, folder_id, collected_at DESC, id ASC);

CREATE INDEX idx_user_collections_article
    ON user_collections (article_id);
```

### 约束与操作规则

- `(user_id, article_id)` 唯一，保证同一用户不会重复收藏文章。
- `(user_id, name)` 唯一，保证同一用户不会创建同名文件夹；名称由应用去除首尾空白后入库。
- `(user_id, folder_id)` 复合外键保证收藏只能放进当前用户的文件夹；文件夹表的 `(user_id, id)` 唯一约束供该外键引用。`folder_id` 为 `NULL` 时允许保存在默认收藏夹。
- 文件夹 UUID 由应用使用 `UUID.randomUUID()` 生成，创建后不变；`updated_at` 由应用在名称实际变化时更新，SQL 默认值仅在插入时生效。
- 删除文件夹时，在同一事务内先将该用户、该文件夹的收藏 `folder_id` 更新为 `NULL`，再删除文件夹；不修改 `collected_at`。外键防止并发写入留下悬空关联，遇到并发冲突应回滚并重试整个操作。
- 删除用户时级联删除其文件夹与收藏；物理删除文章时级联删除对应收藏。
- 三个分页索引分别支持文件夹默认排序、全部收藏默认排序、指定／默认文件夹内收藏默认排序。`article_id` 索引用于按文章查找收藏及文章删除时的级联清理。唯一约束会自动创建对应索引，无需重复创建。

### 对外 UUID 与内部 Long ID 的转换

收藏请求中的 `articleId` 通过 `articles.uid` 查出 `articles.id`；`folderId` 通过当前用户和文件夹 `uid` 查出文件夹 `id`。之后保存、关联、移动、删除收藏均使用数字 ID。列表查询通过 `article_id = articles.id`、`folder_id = user_collection_folders.id` 关联，返回时读取对应 `uid`，映射为接口的 `articleId`、`folderId`。

例如文件夹记录的内部 `id = 12`、对外 `uid = cde78912-3456-4789-abcd-1234567890ab`，收藏表只存 `folder_id = 12`，接口只传 UUID。

现有项目中 `Article.id` 已为 Java `Long`，`User.id` 仍为 Java `Integer`。新表主键和关联字段统一为 `BIGINT`／`Long`；PostgreSQL 支持新表的 `BIGINT user_id` 外键引用现有 `INTEGER users.id`，服务入口将用户 ID 转为 `Long`。若后续将用户主键也统一为 `Long`，需同步迁移 `users.id`、相关引用列、实体及仓储等，本次不改动现有用户主键。

## 12. 历史数据迁移

- 历史收藏已经回填到 PostgreSQL `user_collections`，无法映射的用户、文章或时间会被记录并跳过。
- 旧收藏统一放入默认文件夹，即 `folder_id = NULL`，不创建额外文件夹。
- 推荐画像中的收藏读取 PostgreSQL。一次性迁移命令已经移除，当前服务不再执行重复回填。
