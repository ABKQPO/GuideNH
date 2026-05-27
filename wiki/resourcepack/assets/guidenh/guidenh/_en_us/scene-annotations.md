---
navigation:
  title: Annotations
  parent: index.md
  position: 164
categories:
  - scenes
---

# Annotations

All annotation kinds live in world space and can be toggled with the scene's *Show/Hide annotations* button.

## DiamondAnnotation

Place a **diamond marker** at any world coordinate. The diamond always faces the screen; hovering shows a semi-transparent white overlay while its compiled child content is rendered as a rich tooltip.

Activated beacon - 3x3 diamond block base, beacon on top; marker tooltip contains a nested 3D preview:

<GameScene width="256" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:diamond_block" x="-1" z="-1" />
  <Block id="minecraft:diamond_block"         z="-1" />
  <Block id="minecraft:diamond_block" x="1"  z="-1" />
  <Block id="minecraft:diamond_block" x="-1" />
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:diamond_block" x="1" />
  <Block id="minecraft:diamond_block" x="-1" z="1" />
  <Block id="minecraft:diamond_block"         z="1" />
  <Block id="minecraft:diamond_block" x="1"  z="1" />
  <Block id="minecraft:beacon" y="1" />
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <Color color="#FFD24C">**Effect**</Color>: grants nearby players continuous buffs - speed /
    jump boost / resistance / strength / regeneration.

    Activation: a 3x3 / 5x5 / 7x7 / 9x9 pyramid of **diamond / iron / gold / emerald / netherite**
    blocks beneath the beacon.

    <GameScene width="160" height="128" zoom={5} interactive={false}>
      <Block id="minecraft:diamond_block" x="-1" />
      <Block id="minecraft:diamond_block" />
      <Block id="minecraft:diamond_block" x="1" />
      <Block id="minecraft:beacon" y="1" />
    </GameScene>

    <Color color="#AAFFAA">Tip</Color>: more pyramid tiers = more effect options; the beam color
    is determined by stained glass placed in the beam path.
  </DiamondAnnotation>
</GameScene>

## Box / Block / Line Annotations

- `BoxAnnotation` accepts `min="x y z"` / `max="x y z"` (floats) for an arbitrary AABB.
- `BlockAnnotation` accepts a single `pos="x y z"` (integers); shorthand for a 1x1x1 box.
- `LineAnnotation` accepts `from="x y z"` / `to="x y z"` (floats) for a line segment, or
  `points="x y z; x y z; ..."` for a polyline.

All three support `color="#AARRGGBB" or "#RRGGBB"`, `thickness` in pixel units (default `1`), and `alwaysOnTop` to draw above other geometry. Children are used as a rich-text hover tooltip.
`LineAnnotation` can also render a 3D arrow at `arrow="start"` or `arrow="end"`. Point cubes are hidden by default; enable all of them with `showPoints={true}`, or configure individual points with `<LinePoint index="..." show color="#RRGGBB" size="..."/>`.

<GameScene width="384" height="224" zoom={4} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:gold_block" z="2" />
  <Block id="minecraft:gold_block" x="2" z="2" />

  <BoxAnnotation color="#ee3333" min="0 1 0" max="1 1.6 0.6" thickness="0.04">
    **Box annotation** wraps half a block, thickness `0.04`. The tooltip is rich content:

    <Row>
      <ItemImage id="minecraft:iron_ingot" scale="2" />
      Iron ingot - smelt iron ore in a furnace.
    </Row>
    <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
  </BoxAnnotation>

  <BlockAnnotation color="#33ddee" pos="2 0 2" alwaysOnTop={true}>
    **Block annotation**: `alwaysOnTop` punches through depth. Recipe inside the tooltip:

    <RecipeFor id="minecraft:gold_block" />
  </BlockAnnotation>

  <LineAnnotation color="#ffd24c" from="0.5 1.2 0.5" to="2.5 1.2 2.5" thickness="0.08">
    **Line annotation**: a slightly thicker line (`thickness=0.08`). Tooltips can embed a 3D preview:

    <GameScene width="160" height="96" zoom={5} perspective="isometric_north_east" interactive={false}>
      <Block id="minecraft:iron_block" />
      <Block id="minecraft:gold_block" x="1" />
      <DiamondAnnotation pos="0.5 1.2 0.5" color="#ffd24c">Endpoint A</DiamondAnnotation>
      <DiamondAnnotation pos="1.5 1.2 0.5" color="#ee3333">Endpoint B</DiamondAnnotation>
    </GameScene>
  </LineAnnotation>

  <LineAnnotation
    color="#66ccff"
    points="0.5 1.8 2.5; 1.5 2.25 1.5; 2.5 1.8 0.5"
    thickness="0.08"
    arrow="end"
  >
    <LinePoint index="0" show color="#66ccff" />
    <LinePoint index="1" show color="#ff8844" size="0.12" />
    **Polyline annotation**: the end arrow is a 3D arrowhead, and individual points can be shown as cubes.
  </LineAnnotation>
</GameScene>

## Text Annotation

`<TextAnnotation>` is the shared speech-bubble annotation for normal `<GameScene>` pages and imported Ponder timelines.

