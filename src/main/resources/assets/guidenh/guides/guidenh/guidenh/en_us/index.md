---
item_ids:
  - guidenh:guide
navigation:
  title: Root
---

# Start Page

[Japanese](./japanese.md)

[Markdown](./markdown.md)

<Recipe id="missingrecipe" fallbackText="The recipe for special item is disabled." />

Welcome to the world of <ItemImage id="minecraft:stone" />, <PlayerName />!

Keybinding Test: <KeyBind id="key.jump" />. Unbound key: <KeyBind id="key.attack" />.

You may ~~need~~ a <Color color="#ff0000">door</Color> <Color id="RED">door</Color>!

<CommandLink command="/tp @s 0 90 0" title="Tooltip" close={true}>Teleport!</CommandLink>

<BlockImage id="minecraft:crafting_table" />

<ItemLink id="minecraft:stick" />

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:stone" x="2" />
    <Block id="minecraft:redstone_wire" y="1" />
    <Block id="minecraft:redstone_wire" x="1" y="1" />
    <Block id="minecraft:redstone_wire" x="2" y="1" />
    <Block id="minecraft:lever" x="-1" y="1" />
    <Block id="minecraft:redstone_lamp" x="3" y="1" />
</GameScene>

<RecipeFor id="minecraft:wooden_door" />
<Recipe id="minecraft:missingrecipe" fallbackText="This recipe is not registered." />
<RecipeFor id="minecraft:iron_pickaxe" />

<GameScene zoom={2} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="1" />
  <Block id="minecraft:log" z="1" />
  <Block id="minecraft:log" x="1" z="1" />

  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      This will be shown in the tooltip! <ItemImage id="minecraft:stone" />
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>

<GameScene zoom="8" interactive={true}>
  <Block id="minecraft:end_portal_frame" />
  <Block id="minecraft:end_portal_frame" x="1" />
  <Block id="minecraft:end_portal_frame" x="2" />
  <Block id="minecraft:end_portal_frame" z="2" />
  <Block id="minecraft:end_portal_frame" x="1" z="2" />
  <Block id="minecraft:end_portal_frame" x="2" z="2" />
  <Block id="minecraft:end_portal_frame" z="1" />
  <Block id="minecraft:end_portal_frame" x="2" z="1" />
</GameScene>

## Recipes

> Recipe boxes are rendered by NEI's native handlers — crafting table, furnace, brewing stand, etc. all show their own background/animation. Without NEI, we fall back to a built-in 3x3 crafting display.

The small icon in the top-left shows which "recipe pool" the entry belongs to (sourced from `GuiRecipeTab.handlerMap`).

**Vanilla 3x3 crafting:**

<Row>
    <RecipeFor id="minecraft:planks" />
    <RecipeFor id="minecraft:bed" />
    <RecipeFor id="minecraft:stick" />
    <RecipesFor id="minecraft:chest" />
</Row>

**Furnace smelting:**

<Row>
    <RecipeFor id="minecraft:iron_ingot" />
    <RecipeFor id="minecraft:glass" />
    <RecipeFor id="minecraft:brick" />
</Row>

**Brewing stand:**

<Row>
    <RecipeFor id="minecraft:speckled_melon" />
    <RecipeFor id="minecraft:fermented_spider_eye" />
</Row>

**Multi-recipe (`RecipesFor` returns all):**

<RecipesFor id="minecraft:torch" />

### Recipe filters & extended id syntax

`id` now accepts `modid:name[:meta[:nbt]]`:
- Missing `meta` defaults to `0`.
- `32767`, `*`, or any uppercase-letter token (e.g. `W`, `ANY`) acts as a wildcard.
- An SNBT tail (beginning with `{`) carries NBT data.
- New filter attributes `handlerName` (substring, case-insensitive), `handlerId` (overlay identifier, exact), and `handlerOrder` (0-based index, applied after the two filters).

**Anvil:** the anvil handler is NEI's `RepairRecipeHandler` (overlay id `"repair"`).

