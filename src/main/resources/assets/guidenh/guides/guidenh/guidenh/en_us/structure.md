---
title: Multiblock Structure Preview
navigation:
  parent: index.md
  title: Structure Preview
item_ids:
  - minecraft:diamond_block
  - minecraft:cobblestone
---

# Multiblock Structure Preview (Phase 7)

The `<Structure>` tag embeds a 2.5D isometric view of a multiblock structure inside a guide page, showing both shape and block composition.

## Simple cube

A 2x2x2 stone cube:

<Structure width="220" height="160">
  0 0 0 minecraft:stone
  1 0 0 minecraft:stone
  0 0 1 minecraft:stone
  1 0 1 minecraft:stone
  0 1 0 minecraft:stone
  1 1 0 minecraft:stone
  0 1 1 minecraft:stone
  1 1 1 minecraft:stone
</Structure>

## Mixed blocks example

Demonstrates blocks with metadata (oak planks = `minecraft:planks:0`):

<Structure width="240" height="180">
  # Foundation: 3x1x3 cobblestone
  0 0 0 minecraft:cobblestone
  1 0 0 minecraft:cobblestone
  2 0 0 minecraft:cobblestone
  0 0 1 minecraft:cobblestone
  1 0 1 minecraft:cobblestone
  2 0 1 minecraft:cobblestone
  0 0 2 minecraft:cobblestone
  1 0 2 minecraft:cobblestone
  2 0 2 minecraft:cobblestone
  # Second layer: oak planks at corners
  0 1 0 minecraft:planks:0
  2 1 0 minecraft:planks:0
  0 1 2 minecraft:planks:0
  2 1 2 minecraft:planks:0
  # Top: diamond block at center
  1 2 1 minecraft:diamond_block
</Structure>

## Syntax

Each entry: `<x> <y> <z> <modid:name>[:meta]`

- Lines starting with `#` are comments
- Blank lines are ignored
- `meta` is optional and defaults to `0`
- Coordinates are integers; `y` is height (up is positive)
