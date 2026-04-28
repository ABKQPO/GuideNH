[English](FAQ)

# 常见问题

## 我应该在哪里编辑内置运行时指南？

在 `wiki/resourcepack/` 下。旧的 `src/main/resources/assets/...` 松散源树已经不再是运行时指南页面的编写真源。

## 为什么这个仓库同时有 wiki 页面和运行时 Markdown 页面？

`wiki/*.md` 下的文件是面向人的 GitHub Wiki 文档。`wiki/resourcepack/...` 下的文件则是 GuideNH 游戏内实际消费的运行时指南页面。

## 两边可以复用同一份 Markdown 吗？

不能直接复用。运行时指南 Markdown 可能包含 GitHub Wiki 无法理解的 GuideNH 专用 MDX 标签。应把运行时语法保留在运行时指南树中，在 wiki 中只通过代码围栏展示它们。

## 我怎样才能快速预览指南改动？

使用 [实时预览](Live-Preview-zh-CN) 中说明的专用 live preview 运行任务。在本仓库里，`runGuide`
会在启动时自动接入内置示例指南源目录，并直接打开指南。
