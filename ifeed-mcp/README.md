# ifeed-mcp

MCP 协议接入模块，作为普通 JAR 由 `ifeed-app` 依赖，与网页 API 同进程、同端口部署。

- `org.bitmagic.ifeed.mcp.McpController`：无状态 Streamable HTTP 入口 `/api/mcp`。
- `McpTools`：12 个工具的注册、Schema、参数和结果适配，调用共享应用查询服务及已有领域服务。
- `McpTokenController`：网页凭证管理接口 `/api/user/mcp-tokens`；签发、校验和撤销能力由身份模块提供。

本模块不依赖 `ifeed-app`，不调用网页 Controller，不通过 HTTP 请求自身。共享查询使用 `ifeed-application`，收藏、喜欢及收藏夹直接复用领域服务，以保留用户隔离、收藏夹归属校验及幂等行为。

工具名称、入参和对外接口见 [JWT 与 MCP 接入](../doc/jwt-mcp.md)。

独立构建和协议测试：

```sh
mvn -pl ifeed-mcp -am test
```

本模块测试覆盖传输、Schema、工具发现、分页、用户身份传递和工具结果。真实 JWT、CORS 和 WEB/MCP 权限隔离测试由 `ifeed-app` 承担。
