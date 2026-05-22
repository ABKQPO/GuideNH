[English](GameScene)

# 游戏场景

`<GameScene>` 是 GuideNH 的 3D 预览标签，`<Scene>` 是具有相同行为的别名。

## 场景属性

| 属性 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `width` | integer | `256` | 视口宽度（像素） |
| `height` | integer | `192` | 视口高度（像素） |
| `zoom` | float | `1.0` | 相机缩放倍率 |
| `perspective` | string | `isometric-north-east` | 相机预设 |
| `rotateX` | float | auto | 显式 X 旋转覆盖值 |
| `rotateY` | float | auto | 显式 Y 旋转覆盖值 |
| `rotateZ` | float | auto | 显式 Z 旋转覆盖值 |
| `offsetX` | float | auto | 屏幕空间水平平移 |
| `offsetY` | float | auto | 屏幕空间垂直平移 |
| `centerX` | float | auto | 显式世界旋转中心 X |
| `centerY` | float | auto | 显式世界旋转中心 Y |
| `centerZ` | float | auto | 显式世界旋转中心 Z |
| `interactive` | boolean expression | `true` | 是否启用鼠标交互 |
| `showBackground` | boolean expression | `true` | 是否显示场景背景与边框 |
| `allowLayerSlider` | boolean | `true` | 是否显示垂直层滑块 |
| `gridButtonEnabled` | boolean | `true` | 是否显示地板网格切换按钮 |
| `showGrid` | boolean | `false` | 地板网格的初始可见性 |

## 方块统计框

包含方块的场景默认会启用方块统计切换按钮。需要覆盖模式、位置、过滤器、可见性或尺寸时，再添加 `<BlockStats>` 子标签。
列表只会在场景方块、思索时间线状态、StructureLib 选择或统计配置发生变化时重建；普通渲染帧会复用已经准备好的行数据。
列表会受 `maxWidth` 和 `maxHeight` 限制；如果省略，它们默认分别为最终场景宽高的 25%。内容过宽或过高时会出现可拖拽滚动条；鼠标在统计框上时滚轮会滚动列表，按住 Shift 可以横向滚动。

自动模式会扫描场景中的非空气方块，并尽量解析成玩家通常看到的物品显示。一个坐标内包含多个可见组件的方块可以贡献多个物品，
例如安装对应模组时的 AE2 cable bus 部件和 facade、ForgeMultipart 部件掉落、Carpenters' Blocks cover 或 overlay。
统计会按 `item:meta` 分组、按数量排序。

自动模式列表也可以用 `dock="left"`、`dock="top"`、`dock="right"` 或 `dock="bottom"` 吸附到场景外侧。
外侧吸附会根据吸附边的长度自动换列或换行，预留布局空间，并在右侧吸附时避开场景按钮列。
点击自动统计列表中的物品会用解析到的碰撞箱，以穿透显示的面覆盖高亮场景里所有对应方块；再次点击同一物品会取消高亮。
数量通过 ItemStack 自带的堆叠数量渲染。设置 `showNames={true}` 后会在名称后面也追加数量，鼠标悬停物品时 Tooltip 会额外显示精确方块数量。

可以用过滤器隐藏常见方块，或者只显示指定方块：

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

如果想展示规划材料表，而不是场景真实内容，可以使用手动模式：

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## Debug 模式叠加层

在 GuideNH Mod 配置中启用 `enableDebugMode` 选项后，3D 场景预览将提供以下额外叠加层。

### 网格坐标标签

当 debug 模式**开启**且地板网格**可见**时，坐标标签将渲染在各网格线旁：

- **X 轴数字**：沿网格的近侧边缘（默认 `isometric-north-east` 视角下为北/−Z 边）显示，
  每个整数 X 世界坐标处各有一个标签。
- **Z 轴数字**：沿网格的近侧边缘（东/+X 边）显示，每个整数 Z 世界坐标处各有一个标签。
- **基本方向首字母**（`N`/`S`/`E`/`W`）：绘制在各方向对应网格边缘的中点处。

坐标值使用场景 Level 中存储的实际世界 X/Z 数值，因此当结构包含负坐标方块时，数字可以为负。

只要 debug 模式激活，**网格切换按钮将始终可用**，不受 `gridButtonEnabled` 属性限制，
随时可显示或隐藏网格及其坐标标签；`showGrid` 属性控制的默认网格可见性不受影响。

