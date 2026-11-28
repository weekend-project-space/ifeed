# JWT 与 MCP 接入

网页和 MCP 使用 `Authorization: Bearer <token>`。网页登录、注册和 LinuxDo 登录返回的 `token` 是 12 小时有效的 HS256 JWT；原有前端请求头和响应字段保持兼容。网页退出会清除浏览器保存的 JWT，服务端不保存网页 token 黑名单，因此已复制的网页 token 会在到期前继续有效。

生产部署必须为所有应用实例配置相同的 `APP_JWT_SECRET`，至少 32 个 UTF-8 字节。网页有效期可用 `APP_JWT_WEB_TTL` 配置，默认 `PT12H`。旧的不透明 session token 不再作为 API 凭证，部署后用户需要重新登录；旧 `user_sessions` 表暂时保留，后续确认回滚需求后再迁移删除。

未携带凭证时，允许匿名调用明确列出的公共 GET 查询接口，以及登录、注册、退出和 LinuxDo 授权入口。个人订阅、收藏、喜欢、凭证管理等业务接口要求 WEB 登录凭证；公共查询路径下的写操作也要求登录。请求中携带无效、过期或格式错误的 Bearer 凭证时返回 401，并提供 `WWW-Authenticate` 响应头，不降级为匿名请求。

MCP 凭证仅允许访问 `/api/mcp`。`/actuator/health` 及其子路径的 GET 请求可匿名访问，其他 Actuator 接口同时要求 WEB 凭证和管理员角色。浏览器跨域请求复用应用已有的 CORS 来源白名单，合法预检无需登录；实际业务请求仍执行认证和授权。

## 创建 MCP 凭证

使用网页登录 JWT 调用凭证接口。完整 token 只在创建时返回，数据库仅保存凭证 ID、名称和到期时间；之后可列出元数据或单独撤销凭证。

```http
POST /api/user/mcp-tokens
Authorization: Bearer <网页 JWT>
Content-Type: application/json

{"name":"Claude Desktop"}
```

网页 token 有效期默认 12 小时，MCP token 默认 30 天；分别可通过 `APP_JWT_WEB_TTL` 和 `APP_JWT_MCP_TTL` 调整。列表接口为 `GET /api/user/mcp-tokens`，撤销接口为 `DELETE /api/user/mcp-tokens/{id}`。MCP token 只允许访问 `/api/mcp`，不能用于普通业务 API 或创建、撤销其他凭证。

客户端通过 Streamable HTTP 连接 `/api/mcp`，例如：

```json
{
  "mcpServers": {
    "ifeed": {
      "url": "https://www.ifeed.cc/api/mcp",
      "headers": {
        "Authorization": "Bearer <MCP token>"
      }
    }
  }
}
```

## 可用工具

协议入口、工具和凭证管理接口位于独立的 `ifeed-mcp` Maven 模块，由 `ifeed-app` 引入并统一部署。工具适配器直接调用 `ifeed-application` 的共享查询服务及已有领域服务，不依赖网页 Controller。网页与 MCP 共用文章详情、搜索、推荐、订阅等查询实现，以及订阅、退订的状态变更方法。当前共 14 个工具，其中 10 个只读、4 个写操作。

所有个人数据都取自当前 MCP 凭证对应的用户，不接受 `userId` 参数。支持分页的工具统一使用 `page`（从 0 开始，最大 1000）和 `size`（默认 10，最大 50）；分页结果包含 `content`、`page`、`size`、`totalPages`、`totalElements`、`meta`。

