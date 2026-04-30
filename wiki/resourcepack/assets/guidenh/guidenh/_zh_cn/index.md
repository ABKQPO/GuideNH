---
item_ids:
  - guidenh:guide
navigation:
  title: 首页
  icon_texture: test1.png
---

# 起始页

[日语](./japanese.md)

[Markdown 语法](./markdown.md)

[调试测试页](./debug.md)

<Recipe id="missingrecipe" fallbackText="这个特殊物品的配方已被禁用。" />

欢迎来到 <ItemImage id="minecraft:stone" /> 的世界，<PlayerName />！

按键测试：跳跃 = <KeyBind id="key.jump" />，攻击 = <KeyBind id="key.attack" />，GuideNH 热键 = <KeyBind id="key.guidenh.open_guide" />。

注释测试：前面的文字可见。{/* 这是一段会被忽略的行内注释 */}后面的文字也可见。

注释块测试：
{/*
这整个注释块都会被解析器忽略。
*/}
多行注释之后的文字仍然会显示。

你 ~~或许~~ 需要一扇 <Color color="#ff0000">门</Color> <Color id="RED">门</Color>！

<CommandLink command="/tp @s 0 90 0" title="提示文本" close={true}>传送！</CommandLink>

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
<Recipe id="minecraft:missingrecipe" fallbackText="该配方未注册。" />
<RecipeFor id="minecraft:iron_pickaxe" />

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    baby={true} 
    name="Circulation_"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>

<GameScene zoom={2} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="1" />
  <Block id="minecraft:log" z="1" />
  <Block id="minecraft:log" x="1" z="1" />

  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      这段文字会在悬停时显示！<ItemImage id="minecraft:stone" />
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

## 配方

> 配方框由 NEI 原生渲染器驱动——合成台、熔炉、酿造台等都会呈现各自的背景纹理与动画。若未安装 NEI，则自动回退到内置的 3x3 合成展示。

左上角小图标显示了该配方所属的"配方池"（由 NEI 的 `GuiRecipeTab.handlerMap` 提供）。

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

### 配方过滤 & id 格式测试

`id` 现在支持 `modid:name[:meta[:nbt]]` 四段式：
- 省略 `meta` 时默认视为 `0`。
- `meta` 填 `32767`、`*`、或任意大写字母（如 `W`、`ANY`）均视为通配符。
- 可在末尾追加 SNBT（以 `{` 开始）来携带 NBT 数据。
- 新增 `handlerName` / `handlerId` / `handlerOrder` 三个过滤属性，按名字包含 / overlay 标识等值 / 过滤后取第 N 个 的顺序叠加。

**铁砧（anvil）：** 铁砧在 NEI 里其实是 `RepairRecipeHandler`，overlay id 为 `"repair"`。

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

**`handlerOrder` 选取单条：** 下面两行分别取铁镐合成链路里过滤后的第 0 条和第 1 条配方。

<Row>
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="0" />
    <Recipe id="minecraft:iron_pickaxe" handlerOrder="1" fallbackText="只有一条配方。" />
</Row>

**`input` / `output` / `limit` 过滤：** `input` 匹配任一材料插槽，`output` 匹配结果插槽，`limit` 限制最终展示数量。全部使用拓展 id 语法（支持通配符），匹配在单次配方遍历内完成，无额外 O(n²)。

- 黑木板→木棍（`input` 确保所举配方来自此材料）：<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />

- 任意木棍参与的火把配方（按 `output` 过滤）：<br/>
  <RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />

- `input + output` 双向约束（仅显示具体基座进指定产出的一条）：<br/>
  <RecipesFor id="minecraft:crafting_table" input="minecraft:planks:*" output="minecraft:crafting_table" limit="1" />

- **多值过滤（逗号分隔，OR 语义）**：`input` / `output` 均支持多个候选 id，命中任一即计匹配。<br/>
  <RecipesFor id="minecraft:stick" input="minecraft:planks:*,minecraft:log:*" limit="4" />

- **表达式语法**：`,`=OR、`&`=AND（同组内全部约束）、`!`前缀=反向（排除该项）。<br/>
  迷红石火把（木棍+迷红石粉同时出现）：<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />

  排除黑木板来源的木棍配方：<RecipesFor id="minecraft:stick" input="!minecraft:planks:0" limit="3" />

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

## 图片渲染测试

相对路径引用当前目录下 `test1.png`：

