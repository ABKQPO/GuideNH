---
navigation:
  title: Tooltips
  parent: index.md
---

# Tooltips

Tooltip hover, content types, and the `<Tooltip>` rich-content container.

## Hover Test

Hover any of the following:

* Item image: <ItemImage id="minecraft:golden_apple" scale="2" />
* Block image: <BlockImage id="minecraft:emerald_block" scale="2" />
* Recipe:

<RecipeFor id="minecraft:crafting_table" />

* 3D preview (hover blocks to see a white 1-pixel outline + block name):

<GameScene width="256" height="160" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:crafting_table" x="1" z="1" />
  <Block id="minecraft:glass" x="-1" />
</GameScene>

### ItemStack Tooltip Toggle & Action

* **Default** (shows vanilla item tooltip): <ItemImage id="minecraft:diamond_sword" scale="2" />
* **Tooltip disabled** (`noTooltip="true"`): <ItemImage id="minecraft:diamond_sword" scale="2" noTooltip="true" />
* **ItemLink** (hover shows item tooltip + click navigates to the item's page if indexed):
  <ItemLink id="minecraft:compass" />
* **Plain text link** (hover shows target-page tooltip, click navigates):
  [Go to Markdown page](markdown.md) ——
  you can also place <ItemImage id="minecraft:book" scale="1" /> next to a link for decoration.

* Markdown inline mix: **bold** / *italic* / ~~strike~~ / `code` / [link](./japanese.md)

## Tooltip Content Types

Verify different tooltip content types (hover to inspect):

**Text tooltip** (link `title` attribute -> `TextTooltip`):

* <a title="Example of a plain-text tooltip">Plain text tooltip link</a>
* <a title="Multi-line wrap test\nLine 1\nLine 2\nLine 3">Multi-line tooltip link</a>
* <a title="When near the right edge of the screen, tooltip should flip to the left of the cursor instead of being clipped">Edge-adaptive tooltip test</a>

**ItemStack tooltip** (item link -> `ItemTooltip`, reuses vanilla item tooltip):

* <ItemLink id="minecraft:diamond_sword" /> Regular item
* <ItemLink id="minecraft:golden_apple" /> Food
* <ItemLink id="minecraft:enchanted_book" /> Enchanted item
* <ItemLink id="minecraft:potion" /> Potion

**Item image tooltip** (`<ItemImage>` / `<BlockImage>` also show vanilla item tooltip):

<ItemImage id="minecraft:iron_ingot" scale="2" />
<ItemImage id="minecraft:diamond" scale="2" />
<BlockImage id="minecraft:chest" scale="2" />

**Recipe tooltip** (each ingredient slot in a recipe can be hovered individually):

<RecipeFor id="minecraft:furnace" />

**3D preview hover tooltip** (hover a block to see its name + white outline):

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:gold_block" x="2" />
  <Block id="minecraft:redstone_block" x="4" />
</GameScene>

## Rich Content Tooltip (`<Tooltip>`)

Use `<Tooltip label="...">` to wrap arbitrary MDX content as a hover trigger. The tooltip box auto-sizes to fit its content and flips against the screen edges like vanilla `drawHoveringText`.

**Pure markdown rich text**:

<Tooltip label="Hover for Markdown rich content">
  ## Markdown heading
  A paragraph with **bold** and *italic* text.

  * List item 1
  * List item 2
  * List item 3
</Tooltip>

**Embedded ItemStack / BlockImage**:

<Tooltip label="Hover for item and block">
  Contains <ItemImage id="minecraft:diamond" scale="2" /> diamond
  and <BlockImage id="minecraft:diamond_block" scale="2" /> diamond block.
</Tooltip>

**Embedded recipe**:

<Tooltip label="Hover for crafting recipe">
  Furnace crafting recipe:
  <RecipeFor id="minecraft:furnace" />
</Tooltip>

**Embedded 3D preview** (fixed parameters; tooltip box auto-sizes):

<Tooltip label="Hover for 3D preview">
  <GameScene width="192" height="96" zoom={5} interactive={false}>
    <Block id="minecraft:chest" />
    <Block id="minecraft:furnace" x="2" />
  </GameScene>
</Tooltip>

**Mixed rich content**:

<Tooltip label="Hover for mixed content">
  ### Mixed content
  Some explanatory text, followed by an item <ItemImage id="minecraft:golden_apple" scale="2" />
  and a recipe:
  <RecipeFor id="minecraft:crafting_table" />
</Tooltip>

> Implemented tooltip types: `TextTooltip` (plain text), `ItemTooltip` (vanilla item tooltip),
> `ContentTooltip` (arbitrary MDX rich content — see `<Tooltip>` above).
