---
navigation:
  title: 配方（导航树标题）
  parent: index.md
---

# 配方（页面左上角标题）

`<Recipe>`、`<RecipeFor>` 和 `<RecipesFor>` 组件测试。

> 配方框由 NEI 原生渲染器驱动——合成台、熔炉、酿造台等都会呈现各自的背景纹理与动画。若未安装 NEI，则自动回退到内置的 3x3 合成展示。

左上角小图标显示了该配方所属的"配方池"（由 NEI 的 `GuiRecipeTab.handlerMap` 提供）。

## 基础示例

<RecipeFor id="minecraft:wooden_door" />
<Recipe id="minecraft:missingrecipe" fallbackText="该配方未注册。" />
<RecipeFor id="minecraft:iron_pickaxe" />

## 原版配方类型

**原版 3x3 合成示例：**

<Row>
    <RecipeFor id="minecraft:planks" />
    <RecipeFor id="minecraft:bed" />
    <RecipeFor id="minecraft:stick" />
    <RecipesFor id="minecraft:chest" />
</Row>

**熔炉（冶炼）示例：**

<Row>
    <RecipeFor id="minecraft:iron_ingot" />
    <RecipeFor id="minecraft:glass" />
    <RecipeFor id="minecraft:brick" />
</Row>

**酿造台示例：**

<Row>
    <RecipeFor id="minecraft:speckled_melon" />
    <RecipeFor id="minecraft:fermented_spider_eye" />
</Row>

**多配方展示（`RecipesFor` 返回全部）：**

<RecipesFor id="minecraft:torch" />

## 处理器过滤器

`id` 支持 `modid:name[:meta[:nbt]]` 四段式：
- 省略 `meta` 时默认视为 `0`。
- `meta` 填 `32767`、`*`、或任意大写字母（如 `W`、`ANY`）均视为通配符。
- 可在末尾追加 SNBT（以 `{` 开始）来携带 NBT 数据。
- 新增 `handlerName`（包含匹配）/ `handlerId`（精确匹配）/ `handlerOrder`（按序取一条）过滤属性。

**铁砧**（overlay id `"repair"`）：

<Row>
    <RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
    <RecipesFor id="minecraft:diamond_sword" handlerId="repair" />
</Row>

**工作台 — 有序合成（shaped）：**

<RecipesFor id="minecraft:chest" handlerName="shaped" />

**工作台 — 无序合成（shapeless）：**

<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />

**熔炉（furnace smelting）：**

<Row>
    <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
    <RecipeFor id="minecraft:glass" handlerId="smelting" />
    <RecipeFor id="minecraft:brick" handlerId="smelting" />
</Row>

**燃料（fuel）：**

<Row>
    <RecipeFor id="minecraft:coal" handlerId="fuel" />
    <RecipeFor id="minecraft:planks:*" handlerId="fuel" fallbackText="没有找到木板燃料条目。" />
</Row>

**炼药台（brewing）：**

<Row>
    <RecipeFor id="minecraft:speckled_melon" handlerId="brewing" />
    <RecipeFor id="minecraft:fermented_spider_eye" handlerId="brewing" />
</Row>

**`handlerOrder` 选取单条：**

<Row>
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="0" />
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="1" fallbackText="只有一条配方。" />
</Row>

## input / output / limit 过滤器

`input` 匹配任一材料插槽，`output` 匹配结果插槽，`limit` 限制最终展示数量。

- 木板→木棍（`input` 确保所举配方来自此材料）：<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />

- 任意木棍参与的火把配方（按 `output` 过滤）：<br/>
  <RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />

- `input + output` 双向约束：<br/>
  <RecipesFor id="minecraft:crafting_table" input="minecraft:planks:*" output="minecraft:crafting_table" limit="1" />

- **多值过滤（逗号分隔，OR 语义）**：<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*,minecraft:log:*" limit="4" />

- **表达式语法**：`,`=OR、`&`=AND、`!`前缀=NOT。<br/>
  迷红石火把（木棍+迷红石粉同时出现）：<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />

  排除黑木板来源的木棍配方：<RecipesFor id="minecraft:stick" input="!minecraft:planks:0" limit="3" />

## 矿辞与扩展 id

**meta 通配符 / NBT 传入测试：**

- 通配符 `*`：<ItemImage id="minecraft:wool:*" /> 羊毛（任意颜色）
- 通配符 `ANY`：<ItemImage id="minecraft:dye:ANY" /> 染料
- 具体 meta：<ItemImage id="minecraft:wool:14" /> 红色羊毛
- 携带 NBT（SNBT 中裸标识符可省略引号）：<ItemImage id="minecraft:written_book:0:{title:TestBook,author:GuideNH}" />

**矿辞 `ore` 属性测试：**

- 第一个匹配到的物品：<ItemImage ore="ingotIron" /> 铁锭
- 第一个匹配到的文本链接：<ItemLink ore="stickWood" />
- 方块物品形态：<BlockImage ore="logWood" scale="3" />
- `ore` 优先于 `id`：<ItemImage id="minecraft:apple" ore="gemDiamond" />

<GameScene width="192" height="128" zoom={5} interactive={true}>
  <Block ore="logWood" />
  <Block ore="logWood" x="2" meta="1" />
</GameScene>
