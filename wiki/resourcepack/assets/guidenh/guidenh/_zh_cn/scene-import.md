---
navigation:
  title: 导入结构
  parent: index.md
  position: 162
categories:
  - scenes
---

# 导入结构

`<ImportStructure>` 和 `<ImportStructureLib>` 将外部结构数据展开到 `<GameScene>` 中。

## StructureLib 预览

`<ImportStructureLib controller="modid:name" />` 加载由 StructureLib 控制器注册的多方块结构：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
</GameScene>

将光标移到 StructureLib 结构方块上可以看到额外的结构说明；按住 `Shift` 会展开候选替换方块。如果该结构提供仓室或信道元数据，还会自动出现仓室高亮按钮和底部滑条。

具名的 StructureLib 导入也可以驱动按状态显示的注解和音效：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib name="main" controller="botanichorizons:automatedCraftingPool" />
  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="1..3"
    showWhenChannels="input:1..3"
  >
    这个标记会跟随 `main` 结构当前选择的 StructureLib 状态。
  </BlockAnnotation>
  <PlaySound
    sound="guidenh:guide.sample_click"
    trigger="click"
    showWhenStructure="main"
    showWhenTier="2..3"
  />
</GameScene>

## ImportStructure + RemoveBlocks

`<ImportStructure src="..." />` 展开外部 SNBT/NBT 文件。`<RemoveBlocks id="..." />` 在导入后按 id 移除方块：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
  <PlaySound sound="guidenh:guide.sample_click" trigger="click" volume="0.75" />
  <PlaySound sound="guidenh:guide.sample_hover" trigger="hover" volume="0.35" />
</GameScene>

## ReplaceBlock

`<ReplaceBlock from="..." to="..." />` 将已放置的匹配方块替换为新方块。未指定范围时全局扫描，
提供 `x/y/z/dx/dy/dz` 中任意一项即启用包围盒模式：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
  <ReplaceBlock from="minecraft:cobblestone" to="minecraft:brick_block" x="1" y="0" z="1" dx="3" dy="1" dz="3" />
</GameScene>

使用 `from_nbt` 可进一步将匹配范围缩小到 TileEntity NBT 中包含特定键的方块；使用 `to_nbt`
可为替换后的方块指定 TileEntity 数据。

## PlaceBlock

`<PlaceBlock id="..." />` 无条件地用单一方块类型填充一个轴对齐的包围盒，覆盖原有方块。
使用 `dx`/`dy`/`dz` 可一次填充多方块区域：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <PlaceBlock id="minecraft:stone" dx="5" dy="1" dz="5" />
  <PlaceBlock id="minecraft:glass" y="1" dx="5" dz="5" />
</GameScene>

## SNBT 文件格式

`<ImportStructure src="..." />` 接受 SNBT（1.7.10 原生 `JsonToNBT`：`pos:[0,1,2]` 会被识别为 IntArray，不需要现代的 `[I; ...]` 前缀），也可读取 gzip / 未压缩的二进制 NBT。schema 为 `{size, palette, blocks}`，每个 `block` 可携带 `meta` 与 `nbt`（`nbt` 需含与原版一致的 TileEntity `id` 字段，例如 `"Chest"`）。`x/y/z` 属性可整体平移结构。

示例引用 `assets/example_structure.snbt`：5×3×5 鹅卵石平台、中心荧石、四角不同朝向的橡木台阶（meta=2/3）、两块上下不同的石质台阶（meta=0 / meta=8）、两支竖火把（meta=5），以及一个带着钻石/铁锭/红石/面包的箱子（TileEntity NBT）。

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotation color="#ffd24c" pos="2 1 2" alwaysOnTop={true}>
    **这个箱子是带 NBT 的**：里面预先装了钻石 / 铁锭 / 红石 / 面包。SNBT 写法：

    ```
    {pos:[2,1,2], state:4, meta:3, nbt:{id:"Chest", Items:[
      {Slot:0b, id:"minecraft:diamond",    Count:5b,  Damage:0s},
      {Slot:1b, id:"minecraft:iron_ingot", Count:32b, Damage:0s},
      ...
    ]}}
    ```
  </BlockAnnotation>
  <BoxAnnotation color="#ee3333" min="1 1 1" max="4 1.5 2" thickness="0.04">
    **台阶区**：meta `0` / `8` 分别对应下半、上半台阶。
  </BoxAnnotation>
  <LineAnnotation color="#33ddee" from="1 1.4 3" to="3 1.4 3" thickness="0.06">
    **竖火把连线**：这两支火把 meta=5（竖立在地面上）。
  </LineAnnotation>
</GameScene>

搭配区域选择魔棒：选区是客户端全局状态，所有魔棒共享同一组 Pos1/Pos2。左键设置 Pos1，右键设置 Pos2；两种点击都可以对准空气，按光标 reach 终点选取坐标。潜行 + 左键清空当前选区，潜行 + 右键按当前模式导出。也可以使用 `/guidenhc pos1 <x> <y> <z>`、`/guidenhc pos2 <x> <y> <z>`、`/guidenhc clearselection`，其中 `~` 坐标相对玩家。`/guidenhc exportstructure [--mode snbt|snbt_e|blocks|blocks_e]` 会导出当前选区，也可以额外传入 `<x> <y> <z> <sizeX> <sizeY> <sizeZ>`。Scene Editor 读取同一片客户端选区；如果服务端也安装了 GuideNH，会优先请求服务端按选区导出方块数据，从权威世界取得 TileEntity 信息。
