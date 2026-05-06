---
navigation:
  title: Recipes But Test Yes
  parent: index.md
---

# Recipes

`<Recipe>`, `<RecipeFor>`, and `<RecipesFor>` widget tests.

> Recipe boxes are rendered by NEI's native handlers — crafting table, furnace, brewing stand, etc. all show their own background/animation. Without NEI, we fall back to a built-in 3x3 crafting display.

The small icon in the top-left shows which "recipe pool" the entry belongs to (sourced from `GuiRecipeTab.handlerMap`).

## Basic Examples

<RecipeFor id="minecraft:wooden_door" />
<Recipe id="minecraft:missingrecipe" fallbackText="This recipe is not registered." />
<RecipeFor id="minecraft:iron_pickaxe" />

## Vanilla Recipe Types

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

## Handler Filters

`id` accepts `modid:name[:meta[:nbt]]`:
- Missing `meta` defaults to `0`.
- `32767`, `*`, or any uppercase-letter token (e.g. `W`, `ANY`) acts as a wildcard.
- An SNBT tail (beginning with `{`) carries NBT data.
- Filter attributes `handlerName` (substring), `handlerId` (overlay id, exact), and `handlerOrder` (0-based index).

**Anvil** (overlay id `"repair"`):

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

## Input / Output / Limit Filters

`input` matches any ingredient slot, `output` matches the result slot, `limit` caps the rendered count. All accept the full extended id syntax (wildcards included).

- Planks → stick (`input` filter keeps only plank-sourced variants):<br/>
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

## Ore Dictionary & Extended ID

**Meta wildcard / NBT id samples:**

- Wildcard `*`: <ItemImage id="minecraft:wool:*" /> wool (any color)
- Wildcard `ANY`: <ItemImage id="minecraft:dye:ANY" /> dye
- Concrete meta: <ItemImage id="minecraft:wool:14" /> red wool
- Carrying NBT (bare identifiers can skip quotes in SNBT): <ItemImage id="minecraft:written_book:0:{title:TestBook,author:GuideNH}" />

**Ore dictionary `ore` attribute:**

- First match item: <ItemImage ore="ingotIron" /> iron ingot via ore name
- First match text link: <ItemLink ore="stickWood" />
- Block item form: <BlockImage ore="logWood" scale="3" />
- `ore` takes precedence over `id`: <ItemImage id="minecraft:apple" ore="gemDiamond" />

<GameScene width="192" height="128" zoom={5} interactive={true}>
  <Block ore="logWood" />
  <Block ore="logWood" x="2" meta="1" />
</GameScene>
