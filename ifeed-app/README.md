# ifeed-app

Controller 的业务方法和查询组装封装位于 `ifeed-application`；MCP 接入位于 `ifeed-mcp`。本模块负责启动、网页 HTTP 接口、安全过滤链及部署资源。

这是整个 iFeed 应用的启动部分。

它把其他功能组合起来，启动网站和后端接口，并读取配置、登录安全设置和数据库升级脚本。

升级时由 Flyway 执行 `V13__remove_mix_feeds.sql`，删除历史 MixFeed 订阅记录和 `mix_feeds` 表。普通 RSS 订阅、文章及用户收藏和喜欢数据保留。
