---
navigation:
  title: 导入结构
  parent: index.md
---

# 导入结构

`<ImportStructure>` 和 `<ImportStructureLib>` 将外部结构数据展开到 `<GameScene>` 中。

## StructureLib 预览

`<ImportStructureLib controller="modid:name" />` 加载由 StructureLib 控制器注册的多方块结构：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
</GameScene>

将光标移到 StructureLib 结构方块上可以看到额外的结构说明；按住 `Shift` 会展开候选替换方块。如果该结构提供仓室或信道元数据，还会自动出现仓室高亮按钮和底部滑条。

## ImportStructure + RemoveBlocks

`<ImportStructure src="..." />` 展开外部 SNBT/NBT 文件。`<RemoveBlocks id="..." />` 在导入后按 id 移除方块：

<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
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

搭配区域选择魔棒：默认即 SNBT 模式，潜行+右键导出当前 `<ImportStructure>` 兼容的 SNBT；空中右键可在 `snbt` 与 `blocks`（旧版 `<Block>` 列表，向下兼容）之间切换。
