# ifeed-application

Controller 与 MCP 共用的方法封装层。将原 Controller 中的查询组合、参数规则和结果组装提取成服务，两个入口调用相同的方法。

本模块中的封装：

- `application.article`：文章详情和列表、推荐结果及个人状态、文章增强操作的调用。
- `application.search.ArticleSearchQueryService`：搜索模式选择、参数规则、结果及个人状态组装。
- `application.subscription`：订阅列表和阅读状态组装，以及原订阅接口方法的封装。
- `application.feed`：RSS/混合源详情、源搜索与混合源接口方法的封装。
- `application.radar.RadarQueryApplicationService`：统一热点查询参数规则并调用原 `RadarQueryService`。
- `application.user.UserPlanApplicationService`：原用户套餐更新方法的封装。

原有推荐引擎、检索实现、话题追踪、采集和定时任务保留在原模块；模型、仓储及领域服务不迁移。现有写操作的事务和数据归属校验仍由原服务负责。

应用服务返回 DTO、分页或领域结果，不返回 `ResponseEntity`，不依赖 Controller 或 MCP 协议类型。网页 Controller 负责 HTTP 参数和响应，`ifeed-mcp` 负责工具 Schema、参数及协议结果适配。

共享身份类型属于 `ifeed-identity` 的 `org.bitmagic.ifeed.security`。应用方法接收入口提供的用户 ID；MCP 工具参数不能指定其他用户。

```sh
mvn -pl ifeed-application -am test
```

完整网页与认证集成测试保留在 `ifeed-app`，MCP 协议测试位于 `ifeed-mcp`。