<Row>
    <RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
    <RecipesFor id="minecraft:diamond_sword" handlerId="repair" />
</Row>

**Crafting table — shaped:**

<RecipesFor id="minecraft:chest" handlerName="shaped" />

**Crafting table — shapeless:**

<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />

**Furnace smelting:**

<Row>
    <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
    <RecipeFor id="minecraft:glass" handlerId="smelting" />
    <RecipeFor id="minecraft:brick" handlerId="smelting" />
</Row>

**Fuel:**

<Row>
    <RecipeFor id="minecraft:coal" handlerId="fuel" />
    <RecipeFor id="minecraft:planks:*" handlerId="fuel" fallbackText="No planks fuel entry." />
</Row>

**Brewing stand:**

<Row>
    <RecipeFor id="minecraft:speckled_melon" handlerId="brewing" />
    <RecipeFor id="minecraft:fermented_spider_eye" handlerId="brewing" />
</Row>

**`handlerOrder` picks a single entry:**

<Row>
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="0" />
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="1" fallbackText="Only one recipe available." />
</Row>

**`input` / `output` / `limit` filters:** `input` matches any ingredient slot, `output` matches the result slot, `limit` caps the rendered count. All three accept the full extended id syntax (wildcards included) and share a single recipe-iteration pass, so no extra cost relative to un-filtered rendering.

- Planks → stick (the `input` filter keeps only plank-sourced variants):<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />

- Any stick-fed recipe (`output` filter selects torches):<br/>
  <RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />

- `input + output` combined (only the concrete plank-source crafting-table entry):<br/>
  <RecipesFor id="minecraft:crafting_table" input="minecraft:planks:*" output="minecraft:crafting_table" limit="1" />

- **Multi-value filter (comma-separated, OR semantics):** both `input` and `output` accept a list, and any hit counts.<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*,minecraft:log:*" limit="4" />

- **Expression syntax**: `,` = OR, `&` = AND (all terms in a group must match), `!` prefix = NOT.<br/>
  Redstone torch (stick AND redstone together): <RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />

  Stick recipes that do NOT use oak planks: <RecipesFor id="minecraft:stick" input="!minecraft:planks:0" limit="3" />

**Meta wildcard / NBT id samples:**

- Wildcard `*`: <ItemImage id="minecraft:wool:*" /> wool (any color)
- Wildcard `ANY`: <ItemImage id="minecraft:dye:ANY" /> dye
- Concrete meta: <ItemImage id="minecraft:wool:14" /> red wool
- Carrying NBT (bare identifiers can skip quotes in SNBT): <ItemImage id="minecraft:written_book:0:{title:TestBook,author:GuideNH}" />

***

<Row>
  <BlockImage id="minecraft:log" scale="4" />
  <BlockImage id="minecraft:log2" scale="4" />
  <BlockImage id="minecraft:planks" scale="4" />
  <BlockImage id="minecraft:cobblestone" scale="4" />
  <BlockImage id="minecraft:stonebrick" scale="4" />
  <BlockImage id="minecraft:mossy_cobblestone" scale="4" />
</Row>

<ItemImage id="minecraft:compass" />

***

## Image Rendering Test

Relative path (test1.png in the same directory):

![Test Image](test1.png)

Inline image mixed with text: here ![inline](test1.png) is an inline image.

### Explicit Size / Stretch

`<FloatingImage>` now accepts `width` / `height` (pixels): giving one keeps the
aspect ratio; giving both **stretches** the image (ratio not preserved); giving
neither falls back to the default "natural / 4 + availableWidth clamp".

Fixed 64×64 (single dim, keeps ratio):

<FloatingImage src="test1.png" align="left" width="64" title="width=64" />

Forced 200×80 stretch (ratio not preserved):

<FloatingImage src="test1.png" align="right" width="200" height="80" title="stretch 200x80" />

Fixed height 40 (width derived):

<FloatingImage src="test1.png" align="left" height="40" title="height=40" />

