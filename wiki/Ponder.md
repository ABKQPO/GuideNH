# Ponder Animation Timeline

GuideNH supports Ponder-style animated timelines inside `<GameScene>` blocks. You supply an
external JSON file that defines keyframes, camera movements and in-world annotations, and
GuideNH renders an interactive progress bar with play/pause controls below the 3D scene.

## Quick Start

1. Create a Ponder JSON file and place it in your resource pack (see [File Placement](#file-placement)).
2. Add `<ImportPonder src="..."/>` inside a `<GameScene>` block alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4" background="#0a0a10">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Note:** `<ImportPonder>` must appear inside a `<GameScene>` block. The `src` attribute is
> required. Structure data is still provided by `<ImportStructure>` or `<ImportStructureLib>`.

## File Placement

Ponder JSON files follow the same resource-pack path rules as SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       ← guide page
  pages/machines/my_machine.snbt      ← structure data
  pages/machines/my_machine.json      ← Ponder JSON
```

The `src` attribute accepts both relative and absolute IDs:

| Example | Resolved as |
|---------|-------------|
| `src="my_machine.json"` | Relative to the current page's directory |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | Absolute (`mymod` namespace) |

## JSON Format

```json
{
  "totalTime": 120,
  "keyframes": [
    {
      "time": 0,
      "label": "Start",
      "camera": {
        "zoom": 2.0,
        "rotX": 25.0,
        "rotY": -30.0,
        "rotZ": 0.0,
        "offX": 0.0,
        "offY": 0.0
      },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Mid-point",
      "camera": {
        "rotY": -90.0
      },
      "annotations": [
        {
          "type": "diamond",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "color": "0xFFFF4444",
          "tooltip": "Input hatch",
          "alwaysOnTop": false
        }
      ]
    },
    {
      "time": 120,
      "camera": {
        "rotY": -150.0
      }
    }
  ]
}
```

### Root fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `totalTime` | integer | Yes | Total duration in ticks (20 ticks = 1 second). Minimum clamped to 1. |
| `keyframes` | array | Yes | List of keyframe objects. May be empty. |

### Keyframe fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `time` | integer | Yes | Tick at which this keyframe occurs (0 ≤ time ≤ totalTime). |
| `label` | string | No | Optional label shown when hovering the keyframe node on the progress bar. |
| `camera` | object | No | Camera state at this keyframe. Null fields inherit from the previous keyframe. |
| `cameraEaseTicks` | integer or null | No | How many ticks the camera takes to ease from the **previous** keyframe to this one. `null` (default) = ease over the full segment. `0` = instant snap. `N > 0` = ease over N ticks, then hold at the target position. |
| `layer` | integer or null | No | Visible layer override. `null` (or omitted) shows all layers. 1-based index. |
| `annotations` | array | No | List of annotation objects shown while this keyframe is active. |
| `blockChanges` | array | No | List of block replacements applied when this keyframe first becomes active. |

### Camera fields

All camera fields are optional. Any `null` or omitted field inherits its value from the nearest
prior keyframe that defined it; if no prior keyframe defined the field, the scene's default
camera value is used.

| Field | Type | Description |
|-------|------|-------------|
| `zoom` | float | Camera zoom level (0.1 – 10.0). |
| `rotX` | float | X-axis rotation in degrees. |
| `rotY` | float | Y-axis rotation in degrees. |
| `rotZ` | float | Z-axis rotation in degrees. |
| `offX` | float | Horizontal pan offset in screen pixels. |
| `offY` | float | Vertical pan offset in screen pixels. |

The camera smoothly interpolates between adjacent keyframes using an **ease-in/ease-out** curve.
Use `cameraEaseTicks` on the **destination** keyframe to control the easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   ← instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  ← ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         ← ease over full segment (default)
```

## Block Changes

The `blockChanges` array in a keyframe replaces blocks in the live structure when that keyframe
becomes active. This allows the animation to show before-and-after states, place or remove
blocks, or animate a machine powering on.

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | integer | — | **Required.** Position of the block to change (structure coordinates). |
| `block` | string | — | **Required.** Registry name, e.g. `"minecraft:furnace"`. Use `"minecraft:air"` to remove. |
| `meta` | integer | `0` | Block metadata / damage value. |
| `particles` | boolean | `true` | Whether to spawn block-texture particle effects when this block change fires during forward playback. Particles are taken from the block's own icon texture. Set to `false` to suppress (e.g., for silent removal). |
| `nbt` | string | `null` | SNBT string for a tile entity tag, e.g. for chests, furnaces, etc. Parsed with `JsonToNBT`. Keys must be **unquoted** (standard SNBT format). Ignored if the block has no tile entity. |

**Seek-safe:** When seeking backwards the runtime restores all changed positions to their
original structure state, then re-applies changes from keyframes 0 through the current one.
The displayed structure is always correct regardless of seek direction.

> **Note on particles:** Block-texture particles fire once, only during forward playback when the
> keyframe first becomes active. They are cleared on seek, restart, or initial load.

---

## Annotation Fade

Annotations smoothly fade in over **5 game ticks** (250 ms) whenever the active keyframe
changes during playback. Seeking or pausing always shows annotations at full opacity.

---

## Annotation Types

Each entry in the `annotations` array requires a `type` field. Six types are available.

---

### `diamond`

Renders a 3D diamond marker at a world position.

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "Click me",
  "alwaysOnTop": false
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | float | `0.0` | World-space position of the diamond tip. |
| `color` | string | `"0xFF00E000"` | ARGB color as `"0xAARRGGBB"`. |
| `tooltip` | string | `""` | Text shown on hover. |
| `alwaysOnTop` | boolean | `false` | If true, rendered through solid blocks. |

---

### `box`

Renders a wireframe axis-aligned box from `min` to `max`.

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `minX/Y/Z` | float | `0.0` | Minimum corner. |
| `maxX/Y/Z` | float | `1.0` | Maximum corner. |
| `color` | string | `"0xFFFFFFFF"` | ARGB line color. |
| `lineWidth` | float | default | GL line width. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `line`

Renders a line segment between two world positions.

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `fromX/Y/Z` | float | `0.0` | Start point. |
| `toX/Y/Z` | float | `1.0` | End point. |
| `color` | string | `"0xFFFFFFFF"` | ARGB line color. |
| `lineWidth` | float | default | GL line width. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `blockface`

Highlights all faces of a single block with a translucent solid overlay.

```json
{
  "type": "blockface",
  "blockX": 1,
  "blockY": 0,
  "blockZ": 1,
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `blockX/Y/Z` | integer | `0` | Block coordinate to highlight. |
| `color` | string | `"0x80FFFFFF"` | ARGB overlay color. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `text`

Renders a speech-bubble label anchored to a world position. The box appears above the anchor
point and is connected to it with a short vertical line.

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "Place items here",
  "color": "0xFF44AAFF"
}
```

For a fixed screen-space position that does not project from world coordinates, use
**independent mode**. The bubble is centered horizontally in the scene and placed at
`yOffset` pixels below the scene's vertical centre.

```json
{
  "type": "text",
  "text": "Independent label",
  "color": "0xFFFFCC00",
  "independent": true,
  "yOffset": 40
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | float | `0.0` | World-space anchor position (ignored in independent mode). |
| `text` | string | — | **Required.** Text to display inside the bubble. |
| `color` | string | `"0xFFAAAAAA"` | ARGB color of the bubble border. |
| `maxWidth` | integer | `0` | If &gt; 0, wraps text at this width in pixels. Omit or set to `0` for a single-line label. |
| `independent` | boolean | `false` | If `true`, position is relative to the scene centre rather than a world point. |
| `yOffset` | integer | `0` | Pixel offset from the scene's vertical centre (positive = downward). Used with `independent: true`. |
| `hlMinX/Y/Z` | float | `0.0` | Minimum corner of an optional highlight box drawn alongside the text bubble. |
| `hlMaxX/Y/Z` | float | `1.0` | Maximum corner of the optional highlight box. |
| `highlightColor` | string | `"0x8000FFAA"` | ARGB color of the highlight box. |

When `hlMinX` (or any `hlMin/Max` coordinate) is present, an `InWorldBoxAnnotation` is also
created at the specified bounds with `highlightColor`. This is useful for pointing at specific
block regions while explaining them.

The background is always a dark semi-transparent navy (`#CC0E0E20`). In world-anchored mode a
connector line links the box to the anchor. Text supports the full GuideNH inline rich-text
syntax — markdown formatting and MDX inline tags — and is rendered with drop-shadow.

> **Rich text:** The `text` field supports the same inline markup used in GuideNH guide pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, and all other inline MDX tags.
> Plain Minecraft `§` format codes are **not** supported; use MDX syntax instead.

> A `text` annotation without a `text` field (or an empty string) is silently ignored.

---

### `input`

Renders a mouse-input icon (left button, right button, or scroll wheel) anchored to a world
position. This is used to hint that the player should perform a specific interaction.

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

The icon is a 16×16 sprite drawn from `ponder_widgets.png`. The box background is semi-transparent
dark (`#CC0E0E20`) with a light-blue border (`#80AAAADD`). When an `item` is specified the box
expands to accommodate both the item icon and the mouse icon side by side.

---

## Color Format

Colors are ARGB hexadecimal strings. Both `"0xFFFFFF00"` (with `0x` prefix) and
`"FFFF00"` (without prefix) are accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` — fully opaque green (default diamond color)
- `"0x8022CCFF"` — semi-transparent blue
- `"0xFFAAAAAA"` — light grey (default text bubble border)

## Playback Behavior

### Controls

| Control | Action |
|---------|--------|
| **◀ (Prev keyframe)** | Jump to the start of the previous keyframe segment. |
| **▶/⏸ (Play/Pause)** | Toggle playback; restarts from the beginning if already finished. |
| **↺ (Restart)** | Return to tick 0, reset state, and begin playing. |
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
- `null` (or omitted) → show all layers.
- `1`, `2`, `3`, … → restrict to that 1-based layer index.

## Complete Example

The following example demonstrates every annotation type across a four-keyframe scene.

### Directory layout

```
assets/mymod/guidebooks/
  pages/machines/grinder.mdx
  pages/machines/grinder.snbt
  pages/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "Overview",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Input hatch",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV Input Bus",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "Insert ore here",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "Output side",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "blockX": 2, "blockY": 1, "blockZ": 1,
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "Collect dust here",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "Scroll layer",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "Scroll to show layers",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

A standard NBT structure file (created with the `/structure save` command or a tool such as
Litematica). See [Getting Started](Getting-Started.md) for the full SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4" background="#0a0a10">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

The Grinder turns ores into doubled dust. Press **Play** to see the animated walkthrough.
```

## Notes

- The Ponder progress bar is drawn above the layer/StructureLib sliders. During playback the
  structural sliders are hidden to keep the UI clean; they reappear when paused.
- Camera interpolation is always smooth (ease-in/out) even if some keyframes only change a
  subset of camera axes. Use `cameraEaseTicks` on a keyframe to snap the camera instantly (`0`)
  or ease over a fixed number of ticks before holding the target position.
- Annotations belong to a single keyframe — they appear only while that keyframe is active
  (i.e., from its `time` tick until the next keyframe's `time` tick). Overlay text annotations
  fade out smoothly when the keyframe changes during playback.
- Only one `<ImportPonder>` tag is effective per `<GameScene>`. A second tag overwrites the first.
- A `text` annotation with an empty or absent `text` field is silently skipped.
- The `inputType` field defaults to `"lmb"` if omitted or unrecognised.
- `blockChanges` are applied in order from the first to the current keyframe every time the
  active keyframe changes, so changing the same position in multiple keyframes works correctly.
- `text` annotations with a `maxWidth` &gt; 0 are word-wrapped using the vanilla font renderer;
  the bubble box height adjusts automatically for multi-line text.
- `nbt` strings in `blockChanges` must use **unquoted** SNBT keys (standard MC 1.7.10 format).
  Quoted keys will be rejected by the parser. String values still require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.

