---
navigation:
  title: 场景粒子
  parent: index.md
  position: 159
categories:
  - scenes
---

# 场景粒子

GuideNH 现在支持两条粒子编写链路：

1. 在 `<GameScene>` 里使用静态 `<Particle>`
2. 在 `<GameScene>` 里使用静态 `<Weather>`
3. 在 `ImportPonder` 的 JSON 里使用运行时 `particles`

## 静态 `<Particle>`

`<Particle>` 会在固定的世界坐标渲染一个静止粒子。不填写 `name` 时会使用默认的面片粒子。

| 属性 | 说明 |
| --- | --- |
| `name` | 粒子外观，默认 `billboard`。支持 `billboard`、`smoke`、`largesmoke`、`explode`、`flash`、`largeexplode`、`hugeexplosion`。`particle`、`quad`、`sheet` 会作为 `billboard` 的别名处理。 |
| `x`、`y`、`z` | 粒子的世界坐标原点，默认都是 `0.5` |
| `size` | 粒子的半尺寸，单位为方块，默认 `0.18` |

```mdx
<GameScene width="192" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:furnace" x="1" />
  <Particle x="1.5" y="1.85" z="0.5" size="0.22" />
  <Particle name="smoke" x="1.5" y="1.35" z="0.5" size="0.18" />
</GameScene>
```

## 静态 `<Weather>`

`<Weather>` 会直接在场景里渲染动画雨雪。它是独立的场景组件，不是普通面片粒子。
场景天气会在普通渲染时持续循环，底层复用与时间轴天气相同的降水几何路径，但不支持
时间轴暂停或拖动。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `weather` / `type` | `rain` | 天气类型。支持 `rain`、`snow`。 |
| `x`、`z` | 场景边界 | 覆盖的降水列。单值表示一列；数组按端点对定义矩形区域。 |
| `density` | 按类型决定 | 覆盖密度。值越大，保留的降水列越多。 |

```mdx
<GameScene width="224" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
```

天气标签说明：

- `<Weather>` 不使用 `y`；垂直范围由场景边界和可阻挡降水的方块共同推导。
- 如果某一轴数组末尾有无法完整配对的值，这部分会被忽略。
- 重叠的天气标签不会在同一个 `x/z` 列叠加；前面声明的标签会保留共享列。

## Ponder `particles`

Ponder 粒子只会在时间轴向前推进到关键帧时生成。向后拖动时间轴不会重复补播，
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
    "count": 3
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
    "weather": "snow",
    "x": [0, 2],
    "z": [0, 2],
    "time": 100,
    "count": 8
  }
]
```

| 字段 | 说明 |
| --- | --- |
| `preset` | 特殊预设。`explosion` 会生成接近原版爆炸的闪光、烟雾和外扩爆裂粒子。`rain` 用于共用天气预设。 |
| `weather` | 用于 `preset: "rain"`。支持 `rain` 和 `snow`。 |
| `name` | 普通粒子外观。支持 `billboard`、`smoke`、`largesmoke`、`explode`、`flash`、`largeexplode`、`hugeexplosion`。 |
| `particle` / `kind` | `name` 的兼容别名。 |
| `x`、`z` | 普通粒子的世界坐标，或 `preset: "rain"` 的天气覆盖范围。单值表示单列，数组按端点对定义矩形区域。 |
| `vx`、`vy`、`vz` | 初速度，`motionX/Y/Z` 也可作为别名。 |
| `time` / `lifetime` | 粒子生命周期，单位为 tick。对 `preset: "rain"` 而言，这里表示包含开始过渡和结束过渡的总天气时长。 |
| `size` | 粒子半尺寸，单位为方块。 |
| `count` | 普通粒子的生成数量；爆炸预设省略时会根据 `power` 自动缩放。对 `preset: "rain"` 而言，表示平均每 tick 的天气密度。 |
| `power` | `explosion` 预设的爆炸强度。 |

天气预设说明：

- `preset: "rain"` 是共用天气预设入口。
- 使用 `weather: "rain"` 生成雨幕，使用 `weather: "snow"` 生成飘雪。
- Ponder 天气归时间轴管理，会随整条时间轴一起回放、暂停、跳转和快进。
- 如果需要独立于时间轴、始终循环的场景天气，请改用 `<GameScene>` 内的 `<Weather>`。
- 天气预设不使用 `y`；垂直范围由场景边界自动推导。
- 如果某一轴数组末尾有无法完整配对的值，这部分会被忽略。
- 运行时会自动添加短暂的淡入、稳定段和淡出。
- 雨效果会带有快速雨滴和少量落地水花，雪效果则使用更慢的飘落粒子。
- 同一个 `x/z` 列在同一时段不会叠加多种天气。
