---
navigation:
  title: 方块场景
  parent: index.md
  position: 170
categories:
  - scenes
---

# 方块场景

本页既是游戏内可直接查看的示例页，也是 `<GameScene>` 方块内容的精简教程。它保留实际渲染示例，同时说明资源包页面最常用的参数：方块放置、TileEntity 数据、不完整方块、注解和方块统计框。

## 游戏场景基础参数

`<GameScene>` 会创建一个 3D 游戏场景预览区域。`<Scene>` 是等价别名。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `width` | `256` | 游戏场景视口宽度，单位为像素。 |
| `height` | `192` | 游戏场景视口高度，单位为像素。 |
| `zoom` | `1.0` | 相机缩放倍率。 |
| `perspective` | `isometric-north-east` | 相机预设：`isometric-north-east`、`isometric-north-west` 或 `up`。 |
| `rotateX`、`rotateY`、`rotateZ` | auto | 显式相机旋转覆盖值。 |
| `offsetX`、`offsetY` | auto | 屏幕空间相机平移，单位为像素。 |
| `centerX`、`centerY`、`centerZ` | auto | 显式世界旋转中心。设置任意一个后会禁用自动居中。 |
| `interactive` | `true` | 启用鼠标旋转、平移、缩放、重置、悬停和场景按钮。 |
| `allowLayerSlider` | `true` | 当游戏场景跨越多个 Y 层时显示可见层滑块。 |
| `gridButtonEnabled` | `true` | 显示地板网格切换按钮。 |
| `showGrid` | `false` | 地板网格的初始可见性。 |

```mdx
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:glass" x="1" z="1" />
</GameScene>
```

## 方块参数

`<Block>` 会向预览世界放置一个方块。

| 属性 | 必需 | 说明 |
| --- | --- | --- |
| `id` | 是，除非使用 `ore` | 方块 id，例如 `minecraft:furnace`。 |
| `ore` | 否 | 矿辞名。使用第一个可解析为方块物品的匹配项。 |
| `x`、`y`、`z` | 否 | 整数世界坐标，均默认为 `0`。 |
| `meta` | 否 | 方块 metadata。省略时，部分方块会根据 `facing` 推导默认值。 |
| `facing` | 否 | `down`、`up`、`north`、`south`、`west` 或 `east`。 |
| `nbt` | 否 | SNBT TileEntity 复合标签。 |

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:furnace" x="2" facing="south" />
  <Block ore="logWood" x="3" />
  <Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
</GameScene>
```

## 水与透明方块

透明方块会参与游戏场景渲染和悬停拾取。

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

## 默认统计按钮的红石线路

这个游戏场景没有声明 `<BlockStats>`，但因为场景包含方块，方块统计切换按钮仍然可用。打开后会显示默认内部列表。如果没有声明 `maxWidth` 和 `maxHeight`，统计框默认限制为游戏场景宽高的 25%，但至少会扩展到能完整显示 1 种方块，不会为了 1 个条目强行出现滚动条。

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

等价的显式写法：

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:redstone_wire" y="1" />
  <BlockStats dock="inside" corner="topRight" />
</GameScene>
```

## 方块统计框参数

`<BlockStats>` 用于自定义半透明统计列表。自动模式统计游戏场景的真实内容。手动模式显示作者手写的行，适合制作规划材料表。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `visible` | config，默认 `false` | 初始是否显示统计框。按钮仍可打开它。 |
| `buttonEnabled` | config，默认 `true` | 是否显示统计切换按钮。 |
| `mode` | `auto` | `auto` 或 `manual`；存在任意 `<BlockStat>` 子项时会强制手动模式。 |
| `corner` | `topRight` | 内部统计框角落：`topRight`、`topLeft`、`bottomRight` 或 `bottomLeft`。 |
| `dock` | `inside` | 自动列表可吸附到 `inside`、`left`、`top`、`right` 或 `bottom`。手动模式始终留在内部。 |
| `showNames` | `false` | 是否在图标旁显示物品名；开启后名称后面也会追加数量。 |
| `filterMode` | `blacklist` | `blacklist` 隐藏匹配项；`whitelist` 只显示匹配项。 |
| `filter` | 空 | 物品键，例如 `minecraft:stone` 或 `minecraft:stone:0`，可用空格、逗号或分号分隔。 |
| `maxWidth` | 游戏场景宽度的 25% | 统计框最大宽度，超出后出现水平滚动条。 |
| `maxHeight` | 游戏场景高度的 25% | 统计框最大高度，超出后出现垂直滚动条。 |

手动模式的 `<BlockStat>` 行：

| 属性 | 必需 | 说明 |
| --- | --- | --- |
| `item` | 是，除非使用 `id` | 列表中显示的物品 id。 |
| `id` | 是，除非使用 `item` | 既有物品栈属性写法。 |
| `count` | 否 | 显示的堆叠数量，默认 `0`。 |

自动模式会按 `item:meta` 分组、按数量排序，并尽量把常见的复合方块解析为玩家实际看到的物品。安装对应模组时，这包括 AE2 cable bus 部件与 facade、ForgeMultipart 部件、Carpenters' Blocks cover 或 overlay。统计结果会缓存，仅在游戏场景方块、思索时间线状态、StructureLib 选择或统计设置变化时重建。

