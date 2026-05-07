---
navigation:
  title: Ponder Animations
  parent: index.md
---

# Ponder Animations

The `<ImportPonder>` tag adds a fully keyframe-driven animation timeline to any `<GameScene>`.
It controls camera interpolation, per-keyframe 3D annotations, block changes, and annotation
fade-in transitions — all declared in a JSON file.

## Full Feature Demo

The scene below demonstrates every supported feature: six annotation types, multi-line text,
independent (screen-space) text, text with a highlight box, block changes between keyframes,
and modifier + item input annotations.

<GameScene width="420" height="280" zoom={2.5} interactive={true}>
  <ImportStructure src="/assets/ponder_demo.snbt" />
  <ImportPonder src="/assets/ponder_demo.json" />
</GameScene>

Press ▶ to play, or drag the timeline. The keyframe nodes snap the timeline to key moments.

---

## JSON Schema

```json
{
  "totalTime": 360,
  "keyframes": [
    {
      "time": 0,
      "label": "Overview",
      "camera": {
        "zoom": 1.8,
        "rotX": 25.0,
        "rotY": 210.0
      },
      "layer": null,
      "annotations": [],
      "blockChanges": []
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `totalTime` | int | Total animation length in game ticks (20 ticks = 1 second) |
| `keyframes` | array | Ordered list of keyframe objects |
| `time` | int | Tick at which this keyframe becomes active |
| `label` | string? | Optional label shown when hovering the keyframe node |
| `camera` | object? | Partial camera override (only specified fields are applied) |
| `layer` | int? | Visible layer override (`null` = show all) |
| `annotations` | array? | Annotation objects to show while this keyframe is active |
| `blockChanges` | array? | Block replacements applied when this keyframe activates |

---

## Camera Keyframe Fields

Camera fields can be partially specified — omitted fields are inherited from the previous keyframe
that set them. The runtime interpolates between consecutive keyframes using an ease-in-out curve.

```json
"camera": {
  "zoom": 1.8,
  "rotX": 25.0,
  "rotY": 210.0,
  "rotZ": 0.0,
  "offX": 0.0,
  "offY": 0.0
}
```

---

## Annotation Types

All six annotation types are supported. Fields not relevant to the type are ignored.

### `diamond` — World-space marker

```json
{
  "type": "diamond",
  "x": 0.5, "y": 1.8, "z": 1.5,
  "color": "0xFF44FF44",
  "tooltip": "Hover text shown as a rich tooltip",
  "alwaysOnTop": true
}
```

### `box` — Axis-aligned bounding box

```json
{
  "type": "box",
  "minX": 0.05, "minY": 1.05, "minZ": 1.05,
  "maxX": 0.95, "maxY": 1.95, "maxZ": 1.95,
  "color": "0x8800AAFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

### `line` — Line segment

```json
{
  "type": "line",
  "fromX": 1.5, "fromY": 2.0, "fromZ": 1.5,
  "toX": 1.5, "toY": 1.5, "toZ": 1.5,
  "color": "0xFFFFCC44",
  "lineWidth": 2.5,
  "alwaysOnTop": true
}
```

### `blockface` — Block face highlight

```json
{
  "type": "blockface",
  "blockX": 1, "blockY": 1, "blockZ": 1,
  "color": "0x60FF8833"
}
```

### `text` — Speech-bubble label

World-anchored (connector line drawn from label to world position):

```json
{
  "type": "text",
  "x": 1.5, "y": 3.0, "z": 1.5,
  "text": "Furnace is now lit and smelting!",
  "color": "0xFFFF8833",
  "maxWidth": 120
}
```

Independent (screen-space, no connector line):

```json
{
  "type": "text",
  "text": "A simple smelting setup: hopper feeds the furnace, chest collects output.",
  "independent": true,
  "yOffset": -60,
  "maxWidth": 180,
  "color": "0xFFCCCCFF"
}
```

Text with an accompanying highlight box:

```json
{
  "type": "text",
  "x": 0.5, "y": 2.8, "z": 1.5,
  "text": "Place fuel and ore here",
  "color": "0xFF44FF44",
  "hlMinX": 0.05, "hlMinY": 1.05, "hlMinZ": 1.05,
  "hlMaxX": 0.95, "hlMaxY": 1.95, "hlMaxZ": 1.95,
  "highlightColor": "0x6044FF44"
}
```

| `text` field | Type | Description |
|---|---|---|
| `text` | string | Label text |
| `color` | string | Border colour in `0xAARRGGBB` format |
| `maxWidth` | int? | Word-wrap width in pixels; `0` or omitted = single line |
| `independent` | bool? | `true` = screen-space mode (no world anchor) |
| `yOffset` | int? | Y offset from scene centre in independent mode |
| `hlMinX/Y/Z` | float? | Highlight box min corner (world space) |
| `hlMaxX/Y/Z` | float? | Highlight box max corner (world space) |
| `highlightColor` | string? | Highlight box colour; defaults to `0x8000FFAA` |

### `input` — Mouse input icon

Renders a mouse button (LMB / RMB / scroll) with an optional modifier label and item icon.

```json
{
  "type": "input",
  "x": 0.5, "y": 1.5, "z": 2.5,
  "inputType": "rmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ore"
}
```

| `input` field | Type | Description |
|---|---|---|
| `inputType` | string | `"lmb"`, `"rmb"`, or `"scroll"` |
| `modifier` | string? | `"sneak"` or `"ctrl"` — shown as a prefix label |
| `item` | string? | Item registry ID, e.g. `"minecraft:iron_ore"` or `"modid:item:meta"` |

---

## Block Changes

`blockChanges` replaces blocks in the structure when a keyframe becomes active. Positions are
restored to their original state before re-applying changes from keyframes 0 through the current
one, so seeking backwards always shows the correct state.

```json
"blockChanges": [
  { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4 },
  { "x": 1, "y": 2, "z": 1, "block": "minecraft:air" }
]
```

| Field | Type | Description |
|---|---|---|
| `x`, `y`, `z` | int | Block position (world coordinates of the structure) |
| `block` | string | Registry name, e.g. `"minecraft:furnace"`. Use `"minecraft:air"` to remove. |
| `meta` | int? | Block metadata / damage value; defaults to `0` |

---

## Annotation Fade Animation

Annotations smoothly fade in over 5 game ticks (250 ms) whenever the active keyframe changes
during playback. Seeking or pausing always shows annotations at full opacity.

---

## Minimal Example

```mdx
<GameScene width="384" height="256" zoom={2} interactive={true}>
  <ImportStructure src="/assets/my_machine.snbt" />
  <ImportPonder src="/assets/my_machine.json" />
</GameScene>
```

The JSON file is loaded from the resource-pack at the path relative to the guidebook root.
The structure file is loaded separately via `<ImportStructure>`.