### 方块坐标 Tooltip

当 debug 模式**开启**且鼠标悬停在场景内的方块上时，除主 Tooltip 外，还会在其上方渲染
一个额外 Tooltip，以金色文字显示该方块的世界空间坐标 `X, Y, Z`。

若坐标 Tooltip 超出屏幕顶部则自动磁吸到光标下方显示。

## 视角预设

可接受的 `perspective` 值：

- `isometric-north-east`
- `isometric-north-west`
- `up`

未知值会回退到 `isometric-north-east`。

## 内容嵌入与文字环绕

所有块级标签（包括 `<GameScene>`）均支持两个可选属性，用于控制其在页面中的嵌入方式，对应 Microsoft Word 的"文字环绕"选项。

| 属性 | 可选值 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | 文字环绕模式 |
| `align` | `left` · `center` · `right` | `left` | 水平对齐方式 |

### 环绕模式

| 模式 | Word 对应 | 效果 |
| --- | --- | --- |
| `inline` | 嵌入型 | 默认行为：场景独占一行（嵌入型） |
| `square` | 方形 | 场景浮动到左侧或右侧，文字在其周围方形环绕（方形环绕） |
| `tight` | 紧密型 | 更紧密的环绕；本布局系统中等价于 `square`（紧密型） |
| `through` | 穿越型 | 穿越型环绕；本布局系统中等价于 `square`（穿越型） |
| `top-bottom` | 上下型 | 文字仅在上下方，不在侧面；`align` 控制水平位置（上下型） |
| `behind` | 衬于文字下方 | 场景渲染在文字下方；`align` 控制水平位置（衬于文字下方） |
| `front` | 浮于文字上方 | 场景渲染在文字上方；`align` 控制水平位置（浮于文字上方） |

### 示例

左浮动场景——后续段落文字环绕在右侧：

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

此处文字将环绕在场景右侧……
````

右浮动场景：

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

此处文字将环绕在场景左侧……
````

居中场景（无文字环绕）：

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

行内嵌入（流式上下文）——文字环绕小场景：

````md
一段文字 {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} 右侧继续的文字将自动环绕。
````

## 示例

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## 场景子元素

GuideNH 当前注册了以下场景子标签：

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
- 各类注解标签，例如 `<BoxAnnotation>` 和 `<LineAnnotation>`

## 场景音效

`<PlaySound>` 可以放在 `<GameScene>` 内，用于通过场景交互或时间轴进入时播放音效。
支持的触发方式：

- `click`，默认值
- `hover`，鼠标进入场景时触发一次
- `enter`，场景首次渲染时触发一次

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

提供 `x`、`y`、`z` 时，音量会从投影后的场景坐标到点击点或场景中心进行屏幕空间衰减。
`radius` 默认是场景较短边的 75%，`minVolume` 默认是 `0.15`。

## `<BlockStats>` 和 `<BlockStat>`

声明或自定义方块统计叠加框。包含方块的场景即使省略该子标签，也会启用自动统计切换按钮；如果包含 `<BlockStat>` 子项，则切换到手动统计模式。

`<BlockStats>` 属性：

| 属性 | 必需 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `visible` | 否 | config，默认 `false` | 初始是否显示统计框 |
| `buttonEnabled` | 否 | config，默认 `true` | 是否显示方块统计切换按钮 |
| `mode` | 否 | `auto` | `auto` 或 `manual`；子 `<BlockStat>` 会强制手动模式 |
| `corner` | 否 | `topRight` | 统计框角落：`topRight`、`topLeft`、`bottomRight` 或 `bottomLeft` |
| `dock` | 否 | `inside` | 自动列表可吸附到 `inside`、`left`、`top`、`right` 或 `bottom`；手动模式始终使用内部统计框 |
| `showNames` | 否 | `false` | 是否在图标旁显示名称；开启后名称后也会追加数量 |
| `filterMode` | 否 | `blacklist` | `blacklist` 或 `whitelist` |
| `filter` | 否 | 空 | 物品键，例如 `minecraft:stone` 或 `minecraft:stone:0`，可用空格、逗号或分号分隔 |
| `maxWidth` | 否 | 场景宽度的 25% | 统计框最大宽度，超出后出现水平滚动条 |
| `maxHeight` | 否 | 场景高度的 25% | 统计框最大高度，超出后出现垂直滚动条 |

