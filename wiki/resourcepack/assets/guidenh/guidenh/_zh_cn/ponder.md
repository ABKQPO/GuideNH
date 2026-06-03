---
navigation:
  title: 思索动画
  parent: index.md
  position: 160
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
| `hidden` | bool? | 为 `true` 时，该关键帧仍会正常执行，但不会渲染可见进度条节点，基于可见节点的导航也会跳过它 |
| `label` | string? | 鼠标悬停关键帧节点时显示的标签 |
| `labelKey` | string? | 关键帧标签的翻译键。解析成功时会覆盖 `label` |
| `camera` | object? | 摄像机部分覆盖（仅指定的字段生效） |
| `layer` | int? | 可见层覆盖（`null` = 显示全部） |
| `annotations` | array? | 该关键帧激活期间显示的标注列表 |
| `blockChanges` | array? | 该关键帧激活时应用的方块替换列表 |
| `mergeTileNBT` / `modifyTileNBT` / `removeTileNBT` | array? | 支持定位重放的方块实体 NBT 操作 |
| `createEntities` | array? | 创建由思索时间轴管理、可用 `ref` 引用的实体，并支持稳定 `sceneEntityId`、骑乘关系、初始朝向与预览玩家姿态 |
| `setEntityNBT` / `mergeEntityNBT` / `modifyEntityNBT` / `removeEntityNBT` | array? | 对引用实体执行支持定位重放的更新；除了 NBT，也可顺便修改位置、稳定骑乘关系与预览玩家外观状态 |
| `removeEntities` | array? | 通过稳定场景实体注册表，按 `ref` 删除思索时间轴实体 |
| `animateEntities` | array? | 对引用的思索实体应用支持重放和拖动时间轴的运行时预设动画 |

可用 `hidden: true` 表示“有中间状态，但不新增可见节点”。这对两个主要可见关键帧之间插入额外的
`modifyTileNBT` 步骤尤其有用。
随包提供的 `/assets/ponder_demo.json` 现在也包含了一个隐藏的中间方块 NBT 更新示例。

---

## 关键帧粒子

Ponder 关键帧可以在时间轴向前播放时生成轻量场景粒子。向后拖动时间轴时不会重复补播，
因此拖动预览和跳转仍然保持确定性。

普通粒子：

```json
"particles": [
  {
    "name": "smoke",
    "x": 1.5,
    "y": 1.85,
    "z": 1.5,
    "vx": 0.0,
    "vy": 0.01,
    "vz": 0.0,
    "size": 0.18,
    "time": 16,
    "amount": 3
  }
]
```

爆炸预设：

```json
"particles": [
  {
    "preset": "explosion",
    "x": 1.5,
    "y": 1.45,
    "z": 1.5,
    "time": 8,
    "power": 2.4
  }
]
```

天气预设：

```json
"particles": [
  {
    "preset": "rain",
    "weather": "rain",
    "x": [0, 2, 4, 4],
    "z": [0, 2, 0, 2],
    "time": 90,
    "amount": 9
  }
]
```

提示预设：

