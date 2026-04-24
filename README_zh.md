# GuideNH

**GuideNH** 是将 GuideME 移植到 Minecraft **1.7.10** / Forge **10.13.4.1614** 的 Mod，为 GTNH 风格整合包提供 Markdown 驱动的游戏内指南系统。

## 功能特性

* 完整 Markdown 渲染（标题、段落、列表、链接、图片、引用、代码块、GFM 表格等）
* 自定义 MDX 风格标签：`<A>` `<Box>` `<Color>` `<ItemLink>` `<Recipe>` `<Structure>` `<ItemGrid>` `<KeyBind>` `<PlayerName>`
* `_manifest.json` 组织多页面
* 多语言 + 自动回退（`zh_cn` → `en_us`）
* 物品索引 + **长按 G 跳转**
* 多方块结构预览（2.5D）
* `F3+T` 热重载 `.md`
* 基于 `GuiScreen`，不依赖 ModularUI / Blaze3D

## 快速开始

```powershell
gradlew spotlessApply
gradlew build
gradlew runClient
```

游戏内：

* `/give @s guidenh:guide`
* 悬停物品 **按住 G**
* `F3+T` 热重载

## 项目结构

```
com.hfstudio.guidenh
├── GuideNH
├── ClientProxy / CommonProxy
├── client.hotkey.OpenGuideHotkey
├── coremod / mixins
├── guide
│   ├── api
│   ├── compiler
│   ├── document
│   ├── indices
│   ├── navigation
│   ├── render
│   ├── style / color
│   ├── layout
│   ├── ui
│   └── internal
└── libs.{mdast,mdx,micromark,unist}
```

## 添加内容

1. 创建 `assets/<命名空间>/guides/<owner>/<guide_id>/`
2. 添加 `_manifest.json`
3. 添加 `<lang>/<page>.md`
4. 注册：

```java
Guide.builder(new ResourceLocation("yourmod", "main")).build();
```

## 扩展开发

| 目标    | 入口                       |
| ----- | ------------------------ |
| 自定义标签 | `TagCompiler`            |
| 布局节点  | `LytBlock`               |
| 渲染    | `MinecraftRenderContext` |
| 热键    | `OpenGuideHotkey`        |

## 许可证

LGPL-3.0
