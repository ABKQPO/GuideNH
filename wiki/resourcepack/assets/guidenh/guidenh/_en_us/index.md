---
item_ids:
  - guidenh:guide
navigation:
  title: GuideNH Examples
  position: 200
  recommend: 3
author: GuideNH
date: 2024-01-01
---

# GuideNH Examples

Welcome to the GuideNH demo guide. This showcases the rendering, layout, and scene capabilities available to guide authors. See [Navigation & Index](./navigation-guide.md) for how to structure your own guide pages.

## Markdown

<Category name="markdown" rows="3" />

## Scenes

Featured scene docs: [Scene Particles](./scene-particles.md)

<Category name="scenes" rows="3" />

## Widgets

<Category name="widgets" rows="3" />

## Other

<Category name="other" rows="3" />

## Hidden MediaWiki Pages

Explore the generated MediaWiki-style pages:

- [Special:SpecialPages](Special:SpecialPages)
- [Special:AllPages](Special:AllPages)
- [Special:Categories](Special:Categories)
- [Category:markdown](Category:markdown)

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
