---
navigation:
  title: 思索动画
  parent: index.md
  position: 40
categories:
  - scenes
---

# 思索动画

`<ImportPonder>` 标签为任意游戏场景（`<GameScene>`）添加完全由关键帧驱动的思索动画时间轴。
所有时间轴数据均在 JSON 文件中声明，包括摄像机插值、每帧的 3D 标注、方块变换和标注淡入过渡。

## 完整功能演示

下方场景展示了所有已支持功能：七种标注类型、折线箭头、多行文本、独立（屏幕空间）文本、
文本连接线位置、带高亮框的文本、关键帧间的方块变换，以及带修饰键和物品图标的输入标注。

<GameScene width="420" height="280" zoom={2.5} interactive={true}>
  <ImportStructure src="/assets/ponder_demo.snbt" />
  <ImportPonder src="/assets/ponder_demo.json" />
</GameScene>

## Keyframe Sounds

```json
"sounds": [
  { "sound": "guidenh:guide.sample_click", "volume": 0.75 },
  { "src": "guidenh:sounds/guide/sample_hover.ogg", "volume": 0.35, "x": 1.5, "y": 1.5, "z": 1.5 }
]
```

`sound` uses a sound event id. `src` points to an `.ogg` path and is converted to the matching
sound event id, so `guidenh:sounds/guide/sample_hover.ogg` becomes `guidenh:guide.sample_hover`.

点击 ▶ 播放，或拖动时间轴。时间轴上的关键帧节点可快速跳转到关键时刻。

---

## JSON 结构

