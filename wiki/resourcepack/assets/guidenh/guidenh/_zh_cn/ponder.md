---
navigation:
  title: Ponder 动画
  parent: index.md
---

# Ponder 动画

`<ImportPonder>` 标签为任意 `<GameScene>` 添加完全由关键帧驱动的动画时间轴。
所有时间轴数据均在 JSON 文件中声明，包括摄像机插值、每帧的 3D 标注、方块变换和标注淡入过渡。

## 完整功能演示

下方场景展示了所有已支持功能：六种标注类型、多行文本、独立（屏幕空间）文本、
带高亮框的文本、关键帧间的方块变换，以及带修饰键和物品图标的输入标注。

<GameScene width="420" height="280" zoom={2.5} interactive={true}>
  <ImportStructure src="/assets/ponder_demo.snbt" />
  <ImportPonder src="/assets/ponder_demo.json" />
</GameScene>

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

所有六种标注类型均已支持，与当前关键帧无关的字段将被忽略。

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

### `line` — 线段

```json
{
  "type": "line",
  "fromX": 1.5, "fromY": 2.0, "fromZ": 1.5,
  "toX": 1.5, "toY": 1.5, "toZ": 1.5,
  "color": "0xFFFFCC44",
  "lineWidth": 2.5,
  "alwaysOnTop": true
}
```

### `blockface` — 方块面高亮

```json
{
  "type": "blockface",
  "blockX": 1, "blockY": 1, "blockZ": 1,
  "color": "0x60FF8833"
}
```

### `text` — 气泡文字标注

世界锚点模式（从标注框向世界坐标绘制连接线）：

```json
{
  "type": "text",
  "x": 1.5, "y": 3.0, "z": 1.5,
  "text": "熔炉已点燃，正在熔炼！",
  "color": "0xFFFF8833",
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
