---
navigation:
  title: 注解
  parent: index.md
  position: 164
categories:
  - scenes
---

# 注解

所有注解都使用世界坐标，可通过场景按钮的“显示/隐藏注解”切换。

## DiamondAnnotation 菱形标注

在 3D 预览任意世界坐标处放一个**菱形标注**。菱形始终朝向屏幕，光标悬停时出现白色半透明遮罩并弹出富文本 tooltip。

激活的信标示例 - 3x3 钻石块底座，顶部放一个信标；菱形标注 tooltip 内含嵌套 3D 预览：

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
    <Color color="#FFD24C">**效果**</Color>：为周围玩家提供速度 / 跳跃提升 / 抗性 / 力量 / 回复等**持续增益**。

    激活条件：下方为 3x3 / 5x5 / 7x7 / 9x9 的**钻石 / 铁 / 金 / 绿宝石 / 下界合金**金字塔底座。

    <GameScene width="160" height="128" zoom={5} interactive={false}>
      <Block id="minecraft:diamond_block" x="-1" />
      <Block id="minecraft:diamond_block" />
      <Block id="minecraft:diamond_block" x="1" />
      <Block id="minecraft:beacon" y="1" />
    </GameScene>

    <Color color="#AAFFAA">提示</Color>：金字塔层数越多，可选效果越多，信标**光柱颜色**取决于光路上的染色玻璃。
  </DiamondAnnotation>
</GameScene>

## 盒子 / 方块 / 线段注解

- `BoxAnnotation` 用 `min="x y z"` / `max="x y z"` 描述任意 AABB（支持小数）。
- `BlockAnnotation` 用 `pos="x y z"`（整数）选中某个方块，等价于 1x1x1 的盒子。
- `LineAnnotation` 用 `from="x y z"` / `to="x y z"` 画一条线段（支持小数），也可以用
  `points="x y z; x y z; ..."` 画多段折线。

所有三种都支持 `color="#AARRGGBB" 或 "#RRGGBB"`、`thickness`（线宽，单位：像素，默认 `1`）以及 `alwaysOnTop`（始终绘制在最前）。子节点是富文本，会作为悬停 tooltip 显示。
`LineAnnotation` 还可以用 `arrow="start"` 或 `arrow="end"` 在端点绘制 3D 箭头。点标记默认隐藏；可以用 `showPoints={true}` 显示所有点，或用 `<LinePoint index="..." show color="#RRGGBB" size="..."/>` 单独配置某个点。

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

  <LineAnnotation
    color="#66ccff"
    points="0.5 1.8 2.5; 1.5 2.25 1.5; 2.5 1.8 0.5"
    thickness="0.08"
    arrow="end"
  >
    <LinePoint index="0" show color="#66ccff" />
    <LinePoint index="1" show color="#ff8844" size="0.12" />
    **折线注解**：终点箭头是 3D 箭头头部，任意点都可以单独显示为立方体标记。
  </LineAnnotation>
</GameScene>

## 文本注解

`<TextAnnotation>` 是通用的气泡文本注解，既可用于普通游戏场景（`<GameScene>`），也可用于导入的思索时间轴。

```mdx
<GameScene width="256" height="192" zoom={4} interactive={true}>
  <TextAnnotation
    pos="1.5 2.0 1.5"
    textKey="guidenh.sample.scene.insert_items"
    color="#FF44AAFF"
    maxWidth={120}
    backgroundAlpha={180}
    connectorSide="right"
    connectorOffset={8}
    connectorLength={12}
  />
</GameScene>
```

使用 `independent={true}` 可以让气泡固定在屏幕坐标上，而不是跟随世界坐标：

