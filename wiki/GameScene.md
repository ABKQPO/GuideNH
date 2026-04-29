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
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<RemoveBlocks>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- annotation tags such as `<BoxAnnotation>` and `<LineAnnotation>`

## `<Block>`

Places a block into the preview world.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes, unless `ore` is used | block id |
| `ore` | no | ore dictionary name; the first matching stack must resolve to a block item |
| `x` | no | integer world X, default `0` |
| `y` | no | integer world Y, default `0` |
| `z` | no | integer world Z, default `0` |
| `meta` | no | integer block metadata |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |

Notes:

- `ore` takes precedence over `id`; if GregTech is installed, the chosen stack is unified through `GTOreDictUnificator.setStack(...)`
- if `meta` is omitted and an `ore` match carries concrete non-wildcard item damage, that damage is used before the `facing` fallback
- if `meta` is omitted, some blocks derive a sensible default from `facing`
- if `nbt` creates a TileEntity successfully, the preview uses it

Example:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
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

## `<ImportStructureLib>`

Imports a StructureLib multiblock preview by controller id.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `controller` | yes | controller block id, using `modid:block[:meta]` |
| `piece` | no | StructureLib piece name override |
| `facing` | no | facing override passed to the importer |
| `rotation` | no | rotation override passed to the importer |
| `flip` | no | flip/mirror override passed to the importer |
| `channel` | no | integer channel override for channel-aware structures |

Notes:

- the imported structure starts from scene `0 0 0`; the controller is not forced to be placed at `0 0 0`
- this tag enables StructureLib-specific tooltip, hatch highlight, and channel slider UI when metadata is available
- controller matching supports the GTNH-style `modid:block:meta` form

Example:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
````

## `<IsometricCamera>`

Applies explicit isometric camera yaw/pitch/roll.

If this tag is omitted, the scene keeps using the `<GameScene>` `perspective` preset. The default
`isometric-north-east` preset is equivalent to:

````md
<IsometricCamera yaw="225" pitch="30" />
````

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
| `id` | yes | block id to remove, using `modid:block[:meta]` |

This is useful after importing a structure when you want to hide specific blocks for clarity.

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<BlockAnnotationTemplate>`

Expands one or more child annotations onto every matching block that already exists in the current scene.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block matcher in `modid:block[:meta]` form |

Rules:

- place it after the blocks or imported structures that it should match
- matching happens against the current scene state at parse time
- child annotations use local coordinates relative to each matched block

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds an entity to the preview scene.

The attributes follow summon-style entity placement and SNBT data.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | entity type id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, and registered mod entity ids in either `modid.entityName` or `modid:entityName` form are accepted |
| `x` | no | float X coordinate the entity is centered on, default `0.5` |
| `y` | no | float Y coordinate at the bottom of the entity, default `0` |
| `z` | no | float Z coordinate the entity is centered on, default `0.5` |
| `rotationY` | no | yaw in degrees, default `-45` |
| `rotationX` | no | pitch in degrees, default `0` |
| `data` | no | summon-style SNBT merged into the entity NBT before spawn |
| `name` | no | preview player name when `id` is `player`, `fakeplayer`, `minecraft:player`, or `minecraft:fakeplayer` |
| `uuid` | no | preview player UUID when using one of the player ids above |
| `showName` | no | boolean expression controlling the preview player nameplate, default `true` for player preview ids |
| `showCape` | no | boolean expression controlling the preview player cape, default `true` for player preview ids |
| `headRotation` | no | preview player head rotation as `x y z` degrees |
| `leftArmRotation` | no | preview player left arm rotation as `x y z` degrees |
| `rightArmRotation` | no | preview player right arm rotation as `x y z` degrees |
| `leftLegRotation` | no | preview player left leg rotation as `x y z` degrees |
| `rightLegRotation` | no | preview player right leg rotation as `x y z` degrees |
| `capeRotation` | no | preview player cape rotation as `x y z` degrees; defaults to the standing-still angle `6 0 0` |

Notes:

- entity bounds participate in scene auto-centering and visible-layer filtering
- entity creation falls back gracefully when the preview world is not ready yet, then binds on first render
- player preview ids create a client-side fake remote player so the normal player renderer and skin pipeline can be used
- when both `name` and `uuid` are omitted for a player preview, GuideNH falls back to `Steve` and the vanilla default skin
- when only `name` is given for a player preview, GuideNH first tries to resolve the real online profile so skins and capes can load; if lookup fails, it falls back to a stable offline UUID
- when only `uuid` is given for a player preview, GuideNH generates a placeholder display name and still tries to resolve the skin from the profile
- `showName={false}` hides the preview player's overhead name without bypassing the normal player renderer
- `showCape={false}` hides the preview player's cape while still respecting the normal player render path and Forge hooks
- player pose attributes use three space-separated floats mapped to model `X Y Z` rotation in degrees
- omitted head and limb rotation attributes keep the normal vanilla idle pose; omitted `capeRotation` falls back to the standing-still cape angle `6 0 0`
- player previews require an active client world at parse time because Minecraft's player entity constructor cannot be created worldless
- hovering an entity shows its localized display name, or its custom name if one was provided

Example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Preview player pose example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

Preview player name and cape example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## Camera Center Behavior

If no explicit `centerX/Y/Z` is given, GuideNH auto-centers the scene from the placed block bounds. If any explicit center coordinate is set, auto-centering is disabled and missing coordinates default to `0`.

## Interaction Notes

When `interactive={true}` the scene supports rotation, pan, zoom, reset, annotation toggles, and other UI controls exposed by the guide screen.

- scenes spanning multiple Y levels show a visible-layer slider above the bottom edge
- StructureLib scenes can add a hatch-highlight toggle button plus a channel slider at the very bottom when the imported metadata provides them
- annotation hover takes priority over block hover; block tooltips appear normally once no annotation hotspot is being hovered
- StructureLib hover keeps the block name on the first tooltip line, adds structure-specific text starting on the second line, and expands replacement candidates when `Shift` is held

## Related Pages

- [Annotations](Annotations)
- [Recipes](Recipes)
- [Examples](Examples)
