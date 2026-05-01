---
navigation:
  title: Markdown Test
---

# Markdown Test

This page is a runtime sample sheet for checking what GuideNH currently renders in-game.

## Inline Formatting

| Markdown                            | Alternative       | Result                            |
|-------------------------------------|-------------------|-----------------------------------|
| `*Italic*`                          | `_Italic_`        | *Italic*                          |
| `**Bold**`                          | `__Bold__`        | **Bold**                          |
| `~~Strikethrough~~`                 | `~Strikethrough~` | ~~Strikethrough~~                 |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

Literal autolinks: visit https://example.com/docs, www.example.org, or guide@example.com

Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

## Headings

Headings can be defined by prefixing them with `#`.

# Heading 1

## Heading 2

### Heading 3

#### Heading 4

##### Heading 5

###### Heading 6

## Blockquote And Alerts

Markdown:

```
> Blockquote
```

Result:

> Blockquote

> [!NOTE]
> GitHub-style note alert with the blue accent line and icon.

> [!TIP]
> Tip alert with a green accent line and icon.

> [!IMPORTANT]
> Important alert with the purple accent line and icon.

> [!WARNING]
> Warning alert with the gold accent line and icon.

> [!CAUTION]
> Caution alert with the red accent line and icon.

Custom runtime quote directives:

```markdown
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Custom title, accent color and text icon.
```

> {: title="Custom Quote" color="#638ef1" icon="i" }
> Custom title, accent color and text icon.

```markdown
> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> ItemStack icon in the quote header.
```

> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> ItemStack icon in the quote header.

```markdown
> {: title="PNG Quote" color="#c79d3e" iconPng="./diamond.png" }
> PNG icon loaded from guide assets.
```

> {: title="PNG Quote" color="#c79d3e" iconPng="./diamond.png" }
> PNG icon loaded from guide assets.

## Lists

Markdown:

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

Result:

* List
* List
* List

1. One
2. Two
3. Three

- [x] Task done
- [ ] Task pending

<Column width="220">
- constrained list line width example
- another constrained list item that should wrap earlier
</Column>

## Tables

Markdown:

```markdown
| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |
```

Result:

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |

Alignment check:

| Left | Center | Right |
| :--- | :----: | ----: |
| iron |   42   |   128 |
| gold |   17   |    64 |

Ordinary markdown tables can also use runtime width hints:

| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }

Another width-hint sample with three columns:

| Material | Count | Notes |
| --- | --- | --- |
| Iron | 42 | base line |
| Gold | 17 | compact |
| Diamond | 9 | rare |
{: widths="130,70,150" }

## Reference Links And Images

[Guide Ref][doc]

![Machine Diagram][img]

[doc]: ./subpage.md#top
[img]: test1.png "Machine Diagram"

## Details

<details open>
<summary>More</summary>

Body text inside runtime details.
</details>

## Code Blocks

Explicit language:

```lua
local value = 42
print(value)
```

Explicit Scala:

```scala
object Demo extends App {
  println("scala language label")
}
```

Explicit Markdown:

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

This unlabeled fence should stay a code block and auto-detect Scala:

```
object Demo extends App {
  println("auto detected scala")
}
```

This unlabeled fence should stay a code block and auto-detect CSV instead of rendering a table:

```
name,value
iron,42
gold,17
```

Indented code block:

    print("indented code block")

Forced code block viewport height with inner scrolling:

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

## CSV Runtime Tables

This explicit `csv` fence renders as a runtime table:

```csv
name,value
iron,42
gold,17
```

This explicit `csv` fence also applies column width hints:

```csv widths=120,80
name,value
iron,42
gold,17
```

Quoted widths and `header=false`:

```csv widths="120,80" header=false
iron,42
gold,17
diamond,9
```

Imported CSV:

<CsvTable src="./markdown-table.csv" />

Imported CSV with widths:

<CsvTable src="./markdown-table.csv" widths="120,80" />

## Mermaid Mindmaps

Inline Mermaid fence:

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

Mindmap with explicit node coordinates:

```mermaid
mindmap
  Root((Pinned Root))
    Branch[Branch]::pos(120,80)
      Child A
      Child B::icon(fa fa-code)
```

Imported Mermaid file:

<Mermaid src="./markdown-mindmap.mmd" />

Fixed runtime Mermaid viewport size:

<Mermaid src="./markdown-mindmap.mmd" width="320" height="220" />

## Footnotes

Footnote ref[^one]

[^one]: tooltip text for the footnote

## Charts

GuideNH ships with five interactive chart tags: `<ColumnChart>` clustered columns, `<BarChart>` horizontal bars, `<LineChart>` line, `<PieChart>` pie, and `<ScatterChart>` XY scatter. All charts show value + percentage tooltips on hover, and highlight the hovered element (column/bar grows, line points pop along the normal, pie slices pop outward, scatter points enlarge).

### Common attributes

* `title`
* `width` / `height` (defaults: 320 x 200)
* `background` / `border` colors
* `titleColor` / `labelColor`
* `legend` position: `none` / `top` / `bottom` / `left` / `right` (default `top`)
* `labelPosition`: `none` / `inside` / `outside` / `above` / `below` / `center`

Color formats: `#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`.

### ColumnChart

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>
```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>

Extra attributes: `categories` (X-axis labels, comma separated), `barWidthRatio` (default 0.7), `xAxisLabel` / `yAxisLabel` / `yAxisMin` / `yAxisMax` / `yAxisStep` / `yAxisUnit` / `yAxisTickFormat`, `showXGrid={true}` / `showYGrid={true}`.

### BarChart

```mdx
<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>
```

<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>

Same attributes as ColumnChart but categories are on the Y-axis.

### LineChart

Categorical X:

```mdx
<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>
```

<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>

Numeric X:

```mdx
<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>
```

<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>

Extra: `numericX={true}` enables a numeric X axis; `showPoints={false}` hides point markers.

### PieChart

```mdx
<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice label="Iron" value="45" color="#4E79A7"/>
  <Slice label="Copper" value="25" color="#F28E2B"/>
  <Slice label="Gold" value="15" color="#E15759"/>
  <Slice label="Diamond" value="10"/>
  <Slice label="Other" value="5"/>
</PieChart>
```

<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice label="Iron" value="45" color="#4E79A7"/>
  <Slice label="Copper" value="25" color="#F28E2B"/>
  <Slice label="Gold" value="15" color="#E15759"/>
  <Slice label="Diamond" value="10"/>
  <Slice label="Other" value="5"/>
</PieChart>

Extra: `startAngle` (default -90, i.e. 12 o'clock); `clockwise={false}` to reverse direction.

### ScatterChart

```mdx
<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>
```

<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>

### Combo: ColumnChart + LineSeries + PieInset

`<ColumnChart>` and `<BarChart>` accept extra `<LineSeries>` (line overlay sharing the value axis) and `<PieInset>` (small corner pie) children, letting one chart combine several styles.

The pie inset's `position` attribute accepts `topRight` / `topLeft` / `bottomRight` / `bottomLeft` (corner overlay) or `right` (the chart auto-extends its width and the pie occupies a dedicated outside column).

Line overlays share hover/tooltip behavior with the underlying columns/bars: hovering a line point thickens its adjacent segments, enlarges the point, and shows a tooltip with the series name and value.

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice label="Iron" value="225" color="#a0a0a0"/>
    <Slice label="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice label="Iron" value="225" color="#a0a0a0"/>
    <Slice label="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
