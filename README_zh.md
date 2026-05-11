<p align="center">
    <img width="690" src="./src/main/resources/assets/logo.png" alt="GuideNH" style="image-rendering: pixelated;">
</p>
<hr>
<p align="center">
    <img src="https://img.shields.io/badge/Available%20for-MC%201.7.10-c70039" alt="支持的 Minecraft 版本">
    <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f6a21a" alt="支持的 Forge 版本">
    <img src="https://img.shields.io/badge/license-LGPL--3.0-green" alt="许可证">
</p>

<p align="center">
    <a href="README.md">English</a> |
    <a href="README_zh.md">简体中文</a>
</p>

## **简介**

* <span style="color: #ff6600;">GuideNH</span> 是面向 Minecraft **1.7.10** / Forge **10.13.4.1614** 的游戏内指南框架。
* 它移植并扩展了 GuideME 风格的 Markdown 指南系统，适合 GTNH 时代的整合包与大型模组。
* 它允许作者直接用资源包结构中的 Markdown 文件编写复杂指南书。

## **功能**

* 支持带 YAML 表头的 Markdown 页面、导航元数据、分类、锚点、表格、脚注、Mermaid、LaTeX、图表与文本高亮。
* 支持 `<ItemLink>`、`<ItemImage>`、`<Recipe>`、`<GameScene>`、`<BlockStats>`、`<Tooltip>`、`<KeyBind>`、`<PlayerName>` 等 MDX 风格运行时标签。
* 支持交互式 3D GameScene 预览，包括方块/实体放置、StructureLib 导入、Ponder 播放、层滑条、网格按钮、注解与方块统计表。
* 支持游戏内指南编辑模式，包括编辑/预览分屏、工具栏操作、短延迟自动保存、外部变更处理与资源包页面创建。
* 支持多语言指南目录与回退、物品索引跳转、搜索、服务端集成和资源重载。

## **作者**

- 程序：`HFstudio`
- 上游灵感：[GuideME](https://github.com/AppliedEnergistics/GuideME)

## **许可证**

- 代码：[LGPL-3.0](LICENSE.txt)
- 内置第三方库遵循各自的许可证。

## **Wiki**

* [English Wiki](wiki/Home-en-US.md)
* [中文 Wiki](wiki/Home-zh-CN.md)
* [结构导出](wiki/Structure-Export-zh-CN.md)
* [运行时示例资源包](wiki/resourcepack)

## **快速开始**

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat build
.\gradlew.bat runClient
```

进入游戏后：

* 使用 `/give Developer guidenh:guide` 获取演示指南书。
* 鼠标悬停在已建立索引的物品上并按住 `G`，可以跳转到对应指南页面。
* 按 `F3+T` 可以重新加载已编辑的指南资源。

## **编写示例**

```md
---
navigation:
  title: 机器
  parent: index.md
author: GuideNH
date: 2026-05-10
---

# 机器

按 <KeyBind action="key.attack" /> 进行交互。

<GameScene width="220" height="150" interactive={true}>
  <Block id="minecraft:furnace" />
  <BlockStats corner="topRight" maxWidth="120" maxHeight="72" />
</GameScene>
```

## **开发**

### **指南目录**

```text
assets/<modid>/guidenh/
|-- assets/
|   `-- shared_structure.snbt
|-- _en_us/
|   |-- index.md
|   `-- machines.md
`-- _zh_cn/
    |-- index.md
    `-- machines.md
```

### **注册指南**

```java
Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

### **验证**

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat build --rerun-tasks
```

## **致谢**

GuideNH 借鉴了 [GuideME](https://github.com/AppliedEnergistics/GuideME) 的设计思路，GuideME 使用 LGPL-3.0 分发。
本项目还使用了 SnakeYAML、Apache Lucene、Apache Commons Lang、FlatBuffers Java、JLaTeXMath 等开源库。
