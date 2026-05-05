---
navigation:
  title: Camera & Viewport
  parent: index.md
---

# Camera & Viewport

Viewport size variants, camera presets, and pan/offset tests.

## Viewport Size Variants

256×96 (wide/short):

<GameScene width="256" height="96" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Block id="minecraft:stone" x="3" />
</GameScene>

128×128 (square):

<GameScene width="128" height="128" zoom={6} interactive={true}>
  <Block id="minecraft:diamond_block" />
</GameScene>

384×256 (large):

<GameScene width="384" height="256" zoom={3} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:iron_block" z="1" />
  <Block id="minecraft:iron_block" x="1" z="1" />
  <Block id="minecraft:iron_block" x="2" z="1" />
  <Block id="minecraft:gold_block" y="1" x="1" z="1" />
</GameScene>

## Camera Presets

`<GameScene>` / `<Scene>` new attributes:

* `perspective="isometric_north_east"` / `"isometric_north_west"` / `"up"` — pick a yaw/pitch/roll preset;
* `rotateX` / `rotateY` / `rotateZ` — explicit per-axis overrides applied on top of the preset;
* `offsetX` / `offsetY` — screen-space pan (units: blocks);
* `centerX` / `centerY` / `centerZ` — explicit world-space rotation center (overrides auto-center).

NE vs NW preset:

<Row>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
  </GameScene>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_west" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
  </GameScene>
</Row>

Top-down view (`up`):

<GameScene width="192" height="160" zoom={5} perspective="up" interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:gold_block" z="1" />
  <Block id="minecraft:gold_block" x="1" z="1" />
</GameScene>

## Pan Offset

`offsetX` / `offsetY` pan — right scene offset by +2 / +1 blocks:

<Row>
  <GameScene width="160" height="128" zoom={4} interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
  <GameScene width="160" height="128" zoom={4} offsetX="2" offsetY="1" interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
</Row>

## DiamondAnnotation Default Color

Without `color`, the diamond defaults to **bright green** (compare against an explicit red diamond):

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="2" />
  <DiamondAnnotation pos="0.5 1.5 0.5">
    Default green diamond (no `color` attribute)
  </DiamondAnnotation>
  <DiamondAnnotation pos="2.5 1.5 0.5" color="#FF0000">
    Explicit red
  </DiamondAnnotation>
</GameScene>

## IsometricCamera Yaw / Pitch / Roll

`<IsometricCamera>` inside a `<GameScene>` sets explicit yaw / pitch / roll angles,
overriding any `perspective` preset.

* `yaw` — horizontal rotation around the Y-axis (degrees, 0–360).
* `pitch` — vertical tilt (degrees, –90 to 90; positive looks downward).
* `roll` — in-plane rotation (degrees, –180 to 180).

NE preset vs explicit yaw 45° pitch 30° (should look identical):

<Row>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
    <IsometricCamera />
  </GameScene>
  <GameScene width="160" height="128" zoom={5} interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
    <IsometricCamera yaw="45" pitch="30" roll="0" />
  </GameScene>
</Row>

Top-down with yaw 90° (rotated 90° clockwise vs the default `up` preset):

<Row>
  <GameScene width="160" height="128" zoom={5} perspective="up" interactive={true}>
    <Block id="minecraft:iron_block" />
    <Block id="minecraft:gold_block" x="1" />
    <Block id="minecraft:diamond_block" z="1" />
    <IsometricCamera />
  </GameScene>
  <GameScene width="160" height="128" zoom={5} interactive={true}>
    <Block id="minecraft:iron_block" />
    <Block id="minecraft:gold_block" x="1" />
    <Block id="minecraft:diamond_block" z="1" />
    <IsometricCamera yaw="90" pitch="90" roll="0" />
  </GameScene>
</Row>

Roll test — left: roll 0°, right: roll 15°:

<Row>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_east" interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <IsometricCamera roll="0" />
  </GameScene>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_east" interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <IsometricCamera roll="15" />
  </GameScene>
</Row>
