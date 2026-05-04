---
item_ids:
  - guidenh:guide
navigation:
  title: Root
  icon_texture: test1.png
---

# Start Page

## Navigation

| Category | Pages |
|---|---|
| Markdown | [Markdown Basics](./markdown.md) · [Charts](./charts.md) · [Function Graphs](./function-graph.md) |
| 3D Scenes | [Block Scenes](./scene-blocks.md) · [Entity Scenes](./scene-entities.md) · [Camera & Viewport](./scene-camera.md) |
| Annotations | [Annotations](./scene-annotations.md) · [Import Structure](./scene-import.md) |
| Widgets | [Recipes](./recipes.md) · [Images](./images.md) · [Tooltips](./tooltips.md) |
| Other | [Rendering Demo](./rendering.md) · [Debug](./debug.md) |

## Inline Elements

<Recipe id="missingrecipe" fallbackText="The recipe for special item is disabled." />

Welcome to the world of <ItemImage id="minecraft:stone" />, <PlayerName />!

Keybinding Test: jump = <KeyBind id="key.jump" />, attack = <KeyBind id="key.attack" />, GuideNH hotkey = <KeyBind id="key.guidenh.open_guide" />.

Comment Test: visible before. {/* hidden inline comment */} visible after.

Comment Block Test:
{/*
This whole block is ignored by the parser.
*/}
Still visible after the multiline comment.

You may ~~need~~ a <Color color="#ff0000">door</Color> <Color id="RED">door</Color>!

<CommandLink command="/tp @s 0 90 0" title="Tooltip" close={true}>Teleport!</CommandLink>

<BlockImage id="minecraft:crafting_table" />

<ItemLink id="minecraft:stick" />
