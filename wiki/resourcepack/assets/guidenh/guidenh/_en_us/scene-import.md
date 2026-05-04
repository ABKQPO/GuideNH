---
navigation:
  title: Import Structure
  parent: index.md
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

The region wand defaults to the SNBT mode and exports a structure compatible with `<ImportStructure>` on sneak + right-click. Right-click in air to toggle between `snbt` and the legacy `blocks` mode that emits an inline `<GameScene><Block .../>...</GameScene>`.
