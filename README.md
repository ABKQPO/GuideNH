<p align="center">
    <img width="690" src="./src/main/resources/assets/logo.png" alt="GuideNH" style="image-rendering: pixelated;">
</p>
<hr>
<p align="center">
    <img src="https://img.shields.io/badge/Available%20for-MC%201.7.10-c70039" alt="Supported Minecraft Version">
    <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f6a21a" alt="Supported Forge Version">
    <img src="https://img.shields.io/badge/license-LGPL--3.0-green" alt="License">
</p>

<p align="center">
    <a href="README.md">English</a> |
    <a href="README_zh.md">简体中文</a>
</p>

## **Introduction**

* <span style="color: #ff6600;">GuideNH</span> is an in-game guide framework for Minecraft **1.7.10** / Forge **10.13.4.1614**.
* It ports and extends GuideME-style Markdown documentation for GTNH-era modpacks.
* It is designed for authoring rich guide books directly from resource-pack style Markdown files.

## **Features**

* Markdown pages with YAML frontmatter, navigation metadata, categories, anchors, tables, footnotes, Mermaid, LaTeX, charts, and highlighted text.
* MDX-style runtime tags such as `<ItemLink>`, `<ItemImage>`, `<Recipe>`, `<GameScene>`, `<ContentTabs>`, `<BlockStats>`, `<Tooltip>`, `<KeyBind>`, and `<PlayerName>`.
* Interactive 3D GameScene previews with block/entity placement, StructureLib import, Ponder playback, layer sliders, grid controls, annotations, and block statistics.
* Live guide editing mode with split editor/preview, toolbar actions, debounced saving, external-change handling, and resource-pack page creation.
* Multi-language guide folders with fallback, item index navigation, search, server integration, and resource reload support.
* System emoji rendering in guide text and the exported static site, using the operating system emoji font when available.

## **Authors**

- Programmer: `HFstudio`
- Upstream inspiration: [GuideME](https://github.com/AppliedEnergistics/GuideME)

## **License**

- Code: [LGPL-3.0](LICENSE.txt)
- Bundled third-party libraries keep their own licenses.

## **Wiki**

* [English Wiki](wiki/Home-en-US.md)
* [中文 Wiki](wiki/Home-zh-CN.md)
* [Structure Export](wiki/Structure-Export.md)
* [Runtime example resource pack](wiki/resourcepack)

## **Quick Start**

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat build
.\gradlew.bat runClient
```

In game:

* Use `/give Developer guidenh:guide` to get the demo guide book.
* Hold `G` while hovering an indexed item to jump to its guide entry.
* Press `F3+T` to reload edited guide resources.

## **Authoring Example**

```md
---
navigation:
  title: Machines
  parent: index.md
author: GuideNH
date: 2026-05-10
---

# Machines

Press <KeyBind action="key.attack" /> to interact.

<GameScene width="220" height="150" interactive={true}>
  <Block id="minecraft:furnace" />
  <BlockStats corner="topRight" maxWidth="120" maxHeight="72" />
</GameScene>
```

## **Develop**

### **Guide Folder**

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

### **Register A Guide**

```java
Guide.builder(new ResourceLocation("yourmod", "guidenh")).build();
```

### **Verification**

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat build --rerun-tasks
```

## **Credits**

Thanks to persephone for providing the icon textures!

Thanks to the following developers for their contributions to GuideNH:

<a href="https://github.com/ABKQPO/GuideNH/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ABKQPO/GuideNH&max=1000" alt="contributors" />
</a>

GuideNH is based on ideas from [GuideME](https://github.com/AppliedEnergistics/GuideME), distributed under LGPL-3.0.
It also uses open-source libraries including SnakeYAML, Apache Lucene, Apache Commons Lang, FlatBuffers Java, and JLaTeXMath.
