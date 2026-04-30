---
navigation:
  title: Markdown 测试
---

# Markdown 测试

这一页是运行时渲染检查页，用来在游戏里确认 GuideNH 当前支持的 Markdown 与相关扩展效果。

## 行内格式

| Markdown                            | 另一种写法        | 效果                              |
|-------------------------------------|-------------------|-----------------------------------|
| `*Italic*`                          | `_Italic_`        | *Italic*                          |
| `**Bold**`                          | `__Bold__`        | **Bold**                          |
| `~~Strikethrough~~`                 | `~Strikethrough~` | ~~Strikethrough~~                 |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

自动链接：访问 https://example.com/docs、www.example.org 或 guide@example.com

Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">打开子页面</a><br clear="all" />

## 标题

标题可以通过前缀 `#` 定义。

# 一级标题

## 二级标题

### 三级标题

#### 四级标题

##### 五级标题

###### 六级标题

## 引用块与提示块

Markdown：

```
> Blockquote
```

效果：

> Blockquote

> [!NOTE]
> GitHub 风格的 NOTE 提示块，检查左侧线、图标与标题颜色。

> [!TIP]
> GitHub 风格的 TIP 提示块，检查左侧线、图标与标题颜色。

> [!IMPORTANT]
> GitHub 风格的 IMPORTANT 提示块，检查左侧线、图标与标题颜色。

> [!WARNING]
> GitHub 风格的 WARNING 提示块，检查左侧线、图标与标题颜色。

> [!CAUTION]
> GitHub 风格的 CAUTION 提示块，检查左侧线、图标与标题颜色。

## 列表

Markdown：

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

效果：

* List
* List
* List

1. One
2. Two
3. Three

- [x] 已完成任务
- [ ] 待处理任务

<Column width="220">
- 限宽列表行示例
- 这一项会更早换行，方便检查自定义宽度
</Column>

## 表格

Markdown：

```markdown
| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |
```

效果：

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |

对齐检查：

| Left | Center | Right |
| :--- | :----: | ----: |
| iron |   42   |   128 |
| gold |   17   |    64 |

普通 Markdown 表格也可以使用运行时列宽 hint：

| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }

三列表宽示例：

| Material | Count | Notes |
| --- | --- | --- |
| Iron | 42 | base line |
| Gold | 17 | compact |
| Diamond | 9 | rare |
{: widths="130,70,150" }

## 引用式链接与图片

[Guide Ref][doc]

![Machine Diagram][img]

[doc]: ./subpage.md#top
[img]: test1.png "Machine Diagram"

## 折叠详情

<details open>
<summary>更多内容</summary>

这里是运行时 details 内部的文本。
</details>

## 代码块

显式语言：

```lua
local value = 42
print(value)
```

显式 Scala：

```scala
object Demo extends App {
  println("scala language label")
}
```

显式 Markdown：

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

这个未标注语言的围栏应保持为代码块，并自动识别为 Scala：

```
object Demo extends App {
  println("auto detected scala")
}
```

这个未标注语言的围栏应保持为代码块，并自动识别为 CSV，而不是直接渲染成表格：

```
name,value
iron,42
gold,17
```

缩进代码块：

    print("indented code block")

强制代码块视口高度，并在内部滚动：

```java height=96
line 01
line 02
line 03
line 04
line 05
line 06
line 07
line 08
line 09
line 10
line 11
line 12
```

## CSV 运行时表格

这个显式 `csv` 围栏会渲染成运行时表格：

```csv
name,value
iron,42
gold,17
```

这个显式 `csv` 围栏还会应用列宽 hint：

```csv widths=120,80
name,value
iron,42
gold,17
```

带引号的 widths 与 `header=false`：

```csv widths="120,80" header=false
iron,42
gold,17
diamond,9
```

导入 CSV：

<CsvTable src="./markdown-table.csv" />

带宽度的导入 CSV：

<CsvTable src="./markdown-table.csv" widths="120,80" />

## Mermaid 思维导图

内联 Mermaid 围栏：

```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Languages
      Lua
      Scala
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
      Wheel to zoom
```

带显式节点坐标的思维导图：

```mermaid
mindmap
  Root((Pinned Root))
    Branch[Branch]::pos(120,80)
      Child A
      Child B::icon(fa fa-code)
```

导入 Mermaid 文件：

<Mermaid src="./markdown-mindmap.mmd" />

## 脚注

脚注引用[^one]

[^one]: 这里是脚注的提示内容

## GameScene 运行时解析

基础方块摆放：

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

红石示例：

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:stone" x="2" />
    <Block id="minecraft:redstone_wire" y="1" />
    <Block id="minecraft:redstone_wire" x="1" y="1" />
    <Block id="minecraft:redstone_wire" x="2" y="1" />
    <Block id="minecraft:lever" x="-1" y="1" />
    <Block id="minecraft:redstone_lamp" x="3" y="1" />
</GameScene>

方块注解模板：

<GameScene zoom={2} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="1" />
  <Block id="minecraft:log" z="1" />
  <Block id="minecraft:log" x="1" z="1" />

  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      这段文字会在悬停时显示。<ItemImage id="minecraft:stone" />
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>

注解 tooltip 内的嵌套 Markdown：

<GameScene width="256" height="192" zoom={4} interactive={false}>
  <Block id="minecraft:diamond_block" x="-1" />
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:diamond_block" x="1" />
  <Block id="minecraft:beacon" y="1" />
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <Color color="#FFD24C">**Effect**</Color>: grants nearby players continuous buffs.

    > [!TIP]
    > More pyramid tiers unlock more options.

    ```markdown
    * Hover tooltip markdown
    * Nested code block sample
    ```
  </DiamondAnnotation>
</GameScene>