```json
"particles": [
  {
    "preset": "indicator",
    "color": "#6EDCFF",
    "x": [1, 2],
    "y": [1, 2],
    "z": [0, 1],
    "time": 12,
    "amount": 6
  },
  {
    "preset": "redstone",
    "x": 3,
    "y": 1,
    "z": 1
  }
]
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `preset` | string? | 当前支持 `explosion`（接近原版爆炸的闪光、烟雾和外扩爆裂粒子）、`rain`（共用天气预设）、`indicator`（可自定义颜色的方块提示粒子），以及默认红色快捷别名 `redstone` |
| `weather` | string? | 用于 `preset: "rain"`。支持 `rain` 和 `snow` |
| `color` | string? | 用于 `preset: "indicator"` / `preset: "redstone"`。支持 `#RRGGBB`、`#RRGGBBAA`、`0xRRGGBB`、`0xRRGGBBAA`。省略时使用 Ponder 风格的默认红色 |
| `name` | string? | 普通粒子外观。支持 `billboard`、`smoke`、`largesmoke`、`explode`、`flash`、`largeexplode`、`hugeexplosion` |
| `particle` / `kind` | string? | `name` 的兼容别名 |
| `x`、`y`、`z` | float 或数组 | 普通粒子的世界坐标。对 `preset: "rain"` 仅使用 `x/z` 作为天气覆盖范围。对 `preset: "indicator"` / `preset: "redstone"`，单值表示单个方块，数组或空白/逗号分隔字符串会展开为该轴上的方块坐标集合 |
| `vx`、`vy`、`vz` | float? | 初速度向量，`motionX/Y/Z` 也可作为别名 |
| `time` / `lifetime` | int? | 粒子生命周期，单位为 tick。对 `preset: "rain"` 而言，这里表示包含开始和结束过渡在内的总天气时长 |
| `size` | float? | 粒子半尺寸，单位为方块 |
| `amount` | int? | 普通粒子的生成数量；爆炸预设省略时会根据 `power` 自动缩放；对 `preset: "rain"` 而言，表示平均每 tick 的天气密度；对 `preset: "indicator"` / `preset: "redstone"` 而言，表示每个目标方块内部发射的提示粒子数量 |
| `power` | float? | `explosion` 预设的爆炸强度 |

天气预设说明：

- `preset: "rain"` 是雨雪共用的天气预设入口。
- 使用 `weather: "rain"` 生成接近原版正常世界的下雨效果，并带有少量落地水花。
- 使用 `weather: "snow"` 生成更慢、更轻的飘雪效果。
- Ponder 天气归时间轴管理，会和时间轴的回放、暂停、跳转、快进保持一致。
- 如果需要独立于时间轴、始终循环的场景天气，请改用 `<GameScene>` 内的 `<Weather>` 标签。
- 天气预设不使用 `y`；垂直范围由运行时根据场景边界自动推导。
- `x/z` 单值表示一个降水列；数组按端点对定义一个或多个矩形区域。
- 如果某一轴数组尾部有无法完整配对的值，这部分会被忽略。
- 运行时会自动补上短暂的开始过渡、稳定段和结束过渡。
- 同一时段内，同一个 `x/z` 列不会叠加多种天气；前面声明的天气优先占用重叠列。

提示预设说明：

- `preset: "indicator"` 会在每个目标方块内部生成一小团短时彩色提示粒子，效果接近 Create/Ponder 的红石提示。
- `preset: "redstone"` 只是同一效果的默认红色快捷别名。
- 对这两个预设，`x/y/z` 都支持单值、数组，或空白/逗号分隔的坐标列表。
- 运行时会分别展开三个坐标轴，然后对组合出的每个目标方块都发射提示粒子。

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
  "textKey": "guidenh.sample.ponder.text.collect_ingots",
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
  "textKey": "guidenh.sample.ponder.text.overview",
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
| `text` | string | 回退显示文本 |
| `textKey` | string? | 优先从资源包 `lang` 文件解析的翻译键；解析失败时回退到 `text` |
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
`ref` 继续修改它们。

```json
"createEntities": [
  {
    "ref": "marker",
    "sceneEntityId": "marker",
    "id": "minecraft:pig",
    "x": 1.5, "y": 1.0, "z": 2.5,
    "yaw": 180.0,
    "bodyYaw": 180.0,
    "headYaw": 210.0,
    "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
  },
  {
    "ref": "operator",
    "id": "player",
    "name": "GuideNH",
    "x": 2.5, "y": 1.0, "z": 1.5,
    "yaw": 225.0,
    "bodyYaw": 225.0,
    "headYaw": 255.0,
    "showName": true,
    "showCape": true,
    "headRotation": "-10 18 0",
    "leftArmRotation": "-70 18 -12",
    "rightArmRotation": "32 -8 18",
    "leftLegRotation": "8 0 0",
    "rightLegRotation": "-6 0 0"
  }
],
"mergeEntityNBT": [
  { "ref": "marker", "nbt": "{Saddle:1b}" }
],
"modifyEntityNBT": [
  { "ref": "marker", "path": "CustomName", "value": "\"After\"" },
  { "ref": "operator", "headYaw": 290.0, "headRotation": "-22 38 0", "leftArmRotation": "-38 0 -8" }
],
"removeEntityNBT": [
  { "ref": "marker", "path": "CustomNameVisible" }
]
```