![测试图片](test1.png)

段落内嵌图：这是一张图 ![inline](test1.png) 嵌在文字里。

### 图片显式尺寸 / 拉伸测试

`<FloatingImage>` 现在支持 `width` / `height`（像素）属性：只给一维则按纹理原比例换算另一维，
两维都给则**拉伸**（不保比例），都不给则沿用默认"纹理尺寸 / 4 + availableWidth 夹取"。

固定 64×64（等比缩到 64，另一维按比例）：

<FloatingImage src="test1.png" align="left" width="64" title="width=64" />

同一张图强制 200×80 拉伸（不保比例，演示横向被压扁）：

<FloatingImage src="test1.png" align="right" width="200" height="80" title="stretch 200x80" />

固定 高度 40（宽度按比例换算）：

<FloatingImage src="test1.png" align="left" height="40" title="height=40" />

## 不同大小的 3D 预览测试

256×96（矮宽）：

<GameScene width="256" height="96" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Block id="minecraft:stone" x="3" />
</GameScene>

128×128（方）：

<GameScene width="128" height="128" zoom={6} interactive={true}>
  <Block id="minecraft:diamond_block" />
</GameScene>

384×256（大）：

<GameScene width="384" height="256" zoom={3} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:iron_block" z="1" />
  <Block id="minecraft:iron_block" x="1" z="1" />
  <Block id="minecraft:iron_block" x="2" z="1" />
  <Block id="minecraft:gold_block" y="1" x="1" z="1" />
</GameScene>

StructureLib 导入示例：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
</GameScene>

将光标移到 StructureLib 结构方块上可以看到额外的结构说明；按住 `Shift` 会展开候选替换方块。如果该结构提供仓室或信道元数据，还会自动出现仓室高亮按钮和底部滑条。

导入结构后移除方块示例：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>

## 不同大小的 ItemStack 渲染测试

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

### 内联图标与文字的纵向对齐

内联的 `<ItemImage>` 默认会向上偏移约 2 像素（随 `scale` 等比例缩放），让图标视觉中心与文字基线对齐。

- 默认偏移（-2px）：这行里有 <ItemImage id="minecraft:diamond" /> 钻石 <ItemImage id="minecraft:apple" /> 苹果和 <ItemImage id="minecraft:iron_ingot" /> 铁锭。
- 禁用偏移（`yOffset="0"`）：这行里有 <ItemImage id="minecraft:diamond" yOffset="0" /> 钻石 <ItemImage id="minecraft:apple" yOffset="0" /> 苹果和 <ItemImage id="minecraft:iron_ingot" yOffset="0" /> 铁锭。
- 加大偏移（`yOffset="-4"`）：这行里有 <ItemImage id="minecraft:diamond" yOffset="-4" /> 钻石 <ItemImage id="minecraft:apple" yOffset="-4" /> 苹果和 <ItemImage id="minecraft:iron_ingot" yOffset="-4" /> 铁锭。

> 偏移量以 scale=1 下的像素数给出，实际渲染时会乘以当前 `scale`。全局默认值在 `LytItemImage.DEFAULT_INLINE_Y_OFFSET`，可在 guide 加载时修改。

<Row>
  <BlockImage id="minecraft:stone" scale="1" />
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" scale="3" />
  <BlockImage id="minecraft:stone" scale="4" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

## Tooltip 悬停测试

将光标移到下列元素上查看 tooltip：

* 物品图：<ItemImage id="minecraft:golden_apple" scale="2" />
* 方块图：<BlockImage id="minecraft:emerald_block" scale="2" />
* 配方：

<RecipeFor id="minecraft:crafting_table" />

* 3D 预览（悬停方块将显示白色 1px 描边 + 方块名）：

<GameScene width="256" height="160" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:crafting_table" x="1" z="1" />
  <Block id="minecraft:glass" x="-1" />
</GameScene>

### ItemStack tooltip 开关与操作

对同一个物品，演示是否显示 vanilla item tooltip 与是否绑定点击操作：

* **默认**（显示 vanilla item tooltip）：<ItemImage id="minecraft:diamond_sword" scale="2" />
* **禁用 tooltip**（`noTooltip="true"`，悬停不弹出）：<ItemImage id="minecraft:diamond_sword" scale="2" noTooltip="true" />
* **ItemLink**（同时 hover 出 item tooltip + 点击跳转到该物品所在页面，若已索引）：
  <ItemLink id="minecraft:compass" />
