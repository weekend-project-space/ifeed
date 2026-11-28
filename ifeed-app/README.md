# ifeed-app

Controller 的业务方法和查询组装封装位于 `ifeed-application`；MCP 接入位于 `ifeed-mcp`。本模块负责启动、网页 HTTP 接口、安全过滤链及部署资源。

这是整个 iFeed 应用的启动部分。

它把其他功能组合起来，启动网站和后端接口，并读取配置、登录安全设置和数据库升级脚本。
