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
| `showBackground` | boolean expression | `true` | shows the scene background fill and border |
| `allowLayerSlider` | boolean | `true` | shows the vertical layer slider |
| `gridButtonEnabled` | boolean | `true` | shows the floor grid toggle button |
| `showGrid` | boolean | `false` | initial visibility of the floor grid |

## Block Statistics Overlay

Scenes that contain blocks enable the block-stat toggle button by default. Add a `<BlockStats>`
child when you want to override its mode, placement, filters, visibility, or size. The list is
cached and only rebuilt when the scene blocks, Ponder timeline state, StructureLib selection, or
block-stat settings change; normal rendering reuses the prepared rows. Long lists are clipped to
`maxWidth` and `maxHeight`; if those are omitted, each defaults to 25% of the final scene size.
Overflow receives draggable scrollbars, and the mouse wheel scrolls the list while the cursor is
over the overlay. Hold Shift to wheel-scroll horizontally.

In automatic mode, GuideNH scans the scene's filled blocks and resolves each block to the item
stack users normally see. Blocks that contain multiple visible components can contribute multiple
items from the same coordinate; this includes AE2 cable bus parts and facades, ForgeMultipart part
drops, and Carpenters' Blocks covers or overlays when those mods are installed. Counts are grouped
by `item:meta` and sorted by count.

Automatic lists can also be docked outside the scene with `dock="left"`, `dock="top"`,
`dock="right"`, or `dock="bottom"`. Docked lists wrap into extra columns or rows based on the
attached side length, reserve layout space, and avoid the scene button column on the right. Click an
item in an automatic list to highlight all matching scene placements with their resolved collision
boxes using an always-on-top face overlay; click the same item again to clear the highlight. Counts
are rendered through the ItemStack stack-size overlay. Set `showNames={true}` to append the count
after each name as well, and hover an item to see the exact block count in the tooltip.

Filters can hide common blocks or show only selected blocks:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

Use manual mode when a guide wants to show a planned material list instead of the literal scene
contents:

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## Debug Mode Overlays

When the `enableDebugMode` option is enabled in the GuideNH mod config, the following extra
overlays become available in the 3D scene preview.

### Grid Coordinate Labels

When debug mode is **on** and the floor grid is **visible**, coordinate labels are rendered
below each grid line:

- **X-axis numbers** are shown along the near edge of the grid (north/−Z edge in the default
  `isometric-north-east` camera).  Each integer X world-coordinate receives a label.
- **Z-axis numbers** are shown along the near edge of the grid (east/+X edge).  Each integer
  Z world-coordinate receives a label.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) are drawn at the midpoint of each
  respective grid edge.

Coordinates follow the actual world X/Z values stored in the scene level, so they can be
negative when the structure contains blocks with negative coordinates.

The grid toggle button is **always enabled** while debug mode is active, regardless of the
`gridButtonEnabled` attribute, so you can show or hide the grid and its labels at any time.
The default grid visibility (`showGrid`) is not affected.

### Block Coordinate Tooltip

When debug mode is **on** and the cursor hovers over a block inside the scene, a second
tooltip is rendered above the primary block tooltip, showing the world-space block position
as `X, Y, Z` in gold text.

If the coordinate tooltip would be clipped at the top of the screen it automatically snaps
below the cursor area instead (magnetic snapping).

## Perspective Presets

Accepted `perspective` values:

- `isometric-north-east`
- `isometric-north-west`
- `up`

Unknown values fall back to `isometric-north-east`.

## Content Embedding and Text Wrapping

Any block-level tag — including `<GameScene>` — supports two optional attributes that control
how it is embedded in the page, mirroring Microsoft Word's "Text Wrapping" options.

| Attribute | Values | Default | Meaning |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | Text-wrapping mode |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Wrap modes

| Mode | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | In line with text | Default flow: scene occupies its own vertical slot (嵌入型) |
| `square` | Square | Scene floats left or right; surrounding text wraps in a rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent to `square` in this layout system (紧密型) |
| `through` | Through | Through-wrap; equivalent to `square` in this layout system (穿越型) |
| `top-bottom` | Top and Bottom | Text only above and below, not beside; respects `align` for horizontal placement (上下型) |
| `behind` | Behind text | Block renders behind surrounding text; respects `align` (衬于文字下方) |
| `front` | In front of text | Block renders in front of surrounding text; respects `align` (浮于文字上方) |

