---
navigation:
  title: Block Scenes
  parent: index.md
---

# Block Scenes

Block rendering, TileEntity, and non-full-block tests inside `<GameScene>`.

## Water & Transparent Blocks

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

## Redstone Circuit

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:stone" x="2" />
    <Block id="minecraft:redstone_wire" y="1" />
    <Block id="minecraft:redstone_wire" x="1" y="1" />
    <Block id="minecraft:redstone_wire" x="2" y="1" />
    <Block id="minecraft:lever" x="-1" y="1" />
    <Block id="minecraft:redstone_lamp" x="3" y="1" />
</GameScene>

## BlockAnnotationTemplate

Applies a `<DiamondAnnotation>` to every instance of the given block id in the scene.

<GameScene zoom="2" interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="1" />
  <Block id="minecraft:log" z="1" />
  <Block id="minecraft:log" x="1" z="1" />

  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      This will be shown in the tooltip! <ItemImage id="minecraft:stone" />
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>

## End Portal Frame

<GameScene zoom="8" interactive={true}>
  <Block id="minecraft:end_portal_frame" />
  <Block id="minecraft:end_portal_frame" x="1" />
  <Block id="minecraft:end_portal_frame" x="2" />
  <Block id="minecraft:end_portal_frame" z="2" />
  <Block id="minecraft:end_portal_frame" x="1" z="2" />
  <Block id="minecraft:end_portal_frame" x="2" z="2" />
  <Block id="minecraft:end_portal_frame" z="1" />
  <Block id="minecraft:end_portal_frame" x="2" z="1" />
</GameScene>

## TileEntity / Directional Blocks

Chest, furnace (default facing south so its front is visible), redstone block, piston, beacon:

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:redstone_block" x="4" />
  <Block id="minecraft:piston" x="6" facing="south" />
  <Block id="minecraft:beacon" x="8" />
  <Block id="minecraft:iron_block" x="8" y="-1" />
</GameScene>

Furnaces in four facings:

<GameScene width="384" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:furnace" facing="north" />
  <Block id="minecraft:furnace" x="2" facing="south" />
  <Block id="minecraft:furnace" x="4" facing="west" />
  <Block id="minecraft:furnace" x="6" facing="east" />
</GameScene>

## Non-full Blocks

Stairs / slabs / fence / trapdoor:

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:oak_stairs" />
  <Block id="minecraft:stone_stairs" x="2" meta="1" />
  <Block id="minecraft:stone_slab" x="4" />
  <Block id="minecraft:stone_slab" x="4" y="1" meta="8" />
  <Block id="minecraft:fence" x="6" />
  <Block id="minecraft:fence" x="6" z="1" />
  <Block id="minecraft:trapdoor" x="8" />
</GameScene>