```mdx
<GameScene width="256" height="192" zoom={4} interactive={true}>
  <TextAnnotation
    text="独立标签"
    color="#FFFFCC00"
    backgroundAlpha={140}
    independent={true}
    yOffset={40}
  />
</GameScene>
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 世界坐标锚点。`independent={true}` 时忽略。 |
| `text` | string | - | 回退文本；当未提供 `textKey` 时直接使用。 |
| `textKey` | string | - | 优先从资源包 `lang` 文件解析的翻译键；解析失败时回退到 `text` 或标签正文。 |
| `color` | string | `"0xFFAAAAAA"` | 气泡边框颜色。 |
| `backgroundAlpha` | integer | `204` | 背景透明度，`0` 为完全透明，`255` 为完全不透明。 |
| `maxWidth` | integer | `0` | 换行宽度（像素）。`0` 表示单行显示。 |
| `independent` | boolean | `false` | 为 `true` 时，气泡相对场景中心定位，而不是跟随世界点。 |
| `yOffset` | integer | `0` | `independent={true}` 时相对场景中心的像素偏移，正值向下。 |
| `connectorSide` | string | `"bottom"` | `bottom`、`top`、`left`、`right` 或 `none`。独立模式下忽略。 |
| `connectorOffset` | integer | `0` | 沿气泡边缘偏移连接点；top/bottom 正值向右，left/right 正值向下。 |
| `connectorLength` | integer | `6` | 连接线像素长度。 |
| `hlMinX/Y/Z` | float | `0.0` | 可选高亮框最小角坐标。 |
| `hlMaxX/Y/Z` | float | `1.0` | 可选高亮框最大角坐标。 |
| `highlightColor` | string | `"0x8000FFAA"` | 高亮框颜色。 |

这里没有单独的 size 参数。气泡大小由文本内容和 `maxWidth` 自动决定。
只要存在任意 `hlMin/Max` 值，就会额外创建一个对应的 `InWorldBoxAnnotation`。
气泡背景默认是深色海军蓝（`#CC0E0E20`），可用 `backgroundAlpha` 调整透明度；世界锚定模式下还会额外画一条连接线。
可以用 `connectorSide`、`connectorOffset` 和 `connectorLength` 控制连接线接在哪条边、沿边偏移多少以及连接线长度。

> **富文本说明：** `text` 支持 GuideNH 页面里同样的行内语法：
> `**bold**`、`*italic*`、`~~strikethrough~~`、`<Color id="RED">colored</Color>`、
> `<ItemLink id="minecraft:iron_ingot" />` 以及其他行内 MDX 标签。

## InputAnnotation

`InputAnnotation` 会渲染鼠标按键图标（左键、右键或滚轮），并锚定到某个世界坐标，用来提示玩家执行特定交互。

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

带修饰键前缀和物品图标的示例：

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 世界坐标锚点。 |
| `inputType` | string | `"lmb"` | `"lmb"`、`"rmb"` 或 `"scroll"`，不区分大小写。 |
| `modifier` | string | `null` | 可选修饰键：`"sneak"` 或 `"ctrl"`。会在图标上方显示前缀文字。 |
| `item` | string | `null` | 可选物品 ID，例如 `"minecraft:iron_ingot"`。会在鼠标图标左侧显示物品图标，也支持 `"modid:item:meta"` 格式。 |

图标来自 `ponder_widgets.png` 的 16x16 精灵。背景是深色半透明块（`#CC0E0E20`），边框是浅蓝色（`#80AAAADD`）。指定 `item` 时，气泡会横向扩展以容纳物品图标和鼠标图标。

## 颜色格式

颜色使用 ARGB 十六进制字符串。`"0xFFFFFF00"`（带 `0x` 前缀）和 `"FFFF00"`（不带前缀）都可以。

- `FF` alpha = 完全不透明
- `80` alpha = 50% 透明
- `00` alpha = 完全透明
- `"0xFF00E000"` - 完全不透明绿色（菱形默认颜色）
- `"0x8022CCFF"` - 半透明蓝色
- `"0xFFAAAAAA"` - 浅灰色（文本气泡默认边框色）

## 播放行为

### 控件说明

| 控件 | 功能 |
|------|------|
| **◀（上一关键帧）** | 跳到上一个关键帧片段的起点。 |
| **▶ / ⏸（播放/暂停）** | 切换播放状态；如果已经播放完毕，则会从头开始。 |
| **↻（重新开始）** | 回到第 0 刻，重置状态并重新播放。 |
| 进度条 | 点击或拖拽可跳转到任意位置；跳转时会自动暂停。 |
| 关键帧节点 | 进度条上的小刻度；悬停可查看标签和方向箭头。 |

### 初始状态

当包含 `<ImportPonder>` 的页面首次打开时，场景默认**暂停在第 0 刻**。按播放键（▶）开始播放。

### 摄像机锁定

播放**进行中**（未暂停）时：
- 摄像机跟随关键帧定义的插值路径。
- 鼠标拖拽和滚轮缩放**被禁用**。
- 层滑条和 StructureLib 滑条**被隐藏**。

播放**暂停**或**结束**时：
- 所有交互都恢复正常。

### 关键帧节点标签

悬停在进度条的关键帧节点上时：
- 节点会略微放大。
- 如果该关键帧有 `label`，会在节点旁显示标签文本。

### 播放期间的层控制

活动关键帧的 `layer` 字段会覆盖播放期间的可见层过滤：
- `null`（或省略）-> 显示所有层。
- `1`、`2`、`3`、... -> 只显示对应的 1-based 层索引。
