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
| `++下划线++`                          |                   | ++下划线++                          |
| `^^波浪下划线^^`                      |                   | ^^波浪下划线^^                      |
| `::着重号::`                          |                   | ::着重号::                          |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

自动链接：访问 https://example.com/docs、www.example.org 或 guide@example.com

Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./index.md" title="Open index">打开首页</a><br clear="all" />

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

[doc]: ./index.md#top
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

## 文件树

`tree` / `filetree` 围栏代码块会渲染目录式大纲，并绘制真实的连接线。前缀字符同时支持 Unicode 框线（`│ ├ └ ─`）和 ASCII 形式（`| +-- \-- ` / 4 个空格），可任意混用。每行的文本部分支持常规行内 Markdown（链接、**加粗**、`代码` 等）。

```tree
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
```

ASCII 形式，并演示按行图标（`{:icon=…}` 纯文本，`{:iconPng=path.png}` 图片，`{:iconItem=mod:item[:meta][:{snbt}]}` 物品）：

```filetree
world
|-- {:iconItem=minecraft:grass} 草地生物群系
|   |-- {:icon=橡} 橡木森林
|   \-- {:icon=丘} 起伏丘陵
|-- {:iconItem=minecraft:wool:14} 红色羊毛
\-- {:iconPng=test1.png} 示例资源
```

在 MDX 块标签中也可使用 `<FileTree>`，可选属性 `indent`（每层缩进像素，默认 `14`）和 `gap`（行间额外像素，默认 `0`）：

```html
<FileTree indent="16" gap="2">
docs
├── intro.md
└── advanced
    ├── tags.md
    └── recipes.md
</FileTree>
```

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

## 函数图

GuideNH 提供 Desmos 风格的交互函数图，支持三种写法：`funcgraph` 围栏代码块、
`<FunctionGraph>` MDX 容器，以及只画一条曲线的简写 `<Function>`。三者共享同一
面板：可配置尺寸、X/Y 范围、可选网格/坐标轴、自动象限扩展，以及每条曲线的定义域
限制。在曲线上按住鼠标拖动可沿曲线滑动一个带标签的点；提示框默认锚定在该点正上方，
顶部空间不足时自动翻到下方。

任何带有 `label` 的曲线都会出现在面板下方的图例里：每一项是一个小色块加曲线名，
按从左到右的顺序排列，宽度不够时自动换到下一行。

### 围栏代码块

用 ` ```funcgraph ` 作为语言标识符。**第一行**设置面板属性（空格分隔的 `key=value`
对），后续每行是一条表达式或标记点，`#` 之后到行尾为注释。

```funcgraph
width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all
sin(x)        | color=#ff5566 label="sin"
cos(x)        | color=#3399ff label="cos"
x/2           | color=#88cc77 domain=-pi..pi
:0,0
@plot=0 atX=1.5708
```

### `<FunctionGraph>` 容器

容器接受与围栏首行完全相同的面板属性。子元素：曲线用 `<Plot expr="…" />`（或
`<Function expr="…" />`），标记点用 `<Point … />`。

```mdx
<FunctionGraph width="360" height="220" xRange="-6..6" yRange="-3..3" quadrants="all">
  <Plot expr="sin(x)" color="#ff5566" label="sin x"/>
  <Plot expr="x^2 / 4" color="#3399ff" domain="-4..4" label="x² / 4"/>
  <Plot expr="|x| - 1" color="#88cc77" label="|x| - 1"/>
  <Point x="0" y="0"/>
  <Point plot="0" atX="1.5708"/>
</FunctionGraph>
```

<FunctionGraph width="360" height="220" xRange="-6..6" yRange="-3..3" quadrants="all">
  <Plot expr="sin(x)" color="#ff5566" label="sin x"/>
  <Plot expr="x^2 / 4" color="#3399ff" domain="-4..4" label="x² / 4"/>
  <Plot expr="|x| - 1" color="#88cc77" label="|x| - 1"/>
  <Point x="0" y="0"/>
  <Point plot="0" atX="1.5708"/>
</FunctionGraph>

### `<Function>` 简写

只想画一条曲线时，`<Function>` 不必再套外壳，面板属性直接写在元素上：

```mdx
<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff"/>
```

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff"/>

### 面板属性参考

面板属性可以写在围栏首行（`key=value`）或 `<FunctionGraph>` / `<Function>` 的 JSX
属性里（`attr="value"` 或 `attr={value}`）。

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `width` | 整数 | `360` | 面板宽度（像素） |
| `height` | 整数 | `220` | 面板高度（像素） |
| `xRange` | `a..b` | — | X 轴范围简写，等同同时设置 `xMin` 和 `xMax` |
| `xMin` | 数字 | 自动 | X 轴下限；省略时由曲线内容自动推算 |
| `xMax` | 数字 | 自动 | X 轴上限；省略时由曲线内容自动推算 |
| `xStep` | 数字 | 自动 | X 轴采样步长（越小越平滑，越大越快） |
| `yRange` | `a..b` | — | Y 轴范围简写，等同同时设置 `yMin` 和 `yMax` |
| `yMin` | 数字 | 自动 | Y 轴下限 |
| `yMax` | 数字 | 自动 | Y 轴上限 |
| `yStep` | 数字 | 自动 | Y 轴刻度步长 |
| `quadrants` | `all` 或 `1,2,3,4` | `1`（仅第一象限） | 初始显示的象限集合；`all` 等同 `1,2,3,4` |
| `title` | 字符串 | — | 显示在面板顶部的标题文字 |
| `background` | 颜色 | 主题默认 | 面板背景色（`#RGB` / `#RRGGBB` / `#AARRGGBB`） |
| `border` | 颜色 | 主题默认 | 面板边框颜色 |
| `axisColor` | 颜色 | 主题默认 | X/Y 坐标轴颜色 |
| `gridColor` | 颜色 | 主题默认 | 网格线颜色 |
| `showGrid` | 布尔 | `true` | 是否渲染网格线 |
| `showAxes` | 布尔 | `true` | 是否渲染 X/Y 坐标轴 |

