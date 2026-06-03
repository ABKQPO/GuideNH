---
navigation:
  title: Scene Particles
  parent: index.md
  position: 159
categories:
  - scenes
---

# Scene Particles

GuideNH now supports two particle authoring paths:

1. Static scene particles through `<Particle>` inside `<GameScene>`
2. Static scene weather through `<Weather>` inside `<GameScene>`
3. Runtime Ponder particles through the `particles` array in `ImportPonder` JSON

## Static `<Particle>`

`<Particle>` renders a stationary particle at a fixed world-space position. When `name` is omitted,
it uses the default billboard particle.

| Attribute | Default | Description |
| --- | --- | --- |
| `name` | `billboard` | Particle appearance. Supported values: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. `particle`, `quad`, and `sheet` are accepted aliases for `billboard`. |
| `x`, `y`, `z` | `0.5`, `0.5`, `0.5` | Particle origin in world space |
| `size` | `0.18` | Particle half-size in block units |

```mdx
<GameScene width="192" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:furnace" x="1" />
  <Particle x="1.5" y="1.85" z="0.5" size="0.22" />
  <Particle name="smoke" x="1.5" y="1.35" z="0.5" size="0.18" />
</GameScene>
```

## Static `<Weather>`

`<Weather>` renders animated rain or snow directly in the scene. It is a separate scene component,
not a particle billboard. Scene weather loops continuously during normal rendering, uses the same
precipitation geometry path as timeline weather, and does not support timeline pause or scrubbing.

| Attribute | Default | Description |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weather kind. Supported values: `rain`, `snow`. |
| `x`, `z` | scene bounds | Covered precipitation columns. Scalars target one column. Arrays use endpoint pairs for rectangles. |
| `density` | type-specific | Coverage density. Larger values keep more columns active. |

```mdx
<GameScene width="224" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
```

Weather tag notes:

- `<Weather>` ignores `y`; vertical span is derived from the scene bounds and from precipitation-blocking blocks.
- If one axis has extra unmatched endpoint values, the unmatched tail is ignored.
- Overlapping weather tags do not stack on the same `x/z` column. Earlier tags keep shared columns.

## Ponder `particles`

Ponder particles spawn only when the timeline advances forward into a keyframe. They are not
replayed during reverse scrubbing, which keeps seek behaviour deterministic.

Generic particles:

```json
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
```

Explosion preset:

```json
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
```

Weather preset:

```json
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
```

Indicator preset:

```json
"particles": [
  {
    "preset": "indicator",
    "color": "#6EDCFF",
    "x": [1, 2],
    "y": [1, 2],
    "z": [0, 1],
    "time": 16,
    "amount": 6,
    "size": 0.12
  },
  {
    "preset": "redstone",
    "x": 3,
    "y": 1,
    "z": 1
  }
]
```

| Field | Description |
| --- | --- |
| `preset` | Special preset. `explosion` reproduces a vanilla-style flash, smoke, and outward burst. `rain` enables the shared weather preset. `indicator` emits Create/Ponder-style block hint particles. `redstone` is a shortcut alias for `indicator` with the default red color. |
| `weather` | Used by `preset: "rain"`. Supports `rain` and `snow`. |
| `color` | Used by `preset: "indicator"` / `preset: "redstone"`. Accepts `#RRGGBB`, `#RRGGBBAA`, or `0xRRGGBB` / `0xRRGGBBAA`. Omitted color defaults to the Ponder-style red hint color. |
| `name` | Generic particle appearance. Supported values: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | Compatibility aliases for `name`. |
| `x`, `y`, `z` | Generic particle origin in world space. For `preset: "rain"`, only `x/z` are used as weather coverage. For `preset: "indicator"` / `preset: "redstone"`, scalars target one block, while arrays or whitespace/comma-separated coordinate strings expand into block-coordinate sets on that axis. |
| `vx`, `vy`, `vz` | Initial velocity. `motionX/Y/Z` are accepted aliases. |
| `time` / `lifetime` | Particle lifetime in ticks. For `preset: "rain"` this is the full weather duration including the start/end transition. |
| `size` | Particle half-size in block units. |
| `amount` | Generic particle count. When omitted for `explosion`, it scales from `power`. For `preset: "rain"` it controls average density per tick. For `preset: "indicator"` / `preset: "redstone"` it is the number of hint particles emitted inside each targeted block. Indicator and redstone presets still default to `10` when omitted. |
| `power` | Explosion strength for the `explosion` preset. |

Weather preset notes:

- `preset: "rain"` is the shared weather preset entry point.
- Set `weather: "rain"` for rainfall or `weather: "snow"` for snowfall.
- Ponder weather is timeline-owned. It replays, pauses, seeks, and fast-forwards with the rest of the timeline.
- For always-on scene weather outside the timeline, use `<Weather>` inside `<GameScene>`.
- Weather presets ignore `y`; vertical range is derived from the scene bounds.
- Extra unmatched endpoint values on one axis are ignored.
- The runtime automatically adds a short fade-in, steady middle section, and fade-out.
- Rain adds quick drops and small ground splashes. Snow uses slower drifting flakes.
- The same `x/z` column will not stack multiple weather types at the same time.

Indicator preset notes:

- `preset: "indicator"` emits a short colored hint burst inside each targeted block, similar to Create/Ponder redstone indicators.
- `preset: "redstone"` is just `indicator` with the default red color, so it is useful as a concise shorthand.
- For `indicator` and `redstone`, `x/y/z` all support scalar coordinates, arrays, or whitespace/comma-separated coordinate lists.
- The runtime expands the three axes independently, then emits particles for every `x * y * z` block combination in the resulting volume/set.
