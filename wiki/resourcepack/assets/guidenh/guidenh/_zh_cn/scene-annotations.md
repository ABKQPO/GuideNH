---
navigation:
  title: 注解
  parent: index.md
---

# 注解

所有注解都使用世界坐标，可通过场景按钮的"显示/隐藏注解"切换。

## DiamondAnnotation 菱形标注

在 3D 预览任意世界坐标处放一个**菱形标注**。菱形始终朝向屏幕，光标悬停时出现白色半透明遮罩并弹出富文本 tooltip。

激活的信标示例——3×3 钻石块底座，顶部放一个信标；菱形标注 tooltip 内含嵌套 3D 预览：

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

    激活条件：下方为 3×3 / 5×5 / 7×7 / 9×9 的**钻石 / 铁 / 金 / 绿宝石 / 下界合金**金字塔底座。

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
- `BlockAnnotation` 用 `pos="x y z"`（整数）选中某个方块，等价于 1×1×1 的盒子。
- `LineAnnotation` 用 `from="x y z"` / `to="x y z"` 画一条线段（支持小数）。

所有三种都支持 `color="#AARRGGBB" 或 "#RRGGBB"`、`thickness`（线宽，单位：像素，默认 `1`）以及 `alwaysOnTop`（始终绘制在最前）。子节点是富文本，会作为悬停 tooltip 显示。

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