### 曲线属性参考

`<Plot>` / `<Function>` 子元素（或围栏中每条非注释、非点行）的可用属性。围栏写法用
管道符 `|` 分隔：`expr | attr1=val1 attr2=val2`。

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expr` | 字符串 | **必填** | 数学表达式，如 `sin(x)`、`x^2 - 1`、`\|x\|` |
| `color` | 颜色 | 自动循环 | 曲线颜色（`#RRGGBB` / `#AARRGGBB`） |
| `label` | 字符串 | — | 图例标签；填写后曲线出现在面板下方图例中 |
| `domain` | 字符串 | 全轴 | 定义域限制，格式为 `min..max` 或逗号分隔的比较子句，如 `x>=0, x<=pi`；两侧支持常量 |
| `inverse` | 布尔 | `false` | `true` 时将表达式解释为 `x = f(y)`，对 y 求值并旋转曲线 |

### 标记点属性参考

`<Point>` 子元素（或围栏中的 `:x,y` / `@plot=N atX=v` 行）。

| 属性 | 说明 |
|---|---|
| `x` + `y` | 固定坐标点，直接在面板对应位置渲染 |
| `plot="N"` + `atX="v"` | 锚定到第 N 条曲线（0-indexed），在 x = v 处求 y 后放置标记 |
| `plot="N"` + `atY="v"` | 锚定到第 N 条曲线，在 y = v 处求 x 后放置标记 |

围栏写法对应：`:x,y` 为固定点；`@plot=N atX=v` / `@plot=N atY=v` 为锚定点。

### 表达式语法参考

| 语法 / 函数 | 说明 |
|---|---|
| `+ - * / %` | 基本算术；`%` 为取模 |
| `^` | 幂运算，**右结合**：`2^3^2` = `2^(3^2)` = 512 |
| 一元 `-` | 取负；优先级低于 `^`，高于 `*` |
| `n!` | 后缀阶乘；通过 Gamma 函数推广到实数（负整数返回 NaN） |
| `\|expr\|` | 绝对值（管道符对） |
| `2x`、`2pi`、`(a)(b)` | 隐式乘法 |
| `sqrt(x)` / `√x` | 平方根 |
| `cbrt(x)` / `∛x` | 立方根 |
| `abs(x)` | 绝对值，等价于 `\|x\|` |
| `sign(x)` | 符号函数：`-1` / `0` / `1` |
| `floor(x)` | 向下取整 |
| `ceil(x)` | 向上取整 |
| `round(x)` | 四舍五入 |
| `sin(x)` / `cos(x)` / `tan(x)` | 三角函数（弧度制） |
| `asin(x)` / `acos(x)` / `atan(x)` | 反三角函数 |
| `atan2(y, x)` / `atan(y, x)` | 双参数反正切，结果范围 (−π, π] |
| `sinh(x)` / `cosh(x)` / `tanh(x)` | 双曲函数 |
| `exp(x)` | 自然指数 eˣ |
| `ln(x)` | 自然对数 |
| `log(x)` | 常用对数（底 10） |
| `log(b, x)` | 以 `b` 为底的对数 |
| `log2(x)` | 以 2 为底的对数 |
| `log10(x)` | 以 10 为底的对数（同 `log(x)`） |
| `min(a, b, …)` | 最小值，支持任意多参数 |
| `max(a, b, …)` | 最大值，支持任意多参数 |
| `pow(a, b)` | 幂运算，等价于 `a^b` |
| `hypot(a, b)` | 斜边长，等价于 `sqrt(a^2 + b^2)` |
| `mod(x, m)` | 取模，等价于 `x % m` |
| `pi` / `τ` / `tau` | 圆周率 π ≈ 3.14159 / 2π ≈ 6.28318 |
| `e` | 自然常数 e ≈ 2.71828 |
| `phi` / `φ` | 黄金比例 φ ≈ 1.61803 |

### 默认象限行为

省略 `xRange` / `yRange` 与 `quadrants` 时，面板初始仅显示**第一象限**（`x≥0`、
`y≥0`）。若采样过程发现曲线存在 `y<0` 的点且未显式设置 y 轴上下界，面板会自动扩展
到第三、第四象限以保证曲线完整可见；x 轴方向同理。

<FunctionGraph>
  <Function expr="x" color="#4488ff" label="y = x"/>
  <Function expr="-x + 4" color="#ff6644" label="y = −x + 4"/>
</FunctionGraph>
