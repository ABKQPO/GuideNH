# StructureLib Export

## English

`/exportStructureLib` exports PNG screenshots for StructureLib multiblock previews. The command is registered only on the client and only when StructureLib is loaded.

The exporter uses an orthographic camera. Output size changes with structure size, while block scale stays stable. The default scale is 256 pixels per block.

### Command Forms

```text
/exportStructureLib [controller] [options...]
/exportStructureLib @file.json
/exportStructureLib --config file.json
```

`controller` uses `modid:name` or `modid:name:meta`. When omitted, the exporter attempts to discover all StructureLib controllers.

### Parameters

| Parameter | Description |
| --- | --- |
| `--out <dir>` | Output directory. Default: `screenshots/structurelib/<timestamp>/`. |
| `--pixelsPerBlock <int>` | Pixel density per world block. Default: `256`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--tier <expr>` | Master tier values. |
| `--channel <name=expr>` | Named StructureLib channel values. Can be repeated. |
| `--layers <expr\|each\|all>` | Layer visibility. Default: `all`. |
| `--facing <list>` | Facing values for bulk orientation export. |
| `--rotation <list>` | Rotation values for bulk orientation export. |
| `--flip <list>` | Flip values for bulk orientation export. |
| `--orientation <facing:rotation:flip,...>` | Explicit orientation combinations. |
| `--view <preset>` | Camera preset. Default: `isometric-south-east`. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine camera override. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases for camera override. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG background. Default: `transparent`. |
| `--maxPixels <long>` | Maximum pixel count for one image. Default: `655360000`, estimated from a 200x100x200 machine at 128 pixels per projected block. Use `-1` for no limit. |
| `--batchSize <int>` | Flushes `manifest.json` after this many completed results. Default: `16`. |
| `--gt-active-controller` | GregTech only. Renders the controller with its active machine texture when possible. |
| `--gt-place-hatches` | GregTech only. Enables normal GT Hatch channel placement for Hatch-only preview positions; fallback casing remains the default when this is omitted. |
| `--force` | Allows more than 256 generated screenshots. |
| `--dry-run` | Builds the plan and manifest without writing PNG files. |
| `--config <file>` / `@file.json` | Loads JSON configuration. |

### Numeric Filters

Numeric filters are used by `--tier`, `--channel`, and `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` matches one value. `0-12` matches an inclusive range. `!` excludes values. Commas combine tokens.

### Layers

`--layers all` exports the full structure in one image.

`--layers 0-12,!5` exports one image where only matching layers are visible.

`--layers each` exports one image for every actual Y layer in the generated structure.

Layer-filtered rendering forces exposed block faces to render so hidden neighboring layers do not leave missing faces.

### Tiers And Channels

When `--tier` and `--channel` are omitted, the exporter inspects the controller and exports one screenshot for each
available unified tier. The same tier value is applied to the master tier and every discovered StructureLib channel.
Automatic unified tier export is capped at 100 screenshots per controller/orientation.

When `--tier` is provided and `--channel` is omitted, each requested tier also drives every discovered channel with the
same value, clamped to each channel's supported range.

When one or more `--channel` options are provided, those explicit channel values are used. Multiple explicit tier and
channel values are combined as a Cartesian product.

```text
/exportStructureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

This explicit example produces every combination of tier, `coil`, and `casing`.

### GregTech Options

GregTech integration keeps optional Hatch-capable positions as their normal fallback casing by default. Forced Hatch elements, such as a Muffler position, still render as the requested Hatch.

Use `--gt-place-hatches` when you want GT's normal StructureLib Hatch channel logic to place required Hatches for preview-only screenshots. For example, an element declared with `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` can place the required Hatch previews and update their textures instead of showing only the fallback casing.

Use `--gt-active-controller` when a controller should render with its active texture. The exporter still performs preview state synchronization without treating a failed machine check as an export failure.

### Orientation

Bulk syntax:

```text
--facing north,south --rotation normal,clockwise --flip none
```

Explicit syntax:

```text
--orientation north:normal:none,south:clockwise:none
```

Both forms can be used together. Invalid combinations are skipped when StructureLib alignment limits reject them. If no orientation is specified, the controller default is used.

### Views

Supported presets include:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You can refine any preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 256,
  "scale": 1.0,
  "tier": "1-4",
  "channels": {
    "coil": "1-4",
    "casing": "1,2"
  },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

Run it with:

```text
/exportStructureLib @my_export.json
```

Relative config paths are searched from the current working directory and `config/guidenh/structurelib_exports/`.

### Output And Manifest

Each export directory contains PNG files and `manifest.json`.

Image names start with the controller item display name. Variant suffixes include tier, channels, layers, orientation, and view. Names are sanitized for Windows filename rules.

The manifest records controller ID, display name, output path, image size, tier, channels, layers, orientation, view, warnings, and errors.

### Performance

The command refuses plans over 256 screenshots unless `--force` is present.

By default, one image cannot exceed `655360000` pixels. This default is sized from a 200x100x200 class machine using a 128 pixels-per-projected-block budget. If a giant structure still needs more room, lower `--pixelsPerBlock` or `--scale`, crop with `--layers`, raise `--maxPixels`, or use `--maxPixels -1` to disable the pixel limit.

Large images use tiled framebuffer rendering when they exceed the GPU texture size.
