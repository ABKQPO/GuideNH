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

自定义运行时引用块：

```markdown
> {: title="Custom Quote" color="#638ef1" icon="i" }
> 自定义标题、颜色和文本图标。
```

> {: title="Custom Quote" color="#638ef1" icon="i" }
> 自定义标题、颜色和文本图标。

```markdown
> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> 头部使用物品图标。
```

> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> 头部使用物品图标。

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

```java width=220 height=96
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
line 13
line 14
line 15
line 16
line 17
line 18
line 19
line 20
line 21
line 22
line 23
line 24
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

<Mermaid src="./markdown-mindmap.mmd" width="320" height="220" />

## 脚注

脚注引用[^one]

[^one]: 这里是脚注的提示内容

## 图表

GuideNH 内置五种交互式图表标签：`<ColumnChart>` 簇状柱形图、`<BarChart>` 横向条形图、`<LineChart>` 折线图、`<PieChart>` 饼图、`<ScatterChart>` XY 散点图。鼠标悬停时所有图表都会展示数值与百分比的提示框，并高亮当前元素（柱/条变长，折线点沿法线弹出，饼图扇区向外弹出，散点变大）。

### 通用属性

* `title`
* `width` / `height`（默认 320 x 200）
* `background` / `border` 颜色
* `titleColor` / `labelColor`
* `legend` 位置：`none` / `top` / `bottom` / `left` / `right`（默认 `top`）
* `labelPosition`：`none` / `inside` / `outside` / `above` / `below` / `center`

颜色支持 `#RGB`、`#RRGGBB`、`#AARRGGBB`、`0x...`。

### ColumnChart

```mdx
<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="铁" data="120,180,150,210" color="#4E79A7"/>
  <Series name="金" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>
```

<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="铁" data="120,180,150,210" color="#4E79A7"/>
  <Series name="金" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>

额外属性：`categories`（X 轴标签，逗号分隔）、`barWidthRatio`（默认 0.7）、`xAxisLabel` / `yAxisLabel` / `yAxisMin` / `yAxisMax` / `yAxisStep` / `yAxisUnit` / `yAxisTickFormat`、`showXGrid={true}` / `showYGrid={true}`。

### BarChart

```mdx
<BarChart title="模组下载量 (万)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>
```

<BarChart title="模组下载量 (万)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>

属性同 ColumnChart，但 `categories` 在 Y 轴上。

### LineChart

类别 X 轴：

```mdx
<LineChart title="温度" categories="周一,周二,周三,周四,周五" yAxisUnit="℃">
  <Series name="室外" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="室内" data="18,19,20,21,20" color="#E15759"/>
</LineChart>
```

<LineChart title="温度" categories="周一,周二,周三,周四,周五" yAxisUnit="℃">
  <Series name="室外" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="室内" data="18,19,20,21,20" color="#E15759"/>
</LineChart>

数值 X 轴：

```mdx
<LineChart title="信号衰减" numericX={true} xAxisLabel="距离 (m)" yAxisLabel="强度 (dB)">
  <Series name="实测" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>
```

<LineChart title="信号衰减" numericX={true} xAxisLabel="距离 (m)" yAxisLabel="强度 (dB)">
  <Series name="实测" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>

额外属性：`numericX={true}` 启用数值 X 轴；`showPoints={false}` 隐藏数据点标记。

### PieChart

```mdx
<PieChart title="资源占比" labelPosition="outside" legend="right">
  <Slice label="铁" value="45" color="#4E79A7"/>
  <Slice label="铜" value="25" color="#F28E2B"/>
  <Slice label="金" value="15" color="#E15759"/>
  <Slice label="钻石" value="10"/>
  <Slice label="其它" value="5"/>
</PieChart>
```

<PieChart title="资源占比" labelPosition="outside" legend="right">
  <Slice label="铁" value="45" color="#4E79A7"/>
  <Slice label="铜" value="25" color="#F28E2B"/>
  <Slice label="金" value="15" color="#E15759"/>
  <Slice label="钻石" value="10"/>
  <Slice label="其它" value="5"/>
</PieChart>

额外属性：`startAngle`（默认 -90，即 12 点方向）；`clockwise={false}` 反转方向。

### ScatterChart

```mdx
<ScatterChart title="身高-体重" xAxisLabel="身高 (cm)" yAxisLabel="体重 (kg)">
  <Series name="样本 A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="样本 B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>
```

<ScatterChart title="身高-体重" xAxisLabel="身高 (cm)" yAxisLabel="体重 (kg)">
  <Series name="样本 A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="样本 B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>

### 组合：ColumnChart + LineSeries + PieInset

`<ColumnChart>` 与 `<BarChart>` 支持 `<LineSeries>`（叠加共享数值轴的折线）和 `<PieInset>`（在角落显示小型饼图）子元素，可在一张图中组合多种图表样式。

`<PieInset>` 的 `position` 取值：`topRight` / `topLeft` / `bottomRight` / `bottomLeft`（覆盖在绘图区角落）或 `right`（图表自动加宽，把饼图放到外侧独立区域）。

折线叠加层与柱状/条形图共享悬停与提示：把鼠标移到折线点上，相邻线段会加粗、数据点放大，并弹出包含系列名和数值的提示。

```mdx
<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="铁"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="金"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="合计" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="合计占比">
    <Slice label="铁" value="225" color="#a0a0a0"/>
    <Slice label="金" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="铁"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="金"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="合计" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="合计占比">
    <Slice label="铁" value="225" color="#a0a0a0"/>
    <Slice label="金" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
