# Ponder Animation Timeline

GuideNH supports Ponder-style animated timelines inside `<GameScene>` blocks. You supply an
external JSON file that defines keyframes, camera movements and in-world annotations, and
GuideNH renders an interactive progress bar with play/pause controls below the 3D scene.

## Quick Start

1. Create a Ponder JSON file and place it in your resource pack (see [File Placement](#file-placement)).
2. Add `<ImportPonder src="..."/>` inside a `<GameScene>` block alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
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
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
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
| `time` | integer | Yes | Tick at which this keyframe occurs (0 <= time <= totalTime). |
| `hidden` | boolean | No | When `true`, the keyframe still applies camera/NBT/entity/annotation state at its `time`, but no visible node is drawn for it on the progress bar and visible keyframe navigation skips it. |
| `label` | string | No | Optional fallback label shown when hovering the keyframe node on the progress bar. |
| `labelKey` | string | No | Translation key for the keyframe label. When resolved, it overrides `label`. |
| `camera` | object | No | Camera state at this keyframe. Null fields inherit from the previous keyframe. |
| `cameraEaseTicks` | integer or null | No | How many ticks the camera takes to ease from the **previous** keyframe to this one. `null` (default) = ease over the full segment. `0` = instant snap. `N > 0` = ease over N ticks, then hold at the target position. |
| `layer` | integer or null | No | Visible layer override. `null` (or omitted) shows all layers. 1-based index. |
| `annotations` | array | No | List of annotation objects shown while this keyframe is active. |
| `sounds` | array | No | List of sounds played once when this keyframe becomes active during forward playback. |
| `particles` | array | No | List of runtime particle bursts or presets fired when this keyframe becomes active during forward playback. |
| `blockChanges` | array | No | List of block replacements applied when this keyframe first becomes active. |
| `mergeTileNBT` | array | No | Merge SNBT compounds into tile entities at block positions. |
| `modifyTileNBT` | array | No | Set one tile-entity NBT path to an SNBT value. |
| `removeTileNBT` | array | No | Remove one tile-entity NBT path. |
| `createEntities` | array | No | Create Ponder-owned entities that can be referenced by later entity NBT operations. |
| `setEntityNBT` | array | No | Replace a referenced entity's NBT with the supplied SNBT compound. |
| `mergeEntityNBT` | array | No | Merge an SNBT compound into a referenced entity. |
| `modifyEntityNBT` | array | No | Set one referenced entity NBT path to an SNBT value. |
| `removeEntityNBT` | array | No | Remove one referenced entity NBT path. |
| `removeEntities` | array | No | Remove one or more Ponder-owned entities by `ref` using the stable scene-entity registry. |

Hidden keyframes are useful when you want additional intermediate state changes without adding a new visible node
to the timeline. For example, you can split several `modifyTileNBT` updates across multiple ticks, mark the
intermediate keyframes as hidden, and keep only the major beats visible on the progress bar.

### Camera fields

All camera fields are optional. Any `null` or omitted field inherits its value from the nearest
prior keyframe that defined it; if no prior keyframe defined the field, the scene's default
camera value is used.

| Field | Type | Description |
|-------|------|-------------|
| `zoom` | float | Camera zoom level (0.1 - 10.0). |
| `rotX` | float | X-axis rotation in degrees. |
| `rotY` | float | Y-axis rotation in degrees. |
| `rotZ` | float | Z-axis rotation in degrees. |
| `offX` | float | Horizontal pan offset in screen pixels. |
| `offY` | float | Vertical pan offset in screen pixels. |

The camera smoothly interpolates between adjacent keyframes using an **ease-in/ease-out** curve.
Use `cameraEaseTicks` on the **destination** keyframe to control the easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
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
| `x`, `y`, `z` | integer | - | **Required.** Position of the block to change (structure coordinates). |
| `block` | string | - | **Required.** Registry name, e.g. `"minecraft:furnace"`. Use `"minecraft:air"` to remove. |
| `meta` | integer | `0` | Block metadata / damage value. |
| `particles` | boolean | `true` | Whether to spawn block-texture particle effects when this block change fires during forward playback. Particles are taken from the block's own icon texture. Set to `false` to suppress (e.g., for silent removal). |
| `nbt` | string | `null` | SNBT string for a tile entity tag, e.g. for chests, furnaces, etc. Parsed with `JsonToNBT`. Keys must be **unquoted** (standard SNBT format). Ignored if the block has no tile entity. |

**Seek-safe:** When seeking backwards the runtime restores all changed positions to their
original structure state, then re-applies changes from keyframes 0 through the current one.
The displayed structure is always correct regardless of seek direction.

> **Note on particles:** Block-texture particles fire once, only during forward playback when the
> keyframe first becomes active. They are cleared on seek, restart, or initial load.

## Keyframe Sounds

Add a `sounds` array to a keyframe to play one or more guide sounds when the keyframe becomes active
during forward playback. Seeking and initial load do not play keyframe sounds; restart clears the
play history so the sounds can fire again.

```json
{
  "time": 130,
  "label": "Furnace lights up",
  "sounds": [
    { "sound": "guidenh:machine.start", "volume": 0.8 },
    { "src": "guidenh:sounds/machine/hum.ogg", "volume": 0.4, "x": 1.5, "y": 1.5, "z": 1.5 }
  ]
}
```

Sound fields:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `sound` | string | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | string | - | Sound file id or path; `guidenh:sounds/machine/start.ogg` becomes `guidenh:machine.start`. |
| `volume` | float | `1.0` | Playback volume before attenuation. |
| `pitch` | float | `1.0` | Playback pitch. |
| `cooldown` | integer | `250` | Minimum milliseconds before the same sound can play again. |
| `x`, `y`, `z` | float | none | Optional scene-space source position for screen-space attenuation. |
| `radius` | float | scene short side * 0.75 | Attenuation radius in screen pixels. |
| `minVolume` | float | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Add a `particles` array to a keyframe when you want one-shot particle bursts or timeline-local
weather overlays during forward playback. These particles are not re-fired during reverse
scrubbing, and seek/restart clears them before replaying the active state.

Generic particles:

```json
{
  "time": 110,
  "particles": [
    {
      "name": "smoke",
      "x": 1.5,
      "y": 1.85,
      "z": 1.5,
      "vx": 0.0,
      "vy": 0.01,
      "vz": 0.0,
      "size": 0.18,
      "time": 16,
      "amount": 3
    }
  ]
}
```

Explosion preset:

```json
{
  "time": 160,
  "particles": [
    {
      "preset": "explosion",
      "x": 1.5,
      "y": 1.45,
      "z": 1.5,
      "time": 8,
      "power": 2.4
    }
  ]
}
```

Weather preset:

```json
{
  "time": 220,
  "particles": [
    {
      "preset": "rain",
      "weather": "snow",
      "x": [0, 2],
      "z": [0, 2],
      "time": 100,
      "amount": 8
    }
  ]
}
```

Particle fields:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `preset` | string | none | Special preset. `explosion` spawns a vanilla-style flash/smoke burst. `rain` enables the weather preset. |
| `weather` | string | `rain` | Weather type used by `preset: "rain"`. Supported values: `rain`, `snow`. |
| `name` | string | none | Generic particle appearance. Supported values: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | string | none | Compatibility aliases for `name`. |
| `x`, `z` | float or array | scene bounds | Particle origin or weather coverage. Generic particles use scalar coordinates. For `preset: "rain"`, scalar values target one precipitation column and arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | float | `0.0` | Initial motion vector. `motionX/Y/Z` are accepted aliases. |
| `time` / `lifetime` | integer | preset-specific | Particle lifetime in ticks. For `preset: "rain"` this is the total weather duration, including fade-in and fade-out. |
| `size` | float | preset-specific | Generic particle half-size in block units. |
| `amount` | integer | preset-specific | Generic particle count. For `explosion`, omitted amount scales from `power`. For `preset: "rain"`, this is the average per-tick weather density. |
| `power` | float | `2.0` | Explosion strength for the `explosion` preset. |

Weather preset notes:

- `preset: "rain"` is the shared weather preset entry point. Use `weather: "rain"` for rainfall or
  `weather: "snow"` for snowfall.
- This preset is timeline-owned weather. It supports replay, pause, seek, and fast-forward together
  with the rest of the Ponder timeline.
- For always-on scene weather outside the Ponder timeline, use the `<Weather>` tag inside
  `<GameScene>` instead.
- Weather presets ignore `y`; the vertical spawn range is derived from the current scene bounds.
- `x: 5, z: 8` targets one precipitation column. Arrays use endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` creates two covered rectangles.
- If one axis has extra unmatched array values, the unmatched tail is ignored.
- The runtime automatically shapes the effect with a short start transition, a steady middle
  section, and an end transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes without splash particles.
- The weather area is derived from the current GameScene bounds so the effect scales with the
  imported structure instead of a hard-coded box.
- The same `x/z` column never stacks multiple weather types at the same time. Earlier overlapping
  weather declarations keep the shared columns; later ones only render on the remaining area.

---

## Tile Entity NBT Operations

Use `mergeTileNBT`, `modifyTileNBT`, and `removeTileNBT` when the block stays in place but its
tile entity data changes. Operations are seek-safe: GuideNH restores the original tile NBT and
then replays all operations from keyframe 0 through the active keyframe.

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| Field | Used by | Description |
|-------|---------|-------------|
| `x`, `y`, `z` | all | Tile-entity block position in structure coordinates. |
| `nbt` | `mergeTileNBT` | SNBT compound merged into the tile entity. Existing compound keys are merged recursively; other values replace the old value. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT path with list indexes, e.g. `Items[0].Count` or `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT value written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow the same idea as Minecraft's `/data` paths: use dots for compound keys and
`[index]` for list entries. List traversal currently expects the traversed list entries to be
compounds, which matches common tile NBT such as inventories, tanks, and recipe slots.

---

## Entity Actions

Regular `<Entity>` tags are already supported in `GameScene`. Ponder timelines can also create
their own entities with `createEntities`, then target those entities by `ref` in later keyframes.

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "sceneEntityId": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `ref` | Required local reference name for later operations. |
| `sceneEntityId` | Optional stable scene-local id used for mount relations, replay-safe replacement, import/export restore, and any later removal of this logical entity. Defaults to an internal id derived from `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, or a mod entity ID supported by the scene entity loader. |
| `x`, `y`, `z` | Optional spawn position. Defaults to `0, 0, 0` unless `nbt` supplies `Pos`. |
| `yaw`, `pitch` | Optional spawn rotation. Defaults to `0, 0` unless `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | Optional living-entity body/head yaw overrides. If omitted while `yaw` is present, they follow `yaw`. |
| `nbt` | Optional SNBT compound applied when the entity is created. |
| `name`, `uuid` | Optional preview-player profile fields when creating a preview player entity. |
| `mount` | Optional stable `sceneEntityId` of the vehicle that this entity should ride after creation or later replay. |
| `unmount` | Optional boolean that clears the entity's current stable mount relation before any later `mount` is applied. |

After creation, use the entity NBT operations:

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

`setEntityNBT` is also available when you want to replace the entity's NBT instead of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

Entity actions can also update transform, preview-player pose, and stable mount state without
changing NBT:

```json
{
  "time": 80,
  "createEntities": [
    { "ref": "cart", "sceneEntityId": "cart", "id": "minecraft:minecart", "x": 1.5, "y": 1.0, "z": 1.5 },
    { "ref": "rider", "sceneEntityId": "rider", "id": "player", "name": "GuideNH", "x": 1.5, "y": 1.0, "z": 1.5, "mount": "cart" }
  ],
  "modifyEntityNBT": [
    { "ref": "rider", "headYaw": 270.0, "leftArmRotation": "-40 0 0" }
  ]
}
```

To detach or remove a timeline entity, use `unmount` or `removeEntities`:

```json
{
  "time": 120,
  "modifyEntityNBT": [
    { "ref": "rider", "unmount": true, "x": 2.5, "y": 1.0, "z": 1.5 }
  ],
  "removeEntities": [
    { "ref": "cart" }
  ]
}
```

Like tile operations, entity operations are replayed from the beginning whenever the active
keyframe changes, so seeking backwards removes Ponder-created entities and recreates the correct
state for the target tick.

Notes:

- `ref` identifies which Ponder-owned entity the current action should edit or remove.
- `sceneEntityId` and `mount` identify stable cross-entity relations. `mount` always points to a
  stable scene id, not to another `ref`.
- Relying on raw passenger NBT alone is not recommended for cross-entity scene relationships. The
  stable registry is what keeps mount and removal behavior deterministic across replay, rebuild,
  import/export, and editor preview refresh.

---

## Annotation Fade

Annotations smoothly fade in over **5 game ticks** (250 ms) whenever the active keyframe
changes during playback. Seeking or pausing always shows annotations at full opacity.

---

## Annotation Types

Each entry in the `annotations` array requires a `type` field. Seven types are available.

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
| `tooltip` | string | `""` | Fallback text shown on hover. |
| `tooltipKey` | string | `""` | Translation key for the hover text. When resolved, it overrides `tooltip`. |
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

### `block`

Renders a wireframe around one whole block. This is the Ponder JSON equivalent of
`<BlockAnnotation pos="x y z">` in a regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` and `"type": "block_box"` are accepted aliases.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `pos` | number[3] or string | `[0, 0, 0]` | Block coordinate as `[x, y, z]` or `"x y z"`. |
| `x`, `y`, `z` | number | `0` | Alternative block coordinate fields. Values are floored. |
| `blockX/Y/Z` | integer | `0` | Legacy-style block coordinate fields. |
| `color` | string | `"0xFFFFFFFF"` | ARGB line color. |
| `lineWidth` | float | default | GL line width. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `line`

Renders a line segment or polyline between world positions. `points` takes priority over
`fromX/Y/Z` and `toX/Y/Z` when it contains at least two valid points.

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "arrow": "end",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

Polyline points can be written either as a string or as an array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `fromX/Y/Z` | float | `0.0` | Start point. |
| `toX/Y/Z` | float | `1.0` | End point. |
| `points` | string or array | `null` | Polyline points. Use `"x y z; x y z; ..."` or `[[x,y,z], ...]`. |
| `color` | string | `"0xFFFFFFFF"` | ARGB line color. |
| `arrow` | string | `null` | `start` or `end`; omitted or invalid values draw no arrow. |
| `lineWidth` | float | default | GL line width. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `blockface`

Highlights all faces of a single block with a translucent solid overlay.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` and `"type": "block_face"` are accepted aliases.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `pos` | number[3] or string | `[0, 0, 0]` | Block coordinate as `[x, y, z]` or `"x y z"`. |
| `x`, `y`, `z` | number | `0` | Alternative block coordinate fields. Values are floored. |
| `blockX/Y/Z` | integer | `0` | Legacy-style block coordinate fields. |
| `color` | string | `"0x80FFFFFF"` | ARGB overlay color. |
| `alwaysOnTop` | boolean | `false` | Render through blocks. |

---

### `text`

Renders a speech-bubble label anchored to a world position. The box appears above the anchor by
default and is connected to it with a short vertical line. Text content is keyframe-driven: use
different `text` annotation entries on different keyframes to change the displayed text over time.

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "Place items here",
  "color": "0xFF44AAFF",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12
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
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | float | `0.0` | World-space anchor position (ignored in independent mode). |
| `text` | string | - | **Required.** Text to display inside the bubble. |
| `color` | string | `"0xFFAAAAAA"` | ARGB color of the bubble border. |
| `backgroundAlpha` | integer | `204` | Background opacity from `0` (transparent) to `255` (opaque). The RGB color remains the default dark navy. |
| `maxWidth` | integer | `0` | If &gt; 0, wraps text at this width in pixels. Omit or set to `0` for a single-line label. |
| `independent` | boolean | `false` | If `true`, position is relative to the scene centre rather than a world point. |
| `yOffset` | integer | `0` | Pixel offset from the scene's vertical centre (positive = downward). Used with `independent: true`. |
| `connectorSide` | string | `"bottom"` | `bottom`, `top`, `left`, `right`, or `none`. Ignored in independent mode. |
| `connectorOffset` | integer | `0` | Pixel offset along the selected bubble edge; positive moves right for top/bottom and down for left/right. |
| `connectorLength` | integer | `6` | Pixel length of the connector line. `0` hides the line while keeping side-based placement. |
| `hlMinX/Y/Z` | float | `0.0` | Minimum corner of an optional highlight box drawn alongside the text bubble. |
| `hlMaxX/Y/Z` | float | `1.0` | Maximum corner of the optional highlight box. |
| `highlightColor` | string | `"0x8000FFAA"` | ARGB color of the highlight box. |

When `hlMinX` (or any `hlMin/Max` coordinate) is present, an `InWorldBoxAnnotation` is also
created at the specified bounds with `highlightColor`. This is useful for pointing at specific
block regions while explaining them.

The background is a dark navy bubble by default (`#CC0E0E20`), and `backgroundAlpha` controls its
opacity. In world-anchored mode a connector line links the box to the anchor. Text supports the
full GuideNH inline rich-text syntax: markdown formatting and MDX inline tags. It is rendered with
drop-shadow.

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

The icon is a 16x16 sprite drawn from `ponder_widgets.png`. The box background is semi-transparent
dark (`#CC0E0E20`) with a light-blue border (`#80AAAADD`). When an `item` is specified the box
expands to accommodate both the item icon and the mouse icon side by side.

---

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
| **Prev keyframe** | Jump to the start of the previous visible keyframe segment. Hidden keyframes are skipped. |
| **Play/Pause** | Toggle playback; restarts from the beginning if already finished. |
| **Restart** | Return to tick 0, reset state, and begin playing. |
| Progress bar | Click or drag to seek to any position. Seeking always pauses playback. |
| Keyframe nodes | Small tick marks on the bar for visible keyframes; hover to see the label and direction arrow. |

### Initial state

When a page containing `<ImportPonder>` is first opened, the scene starts **paused at tick 0**.
Press Play to begin.

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
- Hidden keyframes do not create hoverable nodes, but they still apply their timeline state when playback or seeking reaches them.

### Layer control during playback

The `layer` field of the active keyframe overrides the visible-layer filter during playback:
- `null` (or omitted) -> show all layers.
- `1`, `2`, `3`, ... -> restrict to that 1-based layer index.

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
          "pos": [2, 1, 1],
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

<GameScene zoom="4">
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
- Annotations belong to a single keyframe - they appear only while that keyframe is active
  (i.e., from its `time` tick until the next keyframe's `time` tick). Overlay text annotations
  fade out smoothly when the keyframe changes during playback.
- Ponder `line` annotations use the same runtime renderer as regular `LineAnnotation`, including
  polyline bends and start/end arrows. Point marker cubes remain an MDX-only feature.
- Ponder `text` annotations use the same runtime renderer as regular `TextAnnotation`, including
  connector side, offset, and length. Dynamic text is keyframe-based rather than interpolated per tick.
- Only one `<ImportPonder>` tag is effective per `<GameScene>`. A second tag overwrites the first.
- A `text` annotation with an empty or absent `text` field is silently skipped.
- The `inputType` field defaults to `"lmb"` if omitted or unrecognised.
- `blockChanges` are applied in order from the first to the current keyframe every time the
  active keyframe changes, so changing the same position in multiple keyframes works correctly.
- Tile/entity NBT operations use the same replay model as `blockChanges`; they are safe to seek
  forwards or backwards.
- `text` annotations with a `maxWidth` &gt; 0 are word-wrapped using the vanilla font renderer;
  the bubble box height adjusts automatically for multi-line text.
- `nbt` strings in `blockChanges` must use **unquoted** SNBT keys (standard MC 1.7.10 format).
  Quoted keys will be rejected by the parser. String values still require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` and `modifyEntityNBT` values are SNBT values, not JSON values. For a string
  value, escape the SNBT quotes inside JSON: `"value": "\"hello\""`.

