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

## Footnotes

Footnote ref[^one]

[^one]: tooltip text for the footnote

## GameScene Runtime Parsing

Basic block placement:

<GameScene zoom={4} interactive={true}>
    <Block id="minecraft:water" />
    <Block id="minecraft:water" x="-1" />
    <Block id="minecraft:water" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:glass" z="2" />
    <Block id="minecraft:glass" x="1" z="2" />
</GameScene>

Redstone sample:

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

Block annotation template:

<GameScene zoom={2} interactive={true}>
  <Block id="minecraft:log" />
  <Block id="minecraft:log" x="1" />
  <Block id="minecraft:log" z="1" />
  <Block id="minecraft:log" x="1" z="1" />

  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      This text should appear on hover. <ItemImage id="minecraft:stone" />
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>

Nested markdown inside annotation tooltip:

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
