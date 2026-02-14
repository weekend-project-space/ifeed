# 信息雷达 API（滚动主题快照）

本模块用于“兴趣资讯雷达”：后端定时（滚动）生成一批当下主题（AI 命名+描述），前端分页浏览主题；点进主题后分页浏览相关文章。

## 通用约定：PageResponse（带 meta）

分页响应采用**精简结构**，保留必要分页字段，并额外增加 `meta` 承载快照/窗口等元信息。

- `content`: 当前页数据
- `page`: 当前页码（从 0 开始）
- `size`: 每页大小
- `totalPages`, `totalElements`
- `meta`: 非分页字段（如 `snapshotId`、`generatedAt`、`windowHours`）

> 说明：这是“简化版 Page + meta”的自定义响应体，减少返回体体积。

## 1) 主题列表（Digest）

### GET `/api/user/radar/digest`

按滚动窗口返回主题列表（分页）。默认不返回主题下 items，仅返回 `articleCount` 等统计。

#### Auth

- 可匿名：返回全局主题（不个性化）
- 已登录：主题排序可做个性化（收藏/赞踩/停留时长）

#### Query Params

- `windowHours` (optional, default `24`, range `6..72`): 滚动窗口
- `snapshotId` (optional):
  - 不传：返回“最新快照”的结果，并在 `meta.snapshotId` 返回该快照 ID
  - 传入：基于该快照分页（保证翻页一致）
- `page` (optional, default `0`)
- `size` (optional, default `6`, range `1..20`)
- `sort` (optional): 例如 `score,desc`、`updatedAt,desc`（也可后端限制为枚举）
> 注意：当前实现会以服务端“最新快照”为准返回 `meta.snapshotId`；如需翻页一致性，请携带该 `snapshotId` 继续翻页。

#### Response 200

```json
{
  "content": [
    {
      "topicId": "d9a8b6f0-7e3a-4b0d-9c2c-1b2b9e2e6b3a",
      "title": "AI 芯片竞争升温",
      "description": "围绕新一代 GPU/NPU 发布与算力价格的讨论集中爆发。",
      "createdAt": "2026-02-13T10:00:00Z",
      "updatedAt": "2026-02-13T19:00:00Z",
      "articleCount": 42,
      "topKeywords": ["GPU", "NPU", "推理", "算力", "价格"]
    }
  ],
  "totalPages": 3,
  "totalElements": 14,
  "size": 6,
  "page": 0,
  "meta": {
    "snapshotId": "radar_20260213T190000Z_u123_w24h",
    "generatedAt": "2026-02-13T19:00:00Z",
    "windowHours": 24
  }
}
```

#### 翻页示例

- 第一页（拿到 `meta.snapshotId`）
  - `GET /api/user/radar/digest?windowHours=24&page=0&size=6`
- 第二页（用同快照继续翻页）
  - `GET /api/user/radar/digest?snapshotId=radar_20260213T190000Z_u123_w24h&page=1&size=6`

#### Status Codes

- `200` OK
- `400` 参数非法
- `410` 快照过期/不存在（建议错误码：`SNAPSHOT_EXPIRED`）
- `500` 服务器错误

## 2) 主题详情（文章分页）

### GET `/api/user/radar/topics/{topicId}`

返回指定主题下文章列表（分页），返回体同样是 PageResponse，并在 `meta.topic` 里带上主题信息。建议带上 `snapshotId` 以保证与 Digest 一致。

#### Path Params

- `topicId`: UUID（来自 `/api/user/radar/digest` 的 `content[].topicId`）

#### Query Params

- `snapshotId` (required, 推荐必填): 快照 ID
- `page` (optional, default `0`)
- `size` (optional, default `20`, range `1..50`)
- `sort` (optional): 例如 `score,desc` 或 `publishedAt,desc`
- `includeContent` (optional, default `false`): 是否返回正文（默认只返回摘要字段）

#### Response 200

```json
{
  "content": [
    {
      "articleId": "7c3db9d8-10a0-4a69-9f3c-2f1b1d0b7d22",
      "title": "某厂新卡发布后，推理成本再降",
      "summary": "文章总结了新卡的定价、能效与推理场景的变化……",
      "thumbnail": "https://example.com/img.jpg",
      "feedTitle": "Tech Source",
      "publishedAt": "2026-02-13T16:12:00Z",
      "relativeTime": "3 hours ago",
      "tags": ["ai", "hardware"],
      "score": 0.87
    }
  ],
  "totalPages": 3,
  "totalElements": 42,
  "size": 20,
  "page": 0,
  "meta": {
    "snapshotId": "radar_20260213T190000Z_u123_w24h",
    "includeContent": false,
    "topic": {
      "topicId": "d9a8b6f0-7e3a-4b0d-9c2c-1b2b9e2e6b3a",
      "title": "AI 芯片竞争升温",
      "description": "围绕新一代 GPU/NPU 发布与算力价格的讨论集中爆发。",
      "createdAt": "2026-02-13T10:00:00Z",
      "updatedAt": "2026-02-13T19:00:00Z",
      "articleCount": 42,
      "topKeywords": ["GPU", "NPU", "推理", "算力", "价格"]
    }
  }
}
```

#### Status Codes

- `200` OK
- `400` 参数非法
- `404` 主题不存在（在该 snapshot 下）
- `410` 快照过期（建议错误码：`SNAPSHOT_EXPIRED`）
- `500` 服务器错误
