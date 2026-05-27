---
navigation:
  title: Navigation & Index
  parent: index.md
  position: 195
  recommend: 5
categories:
  - markdown
---

# Navigation & Content Index

GuideNH uses YAML frontmatter to declare navigation structure — no `index.md` hardcoding, no manifest files. Every page controls where it appears in the sidebar through its own frontmatter.

When nested pages are expanded in the in-game sidebar, every expanded ancestor that still owns
visible descendants stays pinned at the top. Multiple ancestor levels can stack there at once,
making deep trees easier to track while scrolling.

## Quick Example

```yaml
---
navigation:
  title: Advanced IO Bus
  parent: aae_intro/aae_intro-index.md
  icon: advanced_ae:advanced_io_bus_part
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
---
```

## Navigation Fields

### `navigation.title`

Display title in the sidebar. Required for the page to appear in navigation.

### `navigation.parent`

Page ID of the parent node. A relative path (e.g., `getting-started.md`) resolves against the page's own namespace. Omit to make this page a top-level root node.

```yaml
# Root node (no parent)
navigation:
  title: Getting Started

# Child of another page
navigation:
  title: Advanced IO Bus
  parent: aae_intro/aae_intro-index.md

# Explicit namespace, rooted path
navigation:
  title: GregTech Integration
  parent: gregtech:/index.md
```

The same rules are used by Markdown page links. `guide.md`, `./guide.md`, and `/guide.md` stay inside the current
page namespace. `gregtech:guide.md` and `gregtech:/guide.md` explicitly open the `gregtech` namespace, which means
the page is loaded from the `gregtech:guidenh` data-driven guide when this guide folder is `guidenh`.

### `navigation.position`

Integer sort order within siblings. Larger numbers appear first. Ties are broken by `title` alphabetically.

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  position: 10
```

### `navigation.recommend`

Optional integer used by the home page Recommended panel. The page only appears there when this field is present.
`0` is valid. Larger values appear earlier. Equal values are sorted by title alphabetically.

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

### `navigation.priority`

Load priority for same-path page overrides across mods and resource packs. Missing priority is `0`; higher values
win. If two candidates have the same priority, the later resource pack entry wins, matching normal Minecraft
resource-pack order.

```yaml
navigation:
  title: Pack Override Page
  parent: index.md
  priority: 100
```

### `navigation.icon`

An item ID shown next to the page title in the sidebar.

```yaml
navigation:
  title: Recipes
  icon: minecraft:crafting_table
```

### `navigation.icon_texture`

A PNG texture path (relative to the guide assets folder) used as the navigation icon.

```yaml
navigation:
  title: Home
  icon_texture: my_icon.png
```

### `navigation.icons`

A list of item IDs that cycle in the sidebar. The icon changes periodically.

```yaml
navigation:
  title: Color Demo
  icons:
    - minecraft:wool:1
    - minecraft:wool:4
    - minecraft:wool:14
```

### `navigation.icon_textures`

A list of texture paths that cycle as the navigation icon.

```yaml
navigation:
  title: Slideshow
  icon_textures:
    - frame_1.png
    - frame_2.png
```

## Content Indexing Fields

### `categories`

A list of MediaWiki-style categories. Each entry can be either `category` or `category|sort key`.

```yaml
categories:
  - markdown
  - charts|Charts Overview
```

On the parent page, use `<Category name="markdown" rows="3" />` to auto-generate a list of all pages in that category.
Each category also gets an auto-generated hidden searchable page such as `Category:markdown`.
GuideNH also provides hidden special pages such as `Special:SpecialPages`, `Special:AllPages`, and `Special:Categories`.
Use `<Special name="SpecialPages" rows="3" />`, `<Special name="AllPages" rows="3" />`, or `<Special name="Categories" rows="3" />` to embed them directly.
All `Special:*` pages stay out of normal guide search, while `Category:*` pages remain searchable.

### `item_ids`

Links items to this page for G-key lookup and right-click navigation. Format: `namespace:name` or `namespace:name:meta`. Append `#anchor` to jump to a specific heading.

```yaml
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - minecraft:diamond#Usage
```

### `required_mods`

Hide this page from navigation and indices unless all listed mods are loaded.

```yaml
navigation:
  title: AE2 Integration
required_mods:
  - appliedenergistics2
```

## Optional Meta Fields

| Field | Description |
|-------|-------------|
| `title` | Page heading override (shown in the top-left of the guide screen) |
| `author` | Author attribution shown in the page footer |
| `date` | Creation date shown in the page footer |
| `updated` | Last-modified date shown in the page footer |

## Complete Example

```yaml
---
title: Advanced IO Bus
navigation:
  title: Advanced IO Bus
  parent: getting-started.md
  position: 10
  icon: advanced_ae:advanced_io_bus_part
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
  - advanced_ae:advanced_io_bus_part:1
required_mods:
  - advanced_ae
author: pedroksl
date: 2025-01-01
---

# Advanced IO Bus

The Advanced IO Bus provides faster item transfer...
```
