---
navigation:
  title: Debug Test Page
---

# Debug Test Page

Component regression tests: links, tables, locked preview, camera params. Back to [index](./index.md).

## Hover underline + click

- Internal: [Markdown syntax](./markdown.md)
- Internal: [Rendering tests](./rendering.md)
- External: [Minecraft Wiki](https://minecraft.wiki/)

## Table

| Name | ID | Note |
|------|----|------|
| Crafting Table | `minecraft:crafting_table` | Standard 3×3 |
| Iron Pickaxe | `minecraft:iron_pickaxe` | Iron-tier |
| Wooden Door | `minecraft:wooden_door` | Basic door |

## Locked camera (non-interactive)

<GameScene zoom={3} interactive={false}>
    <Block id="minecraft:crafting_table" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:log" z="1" />
</GameScene>

## Custom initial rotation + zoom

<GameScene zoom={2} rotateX={60} rotateY={0}>
    <Block id="minecraft:grass" />
    <Block id="minecraft:grass" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:log" y="1" />
</GameScene>

<GameScene zoom={5} rotateX={10} rotateY={45}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" y="1" />
    <Block id="minecraft:stone" y="2" />
</GameScene>

## Recipe (empty slots + arrow)

<RecipeFor id="minecraft:crafting_table" />

<RecipeFor id="minecraft:iron_pickaxe" />