* **文本 a 链接**（悬停显示目标页 tooltip，点击跳转）：
  [跳到子页示例](subpage.md) ——
  也可把 <ItemImage id="minecraft:book" scale="1" /> 放在链接外作为装饰。

以上组合可验证：(1) hover 到 `<ItemImage>` 时是否渲染 vanilla item tooltip；
(2) `noTooltip` 能否关闭该 tooltip；(3) `<ItemLink>` 是否同时触发 tooltip 与点击跳转。

## Tooltip 内容渲染测试

测试 tooltip 内渲染不同内容类型的功能（悬停查看）：

**文本 tooltip**（链接 title 属性 → `TextTooltip`）：

* <a title="这是一个纯文本 tooltip 的示例">纯文本 tooltip 链接</a>
* <a title="多行文本换行测试\n第一行\n第二行\n第三行">多行文本 tooltip 链接</a>
* <a title="靠近屏幕右边缘时 tooltip 应自动贴到光标左侧而非超出屏幕">边界自适应 tooltip 测试</a>

**ItemStack tooltip**（物品链接 → `ItemTooltip`，复用原版物品 tooltip）：

* <ItemLink id="minecraft:diamond_sword" /> 普通物品
* <ItemLink id="minecraft:golden_apple" /> 食物
* <ItemLink id="minecraft:enchanted_book" /> 附魔物品
* <ItemLink id="minecraft:potion" /> 药水

**物品图 tooltip**（`<ItemImage>` / `<BlockImage>` 也会显示 vanilla item tooltip）：

<ItemImage id="minecraft:iron_ingot" scale="2" />
<ItemImage id="minecraft:diamond" scale="2" />
<BlockImage id="minecraft:chest" scale="2" />

**配方 tooltip**（配方内每个素材格都可单独悬停）：

<RecipeFor id="minecraft:furnace" />

**3D 预览内 tooltip**（悬停方块显示方块名 + 白色描边）：

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:gold_block" x="2" />
  <Block id="minecraft:redstone_block" x="4" />
</GameScene>

## 富内容 Tooltip（`<Tooltip>`）

使用 `<Tooltip label="...">` 将任意 MDX 内容包装成悬停触发器；
鼠标悬停触发文字时，tooltip 框会根据内容自动计算宽高，并参照原版 `drawHoveringText`
在屏幕边界自动翻转位置。

**纯 markdown 富文本**：

<Tooltip label="悬停查看 Markdown 富文本">
  ## Markdown 标题
  这是一段 **加粗** 与 *斜体* 文本。

  * 列表项 1
  * 列表项 2
  * 列表项 3
</Tooltip>

**嵌入 ItemStack / BlockImage**：

<Tooltip label="悬停查看物品和方块">
  包含 <ItemImage id="minecraft:diamond" scale="2" /> 钻石
  与 <BlockImage id="minecraft:diamond_block" scale="2" /> 钻石块。
</Tooltip>

**嵌入配方**：

<Tooltip label="悬停查看合成配方">
  熔炉的合成配方：
  <RecipeFor id="minecraft:furnace" />
</Tooltip>

**嵌入 3D 预览**（参数固定，tooltip 框自动适应尺寸）：

<Tooltip label="悬停查看 3D 预览">
  <GameScene width="192" height="96" zoom={5} interactive={false}>
    <Block id="minecraft:chest" />
    <Block id="minecraft:furnace" x="2" />
  </GameScene>
</Tooltip>

**混合富文本**：

<Tooltip label="悬停查看混合内容">
  ### 混合内容
  一段说明文字，随后是物品 <ItemImage id="minecraft:golden_apple" scale="2" />
  与一个配方：
  <RecipeFor id="minecraft:crafting_table" />
</Tooltip>

> 已实现的 tooltip 类型：`TextTooltip`（纯文本）、`ItemTooltip`（原版物品 tooltip）、
> `ContentTooltip`（任意 MDX 富内容，参见上方 `<Tooltip>` 用法）。

## DiamondAnnotation 菱形标注（悬停富文本 tooltip）

在 3D 预览任意世界坐标处放一个 **菱形标注**（`diamond.png` 的左右两半叠加，右半可通过 `color`
属性染色）。菱形始终朝向屏幕，光标悬停时会出现类似原版 ItemStack 的 **白色半透明遮罩**，同时
弹出标注子节点编译而成的富文本 tooltip。

