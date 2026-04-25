# GameScene

`<GameScene>` is GuideNH's 3D preview tag. `<Scene>` is an alias with the same behavior.

## Scene Attributes

| Attribute | Type | Default | Meaning |
| --- | --- | --- | --- |
| `width` | integer | `256` | viewport width in pixels |
| `height` | integer | `192` | viewport height in pixels |
| `zoom` | float | `1.0` | camera zoom multiplier |
| `perspective` | string | `isometric-north-east` | camera preset |
| `rotateX` | float | auto | explicit X rotation override |
| `rotateY` | float | auto | explicit Y rotation override |
| `rotateZ` | float | auto | explicit Z rotation override |
| `offsetX` | float | auto | screen-space horizontal pan |
| `offsetY` | float | auto | screen-space vertical pan |
| `centerX` | float | auto | explicit world rotation center X |
| `centerY` | float | auto | explicit world rotation center Y |
| `centerZ` | float | auto | explicit world rotation center Z |
| `interactive` | boolean expression | `true` | enables mouse interaction |

## Perspective Presets

Accepted `perspective` values:

- `isometric-north-east`
- `isometric-north-west`
- `up`

Unknown values fall back to `isometric-north-east`.

## Example

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Scene Child Elements

GuideNH currently registers these scene child tags:

- `<Block>`
- `<ImportStructure>`
- `<IsometricCamera>`
- `<RemoveBlocks>`
- `<Entity>`
- annotation tags such as `<BoxAnnotation>` and `<LineAnnotation>`

## `<Block>`

Places a block into the preview world.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block id |
| `x` | no | integer world X, default `0` |
| `y` | no | integer world Y, default `0` |
| `z` | no | integer world Z, default `0` |
| `meta` | no | integer block metadata |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |

Notes:

- if `meta` is omitted, some blocks derive a sensible default from `facing`
- if `nbt` creates a TileEntity successfully, the preview uses it

Example:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block id="minecraft:chest" x="4" nbt="{id:\"Chest\",Items:[{Slot:0b,id:\"minecraft:diamond\",Count:1b,Damage:0s}]}" />
````

## `<ImportStructure>`

Loads an external structure file into the scene.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `src` | yes | structure asset path |
| `x` | no | integer translation X |
| `y` | no | integer translation Y |
| `z` | no | integer translation Z |

Supported formats:

- SNBT text
- gzipped binary NBT
- uncompressed binary NBT

Required structure keys:

- `palette`
- `blocks`

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
````

## `<IsometricCamera>`

Applies explicit isometric camera yaw/pitch/roll.

| Attribute | Meaning |
| --- | --- |
| `yaw` | float |
| `pitch` | float |
| `roll` | float |

Example:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes every already-placed block matching a target block id.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block id to remove |

This is useful after importing a structure when you want to hide specific blocks for clarity.

## `<Entity>`

`<Entity id="..."/>` is parsed but entity rendering is not implemented yet. The tag is safe to mention in documentation, but it should not be relied on for visible output today.

## Camera Center Behavior

If no explicit `centerX/Y/Z` is given, GuideNH auto-centers the scene from the placed block bounds. If any explicit center coordinate is set, auto-centering is disabled and missing coordinates default to `0`.

## Interaction Notes

When `interactive={true}` the scene supports rotation, pan, zoom, reset, annotation toggles, and other UI controls exposed by the guide screen.

## Related Pages

- [Annotations](Annotations)
- [Recipes](Recipes)
- [Examples](Examples)