## 3D Preview Sizes

256×96 (wide/short):

<GameScene width="256" height="96" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Block id="minecraft:stone" x="3" />
</GameScene>

128×128 (square):

<GameScene width="128" height="128" zoom={6} interactive={true}>
  <Block id="minecraft:diamond_block" />
</GameScene>

384×256 (large):

<GameScene width="384" height="256" zoom={3} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:iron_block" z="1" />
  <Block id="minecraft:iron_block" x="1" z="1" />
  <Block id="minecraft:iron_block" x="2" z="1" />
  <Block id="minecraft:gold_block" y="1" x="1" z="1" />
</GameScene>

## ItemStack Scale Test

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

### Inline icon vs. text baseline

Inline `<ItemImage>` icons are nudged upward by ~2 pixels (scaled by `scale`) so their visual center lines up with the surrounding text baseline.

- Default offset (-2px): this line mixes <ItemImage id="minecraft:diamond" /> diamond, <ItemImage id="minecraft:apple" /> apple and <ItemImage id="minecraft:iron_ingot" /> iron ingot.
- Disabled (`yOffset="0"`): <ItemImage id="minecraft:diamond" yOffset="0" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" /> apple and <ItemImage id="minecraft:iron_ingot" yOffset="0" /> iron ingot.
- Larger offset (`yOffset="-4"`): <ItemImage id="minecraft:diamond" yOffset="-4" /> diamond, <ItemImage id="minecraft:apple" yOffset="-4" /> apple and <ItemImage id="minecraft:iron_ingot" yOffset="-4" /> iron ingot.

> Values are pixels at `scale=1` and are multiplied by the current scale at render time. The global default lives in `LytItemImage.DEFAULT_INLINE_Y_OFFSET` and can be mutated at guide-load time.

<Row>
  <BlockImage id="minecraft:stone" scale="1" />
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" scale="3" />
  <BlockImage id="minecraft:stone" scale="4" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

## Tooltip Hover Test

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

### ItemStack tooltip toggle & action

Demonstrate the tooltip / click action combinations on the same item:

* **Default** (shows vanilla item tooltip): <ItemImage id="minecraft:diamond_sword" scale="2" />
* **Tooltip disabled** (`noTooltip="true"`): <ItemImage id="minecraft:diamond_sword" scale="2" noTooltip="true" />
* **ItemLink** (hover shows item tooltip + click navigates to the item's page if indexed):
  <ItemLink id="minecraft:compass" />
* **Plain text link** (hover shows target-page tooltip, click navigates):
  [Go to sub-page example](subpage.md) ——
  you can also place <ItemImage id="minecraft:book" scale="1" /> next to a link for decoration.

This verifies: (1) `<ItemImage>` renders the vanilla item tooltip on hover;
(2) `noTooltip` disables that tooltip; (3) `<ItemLink>` fires both tooltip & click navigation.

## Tooltip Content Rendering Test

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

Use `<Tooltip label="...">` to wrap arbitrary MDX content as a hover trigger.
When hovering the trigger text, the tooltip box auto-sizes to fit its content
and flips against the screen edges like vanilla `drawHoveringText`.

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

## DiamondAnnotation (hover rich-content tooltip)

Place a **diamond marker** at any world coordinate inside a 3D preview. It uses `diamond.png`
(two 16×16 tiles — the right half is tinted by the `color` attribute and overlaid on the left
base). The diamond always faces the screen; hovering shows a **semi-transparent white overlay**
like vanilla ItemStack hover, while its compiled child content is rendered as a rich tooltip.

**Example: activated beacon** (3×3 diamond block base, beacon on top; marker hovers above the
beacon and its tooltip contains a side 3D preview of the full beacon column plus colored text):

<GameScene width="256" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:diamond_block" x="-1" z="-1" />
  <Block id="minecraft:diamond_block"         z="-1" />
  <Block id="minecraft:diamond_block" x="1"  z="-1" />
  <Block id="minecraft:diamond_block" x="-1" />
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:diamond_block" x="1" />
  <Block id="minecraft:diamond_block" x="-1" z="1" />
  <Block id="minecraft:diamond_block"         z="1" />
  <Block id="minecraft:diamond_block" x="1"  z="1" />
  <Block id="minecraft:beacon" y="1" />
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <Color color="#FFD24C">**Effect**</Color>: grants nearby players continuous buffs — speed /
    jump boost / resistance / strength / regeneration.

    Activation: a 3×3 / 5×5 / 7×7 / 9×9 pyramid of **diamond / iron / gold / emerald / netherite**
    blocks beneath the beacon.

    <GameScene width="160" height="128" zoom={5} interactive={false}>
      <Block id="minecraft:diamond_block" x="-1" />
      <Block id="minecraft:diamond_block" />
      <Block id="minecraft:diamond_block" x="1" />
      <Block id="minecraft:beacon" y="1" />
    </GameScene>

    <Color color="#AAFFAA">Tip</Color>: more pyramid tiers = more effect options; the beam color
    is determined by stained glass placed in the beam path.
  </DiamondAnnotation>
</GameScene>

## TileEntity / Directional / Non-full Block Tests

Chest, furnace (default facing south so its front is visible), redstone block, piston, beacon:

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:redstone_block" x="4" />
  <Block id="minecraft:piston" x="6" facing="south" />
  <Block id="minecraft:beacon" x="8" />
  <Block id="minecraft:iron_block" x="8" y="-1" />
</GameScene>

Furnaces in four facings:

<GameScene width="384" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:furnace" facing="north" />
  <Block id="minecraft:furnace" x="2" facing="south" />
  <Block id="minecraft:furnace" x="4" facing="west" />
  <Block id="minecraft:furnace" x="6" facing="east" />
</GameScene>

Stairs / slabs / fence / trapdoor (non-full cubes):

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:oak_stairs" />
  <Block id="minecraft:stone_stairs" x="2" meta="1" />
  <Block id="minecraft:stone_slab" x="4" />
  <Block id="minecraft:stone_slab" x="4" y="1" meta="8" />
  <Block id="minecraft:fence" x="6" />
  <Block id="minecraft:fence" x="6" z="1" />
  <Block id="minecraft:trapdoor" x="8" />
</GameScene>

## Camera preset + offset test

`<GameScene>` / `<Scene>` new attributes:

* `perspective="isometric_north_east"` / `isometric_north_west"` / `up"` — pick a yaw/pitch/roll preset;
* `rotateX` / `rotateY` / `rotateZ` — explicit per-axis overrides applied on top of the preset;
* `offsetX` / `offsetY` — screen-space pan (units: blocks);
* `centerX` / `centerY` / `centerZ` — explicit world-space rotation center (overrides auto-center).

NE vs NW preset:

<Row>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
  </GameScene>
  <GameScene width="160" height="128" zoom={5} perspective="isometric_north_west" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:planks" z="1" />
  </GameScene>
</Row>

Top-down view (`up`):

<GameScene width="192" height="160" zoom={5} perspective="up" interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:gold_block" z="1" />
  <Block id="minecraft:gold_block" x="1" z="1" />
</GameScene>

`offsetX` / `offsetY` pan (right scene offset by +2 / +1 blocks):

<Row>
  <GameScene width="160" height="128" zoom={4} interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
  <GameScene width="160" height="128" zoom={4} offsetX="2" offsetY="1" interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
</Row>

## DiamondAnnotation default color test

Without `color`, the diamond should default to **bright green** (compare against an explicit red diamond):

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="2" />
  <DiamondAnnotation pos="0.5 1.5 0.5">
    Default green diamond (no `color` attribute)
  </DiamondAnnotation>
  <DiamondAnnotation pos="2.5 1.5 0.5" color="#FF0000">
    Explicit red
  </DiamondAnnotation>
</GameScene>

* Markdown inline mix: **bold** / *italic* / ~~strike~~ / `code` / [link](./japanese.md)
