---
navigation:
  title: Tooltip
  parent: index.md
---

# Tooltip

`tooltip` 属性和 `<Tooltip>` 富文本容器测试。

## 悬停基础测试

将鼠标移到物品图标或方块图标上时，会浮现原生游戏 tooltip：

<Row>
  <ItemImage id="minecraft:diamond" />
  <BlockImage id="minecraft:furnace" />
  <RecipeFor id="minecraft:stick" />
</Row>

悬停方块时显示白色描边+方块名：

<GameScene width="160" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="1" />
</GameScene>

## ItemStack tooltip 开关

- 默认（显示 tooltip）：<ItemImage id="minecraft:diamond_sword" />
- 禁用（`noTooltip`）：<ItemImage id="minecraft:diamond_sword" noTooltip={true} />
- ItemLink：<ItemLink id="minecraft:diamond_axe" />
- 文字链接：[跳到 Markdown 页](./markdown.md)

---

**Markdown 混合内联格式**：**粗体** / *斜体* / ~~删除线~~ / `代码` / [链接](./japanese.md)

## Tooltip 内容渲染测试

### 文本 tooltip

<Row>
  <ItemImage id="minecraft:apple" tooltip="这是一段纯文本 Tooltip。" />
  <ItemImage id="minecraft:golden_apple" tooltip="第一行\n第二行\n第三行——多行测试" />
  <ItemImage id="minecraft:pumpkin_pie" tooltip="这段文字非常长，用来测试 tooltip 文本框是否会根据文字内容自动调整宽度以避免溢出或截断。这是第二句话，继续加长。" />
</Row>

### ItemStack tooltip

<Row>
  <ItemImage id="minecraft:bread" />
  <ItemImage id="minecraft:cooked_porkchop" />
  <ItemImage id="minecraft:diamond_sword:0:{ench:[{id:16s,lvl:5s},{id:21s,lvl:3s}]}" />
  <ItemImage id="minecraft:potion:0:{CustomPotionEffects:[{Id:1b,Amplifier:1b,Duration:9600}]}" />
</Row>

### 物品图 tooltip

<ItemImage id="minecraft:diamond" tooltip={<ItemImage id="minecraft:emerald" scale="3" />} />

### 配方 tooltip

<ItemImage id="minecraft:crafting_table" tooltip={<RecipeFor id="minecraft:crafting_table" />} />

### 3D 预览内 tooltip

<GameScene width="192" height="160" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="1" />
</GameScene>

## 富内容 Tooltip（`<Tooltip>`）

`<Tooltip>` 使用 `content` 属性传入 JSX，子节点是触发元素。

### 纯 Markdown 富文本

<Tooltip content={<>
  ## Markdown 标题

  **粗体** / *斜体* / ~~删除线~~ / `代码`

  - 列表项 1
  - 列表项 2：<ItemImage id="minecraft:diamond" />

  > 这是一段引用。
</>}>
  悬停查看 Markdown 富文本
</Tooltip>

### 嵌入 ItemImage / BlockImage

<Tooltip content={<>
  <ItemImage id="minecraft:diamond" scale="2" /> 钻石

  <BlockImage id="minecraft:diamond_block" scale="3" /> 钻石块
</>}>
  悬停查看物品和方块
</Tooltip>

### 嵌入配方

<Tooltip content={<RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />}>
  悬停查看熔炉配方
</Tooltip>

### 嵌入 3D 预览

<Tooltip content={<GameScene width="160" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="1" />
</GameScene>}>
  悬停查看 3D 预览
</Tooltip>

### 混合富文本

<Tooltip content={<>
  <RecipeFor id="minecraft:wooden_door" />

  ---

  <GameScene width="160" height="128" zoom={5} interactive={false}>
    <Block id="minecraft:oak_door_block" />
    <Block id="minecraft:oak_door_block" y="1" />
  </GameScene>

  <ItemImage id="minecraft:wooden_door" /> 木门：两格高，可被玩家手动或红石信号开关。
</>}>
  悬停查看混合内容
</Tooltip>
