---
title: 多方块结构预览
navigation:
  parent: index.md
  title: 结构预览
item_ids:
  - minecraft:diamond_block
  - minecraft:cobblestone
---

# 多方块结构预览（Phase 7）

`<Structure>` 标签可以在指南页面中嵌入一个 2.5D 等距视图，用于展示多方块结构的形状与方块组成。

## 简单立方体

下面是一个 2x2x2 的石头立方体：

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

## 混合方块示例

下面演示带 metadata 的方块（橡木板 = `minecraft:planks:0`）：

<Structure width="240" height="180">
  # 地基：3x1x3 圆石
  0 0 0 minecraft:cobblestone
  1 0 0 minecraft:cobblestone
  2 0 0 minecraft:cobblestone
  0 0 1 minecraft:cobblestone
  1 0 1 minecraft:cobblestone
  2 0 1 minecraft:cobblestone
  0 0 2 minecraft:cobblestone
  1 0 2 minecraft:cobblestone
  2 0 2 minecraft:cobblestone
  # 第二层：四角橡木板
  0 1 0 minecraft:planks:0
  2 1 0 minecraft:planks:0
  0 1 2 minecraft:planks:0
  2 1 2 minecraft:planks:0
  # 顶层：中央钻石块
  1 2 1 minecraft:diamond_block
</Structure>

## 语法说明

每行格式：`<x> <y> <z> <modid:name>[:meta]`

- 以 `#` 开头的行视为注释
- 空行被忽略
- `meta` 可选，默认为 `0`
- 坐标为整数；`y` 为高度（向上为正）