### Examples

Left-floating scene — text in the next paragraph wraps to the right:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Text that flows to the right of the scene...
````

Right-floating scene:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Text that flows to the left of the scene...
````

Centred scene (no text wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline in text (flow context) — text wraps around a small scene:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

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
- `<BlockStats>`
- `<PlaySound>`
- `<RemoveBlocks>`
- `<ReplaceBlock>`
- `<PlaceBlock>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- annotation tags such as `<BoxAnnotation>` and `<LineAnnotation>`

## Scene Sounds

`<PlaySound>` can be placed inside `<GameScene>` to play sounds from scene interaction or timeline
entry. Supported triggers are:

- `click`, the default
- `hover`, fired once when the cursor enters the scene
- `enter`, fired once when the scene first renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

When `x`, `y`, and `z` are provided, the sound volume is attenuated in screen space from the
projected scene coordinate to the click point or scene center. `radius` defaults to 75% of the
shorter scene side, and `minVolume` defaults to `0.15`.

## `<BlockStats>` and `<BlockStat>`

Declares or customizes a block statistics overlay. Scenes with blocks enable the automatic toggle
button even when this child is omitted. Adding one or more `<BlockStat>` children switches the
overlay to manual statistics mode for that scene.

`<BlockStats>` attributes:

| Attribute | Required | Default | Meaning |
| --- | --- | --- | --- |
| `visible` | no | config, default `false` | initial overlay visibility |
| `buttonEnabled` | no | config, default `true` | shows the block statistics toggle button |
| `mode` | no | `auto` | `auto` or `manual`; child `<BlockStat>` entries force manual mode |
| `corner` | no | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, or `bottomLeft` |
| `dock` | no | `inside` | automatic lists can attach to `inside`, `left`, `top`, `right`, or `bottom`; manual mode always uses the inside overlay |
| `showNames` | no | `false` | whether to show item names beside icons; when enabled the count is also appended after the name |
| `filterMode` | no | `blacklist` | `blacklist` or `whitelist` |
| `filter` | no | empty | item keys such as `minecraft:stone` or `minecraft:stone:0`, separated by spaces, commas, or semicolons |
| `maxWidth` | no | 25% of scene width | maximum overlay width in pixels before horizontal scrolling |
| `maxHeight` | no | 25% of scene height | maximum overlay height in pixels before vertical scrolling |

`<BlockStat>` attributes:

| Attribute | Required | Meaning |
| --- | --- | --- |
| `item` | yes, unless `id` is used | item id shown in the list |
| `id` | yes, unless `item` is used | existing item-stack attribute form |
| `count` | no | displayed count, default `0` |

Example:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

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
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

Loads an external structure file into the scene.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `src` | yes | structure asset path |
| `x` | no | integer translation X (alias for `offsetX`) |
| `y` | no | integer translation Y (alias for `offsetY`) |
| `z` | no | integer translation Z (alias for `offsetZ`) |
| `offsetX` | no | integer translation X (preferred over `x`) |
| `offsetY` | no | integer translation Y, clamped to `[0, worldHeight-1]` (preferred over `y`) |
| `offsetZ` | no | integer translation Z (preferred over `z`) |

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
| `name` | no | optional binding name used by `showWhenStructure` on annotations, templates, and sounds |
| `piece` | no | StructureLib piece name override |
| `facing` | no | facing override passed to the importer |
| `rotation` | no | rotation override passed to the importer |
| `flip` | no | flip/mirror override passed to the importer |
| `channel` | no | integer channel override for channel-aware structures |
| `offsetX` | no | integer X offset applied to all placed blocks (default `0`) |
| `offsetY` | no | integer Y offset applied to all placed blocks, clamped to `[0, worldHeight-1]` (default `0`) |
| `offsetZ` | no | integer Z offset applied to all placed blocks (default `0`) |

Notes:

- the imported structure starts from scene `0 0 0`; the controller is not forced to be placed at `0 0 0`
- this tag enables StructureLib-specific tooltip, hatch highlight, and channel slider UI when metadata is available
- controller matching supports the GTNH-style `modid:block:meta` form
- use `name` when the scene contains multiple StructureLib imports and another tag needs to target one specific structure state

Example:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
````

Structure-aware annotation and sound example:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <ImportStructureLib name="aux" controller="gregtech:gt.blockmachines:15412" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Visible only for matching `main` states.
  </BlockAnnotation>

  <PlaySound
    sound="guidenh:machine.start"
    trigger="click"
    showWhenStructure="aux"
    showWhenTier="1..2"
  />
</GameScene>
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

## `<ReplaceBlock>`

Replaces already-placed blocks that match a source block id (and optionally a partial tile entity NBT
pattern) with a new block. The search can be global (all filled blocks) or restricted to a
bounding box.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `from` | yes | source block to match, using `modid:block[:meta]` |
| `from_nbt` | no | partial SNBT compound; a block matches only when its tile entity NBT contains all listed keys |
| `to` | yes | replacement block, using `modid:block[:meta]` |
| `to_nbt` | no | SNBT TileEntity compound to apply to the replacement |
| `x` | no | bounding box start X; if any of `x/y/z/dx/dy/dz` is present, the box mode is activated |
| `y` | no | bounding box start Y |
| `z` | no | bounding box start Z |
| `dx` | no | bounding box width (default `1`) |
| `dy` | no | bounding box height (default `1`) |
| `dz` | no | bounding box depth (default `1`) |

Notes:

- when none of `x/y/z/dx/dy/dz` are provided, all filled blocks are scanned globally
- `from_nbt` is a **partial** match: only the keys listed in the pattern must match; extra keys in
  the actual tile entity are ignored
- the replacement is performed via the same block placement pipeline as `<Block>`, so GregTech MetaTile
  and BartWorks tile entities are handled correctly

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills an axis-aligned box with a single block type, overwriting whatever was there before.
Unlike `<Block>` (which targets a single position), `<PlaceBlock>` supports multi-block regions via
`dx`/`dy`/`dz`.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block id, using `modid:block[:meta]` |
| `nbt` | no | SNBT TileEntity compound applied to every placed block |
| `x` | no | region start X, default `0` |
| `y` | no | region start Y, default `0` |
| `z` | no | region start Z, default `0` |
| `dx` | no | region width, default `1` |
| `dy` | no | region height, default `1` |
| `dz` | no | region depth, default `1` |

Notes:

- all blocks in the box are unconditionally placed (no prior-block check)
- the NBT compound is copied for each individual placement
- the same block placement pipeline as `<Block>` is used, so GregTech MetaTile and BartWorks tile entities
  are fully supported

Example:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
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
| `baby` | no | boolean expression forcing supported entities into baby form; omitted leaves the entity's normal age/state unchanged |
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
- `baby={true}` currently supports preview players, ageable mobs, vanilla zombies, and modded entities that expose stable `setChild(boolean)` or `setBaby(boolean)` style APIs
- child-state entities are re-aligned to their current position after resizing so hover and pick bounds stay centered on the rendered model
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

Baby entity example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
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

## Weather

`<Weather>` adds animated rain or snow directly to a `GameScene`. Unlike Ponder weather presets,
scene weather is not timeline-owned: it keeps looping during normal scene rendering, it does not
fade in or fade out, and it cannot be paused or scrubbed independently. The renderer still uses the
same precipitation geometry path as Ponder weather, so local preview and site export stay aligned.

| Attribute | Default | Description |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weather kind. Supported values: `rain`, `snow`. |
| `x`, `z` | scene bounds | Covered precipitation columns. A scalar targets one column. Arrays use endpoint pairs to define one or more rectangles. |
| `density` | type-specific | Coverage density. Higher values keep more precipitation columns active; lower values sparsify the effect. |

Notes:

- `<Weather>` ignores `y`; the vertical span is derived from the current scene bounds and from the
  highest precipitation-blocking block in each covered column.
- If one axis has unmatched extra array values, the unmatched tail is ignored.
- Within one weather declaration, rain and snow never stack on the same `x/z` column. If multiple
  weather tags overlap, earlier tags keep the shared columns.
- Different non-overlapping columns in the same `GameScene` can render rain and snow at the same
  time.

Example:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
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
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
