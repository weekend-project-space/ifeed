# 前端国际化

支持 `zh-CN` 和 `en`。首次使用跟随浏览器语言，手动切换后保存到
`localStorage` 的 `ifeed-locale`，刷新后继续使用该语言。

## 文件约定

- `locales/{locale}/common.ts`：共享按钮、状态、校验和通用组件文案。
- `locales/{locale}/mainLayout.ts`：全局导航与搜索入口文案。
- `locales/{locale}/topicTrackingPage.ts` 等：按页面名称拆分，文件名使用 lowerCamelCase。
- 页面专属子组件使用对应组件名称，例如 `subscriptionsAddOpml.ts`。
- 新模块需要在两种语言的 `index.ts` 中注册，保持键名和插值参数一致。

## 使用约定

在 Vue 组件中使用 `useI18n().t`；在 store、工具函数和渲染函数中使用
`@/i18n` 导出的 `translate`。`schema.ts` 提供词条键名的类型检查。

```ts
import { translate as tr } from '@/i18n';

tr('common.articleCount', { count: 2 });
```

数组、标签页、页面标题等派生文案通过 `computed` 或 getter 生成，避免在
初始化时固定翻译。包含链接或强调文字的完整句子使用
`<i18n-t scope="global">` 和命名插槽，不拼接翻译片段。

文章正文、订阅源名称、文件夹名称及用户输入保留原文。后端返回的消息保持
原样，前端兜底提示使用词条。日期使用当前语言的 `Intl` 格式。

提交前运行 `npm run check:i18n`，检查重复键、双语键名、插值参数、消息
语法及代码中的静态引用，然后运行 `npm run build`。
