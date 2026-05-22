---
navigation:
  title: Markdown Basics
  parent: index.md
  position: 190
  recommend: 0
categories:
  - markdown
---

# Markdown Test

This page is a runtime sample sheet for checking what GuideNH currently renders in-game.

See also: [Charts](./charts.md) · [Function Graphs](./function-graph.md)

## LaTeX Formulas

Render LaTeX math inline with `<Latex formula="..." />`, or as a centered display block by using the same tag
in block context (on its own paragraph line).

### Inline formulas

Einstein's mass-energy equivalence: <Latex formula="E=mc^2" /> and Pythagoras: <Latex formula="a^2+b^2=c^2" />

A formula with a fraction expands the line height automatically:
contains <Latex formula="\frac{1}{2}" /> and also <Latex formula="\frac{a+b}{c-d}" /> in the same line.

Custom colour (gold): <Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

Scaled up (scale=1.5): <Latex formula="\pi" scale="1.5" />

Tooltip on hover (showTooltip): <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}" showTooltip={true} />

Plain custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

Rich tooltip:
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **Gibbs free energy** combines enthalpy and entropy.

  - <Latex formula="\Delta H" /> is the enthalpy change.
  - <Latex formula="T\Delta S" /> is the entropy term.
  - Hovered tooltips can use **regular Markdown** and inline guide tags.
</Latex>

Bottom-aligned (valign=bottom): <Latex formula="\frac{a}{b}" valign="bottom" /> sits below a regular line.

Top-aligned (valign=top): <Latex formula="x^2" valign="top" /> is flush with the line top.

Manual offset: <Latex formula="E=mc^2" offsetX="2" offsetY="-1" /> nudged right 2 px and up 1 px.

### `$$formula$$` shorthand

Use `$$formula$$` directly in text for quick formulas with default parameters.

Inline shorthands: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{1}{2}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$

### Block (display) formulas

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />

<Latex formula="\oint_C \mathbf{E} \cdot d\mathbf{l} = -\frac{d}{dt}\iint_S \mathbf{B} \cdot d\mathbf{S}" showTooltip={true} />

### Attribute reference

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `formula` | String (required) | — | LaTeX source |
| `color` | `#RRGGBB` or `#AARRGGBB` | `#FFFFFF` | Glyph colour |
| `scale` | float | `1.0` | Display size multiplier |
| `sourceScale` | float | `100.0` | jlatexmath render quality |
| `tooltip` | String | — | Plain custom tooltip text |
| `showTooltip` | boolean | `false` | Show formula source on hover when no custom tooltip is provided |
| `valign` | `baseline`/`top`/`center`/`bottom` | `baseline` | Inline vertical alignment |
| `offsetX` | int | `0` | Horizontal pixel offset after alignment |
| `offsetY` | int | `0` | Vertical pixel offset after alignment |

## Inline Formatting

| Markdown                            | Alternative       | Result                            |
|-------------------------------------|-------------------|-----------------------------------|
| `*Italic*`                          | `_Italic_`        | *Italic*                          |
| `**Bold**`                          | `__Bold__`        | **Bold**                          |
| `~~Strikethrough~~`                 | `~Strikethrough~` | ~~Strikethrough~~                 |
| `++Underline++`                     |                   | ++Underline++                     |
| `^^Wavy underline^^`                |                   | ^^Wavy underline^^                |
| `::Emphasis dots::`                 |                   | ::Emphasis dots::                 |
| `==Highlight==`                     | `<mark>Highlight</mark>` | ==Highlight==               |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `[Other Namespace](gregtech:guide.md)` |                | [Other Namespace](gregtech:guide.md) |
| `[Anchor Link](#Headings)`          |                   | [Anchor Link](#Headings)          |
| `[Cross-Page Anchor](./index.md#navigation)` |      | [Cross-Page Anchor](./index.md#navigation)      |
| `[Absolute Anchor](guidenh:index.md#navigation)` |   | [Absolute Anchor](guidenh:index.md#navigation)  |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

Literal autolinks: visit https://example.com/docs, www.example.org, or guide@example.com

Custom mark color: <mark color="#8A6A00">dark golden highlight</mark>

Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./index.md" title="Open index">Back to index</a><br clear="all" />

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

[doc]: ./index.md#top
[img]: test1.png "Machine Diagram"

## Details

`<details>` accepts `width`, `height`, `wrap`, and `align`. The summary supports inline tags, and
the body can mix ordinary text with arbitrary runtime blocks.

<details open width="220" height="150" wrap="square" align="right">
<summary>Mixed runtime content <ItemImage id="minecraft:diamond" /></summary>

This details body keeps normal [page links](./index.md), inline formatting, and block content
together inside the same scrollable panel.

![Machine Diagram](test1.png)

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />

<GameScene width="120" height="90" zoom={5} interactive={false}>
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:glass" x="1" />
</GameScene>
</details>

Text outside the block should still wrap around it when `wrap="square"` is used.

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

## File Trees

Fenced `tree` / `filetree` blocks render directory-style outlines with real connector lines. Both Unicode box-drawing (`│ ├ └ ─`) and ASCII (`| +-- \-- ` / four spaces) prefixes are accepted, mixed freely. Payload text supports the usual inline Markdown (links, **bold**, `code`, etc.), and those links should be clickable both in-game and in site export.

```tree
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
```

ASCII variant with optional per-row icons (`{:icon=…}` for plain text, `{:iconPng=path.png}` for an image, `{:iconItem=mod:item[:meta][:{snbt}]}` for an item stack):

```filetree
world
|-- {:iconItem=minecraft:grass} grass biome
|   |-- {:icon=Oak} oak forest
|   \-- {:icon=Hill} rolling hills
|-- {:iconItem=minecraft:wool:14} red wool patch
\-- {:iconPng=test1.png} sample asset
```

Inside an MDX block tag, the same syntax works through `<FileTree>` with optional `indent` (px per depth, default `14`) and `gap` (extra px between rows, default `0`):

```html
<FileTree indent="16" gap="2">
docs
├── [intro.md](./index.md#headings)
└── advanced
    ├── [tags.md](./index.md#details)
    └── [recipes.md](./charts.md)
</FileTree>
```

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

Mindmap with rich labels and explicit node content:

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    preview["Scene preview"]

<NodeContent id="runtime">
Runtime nodes can mix normal text with embedded guide blocks.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="preview">
<BlockImage id="minecraft:diamond_block" scale={2} />
</NodeContent>
</Mermaid>

## Footnotes

Footnote ref[^one]

[^one]: tooltip text for the footnote
