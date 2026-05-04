---
navigation:
  title: 镜头与视口
  parent: index.md
---

# 镜头与视口

视口尺寸变体、镜头预设与偏移测试。

## 视口尺寸变体

256×96（矮宽）：

<GameScene width="256" height="96" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Block id="minecraft:stone" x="3" />
</GameScene>

128×128（正方）：

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

## 镜头预设

`<GameScene>` / `<Scene>` 新增属性：

* `perspective="isometric_north_east"` / `"isometric_north_west"` / `"up"` — 选择预设视角（yaw/pitch/roll）；
* `rotateX` / `rotateY` / `rotateZ` — 在预设之上显式覆盖单个轴的旋转；
* `offsetX` / `offsetY` — 屏幕空间平移（单位：方块）；
* `centerX` / `centerY` / `centerZ` — 显式指定旋转中心（覆盖自动居中）。

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

## 屏幕空间平移

`offsetX` / `offsetY` 平移——右侧场景向右下偏移 2/1 个方块：

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

不传 `color` 时应默认为**亮绿**（与显式指定 `#FF0000` 红色的菱形并排对比）：

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