```mdx
<GameScene width="256" height="192" zoom={4} interactive={true}>
  <TextAnnotation
    pos="1.5 2.0 1.5"
    textKey="guidenh.sample.scene.insert_items"
    color="#FF44AAFF"
    maxWidth={120}
    backgroundAlpha={180}
    connectorSide="right"
    connectorOffset={8}
    connectorLength={12}
  />
</GameScene>
```

Use `independent={true}` to place the bubble in fixed screen space instead of following a world point:

```mdx
<GameScene width="256" height="192" zoom={4} interactive={true}>
  <TextAnnotation
    text="Independent label"
    color="#FFFFCC00"
    backgroundAlpha={140}
    independent={true}
    yOffset={40}
  />
</GameScene>
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | float | `0.0` | World-space anchor position. Ignored when `independent={true}`. |
| `text` | string | - | Required. Text shown inside the bubble. |
| `textKey` | string | - | Translation key resolved from resource-pack `lang` files before falling back to `text` or the tag body. |
| `color` | string | `"0xFFAAAAAA"` | Bubble border color. |
| `backgroundAlpha` | integer | `204` | Background opacity from `0` (transparent) to `255` (opaque). |
| `maxWidth` | integer | `0` | Word-wrap width in pixels. `0` keeps the bubble on one line. |
| `independent` | boolean | `false` | If true, the bubble follows the scene center instead of a world point. |
| `yOffset` | integer | `0` | Pixel offset from the scene center when `independent={true}`. Positive values move downward. |
| `connectorSide` | string | `"bottom"` | `bottom`, `top`, `left`, `right`, or `none`. Ignored in independent mode. |
| `connectorOffset` | integer | `0` | Pixel offset along the bubble edge; positive moves right for top/bottom and down for left/right. |
| `connectorLength` | integer | `6` | Pixel length of the connector line. |
| `hlMinX/Y/Z` | float | `0.0` | Minimum corner of an optional highlight box. |
| `hlMaxX/Y/Z` | float | `1.0` | Maximum corner of the optional highlight box. |
| `highlightColor` | string | `"0x8000FFAA"` | Highlight box color. |

There is no separate size parameter. The bubble grows from the text content and `maxWidth`.
When any `hlMin/Max` value is present, a matching `InWorldBoxAnnotation` is also created.
The bubble background is dark navy by default (`#CC0E0E20`), `backgroundAlpha` controls its
opacity, and world-anchored mode adds a connector line to the anchor point. Use
`connectorSide`, `connectorOffset`, and `connectorLength` to move that connector around the bubble.

> **Rich text:** `text` supports the same inline markup used in GuideNH pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, and other inline MDX tags.

## Input Annotation

`InputAnnotation` renders a mouse-input icon (left button, right button, or scroll wheel) anchored to a world position. This is used to hint that the player should perform a specific interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

With an optional modifier key prefix and an item icon:

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | float | `0.0` | World-space anchor position. |
| `inputType` | string | `"lmb"` | One of `"lmb"`, `"rmb"`, or `"scroll"`. Case-insensitive. |
| `modifier` | string | `null` | Optional modifier key: `"sneak"` or `"ctrl"`. Shows prefix text above the icon. |
| `item` | string | `null` | Optional item registry ID (e.g. `"minecraft:iron_ingot"`). Renders the item icon to the left of the mouse icon. Supports `"modid:item:meta"` format for meta values. |

The icon is a 16x16 sprite drawn from `ponder_widgets.png`. The box background is semi-transparent
dark (`#CC0E0E20`) with a light-blue border (`#80AAAADD`). When an `item` is specified the box
expands to accommodate both the item icon and the mouse icon side by side.

## Color Format

Colors are ARGB hexadecimal strings. Both `"0xFFFFFF00"` (with `0x` prefix) and
`"FFFF00"` (without prefix) are accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (default diamond color)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (default text bubble border)

## Playback Behavior

### Controls

| Control | Action |
|---------|--------|
| **◀ (Prev keyframe)** | Jump to the start of the previous keyframe segment. |
| **▶ / ⏸ (Play/Pause)** | Toggle playback; restarts from the beginning if already finished. |
| **↻ (Restart)** | Return to tick 0, reset state, and begin playing. |
| Progress bar | Click or drag to seek to any position. Seeking always pauses playback. |
| Keyframe nodes | Small tick marks on the bar; hover to see the label and direction arrow. |

### Initial state

When a page containing `<ImportPonder>` is first opened, the scene starts **paused at tick 0**.
Press Play (▶) to begin.

### Camera lock

While playback is **active** (not paused):
- The camera follows the interpolated path defined by keyframes.
- Mouse drag and scroll zoom are **disabled**.
- The layer slider and StructureLib sliders are **hidden**.

While playback is **paused** or **finished**:
- Full interactive camera drag, zoom, and layer/StructureLib control are restored.

### Keyframe node labels

When you hover over a keyframe node on the progress bar:
- The node grows slightly to indicate it is hovered.
- If the keyframe has a `label`, it is displayed beside the node.

### Layer control during playback

The `layer` field of the active keyframe overrides the visible-layer filter during playback:
- `null` (or omitted) -> show all layers.
- `1`, `2`, `3`, ... -> restrict to that 1-based layer index.
