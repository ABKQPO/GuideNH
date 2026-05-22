---
navigation:
  title: 内容嵌入
  parent: index.md
  position: 135
  icon: minecraft:wool:3
categories:
  - widgets
---

# 内容嵌入与文字环绕

所有块级标签均支持 `wrap` 和 `align` 属性，对应 Microsoft Word 的"文字环绕"选项。

## 环绕模式

| 模式 | 说明 |
|---|---|
| `inline` | 默认——块独占一行垂直槽（嵌入型） |
| `square` | 浮动到左侧或右侧，文字在方形框内环绕（方形环绕） |
| `tight` | 本布局系统中等同 `square`（紧密型） |
| `through` | 本布局系统中等同 `square`（穿越型） |
| `top-bottom` | 两侧无文字；`align` 控制水平位置（上下型） |
| `behind` | 同 `inline` 对齐方式，但渲染在文字下方（衬于文字下方） |
| `front` | 同 `inline` 对齐方式，但渲染在文字上方（浮于文字上方） |

## 对齐方式

| 值 | 含义 |
|---|---|
| `left` | 靠左（默认） |
| `center` | 水平居中 |
| `right` | 靠右 |

## 嵌入型（默认）

默认嵌入模式下的方块 3D 预览，会占据自己的垂直位置。

<BlockImage id="minecraft:stone" />

方块预览下方的文字。

## 上下型，居中对齐

<BlockImage id="minecraft:planks" align="center" />

方块预览两侧没有文字，预览水平居中。

## 上下型，右对齐

<BlockImage id="minecraft:planks" align="right" />

方块预览推至右侧边缘。

## 方形环绕，左浮动

<BlockImage id="minecraft:stone" wrap="square" align="left" scale={2} />

此段落文字将流向方块预览右侧。预览会被注册为左侧文档级浮动元素，后续段落会自动
收窄可用宽度以避开它，效果等同于 CSS 的 `float: left`。更多文字用于演示
多行文字的环绕效果。

## 方形环绕，右浮动

<BlockImage id="minecraft:glass" wrap="square" align="right" scale={2} />

此段落文字流向方块预览左侧。`wrap="square"` 配合 `align="right"` 将在布局
引擎中注册一个右侧浮动元素。更长的句子会在浮动块左侧折行显示。

## 居中对齐（无文字环绕）

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />

方块预览水平居中，两侧没有文字。

## 物品图标

<ItemImage id="minecraft:diamond" align="center" />

<ItemImage id="minecraft:emerald" align="right" />

## FloatingImage 使用 wrap / align

<FloatingImage src="test1.png" wrap="square" align="left" width="64" title="左浮动" />

图片左浮动，右侧文字环绕——使用新的 `wrap` + `align` 语法，
效果等同于 `<FloatingImage>` 的旧版 `align="left"` 属性。

<FloatingImage src="test1.png" wrap="square" align="right" width="64" title="右浮动" />

图片右浮动，左侧文字环绕——使用 `wrap="square" align="right"`。

## 配方

<Recipe id="minecraft:stone" wrap="square" align="left" fallbackText="（配方不可用）" />

配方框左浮动，右侧文字环绕。

<Recipe id="minecraft:glass" wrap="square" align="right" fallbackText="（配方不可用）" />

配方框右浮动，左侧文字环绕。

## 游戏场景左浮动

<GameScene wrap="square" align="left" zoom={4} showBackground={false} width="120" height="90">
  <Block id="minecraft:furnace" />
</GameScene>

本段文字流向场景视口右侧。`wrap="square" align="left"` 组合将场景注册为左侧
文档级浮动，后续段落都会收窄可用宽度直到浮动清除。此处追加额外文字，
以展示多行文字环绕场景的效果。

## 游戏场景居中

<GameScene align="center" zoom={4} showBackground={false} width="200" height="120">
  <Block id="minecraft:crafting_table" />
</GameScene>

使用 `align="center"` 将场景水平居中，无浮动效果——文字只出现在场景上方和下方。

## 游戏场景右浮动

<GameScene wrap="square" align="right" zoom={4} showBackground={false} width="120" height="90">
  <Block id="minecraft:chest" />
</GameScene>

本段文字流向场景视口左侧。右侧浮动将每一行文字的行框向右收窄，
直到场景的注册浮动清除为止。

## Column 左浮动

<Column wrap="square" align="left" gap="4" width="90">

**合成台**

用于合成物品。

</Column>

带有 `wrap="square" align="left"` 的 `<Column>` 标签与其他块级标签一样左浮动。
Column 包含结构化内容（标题、文字），右侧文字环绕在其旁边。

## Column 居中

<Column align="center" gap="4" width="160">

**提示**

此 Column 水平居中，不产生浮动。

</Column>

居中 Column 下方的文字。
