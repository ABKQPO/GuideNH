---
navigation:
  title: Import Structure
  parent: index.md
  position: 38
categories:
  - scenes
---

# Import Structure

`<ImportStructure>` and `<ImportStructureLib>` expand external structure data into a `<GameScene>`.

## StructureLib Preview

`<ImportStructureLib controller="modid:name" />` loads the multiblock structure registered by a StructureLib controller:

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
</GameScene>

Hover blocks in the StructureLib preview to inspect the extra structure text. Hold `Shift` to expand replacement candidates. If the imported structure exposes hatch or channel metadata, the preview also adds the hatch highlight button and the bottom sliders automatically.

## ImportStructure + RemoveBlocks

`<ImportStructure src="..." />` expands an external SNBT/NBT file. `<RemoveBlocks id="..." />` strips blocks by id after import:

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>

## ReplaceBlock

`<ReplaceBlock from="..." to="..." />` replaces matching already-placed blocks with a new block. The
search is global when no bounds are specified, or restricted to a box when any of `x/y/z/dx/dy/dz`
are provided:

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
  <ReplaceBlock from="minecraft:cobblestone" to="minecraft:brick_block" x="1" y="0" z="1" dx="3" dy="1" dz="3" />
</GameScene>

Add `from_nbt` to narrow the match to blocks whose TileEntity NBT contains specific keys, and
`to_nbt` to supply tile entity data for the replacement block.

## PlaceBlock

`<PlaceBlock id="..." />` fills an axis-aligned box unconditionally, overwriting whatever was
already there. Use `dx`/`dy`/`dz` to fill multi-block regions:

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <PlaceBlock id="minecraft:stone" dx="5" dy="1" dz="5" />
  <PlaceBlock id="minecraft:glass" y="1" dx="5" dz="5" />
</GameScene>

## SNBT File Format

`<ImportStructure src="..." />` accepts SNBT (1.7.10's stock `JsonToNBT`: `pos:[0,1,2]` is recognized as IntArray automatically; the modern `[I; ...]` typed-array prefix is **not** supported and must be omitted). Gzipped or uncompressed binary NBT also work. Schema is `{size, palette, blocks}`; each block entry may carry `meta` and an optional `nbt` compound (the latter must include the vanilla TileEntity `id` field, e.g. `"Chest"`). Optional `x/y/z` attributes translate the whole structure.

This example references `assets/example_structure.snbt`: a 5×3×5 cobblestone platform with a glowstone center, four corner stairs in different facings (meta=2/3), two stone slabs (meta=0 bottom, meta=8 top), two upward-facing torches (meta=5), and a chest pre-filled with diamonds, iron, redstone and bread via TileEntity NBT.

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotation color="#ffd24c" pos="2 1 2" alwaysOnTop={true}>
    **Loaded chest carries TileEntity NBT** with diamonds, iron, redstone and bread. The SNBT entry:

    ```
    {pos:[2,1,2], state:4, meta:3, nbt:{id:"Chest", Items:[
      {Slot:0b, id:"minecraft:diamond",    Count:5b,  Damage:0s},
      {Slot:1b, id:"minecraft:iron_ingot", Count:32b, Damage:0s},
      ...
    ]}}
    ```
  </BlockAnnotation>
  <BoxAnnotation color="#ee3333" min="1 1 1" max="4 1.5 2" thickness="0.04">
    **Slab strip**: the two slabs use `meta=0` (bottom half) and `meta=8` (top half).
  </BoxAnnotation>
  <LineAnnotation color="#33ddee" from="1 1.4 3" to="3 1.4 3" thickness="0.06">
    **Torch line**: both torches use `meta=5` (standing on the floor).
  </LineAnnotation>
</GameScene>

The region wand uses one client-side global selection shared by all wand stacks. Left click sets Pos1, right click sets Pos2, and both clicks can target air at the cursor reach endpoint. Sneak + left click clears the current selection; sneak + right click exports with the wand's current mode. You can also use `/guidenhc pos1 <x> <y> <z>`, `/guidenhc pos2 <x> <y> <z>`, and `/guidenhc clearselection`; `~` coordinates are relative to the player. `/guidenhc exportstructure [--mode snbt|snbt_e|blocks|blocks_e]` exports the current selection, or accepts an explicit `<x> <y> <z> <sizeX> <sizeY> <sizeZ>`. Scene Editor reads the same client-side selection and, when the server also has GuideNH, first asks the server to export the selected blocks so TileEntity data comes from the authoritative world.