## 外侧吸附的自动统计框

外侧吸附会在游戏场景外预留空间。`dock="right"` 会避开场景按钮列。`left` 和 `right` 会根据吸附边的高度自动增加列；`top` 和 `bottom` 会根据吸附边的宽度自动增加行。

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:stone" x="2" />
    <Block id="minecraft:furnace" x="1" y="1" />
    <Block id="minecraft:torch" x="2" y="1" />
    <BlockStats dock="right" showNames={true} maxWidth="180" maxHeight="96" />
</GameScene>

点击自动统计列表中的物品，会高亮游戏场景里所有匹配位置。高亮使用每个条目解析出的碰撞箱，所以多部件方块和不完整方块会按实际可见面高亮，而不是整格方块。高亮层会穿透方块显示，并使用比较醒目的颜色。再次点击同一个物品会取消选择。列表中被选中的物品行也会显示高亮背景，便于确认当前选择。

数量通过 ItemStack 自带的堆叠数量渲染。`showNames={false}` 时，堆叠数量仍能快速表示大概数量；`showNames={true}` 时，名称后面也会追加数量。鼠标悬停物品时，Tooltip 会插入一行精确方块数量。

## 过滤示例

用黑名单隐藏太常见或不需要展示的方块：

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <Block id="minecraft:torch" x="2" />
  <BlockStats filterMode="blacklist" filter="minecraft:stone,minecraft:air" />
</GameScene>
```

用白名单只关注少数方块：

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <Block id="minecraft:torch" x="2" />
  <BlockStats filterMode="whitelist" filter="minecraft:furnace minecraft:torch" showNames={true} />
</GameScene>
```

## 手动材料表

手动模式不绑定游戏场景真实内容，也不会使用外侧吸附或方块高亮选择。它更适合展示“建造这个结构需要哪些材料”。

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:furnace" />
    <Block id="minecraft:cobblestone" x="1" />
    <Block id="minecraft:cobblestone" x="-1" />
    <BlockStats mode="manual" corner="bottomRight" maxWidth="160" maxHeight="96">
        <BlockStat item="minecraft:cobblestone" count="8" />
        <BlockStat item="minecraft:furnace" count="1" />
    </BlockStats>
</GameScene>

```mdx
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
```

## BlockAnnotationTemplate

`<BlockAnnotationTemplate>` 会把子注解应用到所有已经存在且匹配 id 的方块上。它应放在要匹配的方块或导入结构之后。

<GameScene zoom="2" interactive={true}>
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

```mdx
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    这段文字会在悬停时显示！
  </DiamondAnnotation>
</BlockAnnotationTemplate>
```

## 末地传送门框

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

## TileEntity / 方向性方块

箱子、熔炉、红石块、活塞、信标：

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

## 不完整方块

楼梯、台阶、栅栏、活板门、多部件方块和其他不完整方块，都应在悬停与统计高亮选择时使用真实碰撞箱或渲染边界。

<GameScene width="384" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:oak_stairs" />
  <Block id="minecraft:stone_stairs" x="2" meta="1" />
  <Block id="minecraft:stone_slab" x="4" />
  <Block id="minecraft:stone_slab" x="4" y="1" meta="8" />
  <Block id="minecraft:fence" x="6" />
  <Block id="minecraft:fence" x="6" z="1" />
  <Block id="minecraft:trapdoor" x="8" />
</GameScene>

## 静态天气

`<Weather>` 是专门用于动画雨雪的场景组件。它独立于普通面片粒子，在普通 `GameScene`
渲染期间持续循环，并遵循与 Ponder 天气运行时相同的降水列规则。

<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:stone" x="0" y="0" z="0" />
  <Block id="minecraft:stone" x="1" y="0" z="0" />
  <Block id="minecraft:stone" x="2" y="0" z="0" />
  <Block id="minecraft:glass" x="1" y="1" z="0" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2" z="0" density="7" />
</GameScene>

- `x` 和 `z` 都支持单值，也支持端点数组。
- 天气忽略 `y`；垂直范围由场景边界和降水遮挡方块自动推导。
- 同一个 `x/z` 列在同一时间不会叠加多个天气效果。

## 静态粒子

`<Particle>` 会在场景里放置一个静止粒子。不填写 `name` 时会使用默认的面片粒子，
适合在不增加额外注解几何体的前提下强调某个精确位置，也能用于烟雾和发光提示。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `name` | `billboard` | 粒子外观。支持 `billboard`、`smoke`、`largesmoke`、`explode`、`flash`、`largeexplode`、`hugeexplosion`。`particle`、`quad`、`sheet` 会作为 `billboard` 的别名处理。 |
| `x`、`y`、`z` | `0.5`、`0.5`、`0.5` | 粒子的世界坐标原点。 |
| `size` | `0.18` | 粒子的半尺寸，单位为方块。 |

```mdx
<GameScene width="192" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:furnace" x="1" />
  <Particle x="1.5" y="1.85" z="0.5" size="0.22" />
  <Particle name="smoke" x="1.5" y="1.35" z="0.5" size="0.18" />
</GameScene>
```