| 工具 | 用途 | 关键参数 |
| --- | --- | --- |
| `search_articles` | 搜索文章 | `query`，可选 `type: keyword/semantic`、`source: owner/global`、分页参数 |
| `get_article` | 读取正文及当前用户的收藏、喜欢状态 | `articleId` |
| `get_recommendations` | 获取当前用户的个性化文章推荐 | 分页参数 |
| `get_radar_digest` | 获取系统默认时间窗口下最新热榜的前 10 条 | 无参数 |
| `get_radar_topic` | 查看热点下的文章 | `snapshotId`、`topicId`，可选 `includeContent`（默认 false） |
| `list_my_subscriptions` | 查看自己订阅的 RSS 源 | 分页参数 |
| `subscribe_feed` | 添加或恢复自己的 RSS 订阅 | `feedUrl` 或 `feedId`，必须且只能提供一个 |
| `unsubscribe_feed` | 取消自己的 RSS 订阅 | `feedId` |
| `list_articles` | 查看全部订阅或按源、标签、分类筛选文章 | 可选 `source`、`feedId`、`tags`、`category`、分页参数 |
| `list_my_collections` | 查看自己的收藏 | 可选 `folderId`（UUID 或 `default`）、分页参数 |
| `list_my_likes` | 查看自己喜欢的文章 | 分页参数 |
| `list_collection_folders` | 查看自己的收藏夹 | 分页参数 |
| `set_article_collection` | 收藏、取消收藏或移动收藏 | `articleId`、`collected`，可选 `folderId` |
| `set_article_like` | 喜欢或取消喜欢 | `articleId`、`liked` |

`list_articles` 默认 `source=owner`，读取当前用户全部订阅；显式传 `global` 可读全站。`search_articles` 为兼容已有调用仍默认 `source=global`，搜索自己的订阅需显式传 `owner`。`search_articles` 只接收 `query`、`type`、`source`、`page` 和 `size`。`list_articles` 的 `tags` 是逗号分隔的标签字符串，`category` 是分类标识；指定 `feedId` 时沿用网页的公开源内查询规则。文章查询不会自动写阅读历史或更新已读状态。

RSS 管理流程：先调用 `list_my_subscriptions` 获取已有源的 `feedId`、地址与状态。用户提供已知 RSS/Atom 地址时，可直接调用 `subscribe_feed`；这两个写工具不提供源发现、话题配置或批量导入功能。

```json
{"feedUrl":"https://example.com/feed.xml"}
```

也可使用 `{"feedId":"源 UUID"}` 订阅一个已有源；退订调用 `unsubscribe_feed` 并传同一个源 UUID。ID 取自订阅列表、文章的 `feedId` 等源信息，不接受内部整数 ID。工具不接受 `userId`，始终操作当前凭证所属账户。

订阅和退订返回 `message`、`feedId`、`title`、`url`、`subscribed`、`changed`、`fetchStatus`、`lastFetched`、`lastUpdated` 和 `fetchError`。重复添加返回 `subscribed=true, changed=false`，不额外占用额度；已取消的订阅可在额度允许时恢复。重复退订保持 `subscribed=false, changed=false`。退订只停用当前用户的订阅关系，保留源、文章、收藏和喜欢。

`fetchStatus` 是源最近的抓取状态（`PENDING`、`SUCCEEDED`、`FAILED`），历史数据也可能为空。订阅结果不代表首次抓取已经完成；新源一般返回 `PENDING` 且 `lastFetched=null`，之后可查询文章列表。已有源可能已经有抓取结果或抓取错误。

阅读流程：无参数调用 `get_radar_digest`（参数对象为空 `{}`），取返回的 `meta.snapshotId` 和文章所属热点的 `topicId` 调用 `get_radar_topic`；详情文章翻页时沿用同一个 `snapshotId`；热榜列表工具不接受快照、窗口或分页参数。也可先调用 `list_my_subscriptions` 获取 `feedId`，再调用 `list_articles` 并传入该 `feedId`，查询 RSS 源内文章；不传 `feedId` 则查询全部订阅文章。

订阅、退订、收藏和喜欢是写操作，只应在用户明确要求时调用。其余工具为只读。MCP 凭证现在也能管理自己账户的收藏和喜欢，已有未过期凭证同样适用。需要停用某个客户端时在网页撤销对应凭证。

```json
{"articleId":"从查询结果中取得的文章 UUID", "collected":true}
```

`collected` 为 `true` 时收藏文章，为 `false` 时取消收藏；`liked` 同理。重复发送相同的状态不会反转操作或重复创建记录。`folderId` 不传时保留已收藏文章的原收藏夹，首次收藏则使用默认收藏夹；显式传 `null` 表示移到默认收藏夹，传 UUID 时只能指定自己已有的收藏夹（先调用 `list_collection_folders`）。取消收藏时不能传 `folderId`。操作返回最终状态，也可通过 `get_article` 或个人列表查询结果。

MCP 请求体限制为 1 MiB；若带 `Origin`，必须与访问地址同源。部署代理需要将 `/api/mcp` 和 MCP 协议请求头透传到应用。
