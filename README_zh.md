# GuideNH

[English](README.md)

**GuideNH** 是将 GuideME 移植到 Minecraft **1.7.10** / Forge **10.13.4.1614** 的版本，为 GTNH 风格整合包提供一个游戏内、由 Markdown 驱动的指南系统。

## 功能特性

* 完整支持 Markdown 渲染，包括标题、段落、列表、链接、图片、引用、代码块，以及通过 GFM 提供的表格等内容
* 支持自定义 MDX 风格标签，例如 `<A>`、`<Box>`、`<Color>`、`<ItemLink>`、`<Recipe>`、`<Structure>`、`<ItemGrid>`、`<KeyBind>` 和 `<PlayerName>`
* 可直接从资源树中发现并加载多页指南
* 支持多语言页面，并自动进行回退（`zh_cn` -> `en_us`）
* 支持物品索引：对着已注册指南条目的物品按住 `G`，即可直接跳转到对应页面
* 支持多方块结构预览（当前为基于物品图标的 2.5D 等轴测 MVP）
* 支持通过 `F3+T` 重载资源后立即读取修改过的 `.md` 文件
* 基于原版 `GuiScreen` 的界面实现，不依赖 ModularUI 或 Blaze3D

## 快速开始

```powershell
gradlew spotlessApply
gradlew build
gradlew runClient
```

进入游戏后：

* 使用 `/give @s guidenh:guide` 获取演示指南书
* 将鼠标悬停在已注册指南条目的物品上，按住 `G` 约 10 tick 以跳转到对应指南
* 在开发过程中按 `F3+T` 可热重载指南内容

## 示例指南资源

```text
wiki/resourcepack/assets/guidenh/guidenh/
|-- assets/
|   `-- example_structure.snbt
|-- _en_us/
|   |-- index.md
|   |-- rendering.md
|   `-- structure.md
`-- _zh_cn/
    |-- index.md
    |-- rendering.md
    `-- structure.md
```

GuideNH 现在会直接从 `assets/<modid>/guidenh/_<lang>/...` 中发现指南页面，因此已经不再需要 `_manifest.json`。

## 添加内容

1. 创建 `assets/<modid>/guidenh/_<lang>/`
2. 添加一个或多个 `.md` 页面，例如 `index.md`
3. 将页面私有资源放在页面旁边，或将共享资源放在 `assets/<modid>/guidenh/assets/` 下
4. 如果你需要显式 API 控制，可以在代码中注册 Guide；否则也可以直接依赖自动发现的 `<modid>:guidenh` 指南树

```java
Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

## 扩展点

| 目标 | 入口 |
| --- | --- |
| 自定义标签 | `TagCompiler` + `DefaultExtensions` |
| 布局节点 | `LytBlock` / `LytFlowContent` |
| 渲染 | `MinecraftRenderContext` |
| 热键 | `OpenGuideHotkey` |

## 许可证

LGPL-3.0
