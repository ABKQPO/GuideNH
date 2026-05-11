# 思索动画时间轴

GuideNH 在游戏场景（`<GameScene>`）块中支持思索风格的动画时间轴。你只需提供一个外部 JSON 文件来定义关键帧、摄像机运动和世界内注释，GuideNH 就会在 3D 场景下方渲染一个带播放/暂停控件的交互式进度条。

## 快速上手

1. 创建一个思索 JSON 文件，并将其放入资源包（参见[文件位置](#文件位置)）。
2. 在 guide 页面的游戏场景（`<GameScene>`）块中，与 `<ImportStructure>` 并列添加 `<ImportPonder src="..."/>`。

```mdx
<GameScene zoom="4" background="#0a0a10">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **注意：** `<ImportPonder>` 必须位于游戏场景（`<GameScene>`）块内，`src` 属性为必填项。结构数据仍由 `<ImportStructure>` 或 `<ImportStructureLib>` 提供。

## 文件位置

思索 JSON 文件遵循与 SNBT 结构文件相同的资源包路径规则：

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       ← guide 页面
  pages/machines/my_machine.snbt      ← 结构数据
  pages/machines/my_machine.json      ← 思索 JSON
```

`src` 属性支持相对路径和绝对 ID：

| 示例 | 解析方式 |
|------|----------|
| `src="my_machine.json"` | 相对于当前页面目录 |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | 绝对路径（`mymod` 命名空间） |

## JSON 格式

根对象包含两个必填字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `totalTime` | 整数 | 是 | 动画总时长（20 刻 = 1 秒），最小为 1。 |
| `keyframes` | 数组 | 是 | 关键帧对象列表。可为空数组。 |

### 关键帧字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `time` | 整数 | 是 | 该关键帧所在刻（0 ≤ time ≤ totalTime）。 |
| `label` | 字符串 | 否 | 悬停在进度条节点上时显示的标签及方向箭头。 |
| `camera` | 对象 | 否 | 该关键帧的摄像机状态，缺省字段从前一关键帧继承。 |
| `cameraEaseTicks` | 整数 或 null | 否 | 摄像机从**上一个**关键帧的位置缓动到当前关键帧的时间（刻数）。`null`（默认）= 在整个片段内缓动；`0` = 立即跳转；`N > 0` = 在 N 刻内缓动，之后保持目标位置。 |
| `layer` | 整数 或 null | 否 | 可见层覆盖。`null` 显示所有层；从 1 开始的整数限制到指定层。 |
| `annotations` | 数组 | 否 | 该关键帧激活时显示的注释列表。 |
| `blockChanges` | 数组 | 否 | 该关键帧激活时执行的方块替换列表。 |
| `mergeTileNBT` | 数组 | 否 | 将 SNBT 复合标签合并到指定坐标的方块实体。 |
| `modifyTileNBT` | 数组 | 否 | 将某个方块实体 NBT 路径设置为指定 SNBT 值。 |
| `removeTileNBT` | 数组 | 否 | 删除某个方块实体 NBT 路径。 |
| `createEntities` | 数组 | 否 | 创建可被后续实体 NBT 操作引用的思索专用实体。 |
| `setEntityNBT` | 数组 | 否 | 用提供的 SNBT 复合标签替换引用实体的 NBT。 |
| `mergeEntityNBT` | 数组 | 否 | 将 SNBT 复合标签合并到引用实体。 |
| `modifyEntityNBT` | 数组 | 否 | 将引用实体的某个 NBT 路径设置为指定 SNBT 值。 |
| `removeEntityNBT` | 数组 | 否 | 删除引用实体的某个 NBT 路径。 |

### 摄像机字段（均为可选）

| 字段 | 说明 |
|------|------|
| `zoom` | 摄像机缩放（0.1 – 10.0）。 |
| `rotX` | X 轴旋转角度（度）。 |
| `rotY` | Y 轴旋转角度（度）。 |
| `rotZ` | Z 轴旋转角度（度）。 |
| `offX` | 水平平移偏移（屏幕像素）。 |
| `offY` | 垂直平移偏移（屏幕像素）。 |

相邻关键帧之间的摄像机使用**缓入/缓出**曲线平滑插值。使用目标关键帧的 `cameraEaseTicks` 字段可以控制缓动时长：

```json
{ "time": 60,  "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   ← 立即跳转
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  ← 在 20 刻内缓动后保持
{ "time": 180, "camera": { "rotY": 270 } }                         ← 在整个片段内缓动（默认）
```

## 方块变化（blockChanges）

关键帧的 `blockChanges` 数组可在该关键帧激活时替换结构中的方块。这让动画可以呈现前/后对比、添加或移除方块，或演示机器开机效果。

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `x`, `y`, `z` | 整数 | — | **必填。** 要修改的方块坐标（结构坐标系）。 |
| `block` | 字符串 | — | **必填。** 注册名，如 `"minecraft:furnace"`。使用 `"minecraft:air"` 表示移除方块。 |
| `meta` | 整数 | `0` | 方块元数据（伤害值）。 |
| `particles` | 布尔值 | `true` | 在正向播放中触发该方块变化时，是否生成基于方块自身贴图的粒子效果。设为 `false` 可抑制视觉效果（例如静默移除方块）。 |
| `nbt` | 字符串 | `null` | 方块实体的 SNBT 标签字符串，用于箱子、熔炉等。通过 `JsonToNBT` 解析。键名必须使用**不带引号**的标准 SNBT 格式；字符串值仍需引号。若方块没有方块实体则忽略该字段。 |

**支持倒放定位：** 向前或向后拖动进度条时，运行时会先将所有改动过的位置恢复为原始状态，再从第 0 帧重新应用到当前帧。无论如何定位，显示的结构始终正确。

> **关于粒子效果：** 粒子效果只在正向播放、关键帧第一次激活时触发，粒子使用被替换方块的自身贴图。定位（倒带/快进）、重置或初始加载时粒子**不会**触发，已有粒子会被清除。

---

## 方块实体 NBT 操作

当方块本身不变，只需要改变方块实体数据时，可以使用 `mergeTileNBT`、`modifyTileNBT`
和 `removeTileNBT`。这些操作支持进度条定位：运行时会先恢复初始方块实体 NBT，再从第
0 帧重放到当前关键帧。

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| 字段 | 适用于 | 说明 |
|------|--------|------|
| `x`, `y`, `z` | 全部 | 方块实体所在的结构坐标。 |
| `nbt` | `mergeTileNBT` | 要合并到方块实体的 SNBT 复合标签。复合标签会递归合并，其他值会覆盖旧值。 |
| `path` | `modifyTileNBT`、`removeTileNBT` | 带列表索引的点分 NBT 路径，例如 `Items[0].Count` 或 `InputTanks[0].TankContent.Amount`。 |
| `value` | `modifyTileNBT` | 写入 `path` 的 SNBT 值，例如 `3b`、`500`、`"\"text\""`、`{Count:1b,id:"minecraft:stone"}`。 |

路径写法与 Minecraft `/data` 的思路相同：用 `.` 进入复合标签，用 `[index]` 进入列表。
列表遍历目前面向常见的“列表内是复合标签”的结构，例如物品栏、流体罐和配方槽。

---

## 实体操作

游戏场景（`<GameScene>`）本身已经支持普通 `<Entity>` 标签。思索时间轴现在也可以通过
`createEntities` 创建由时间轴管理的实体，并在后续关键帧中通过 `ref` 引用它们。

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `ref` | 必填，本思索 JSON 内用于后续操作的引用名。 |
| `id` | 实体 ID，例如 `minecraft:pig`、`Pig` 或场景实体加载器支持的模组实体 ID。 |
| `x`, `y`, `z` | 可选生成位置。未填写且 `nbt` 没有 `Pos` 时默认为 `0, 0, 0`。 |
| `yaw`, `pitch` | 可选生成旋转。未填写且 `nbt` 没有 `Rotation` 时默认为 `0, 0`。 |
| `nbt` | 可选，实体创建时应用的 SNBT 复合标签。 |
| `name`, `uuid` | 可选，创建预览玩家实体时使用的玩家档案字段。 |

实体创建后，可以使用实体 NBT 操作：

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

如果需要替换实体 NBT，而不是合并，可以使用 `setEntityNBT`：

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

与方块实体操作一样，实体操作会在关键帧变化时从头重放；向后拖动进度条时，
思索时间轴创建的实体会被移除并重新创建到目标时刻的正确状态。

---

## 注释淡入动画

当播放过程中活跃关键帧切换时，叠加类注释（`text`、`input`）会在 **5 个游戏刻**（250 ms）内平滑淡入。暂停或拖动定位时，注释始终以完全不透明度立即显示。

---

## 注释类型

每个注释条目需要一个 `type` 字段，共支持六种类型。

---

### `diamond` — 菱形标记

在世界坐标渲染一个 3D 菱形标记。

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "点击此处",
  "alwaysOnTop": false
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 菱形尖端的世界坐标。 |
| `color` | 字符串 | `"0xFF00E000"` | ARGB 颜色，格式为 `"0xAARRGGBB"`。 |
| `tooltip` | 字符串 | `""` | 悬停时显示的文本。 |
| `alwaysOnTop` | 布尔值 | `false` | 为 true 时穿透方块渲染。 |

---

### `box` — 线框盒子

从 `min` 到 `max` 渲染一个轴对齐线框盒子。

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minX/Y/Z` | float | `0.0` | 最小角坐标。 |
| `maxX/Y/Z` | float | `1.0` | 最大角坐标。 |
| `color` | 字符串 | `"0xFFFFFFFF"` | ARGB 线条颜色。 |
| `lineWidth` | float | 默认值 | GL 线宽。 |
| `alwaysOnTop` | 布尔值 | `false` | 穿透方块渲染。 |

---

### `line` — 线段

在两个世界坐标之间渲染一条线段。

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `fromX/Y/Z` | float | `0.0` | 起点坐标。 |
| `toX/Y/Z` | float | `1.0` | 终点坐标。 |
| `color` | 字符串 | `"0xFFFFFFFF"` | ARGB 线条颜色。 |
| `lineWidth` | float | 默认值 | GL 线宽。 |
| `alwaysOnTop` | 布尔值 | `false` | 穿透方块渲染。 |

---

### `blockface` — 方块面叠层

用半透明颜色叠层高亮显示某个方块的所有面。

```json
{
  "type": "blockface",
  "blockX": 1,
  "blockY": 0,
  "blockZ": 1,
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `blockX/Y/Z` | 整数 | `0` | 要高亮的方块坐标。 |
| `color` | 字符串 | `"0x80FFFFFF"` | ARGB 叠层颜色。 |
| `alwaysOnTop` | 布尔值 | `false` | 穿透方块渲染。 |

---

### `text` — 文字气泡

在世界坐标附近渲染一个带连接线的文字气泡框，锚定到指定坐标正上方。

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "在此放置物品",
  "color": "0xFF44AAFF"
}
```

使用**独立模式（independent）**可以在固定屏幕坐标处显示气泡框（不跟随世界坐标投影）：

```json
{
  "type": "text",
  "text": "独立标签",
  "color": "0xFFFFCC00",
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `x`, `y`, `z` | float | `0.0` | 锚点的世界坐标（独立模式下忽略）。 |
| `text` | 字符串 | — | **必填。** 气泡框中显示的文本。 |
| `color` | 字符串 | `"0xFFAAAAAA"` | ARGB 边框颜色。 |
| `backgroundAlpha` | 整数 | `204` | 背景透明度，`0` 为完全透明，`255` 为完全不透明。 |
| `maxWidth` | 整数 | `0` | 若 &gt; 0，按此像素宽度自动换行；`0` 表示单行。 |
| `independent` | 布尔值 | `false` | 若为 `true`，位置以场景中心为基准（屏幕坐标），而非世界坐标投影。 |
| `yOffset` | 整数 | `0` | 相对于场景垂直中心的像素偏移（正值向下）。与 `independent: true` 配合使用。 |
| `hlMinX/Y/Z` | float | `0.0` | 可选的伴生高亮框最小角坐标。 |
| `hlMaxX/Y/Z` | float | `1.0` | 可选的伴生高亮框最大角坐标。 |
| `highlightColor` | 字符串 | `"0x8000FFAA"` | 高亮框颜色（存在 `hlMin/Max` 时自动创建一个 `InWorldBoxAnnotation`）。 |

当任意 `hlMin/Max` 坐标存在时，会为该关键帧额外创建一个 `InWorldBoxAnnotation`，颜色由 `highlightColor` 指定。适合在讲解区域时高亮指定的方块范围。

气泡框背景默认是深色半透明（`#CC0E0E20`），可用 `backgroundAlpha` 调整透明度。世界锚定模式下有连接线；独立模式下没有。文本支持完整的 GuideNH 行内富文本语法（Markdown 格式化与 MDX 行内标签），并带阴影渲染。

> **富文本支持：** `text` 字段支持 GuideNH 页面中所有行内富文本语法：
> `**粗体**`、`*斜体*`、`~~删除线~~`、`<Color id="RED">颜色文本</Color>`、
> `<ItemLink id="minecraft:iron_ingot" />` 以及其他所有行内 MDX 标签。
> **不支持** Minecraft 原版的 `§` 格式代码，请改用上述 MDX 语法。

> 缺少 `text` 字段或值为空的 `text` 注释将被静默忽略。

---

### `input` — 鼠标操作提示

在世界坐标附近渲染一个鼠标操作图标（左键、右键或滚轮），用于提示玩家执行特定交互。

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
| `x`, `y`, `z` | float | `0.0` | 锚点的世界坐标。 |
| `inputType` | 字符串 | `"lmb"` | `"lmb"`（左键）、`"rmb"`（右键）或 `"scroll"`（滚轮），不区分大小写。 |
| `modifier` | 字符串 | `null` | 可选修饰键：`"sneak"` 或 `"ctrl"`。在图标上方显示前缀文字。 |
| `item` | 字符串 | `null` | 可选物品注册 ID（如 `"minecraft:iron_ingot"`）。在鼠标图标左侧显示物品图标。支持 `"modid:item:meta"` 格式指定元数据。 |

图标为从 `ponder_widgets.png` 绘制的 16×16 精灵图。背景为深色半透明（`#CC0E0E20`），边框为浅蓝色（`#80AAAADD`）。指定 `item` 时气泡框会横向扩展以容纳两个图标。

---

## 颜色格式

颜色为 ARGB 十六进制字符串。支持 `"0xFFFFFF00"`（带 `0x` 前缀）和 `"FFFF00"`（不带前缀）两种写法。

- `FF` alpha = 完全不透明
- `80` alpha = 50% 半透明
- `00` alpha = 完全透明
- `"0xFF00E000"` — 不透明绿色（菱形默认颜色）
- `"0x8022CCFF"` — 半透明蓝色
- `"0xFFAAAAAA"` — 浅灰色（文字气泡边框默认颜色）

## 播放行为

### 控件说明

| 控件 | 功能 |
|------|------|
| **◄（上一关键帧）** | 跳转到上一关键帧片段的起始位置。 |
| **▶/⏸（播放/暂停）** | 切换播放状态；若已播完则重新从头开始。 |
| **↺（从头开始）** | 返回第 0 刻并重新播放。 |
| 进度条 | 点击或拖拽可跳转到任意位置（始终暂停）。 |
| 关键帧节点 | 进度条上的小刻度标记，悬停时显示标签文本。 |

### 初始状态

当包含 `<ImportPonder>` 的页面首次打开时，场景默认**暂停在第 0 刻**。按播放键（▶）开始播放。

### 摄像机锁定

播放**进行中**（未暂停）时：
- 摄像机跟随关键帧插值路径；拖拽和缩放**被禁用**。
- 层滑条和 StructureLib 滑条**被隐藏**。

播放**暂停**或**结束**时，所有交互完全恢复。

### 关键帧节点标签

悬停在进度条的关键帧节点上时：
- 节点稍微放大以表示悬停。
- 若该关键帧有 `label`，则在节点旁显示标签文本。

### 层控制

活跃关键帧的 `layer` 字段在播放期间覆盖可见层过滤：
- `null`（或省略）→ 显示所有层。
- `1`、`2`、`3`……→ 限制到该 1-based 层索引。

## 完整示例

以下示例展示了所有六种注释类型，跨四个关键帧。

### 目录结构

```
assets/mymod/guidebooks/
  pages/machines/grinder.mdx
  pages/machines/grinder.snbt
  pages/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "总览",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "输入仓",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV 输入总线",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "在此处放入矿石",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "输出侧",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "blockX": 2, "blockY": 1, "blockZ": 1,
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "在此处收取矿粉",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "滚轮翻层",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "滚动鼠标滚轮查看层",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

标准 NBT 结构文件（通过 `/structure save` 命令或 Litematica 等工具创建）。完整的 SNBT 格式参考请见 [Getting Started](Getting-Started-zh-CN.md)。

### `grinder.mdx`

```mdx
# 磨碎机

<GameScene zoom="4" background="#0a0a10">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

磨碎机将矿石加工成双倍矿粉。按**播放**键查看动态演示。
```

## 注意事项

- 思索进度条绘制在层/StructureLib 滑条上方。播放期间结构滑条被隐藏，暂停后重新出现。
- 即使某些关键帧仅更改部分摄像机轴，摄像机插值始终平滑（缓入/缓出）。使用 `cameraEaseTicks` 可以让摄像机立即跳转（`0`）或在指定刻数内完成缓动后保持目标位置。
- 注释属于单个关键帧，仅在该关键帧激活期间（从其 `time` 刻到下一关键帧的 `time` 刻之前）显示。正向播放时叠加文字注释会平滑淡出后切换。
- 每个游戏场景（`<GameScene>`）只有一个 `<ImportPonder>` 标签生效，多个标签时后者覆盖前者。
- `text` 字段缺失或为空的 `text` 注释将被静默跳过。
- `inputType` 字段缺失或无法识别时默认为 `"lmb"`。
- `blockChanges` 按从第 0 帧到当前帧的顺序应用；在多个关键帧中修改同一位置的方块完全正常。
- 方块实体和实体 NBT 操作也使用同样的重放模型；向前或向后拖动进度条都能恢复正确状态。
- `maxWidth` &gt; 0 的 `text` 注释使用原版字体渲染器自动换行；气泡框高度会随多行文本自动调整。
- `nbt` 字符串中键名必须为**不带引号**的标准 SNBT 格式（MC 1.7.10 `JsonToNBT` 要求）。字符串值仍需引号，例如 `{id:"minecraft:iron_ingot",Count:8b}`。
- `modifyTileNBT` 和 `modifyEntityNBT` 的 `value` 是 SNBT 值，不是 JSON 值。字符串值需要在 JSON 内转义 SNBT 引号：`"value": "\"hello\""`。