```json
{
  "totalTime": 360,
  "keyframes": [
    {
      "time": 0,
      "label": "概览",
      "camera": {
        "zoom": 1.8,
        "rotX": 25.0,
        "rotY": 210.0
      },
      "layer": null,
      "annotations": [],
      "blockChanges": []
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `totalTime` | int | 动画总时长（游戏刻，20 刻 = 1 秒） |
| `keyframes` | array | 按时间排序的关键帧列表 |
| `time` | int | 该关键帧激活的游戏刻 |
| `label` | string? | 鼠标悬停关键帧节点时显示的标签 |
| `camera` | object? | 摄像机部分覆盖（仅指定的字段生效） |
| `layer` | int? | 可见层覆盖（`null` = 显示全部） |
| `annotations` | array? | 该关键帧激活期间显示的标注列表 |
| `blockChanges` | array? | 该关键帧激活时应用的方块替换列表 |
| `mergeTileNBT` / `modifyTileNBT` / `removeTileNBT` | array? | 支持定位重放的方块实体 NBT 操作 |
| `createEntities` | array? | 创建由思索时间轴管理、可用 `ref` 引用的实体 |
| `setEntityNBT` / `mergeEntityNBT` / `modifyEntityNBT` / `removeEntityNBT` | array? | 对引用实体执行支持定位重放的 NBT 操作 |

---

## 摄像机关键帧字段

摄像机字段可以只指定部分——未指定的字段从之前设置该字段的关键帧继承。
相邻关键帧之间使用缓入缓出曲线进行插值。

```json
"camera": {
  "zoom": 1.8,
  "rotX": 25.0,
  "rotY": 210.0,
  "rotZ": 0.0,
  "offX": 0.0,
  "offY": 0.0
}
```

---

## 标注类型

所有七种标注类型均已支持，与当前关键帧无关的字段将被忽略。

### `diamond` — 世界空间标记

```json
{
  "type": "diamond",
  "x": 0.5, "y": 1.8, "z": 1.5,
  "color": "0xFF44FF44",
  "tooltip": "鼠标悬停时显示的富文本提示",
  "alwaysOnTop": true
}
```

### `box` — 轴对齐包围盒

```json
{
  "type": "box",
  "minX": 0.05, "minY": 1.05, "minZ": 1.05,
  "maxX": 0.95, "maxY": 1.95, "maxZ": 1.95,
  "color": "0x8800AAFF",
  "lineWidth": 1.5
}
```

### `block` - 整方块线框

当你想在 Ponder JSON 中使用和普通 `<BlockAnnotation pos="x y z">` 一样的整方块标注时，使用 `block`。

```json
{
  "type": "block",
  "pos": [1, 1, 1],
  "color": "0xFFFF8833",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`blockBox` 和 `block_box` 也可以作为别名。坐标可以写成 `pos: [x, y, z]`、
`pos: "x y z"`，也可以写成 `x/y/z`，或兼容旧风格的 `blockX/blockY/blockZ`。

### `line` — 线段或折线

```json
{
  "type": "line",
  "fromX": 1.5, "fromY": 2.0, "fromZ": 1.5,
  "toX": 1.5, "toY": 1.5, "toZ": 1.5,
  "color": "0xFFFFCC44",
  "arrow": "end",
  "lineWidth": 2.5,
  "alwaysOnTop": true
}
```

使用 `points` 可以绘制折线，格式支持 `"x y z; x y z; ..."` 或 `[[x, y, z], ...]`。
`arrow` 支持 `"start"` 或 `"end"`。

```json
{
  "type": "line",
  "points": [[0.5, 1.8, 2.5], [1.5, 2.25, 1.5], [2.5, 1.8, 0.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

### `blockface` — 方块面高亮

```json
{
  "type": "blockface",
  "pos": [1, 1, 1],
  "color": "0x60FF8833"
}
```

`blockFace` 和 `block_face` 也可以作为别名。坐标可以写成 `pos: [x, y, z]`、
`pos: "x y z"`，也可以写成 `x/y/z`，或 `blockX/blockY/blockZ`。

### `text` — 气泡文字标注

世界锚点模式（从标注框向世界坐标绘制连接线）。如果要随关键帧改变文字，在后续关键帧声明新的 `text` 标注即可：

```json
{
  "type": "text",
  "x": 1.5, "y": 3.0, "z": 1.5,
  "text": "熔炉已点燃，正在熔炼！",
  "color": "0xFFFF8833",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12,
  "maxWidth": 120
}
```

独立（屏幕空间）模式（无连接线，固定显示在场景中央附近）：

```json
{
  "type": "text",
  "text": "简单的熔炼流程：漏斗向熔炉投料，箱子收集产出。",
  "independent": true,
  "yOffset": -60,
  "maxWidth": 180,
  "color": "0xFFCCCCFF"
}
```

带高亮框的文字标注（同时渲染一个 box 标注）：

```json
{
  "type": "text",
  "x": 0.5, "y": 2.8, "z": 1.5,
  "text": "在此放入燃料和矿石",
  "color": "0xFF44FF44",
  "hlMinX": 0.05, "hlMinY": 1.05, "hlMinZ": 1.05,
  "hlMaxX": 0.95, "hlMaxY": 1.95, "hlMaxZ": 1.95,
  "highlightColor": "0x6044FF44"
}
```

| `text` 字段 | 类型 | 说明 |
|---|---|---|
| `text` | string | 显示文本 |
| `color` | string | 边框颜色，格式为 `0xAARRGGBB` |
| `maxWidth` | int? | 自动换行宽度（像素）；`0` 或省略表示单行 |
| `independent` | bool? | `true` = 屏幕空间模式（无世界锚点） |
| `yOffset` | int? | 独立模式下相对场景中心的 Y 偏移 |
| `connectorSide` | string? | `bottom`、`top`、`left`、`right` 或 `none`；独立模式下忽略 |
| `connectorOffset` | int? | 沿选定气泡边缘偏移连接点；top/bottom 正值向右，left/right 正值向下 |
| `connectorLength` | int? | 连接线像素长度；默认 `6` |
| `hlMinX/Y/Z` | float? | 高亮框最小角（世界坐标） |
| `hlMaxX/Y/Z` | float? | 高亮框最大角（世界坐标） |
| `highlightColor` | string? | 高亮框颜色；默认 `0x8000FFAA` |

### `input` — 鼠标输入图标

渲染鼠标按键（左键/右键/滚轮），可附带修饰键前缀和物品图标。

```json
{
  "type": "input",
  "x": 0.5, "y": 1.5, "z": 2.5,
  "inputType": "rmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ore"
}
```

| `input` 字段 | 类型 | 说明 |
|---|---|---|
| `inputType` | string | `"lmb"`（左键）、`"rmb"`（右键）或 `"scroll"`（滚轮） |
| `modifier` | string? | `"sneak"` 或 `"ctrl"`，显示为前缀标签 |
| `item` | string? | 物品注册名，如 `"minecraft:iron_ore"` 或 `"modid:item:meta"` |

---

## 方块变换（blockChanges）

`blockChanges` 在关键帧激活时替换结构中的方块。在每次激活时，所有变换位置会先恢复为初始状态，
再从关键帧 0 到当前关键帧重新应用变换，因此拖动时间轴向前或向后都能显示正确状态。

```json
"blockChanges": [
  { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4 },
  { "x": 1, "y": 2, "z": 1, "block": "minecraft:air" }
]
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `x`, `y`, `z` | int | 方块坐标（结构坐标系） |
| `block` | string | 注册名，如 `"minecraft:furnace"`。使用 `"minecraft:air"` 删除方块。 |
| `meta` | int? | 方块元数据/damage 值；默认为 `0` |

---

## 方块实体 NBT

方块实体 NBT 操作使用 SNBT 字符串和类似 `/data` 的路径：

```json
"mergeTileNBT": [
  {
    "x": 2, "y": 1, "z": 1,
    "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:1b,Damage:0s}]}"
  }
],
"modifyTileNBT": [
  { "x": 2, "y": 1, "z": 1, "path": "Items[0].Count", "value": "3b" }
],
"removeTileNBT": [
  { "x": 2, "y": 1, "z": 1, "path": "Items[0].tag" }
]
```

使用点号进入复合标签，使用 `[index]` 进入列表项，例如
`InputTanks[0].TankContent.Amount`。

---

## 思索实体

游戏场景（`<GameScene>`）支持普通 `<Entity>` 元素。思索时间轴也可以创建由时间轴管理的实体，并在后续关键帧中通过
`ref` 修改它们的 NBT。

```json
"createEntities": [
  {
    "ref": "marker",
    "id": "minecraft:pig",
    "x": 1.5, "y": 1.0, "z": 2.5,
    "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
  }
],
"mergeEntityNBT": [
  { "ref": "marker", "nbt": "{Saddle:1b}" }
],
"modifyEntityNBT": [
  { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
],
"removeEntityNBT": [
  { "ref": "marker", "path": "CustomNameVisible" }
]
```

`setEntityNBT` 会用新的 SNBT 复合标签替换引用实体的 NBT。所有实体操作在定位时都会从关键帧
0 重新播放，因此向后拖动时间轴时，思索时间轴创建的实体不会残留在旧状态。

---

## 标注淡入动画

在播放过程中切换关键帧时，标注会在 5 个游戏刻（250 毫秒）内平滑淡入。
拖动时间轴或暂停时，标注始终以完整不透明度显示。

---

## 最简示例

```mdx
<GameScene width="384" height="256" zoom={2} interactive={true}>
  <ImportStructure src="/assets/my_machine.snbt" />
  <ImportPonder src="/assets/my_machine.json" />
</GameScene>
```

JSON 文件从资源包中按相对于指南根目录的路径加载；结构文件通过 `<ImportStructure>` 单独加载。