**示例：激活的信标**（底座为 3×3 钻石块，顶部放一个信标；菱形标注悬停信标处，tooltip 内含
侧面的完整信标塔 3D 预览 + 彩色说明）：

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
    ### 激活的信标
    <Color color="#FFD24C">**效果**</Color>：为周围玩家提供速度 / 跳跃提升 / 抗性 / 力量 / 回复等 **持续增益**。

    激活条件：下方为 3×3 / 5×5 / 7×7 / 9×9 的 **钻石 / 铁 / 金 / 绿宝石 / 下界合金** 金字塔底座。

    <GameScene width="160" height="128" zoom={5} interactive={false}>
      <Block id="minecraft:diamond_block" x="-1" />
      <Block id="minecraft:diamond_block" />
      <Block id="minecraft:diamond_block" x="1" />
      <Block id="minecraft:beacon" y="1" />
    </GameScene>

    <Color color="#AAFFAA">提示</Color>：金字塔层数越多，可选效果越多，信标 **光柱颜色** 取决于光路上的染色玻璃。
  </DiamondAnnotation>
</GameScene>

## TileEntity / 方向性 / 不完整方块测试

箱子、熔炉（默认朝南能看到正面）、红石块、活塞、信标：

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:redstone_block" x="4" />
  <Block id="minecraft:piston" x="6" facing="south" />
  <Block id="minecraft:beacon" x="8" />
  <Block id="minecraft:iron_block" x="8" y="-1" />
</GameScene>

熔炉四个 facing 方向对比：

<GameScene width="384" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:furnace" facing="north" />
  <Block id="minecraft:furnace" x="2" facing="south" />
  <Block id="minecraft:furnace" x="4" facing="west" />
  <Block id="minecraft:furnace" x="6" facing="east" />
</GameScene>

楼梯 / 台阶 / 栅栏 / 活板门（不完整方块）：

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:oak_stairs" />
  <Block id="minecraft:stone_stairs" x="2" meta="1" />
  <Block id="minecraft:stone_slab" x="4" />
  <Block id="minecraft:stone_slab" x="4" y="1" meta="8" />
  <Block id="minecraft:fence" x="6" />
  <Block id="minecraft:fence" x="6" z="1" />
  <Block id="minecraft:trapdoor" x="8" />
</GameScene>

* Markdown 混合内联格式：**粗体** / *斜体* / ~~删除线~~ / `代码` / [链接](./japanese.md)

## 镜头预设 + 偏移测试

## 立体注解：盒子 / 方块 / 线

这三种注解都使用世界坐标，可被场景按钮的“显示/隐藏注解”切换；隐藏后既不会绘制，也不会触发悬浮 tooltip。它们的子节点是富文本，会作为悬停 tooltip 显示。

- `BoxAnnotation` 用 `min="x y z"` / `max="x y z"` 描述任意 AABB（支持小数）。
- `BlockAnnotation` 用 `pos="x y z"`（整数）选中某个方块，等价于 1×1×1 的盒子。
- `LineAnnotation` 用 `from="x y z"` / `to="x y z"` 画一条线段（支持小数）。

所有三种都支持 `color="#AARRGGBB" 或 "#RRGGBB"`、`thickness="0.0625"`（线宽，单位：像素）以及 `alwaysOnTop`（始终绘制在最前）。`thickness` 默认 `1`。

<GameScene width="384" height="224" zoom={4} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:gold_block" z="2" />
  <Block id="minecraft:gold_block" x="2" z="2" />

  <BoxAnnotation color="#ee3333" min="0 1 0" max="1 1.6 0.6" thickness="0.04">
    **盒子注解** 圈出半个方块，线宽 `0.04`。Tooltip 可以是任何富文本：

    <Row>
      <ItemImage id="minecraft:iron_ingot" scale="2" />
      炼钢：在熔炉里烧炼铁矿。
    </Row>
    <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
  </BoxAnnotation>

  <BlockAnnotation color="#33ddee" pos="2 0 2" alwaysOnTop={true}>
    **方块注解**：`alwaysOnTop` 让它穿透其他方块绘制。下面是黄金的合成配方：

    <RecipeFor id="minecraft:gold_block" />
  </BlockAnnotation>

  <LineAnnotation color="#ffd24c" from="0.5 1.2 0.5" to="2.5 1.2 2.5" thickness="0.08">
    **线段注解**：连接两个角，`thickness=0.08` 略粗一些。Tooltip 里能嵌三维预览：

    <GameScene width="160" height="96" zoom={5} perspective="isometric_north_east" interactive={false}>
      <Block id="minecraft:iron_block" />
      <Block id="minecraft:gold_block" x="1" />
      <DiamondAnnotation pos="0.5 1.2 0.5" color="#ffd24c">连接点 A</DiamondAnnotation>
      <DiamondAnnotation pos="1.5 1.2 0.5" color="#ee3333">连接点 B</DiamondAnnotation>
    </GameScene>
  </LineAnnotation>
