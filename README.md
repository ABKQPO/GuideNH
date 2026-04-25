# GuideNH

[简体中文](README_zh.md)

**GuideNH** is a port of GuideME to Minecraft **1.7.10** / Forge **10.13.4.1614**. It provides an in-game Markdown-driven guide system for GTNH-style modpacks.

## Features

* Full Markdown rendering (headings, paragraphs, lists, links, images, blockquotes, code, tables via GFM)
* Custom MDX-style tags such as `<A>`, `<Box>`, `<Color>`, `<ItemLink>`, `<Recipe>`, `<Structure>`, `<ItemGrid>`, `<KeyBind>`, and `<PlayerName>`
* Multi-page guides discovered directly from the resource tree
* Multi-language pages with automatic fallback (`zh_cn` -> `en_us`)
* Item index support: hold `G` on any item to jump straight to the relevant guide page
* Multi-block structure preview (2.5D isometric MVP, item-icon based)
* F3+T resource reload picks up edited `.md` files
* Vanilla `GuiScreen`-based UI with no ModularUI or Blaze3D dependency

## Getting Started

```powershell
gradlew spotlessApply
gradlew build
gradlew runClient
```

In-game:

* `/give @s guidenh:guide` to get the demo guide book
* Hover any item with a registered guide entry and hold `G` for about 10 ticks to jump
* Press `F3+T` to hot-reload guide content during development

## Demo Guide Resources

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

GuideNH now discovers guide pages directly from `assets/<modid>/guidenh/_<lang>/...`, so `_manifest.json` is no longer required.

## Adding Content

1. Create `assets/<modid>/guidenh/_<lang>/`
2. Add one or more `.md` pages such as `index.md`
3. Add page-local assets beside the page, or shared assets under `assets/<modid>/guidenh/assets/`
4. Register a guide in code when you need explicit API control, or rely on the auto-discovered `<modid>:guidenh` guide tree

```java
Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

## Extending

| Goal | Entry point |
| --- | --- |
| Custom tag | `TagCompiler` + `DefaultExtensions` |
| Layout node | `LytBlock` / `LytFlowContent` |
| Rendering | `MinecraftRenderContext` |
| Hotkey | `OpenGuideHotkey` |

## License

LGPL-3.0