支持的实体状态字段：

- `sceneEntityId` 可为实体指定稳定的场景内 id。`mount` 指向的始终是这种稳定 id，而不是另一个 `ref`。
- `x`、`y`、`z` 可重定位引用实体。
- `yaw`、`pitch`、`bodyYaw`、`headYaw` 可控制朝向；如果只写了 `yaw`，未写 `bodyYaw` / `headYaw` 时会默认跟随 `yaw`。
- `mount` 可让当前实体骑乘另一个稳定 `sceneEntityId`；`unmount: true` 会清除当前稳定骑乘关系。
- `showName`、`showCape`、`baby` 与普通 `<Entity>` 标签中的同名属性语义一致。
- 预览玩家实体（`id: "player"` 及其他预览玩家别名）还支持 `headRotation`、`leftArmRotation`、`rightArmRotation`、`leftLegRotation`、`rightLegRotation`、`capeRotation`，写法都是 `"x y z"` 角度。

`setEntityNBT` 会用新的 SNBT 复合标签替换引用实体的 NBT。其余实体动作数组中的 NBT 部分都可以省略，只保留位置、稳定骑乘关系或预览玩家姿态控制。`removeEntities` 则可以按 `ref` 删除实体，而无需暴露运行时实体 id。所有实体操作在定位时都会从关键帧 0 重新播放，因此向后拖动时间轴时，思索时间轴创建的实体不会残留在旧状态。

不建议只依赖原始乘客 NBT 来表达跨实体场景关系。真正保证回放、预览重建、导入导出和站点导出捕获后仍然一致的是稳定场景实体 id。

运行时实体预设动画使用 `animateEntities`：

```json
"animateEntities": [
  { "ref": "operator", "preset": "rightClick", "ticks": 8 },
  { "ref": "operator", "preset": "leftClick", "ticks": 6 },
  { "ref": "operator", "preset": "jump", "ticks": 10, "height": 0.6 },
  { "ref": "marker", "preset": "hurt", "ticks": 10 },
  { "ref": "operator", "preset": "sneak" },
  { "ref": "operator", "preset": "unsneak" },
  { "ref": "marker", "preset": "walkTo", "x": 2.2, "z": 1.6, "ticks": 20 },
  { "ref": "dropped", "preset": "moveTo", "x": 2.0, "y": 1.3, "z": 1.0, "ticks": 12 }
]
```

- `leftClick` 和 `rightClick` 会按给定 `ticks` 或预设默认时长播放预览玩家的挥臂动作。
- `jump` 会播放支持重放的竖直抛物线跳跃；`height` 可选，默认 `1`。
- `hurt` 会驱动原版生物的受伤计时器，也就是渲染里那层红色受伤闪烁。
- `sneak` 和 `unsneak` 会切换潜行状态，并持续生效，直到后续实体动作再次修改。
- `walkTo` 会插值移动到目标位置，沿路径转向，并为生物实体驱动四肢摆动，呈现行走效果。
- `moveTo` 只做直接平移，不额外修正朝向，更适合掉落物或通用展示实体。
- 所有预设都会根据当前时间轴刻重新计算，因此拖动、重播和导出看到的姿态始终一致。

默认预设时长已尽量对齐最接近的原版行为：

- `leftClick`：`6` tick，对应 `EntityLivingBase#getArmSwingAnimationEnd()` 的默认挥手时长。
- `rightClick`：`6` tick，对应原版成功右键交互（例如放置方块）时触发的通用挥手时长。
- `jump`：`12` tick，对应标准实体在无跳跃药水时完整一次原版跳跃的滞空时长。
- `hurt`：`10` tick，对应原版 `hurtTime` / `maxHurtTime`。

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