</GameScene>

## ImportStructure 导入结构

`<ImportStructure src="..." />` 把外部 SNBT/NBT 结构文件展开到当前 `<GameScene>`。声明格式与 SNBT 一致（1.7.10 原生 `JsonToNBT`：`pos:[0,1,2]` 会被识别为 IntArray，不需要现代的 `[I; ...]` 前缀），也可读取 gzip / 未压缩的二进制 NBT。schema 为 `{size, palette, blocks}`，每个 `block` 可携带 `meta` 与 `nbt` （`nbt` 需含与原版一致的 TileEntity `id` 字段，例如 `"Chest"`）。`x/y/z` 属性可整体平移结构。

示例引用 `assets/example_structure.snbt`：5×3×5 鹅卵石平台、中心荧石、四角不同朝向的橡木台阶（meta=2/3）、两块上下不同的石质台阶（meta=0 / meta=8）、两支竖火把（meta=5），以及一个带着钻石/铁锭/红石/面包的箱子（TileEntity NBT）。

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotation color="#ffd24c" pos="2 1 2" alwaysOnTop={true}>
    **这个箱子是带 NBT 的**：里面预先装了钛石 / 铁锭 / 红石 / 面包。SNBT 写法：

    ```
    {pos:[2,1,2], state:4, meta:3, nbt:{id:"Chest", Items:[
      {Slot:0b, id:"minecraft:diamond",    Count:5b,  Damage:0s},
      {Slot:1b, id:"minecraft:iron_ingot", Count:32b, Damage:0s},
      ...
    ]}}
    ```
  </BlockAnnotation>
  <BoxAnnotation color="#ee3333" min="1 1 1" max="4 1.5 2" thickness="0.04">
    **台阶区**：meta `0` / `8` 分别对应下半、上半台阶。
  </BoxAnnotation>
  <LineAnnotation color="#33ddee" from="1 1.4 3" to="3 1.4 3" thickness="0.06">
    **竖火把连线**：这两支火把 meta=5（竖立在地面上）。
  </LineAnnotation>
</GameScene>

搭配区域选择魔棒：默认即 SNBT 模式，潜行+右键导出当前 `<ImportStructure>` 兼容的 SNBT；空中右键可在 `snbt` 与 `blocks`（旧版 `<Block>` 列表，向下兼容）之间切换。


`<GameScene>` / `<Scene>` 新增属性：

* `perspective="isometric_north_east"` / `isometric_north_west"` / `up"` —— 选择预设视角（yaw/pitch/roll）；
* `rotateX` / `rotateY` / `rotateZ` —— 在预设之上显式覆盖单个轴的旋转；
* `offsetX` / `offsetY` —— 屏幕空间平移（单位：方块）；
* `centerX` / `centerY` / `centerZ` —— 显式指定旋转中心（覆盖自动居中）。

NE 与 NW 预设对比：

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

从正上方俯视（`up`）：

<GameScene width="192" height="160" zoom={5} perspective="up" interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:gold_block" z="1" />
  <Block id="minecraft:gold_block" x="1" z="1" />
</GameScene>

`offsetX` / `offsetY` 屏幕空间平移（同一场景向右下偏移 2/1 个方块）：

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

## DiamondAnnotation 默认颜色测试

不传 `color` 时应默认为 **亮绿**（与显式指定 `#FF0000` 红色的菱形并排对比）：

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="2" />
  <DiamondAnnotation pos="0.5 1.5 0.5">
    默认绿色菱形（未指定 `color`）
  </DiamondAnnotation>
  <DiamondAnnotation pos="2.5 1.5 0.5" color="#FF0000">
    显式红色
  </DiamondAnnotation>
</GameScene>