`<BlockStat>` 属性：

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `item` | 是，除非使用 `id` | 列表中显示的物品 id |
| `id` | 是，除非使用 `item` | 既有物品栈属性写法 |
| `count` | 否 | 显示数量，默认 `0` |

示例：

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

在预览世界中放置一个方块。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是，除非提供了 `ore` | 方块 id |
| `ore` | 否 | 矿辞名；第一个匹配结果必须能解析成方块物品 |
| `x` | 否 | 世界坐标 X，整数，默认 `0` |
| `y` | 否 | 世界坐标 Y，整数，默认 `0` |
| `z` | 否 | 世界坐标 Z，整数，默认 `0` |
| `meta` | 否 | 方块 metadata，整数 |
| `facing` | 否 | `down`、`up`、`north`、`south`、`west`、`east` |
| `nbt` | 否 | SNBT TileEntity compound |

说明：

- 同时提供 `ore` 和 `id` 时，优先使用 `ore`；若安装了 GregTech，选中的结果还会先经过 `GTOreDictUnificator.setStack(...)` 统一化
- 若省略 `meta`，且 `ore` 解析出的物品携带具体且非通配符的 damage，则会先使用该值，再回退到 `facing`
- 若省略 `meta`，部分方块会根据 `facing` 推导合理默认值
- 若 `nbt` 能成功创建 TileEntity，预览中会使用该实体

示例：

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

把外部结构文件加载到场景中。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `src` | 是 | 结构资源路径 |
| `x` | 否 | 平移 X，整数（`offsetX` 的别名） |
| `y` | 否 | 平移 Y，整数（`offsetY` 的别名） |
| `z` | 否 | 平移 Z，整数（`offsetZ` 的别名） |
| `offsetX` | 否 | 平移 X，整数（优先于 `x`） |
| `offsetY` | 否 | 平移 Y，整数，会被限制在 `[0, 世界高度-1]`（优先于 `y`） |
| `offsetZ` | 否 | 平移 Z，整数（优先于 `z`） |

支持的格式：

- SNBT 文本
- gzip 压缩的二进制 NBT
- 未压缩的二进制 NBT

必须包含的结构键：

- `palette`
- `blocks`

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
````

## `<ImportStructureLib>`

通过控制器 id 导入 StructureLib 多方块预览。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `controller` | 是 | 控制器方块 id，格式为 `modid:block[:meta]` |
| `name` | 否 | 可选绑定名，供注解、模板和音效上的 `showWhenStructure` 使用 |
| `piece` | 否 | StructureLib piece 名称覆盖值 |
| `facing` | 否 | 传给导入器的朝向覆盖值 |
| `rotation` | 否 | 传给导入器的旋转覆盖值 |
| `flip` | 否 | 传给导入器的镜像覆盖值 |
| `channel` | 否 | 支持频道结构的频道整数覆盖值 |
| `offsetX` | 否 | 所有放置方块的 X 偏移，整数，默认 `0` |
| `offsetY` | 否 | 所有放置方块的 Y 偏移，整数，会被限制在 `[0, 世界高度-1]`，默认 `0` |
| `offsetZ` | 否 | 所有放置方块的 Z 偏移，整数，默认 `0` |

说明：

- 导入结构会从场景 `0 0 0` 开始；控制器不会被强制放在 `0 0 0`
- 若有足够元数据，该标签会启用 StructureLib 专用 tooltip、舱口高亮和频道滑块 UI
- 控制器匹配支持 GTNH 风格 `modid:block:meta`
- 当场景里有多个 StructureLib 导入，而且其他标签需要只针对其中一个结构状态时，请显式提供 `name`

示例：

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
````

按结构状态控制注解和音效的示例：

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
    只会在匹配的 `main` 状态下显示。
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

显式指定等轴相机的 yaw/pitch/roll。

若省略该标签，场景会继续使用 `<GameScene>` 的 `perspective` 预设。默认的
`isometric-north-east` 预设等价于：

````md
<IsometricCamera yaw="225" pitch="30" />
````

| 属性 | 含义 |
| --- | --- |
| `yaw` | float |
| `pitch` | float |
| `roll` | float |

示例：

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

移除所有已放置且匹配目标方块 id 的方块。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 要移除的方块 id，使用 `modid:block[:meta]` 格式 |

