---
navigation:
  title: Block Scenes
  parent: index.md
  position: 170
categories:
  - scenes
---

# Block Scenes

This page is both a live test page and a compact tutorial for block content inside `<GameScene>`.
It keeps the examples visible in game while documenting the parameters that matter when building
resource-pack pages: block placement, TileEntity data, non-full blocks, annotations, and block
statistics.

## Basic Scene Parameters

`<GameScene>` creates a 3D preview area. `<Scene>` is an alias with the same behavior.

| Attribute | Default | Description |
| --- | --- | --- |
| `width` | `256` | Scene viewport width in pixels. |
| `height` | `192` | Scene viewport height in pixels. |
| `zoom` | `1.0` | Camera zoom multiplier. |
| `perspective` | `isometric-north-east` | Camera preset: `isometric-north-east`, `isometric-north-west`, or `up`. |
| `rotateX`, `rotateY`, `rotateZ` | auto | Explicit camera rotation overrides. |
| `offsetX`, `offsetY` | auto | Screen-space camera pan in pixels. |
| `centerX`, `centerY`, `centerZ` | auto | Explicit world rotation center. Setting any one disables automatic centering. |
| `interactive` | `true` | Enables mouse rotation, pan, zoom, reset, hover, and scene buttons. |
| `allowLayerSlider` | `true` | Shows the visible-layer slider when the scene spans multiple Y levels. |
| `gridButtonEnabled` | `true` | Shows the floor-grid toggle button. |
| `showGrid` | `false` | Initial floor-grid visibility. |

```mdx
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:glass" x="1" z="1" />
</GameScene>
```

## Static Particles

`<Particle>` adds a stationary particle inside the scene. When `name` is omitted it uses the
default billboard particle, which is useful for glow markers or highlighting a precise point
without adding a full annotation shape.

| Attribute | Default | Description |
| --- | --- | --- |
| `name` | `billboard` | Particle appearance. Supported values: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. `particle`, `quad`, and `sheet` are accepted aliases for `billboard`. |
| `x`, `y`, `z` | `0.5`, `0.5`, `0.5` | World-space particle origin. |
| `size` | `0.18` | Particle half-size in block units. |

```mdx
<GameScene width="192" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:furnace" x="1" />
  <Particle x="1.5" y="1.85" z="0.5" size="0.22" />
  <Particle name="smoke" x="1.5" y="1.35" z="0.5" size="0.18" />
</GameScene>
```

## Block Parameters

`<Block>` places one block into the preview world.

| Attribute | Required | Description |
| --- | --- | --- |
| `id` | yes, unless `ore` is used | Block id, such as `minecraft:furnace`. |
| `ore` | no | Ore dictionary name. The first matching block item is used. |
| `x`, `y`, `z` | no | Integer world coordinates. Each defaults to `0`. |
| `meta` | no | Block metadata. If omitted, some blocks derive a default from `facing`. |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, or `east`. |
| `nbt` | no | SNBT TileEntity compound. |
| `formed` | no | Whether placed controller previews should be treated as formed during preview sync. Defaults to `false`. |

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:furnace" x="2" facing="south" />
  <Block ore="logWood" x="3" />
  <Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
</GameScene>
```

## Water & Transparent Blocks

Transparent blocks are rendered in scene order and still participate in hover picking.

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

## Controller Preview Control

Use `formed={false}` when a placed controller should stay visibly unformed in preview. This works
for single-block placement as well as multi-block placement tags such as `<PlaceBlock>`,
`<ReplaceBlock>`, `<ImportStructure>`, and `<ImportStructureLib>`.

`formed` now defaults to `false`, so controller previews stay unformed unless a scene explicitly
asks to auto-form them. GregTech controllers are the primary built-in example today.

```mdx
<Block id="gregtech:gt.blockmachines:15411" formed={false} />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
```

Explicit formed preview:

```mdx
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
```

StructureLib import with orientation and offsets:

```mdx
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib
    name="main"
    controller="gregtech:gt.blockmachines:2741"
    facing="north"
    rotation="clockwise_180"
    flip="none"
    offsetX="2"
    offsetY="1"
    offsetZ="-3"
  />
</GameScene>
```

Plain block layouts still work as-is and remain compatible with later controller-based structure
checks:

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:water" />
  <Block id="minecraft:water" x="-1" />
  <Block id="minecraft:water" x="1" />
  <Block id="minecraft:grass" z="1" />
  <Block id="minecraft:grass" x="1" z="1" />
  <Block id="minecraft:glass" z="2" />
  <Block id="minecraft:glass" x="1" z="2" />
</GameScene>
```

## Redstone Circuit With Default Statistics

This scene does not declare `<BlockStats>`, but the block-stat toggle button is still available
because the scene contains blocks. Opening it shows the default inside list. If `maxWidth` and
`maxHeight` are not declared, the overlay is capped to 25% of the scene width and height, but it
will still grow enough to show at least one item type without forcing a scrollbar.

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

Equivalent explicit form:

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:redstone_wire" y="1" />
  <BlockStats dock="inside" corner="topRight" />
