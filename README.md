# GuideNH

[简体中文](README_zh.md)

**GuideNH** is a port of GuideME to Minecraft **1.7.10** / Forge **10.13.4.1614**. It provides an in-game Markdown-driven guide system for GTNH-style modpacks.

## Features

* Full Markdown rendering (headings, paragraphs, lists, links, images, blockquotes, code, tables via GFM)
* Custom MDX-style tags: `<A>`, `<Box>`, `<Color>`, `<ItemLink>`, `<Recipe>`, `<Structure>`, `<ItemGrid>`, `<KeyBind>`, `<PlayerName>`, ...
* Multi-page guides organised by `_manifest.json`
* Multi-language pages with automatic fallback (`zh_cn` → `en_us`)
* Item index → hold-`G` on any item to jump straight to the relevant guide page (configurable, GuideME-style progress bar in tooltip)
* Multi-block structure preview (2.5D isometric MVP, item-icon based)
* F3+T resource reload picks up edited `.md` files
* Vanilla `GuiScreen`-based UI — no ModularUI / Blaze3D dependency

## Getting Started

```powershell
gradlew spotlessApply
gradlew build
gradlew runClient
```

In-game:

* `/give @s guidenh:guide` — get the demo guide book; right-click to open
* Hover any item with a registered guide entry, **hold G** for ~10 ticks to jump
* Press `F3+T` to hot-reload `.md` content during development

## Project Layout

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

## Demo Guide Resources

```
assets/guidenh/guides/guidenh/guidenh/
├── _manifest.json
├── en_us/{index,rendering,structure}.md
└── zh_cn/{index,rendering,structure}.md
```

## Adding Content

1. Create folder `assets/<ns>/guides/<owner>/<guide_id>/`
2. Add `_manifest.json`
3. Add `<lang>/<page>.md`
4. Register:

```java
Guide.builder(new ResourceLocation("yourmod", "main")).build();
```

## Extending

| Goal        | Entry point                         |
| ----------- | ----------------------------------- |
| Custom tag  | `TagCompiler` + `DefaultExtensions` |
| Layout node | `LytBlock` / `LytFlowContent`       |
| Rendering   | `MinecraftRenderContext`            |
| Hotkey      | `OpenGuideHotkey`                   |

## License

LGPL-3.0