这在导入结构后非常有用，适合为了展示清晰度而隐藏某些方块。

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

将已放置且匹配来源方块 id（以及可选的 TileEntity NBT 部分匹配）的方块替换为新方块。
搜索范围可以是全局（所有已填充方块），也可以限定于一个轴对齐的包围盒。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `from` | 是 | 要匹配的来源方块，使用 `modid:block[:meta]` 格式 |
| `from_nbt` | 否 | 部分 SNBT 复合标签；仅当方块的 TileEntity NBT 包含所有列出的键时才匹配 |
| `to` | 是 | 替换目标方块，使用 `modid:block[:meta]` 格式 |
| `to_nbt` | 否 | 应用于替换方块的 SNBT TileEntity 复合标签 |
| `x` | 否 | 包围盒起始 X；只要 `x/y/z/dx/dy/dz` 中任意一个存在，就启用包围盒模式 |
| `y` | 否 | 包围盒起始 Y |
| `z` | 否 | 包围盒起始 Z |
| `dx` | 否 | 包围盒宽度（默认 `1`）|
| `dy` | 否 | 包围盒高度（默认 `1`）|
| `dz` | 否 | 包围盒深度（默认 `1`）|

说明：

- 若 `x/y/z/dx/dy/dz` 均未提供，则全局扫描所有已填充方块
- `from_nbt` 为**部分**匹配：仅检查模式中列出的键，TileEntity 中额外的键将被忽略
- 替换使用与 `<Block>` 相同的方块放置流程，支持 GregTech MetaTile 及 BartWorks 方块

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

用单一方块类型填充一个轴对齐的包围盒，覆盖原有方块。
与 `<Block>`（针对单个位置）不同，`<PlaceBlock>` 通过 `dx`/`dy`/`dz` 支持多方块区域。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 方块 id，使用 `modid:block[:meta]` 格式 |
| `nbt` | 否 | 应用于每个放置方块的 SNBT TileEntity 复合标签 |
| `x` | 否 | 区域起始 X，默认 `0` |
| `y` | 否 | 区域起始 Y，默认 `0` |
| `z` | 否 | 区域起始 Z，默认 `0` |
| `dx` | 否 | 区域宽度，默认 `1` |
| `dy` | 否 | 区域高度，默认 `1` |
| `dz` | 否 | 区域深度，默认 `1` |

说明：

- 包围盒内所有方块均会被无条件放置（不检查原有方块）
- NBT 复合标签在每次放置时独立复制
- 使用与 `<Block>` 相同的方块放置流程，完整支持 GregTech MetaTile 及 BartWorks 方块

示例：

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
````

## `<BlockAnnotationTemplate>`

将一个或多个子注解扩展到当前场景中所有已经存在的匹配方块上。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 方块匹配器，格式为 `modid:block[:meta]` |

规则：

- 应将其放在待匹配方块或导入结构之后
- 匹配发生在解析时，针对的是当前场景状态
- 子注解使用相对于每个匹配方块的局部坐标

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

向预览场景中加入实体。

这些属性遵循类似 summon 的实体放置与 SNBT 数据语义。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 实体类型 id；支持旧式名称如 `Sheep`、现代原版 id 如 `minecraft:sheep`，也支持 `modid.entityName` 或 `modid:entityName` 形式的模组实体 id |
| `x` | 否 | 实体中心点 X，float，默认 `0.5` |
| `y` | 否 | 实体底部 Y，float，默认 `0` |
| `z` | 否 | 实体中心点 Z，float，默认 `0.5` |
| `rotationY` | 否 | yaw，角度，默认 `-45` |
| `rotationX` | 否 | pitch，角度，默认 `0` |
| `data` | 否 | summon 风格 SNBT，会在生成前并入实体 NBT |
| `name` | 否 | 当 `id` 是 `player`、`fakeplayer`、`minecraft:player` 或 `minecraft:fakeplayer` 时使用的预览玩家名 |
| `uuid` | 否 | 使用上述玩家 id 时的预览玩家 UUID |
| `showName` | 否 | 控制预览玩家头顶名牌的布尔表达式；对玩家预览 id 默认 `true` |
| `showCape` | 否 | 控制预览玩家披风显示的布尔表达式；对玩家预览 id 默认 `true` |
| `headRotation` | 否 | 预览玩家头部旋转，格式为 `x y z` 角度 |
| `leftArmRotation` | 否 | 预览玩家左臂旋转，格式为 `x y z` 角度 |
| `rightArmRotation` | 否 | 预览玩家右臂旋转，格式为 `x y z` 角度 |
| `leftLegRotation` | 否 | 预览玩家左腿旋转，格式为 `x y z` 角度 |
| `rightLegRotation` | 否 | 预览玩家右腿旋转，格式为 `x y z` 角度 |
| `capeRotation` | 否 | 预览玩家披风旋转，格式为 `x y z` 角度；默认静止站立角度 `6 0 0` |

