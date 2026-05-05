---
navigation:
  title: Markdown Test
  position: 10
---

# Markdown Test

This page is a runtime sample sheet for checking what GuideNH currently renders in-game.

See also: [Charts](./charts.md) · [Function Graphs](./function-graph.md)

## Inline Formatting

| Markdown                            | Alternative       | Result                            |
|-------------------------------------|-------------------|-----------------------------------|
| `*Italic*`                          | `_Italic_`        | *Italic*                          |
| `**Bold**`                          | `__Bold__`        | **Bold**                          |
| `~~Strikethrough~~`                 | `~Strikethrough~` | ~~Strikethrough~~                 |
| `++Underline++`                     |                   | ++Underline++                     |
| `^^Wavy underline^^`                |                   | ^^Wavy underline^^                |
| `::Emphasis dots::`                 |                   | ::Emphasis dots::                 |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

Literal autolinks: visit https://example.com/docs, www.example.org, or guide@example.com

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

## File Trees

Fenced `tree` / `filetree` blocks render directory-style outlines with real connector lines. Both Unicode box-drawing (`│ ├ └ ─`) and ASCII (`| +-- \-- ` / four spaces) prefixes are accepted, mixed freely. Payload text supports the usual inline Markdown (links, **bold**, `code`, etc.).

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
├── intro.md
└── advanced
    ├── tags.md
    └── recipes.md
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

## Footnotes

Footnote ref[^one]

[^one]: tooltip text for the footnote