</GameScene>
```

## Block Statistics Overlay Parameters

`<BlockStats>` customizes the semi-transparent statistics list. Automatic mode counts the actual
scene contents. Manual mode shows author-provided rows, which is useful for planned material lists.

| Attribute | Default | Description |
| --- | --- | --- |
| `visible` | config, default `false` | Initial overlay visibility. The button can still open it. |
| `buttonEnabled` | config, default `true` | Shows the statistics toggle button. |
| `mode` | `auto` | `auto` or `manual`; any `<BlockStat>` child forces manual mode. |
| `corner` | `topRight` | Inside overlay corner: `topRight`, `topLeft`, `bottomRight`, or `bottomLeft`. |
| `dock` | `inside` | Automatic lists can attach to `inside`, `left`, `top`, `right`, or `bottom`. Manual mode always stays inside. |
| `showNames` | `false` | Shows item names beside icons; when enabled the count is also appended after the name. |
| `filterMode` | `blacklist` | `blacklist` hides matching items; `whitelist` shows only matching items. |
| `filter` | empty | Item keys like `minecraft:stone` or `minecraft:stone:0`, separated by spaces, commas, or semicolons. |
| `maxWidth` | 25% scene width | Maximum overlay width before horizontal scrolling. |
| `maxHeight` | 25% scene height | Maximum overlay height before vertical scrolling. |

`<BlockStat>` rows for manual mode:

| Attribute | Required | Description |
| --- | --- | --- |
| `item` | yes, unless `id` is used | Item id shown in the row. |
| `id` | yes, unless `item` is used | Existing item-stack attribute form. |
| `count` | no | Displayed stack count. Defaults to `0`. |

Automatic mode groups by `item:meta`, sorts by count, and resolves common multipart-style blocks
into the items players expect to see. This includes AE2 cable-bus parts and facades,
ForgeMultipart parts, and Carpenters' Blocks covers or overlays when those mods are present.
Statistics are cached and rebuilt only when scene blocks, Ponder timeline state, StructureLib
selection, or statistics settings change.

## Docked Automatic Statistics

Docked lists reserve space outside the scene. `dock="right"` avoids the scene button column.
For `left` and `right`, rows wrap into extra columns when the attached side is too short. For
`top` and `bottom`, rows wrap into extra rows when the attached side is too narrow.

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:stone" x="2" />
    <Block id="minecraft:furnace" x="1" y="1" />
    <Block id="minecraft:torch" x="2" y="1" />
    <BlockStats dock="right" showNames={true} maxWidth="180" maxHeight="96" />
</GameScene>

Click an item in an automatic list to highlight all matching scene placements. The highlight uses
each resolved collision box, so multipart and non-full blocks are highlighted by their actual
visible faces rather than a full cube. The overlay is always-on-top and deliberately bright so it
remains visible through other blocks. Click the same item again to clear the selection. The selected
row receives a highlighted background as feedback.

The count is rendered with the normal ItemStack stack-size overlay. With `showNames={false}`, the
stack count still gives an approximate count at a glance. With `showNames={true}`, the name also
receives the count suffix. Hovering the item inserts an exact block-count line into its tooltip.

## Filtering Examples

Use a blacklist to hide blocks that are obvious or not useful in the material list:

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <Block id="minecraft:torch" x="2" />
  <BlockStats filterMode="blacklist" filter="minecraft:stone,minecraft:air" />
</GameScene>
```

Use a whitelist to focus on a small set of blocks:

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <Block id="minecraft:torch" x="2" />
  <BlockStats filterMode="whitelist" filter="minecraft:furnace minecraft:torch" showNames={true} />
</GameScene>
```

## Manual Material List

Manual mode is not tied to actual scene contents and does not use outside docking or block
highlight selection. It is meant for recipe-like planning lists.

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:furnace" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:cobblestone" x="-1" />
    <BlockStats mode="manual" corner="bottomRight" maxWidth="160" maxHeight="96">
        <BlockStat item="minecraft:cobblestone" count="8" />
        <BlockStat item="minecraft:furnace" count="1" />
    </BlockStats>
</GameScene>

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
```

## BlockAnnotationTemplate

`<BlockAnnotationTemplate>` applies child annotations to every already-placed matching block.
Place the template after the blocks or imported structure that it should match.

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

```mdx
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    This will be shown in the tooltip!
  </DiamondAnnotation>
</BlockAnnotationTemplate>
```

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

Chest, furnace, redstone block, piston, and beacon:

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

## Non-Full Blocks

Stairs, slabs, fences, trapdoors, multipart parts, and other non-full blocks should use their
actual collision or render bounds for hover and statistics highlight selection.

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:oak_stairs" />
  <Block id="minecraft:stone_stairs" x="2" meta="1" />
  <Block id="minecraft:stone_slab" x="4" />
  <Block id="minecraft:stone_slab" x="4" y="1" meta="8" />
  <Block id="minecraft:fence" x="6" />
  <Block id="minecraft:fence" x="6" z="1" />
  <Block id="minecraft:trapdoor" x="8" />
</GameScene>

## Static Weather

`<Weather>` is a dedicated scene component for animated rain and snow. It is separate from
billboard particles, loops during normal `GameScene` rendering, and follows the same precipitation
column rules as the Ponder weather runtime.

<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:stone" x="0" y="0" z="0" />
  <Block id="minecraft:stone" x="1" y="0" z="0" />
  <Block id="minecraft:stone" x="2" y="0" z="0" />
  <Block id="minecraft:glass" x="1" y="1" z="0" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2" z="0" density="7" />
</GameScene>

- `x` and `z` accept either one value or endpoint arrays.
- Weather ignores `y`; the runtime derives vertical range from scene bounds and precipitation blockers.
- The same `x/z` column never stacks multiple weather effects at the same time.