说明：

- 实体包围盒会参与场景自动居中和可见层筛选
- 当预览世界尚未准备好时，实体创建会优雅回退，并在首次渲染时绑定
- 玩家预览 id 会创建客户端侧 fake remote player，以复用普通玩家渲染器和皮肤管线
- 若玩家预览既未提供 `name` 也未提供 `uuid`，GuideNH 会回退到 `Steve` 和原版默认皮肤
- 只提供 `name` 时，GuideNH 会先尝试解析真实在线 profile 以加载皮肤和披风；失败时回退到稳定离线 UUID
- 只提供 `uuid` 时，GuideNH 会生成占位显示名，并继续尝试从 profile 解析皮肤
- `showName={false}` 会隐藏头顶名称，但仍走普通玩家渲染路径
- `showCape={false}` 会隐藏披风，同时仍保留普通玩家渲染路径和 Forge hook
- 玩家姿态属性使用三个以空格分隔的浮点数，对应模型 `X Y Z` 角度
- 若省略头部/四肢旋转，沿用原版 idle 姿态；若省略 `capeRotation`，回退到站立静止披风角度 `6 0 0`
- 玩家预览在解析时需要活动的客户端世界，因为 Minecraft 的玩家实体构造器无法脱离 world 创建
- 悬停实体时会显示其本地化显示名；若提供了自定义名，则显示自定义名

示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

预览玩家姿态示例：

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

预览玩家名称与披风示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## Weather

`<Weather>` 可直接为 `GameScene` 添加动画雨雪。与 Ponder 的天气预设不同，场景天气不归时间轴管理：
它会在普通场景渲染时持续循环，不带淡入淡出，也不能单独暂停或拖动。底层渲染仍复用与 Ponder
天气相同的降水几何路径，因此本地预览与 site export 的效果保持一致。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `weather` / `type` | `rain` | 天气类型。支持 `rain`、`snow`。 |
| `x`、`z` | 场景边界 | 覆盖的降水列。单值表示一列；数组按端点对定义一个或多个矩形区域。 |
| `density` | 按类型决定 | 覆盖密度。值越高，保留的降水列越多；值越低，效果越稀疏。 |

说明：

- `<Weather>` 不使用 `y`；垂直范围由当前场景边界以及每一列中最高的降水遮挡方块共同推导。
- 如果某一轴数组末尾有无法配对的多余值，这部分会被忽略。
- 在同一个天气声明内，同一个 `x/z` 列不会叠加雨和雪。多个天气标签发生重叠时，前面声明的标签优先占用共享列。
- 同一个 `GameScene` 中，不同且不重叠的列可以同时渲染雨和雪。

示例：

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## 相机中心行为

若未显式提供 `centerX/Y/Z`，GuideNH 会根据已放置方块的包围盒自动居中场景。若设置了任意一个显式中心坐标，则自动居中会被禁用，未提供的其余坐标默认 `0`。

## 交互说明

当 `interactive={true}` 时，场景支持旋转、平移、缩放、重置、注解开关，以及指南界面暴露出的其他交互控件。

- 跨越多个 Y 层的场景会在底部上方显示可见层滑块
- 若 StructureLib 元数据提供相关信息，场景底部还可能出现舱口高亮切换按钮和频道滑块
- 注解悬停优先级高于方块悬停；当没有悬停注解热点时，方块 tooltip 会正常显示
- StructureLib 悬停会把方块名称放在 tooltip 第一行，把结构专用文本放在第二行开始；按住 `Shift` 时还会展开替换候选项

## 相关页面

- [注解](Annotations-zh-CN)
- [结构导出](Structure-Export-zh-CN)
- [配方](Recipes-zh-CN)
- [示例](Examples-zh-CN)
