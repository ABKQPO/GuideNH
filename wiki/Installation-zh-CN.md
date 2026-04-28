[English](Installation)

# 安装

本页介绍 GuideNH 内置教程指南在仓库中的布局，以及对应的开发流程。

## 运行时指南源目录

运行时指南内容应从 `wiki/resourcepack/` 编写，而不是从 `src/main/resources/assets/...` 编写。

当前内置示例指南直接位于：

- `wiki/resourcepack/assets/guidenh/guidenh/`

示例指南内容继续放在语言目录中，例如：

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/`
- `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/`

## 构建输出

在 `processResources` 阶段，项目会把 `wiki/resourcepack/assets/` 下的全部内容直接复制到模组 jar 的资源树中。因此，内置指南会作为普通模组资源打包进 jar，例如：

- `assets/guidenh/guidenh/_en_us/index.md`
- `assets/guidenh/guidenh/_zh_cn/index.md`
- `assets/guidenh/guidenh/assets/example_structure.snbt`

## 开发循环

1. 在 `wiki/resourcepack/` 下编辑运行时页面和资源。
2. 在 `wiki/*.md` 下编辑面向人的说明文档。
3. 如果想快速迭代指南内容，请使用 [实时预览](Live-Preview-zh-CN) 中的工作流。
4. 当你没有使用实时预览，或修改了代码/构建逻辑时，再重新构建资源或重新启动客户端。

## 不要这样做

- 不要把游戏内 MDX 标签直接写进 GitHub Wiki 页面，除非它们位于代码围栏中。
- 不要直接在 `src/main/resources/assets/...` 下新增内置指南源文件；应从 `wiki/resourcepack/assets/...` 编写。
- 不要重新引入 `_manifest.json`，也不要再把运行时指南页面包成资源包 zip；当前加载器已经会直接扫描资源树。

## 相关页面

- [快速开始](Getting-Started-zh-CN)
- [实时预览](Live-Preview-zh-CN)
- [指南页面格式](Guide-Page-Format-zh-CN)
- [示例](Examples-zh-CN)
