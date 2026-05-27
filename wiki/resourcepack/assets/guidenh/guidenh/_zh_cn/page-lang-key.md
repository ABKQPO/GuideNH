---
navigation:
  title: 页面 Lang Key
  parent: markdown.md
  position: 60
categories:
  - markdown
  - localization
---

# 页面 Lang Key 回退页

这是实际存在的 markdown 回退文件。

如果当前语言的 `.lang` 文件中存在 `guidenh.page.guidenh.guidenh.page-lang-key`，GuideNH 会在解析前先用该值
整体替换本页的 markdown 源文本。

如果该 key 缺失或为空，则会回退到这个文件本身的内容。
