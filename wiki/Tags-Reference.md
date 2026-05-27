# Tags Reference

This page lists the built-in runtime tags registered by `DefaultExtensions`.

## Usage Rules

- Tags can appear either in block context or inline context depending on the compiler.
- MDX comments using `{/* ... */}` are supported in page content and are ignored by the runtime parser.
- Invalid tags or invalid attributes render guide errors inline instead of silently failing.
- Large feature tags such as recipes and 3D scenes are documented in their own pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Inline And Flow Tags

| Tag | Purpose | Key attributes |
| --- | --- | --- |
| `<a>` | internal/external link and optional anchor name | `href`, `title`, `name` |
| `<br>` | line break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style text | none |
| `<sup>` | smaller inline superscript-style text | none |
| `<Color>` | colored inline text | `id` or `color` |
| `<Tooltip>` | rich hover tooltip with markdown/tag children | `label` |
| `<SoundLink>` | clickable rich-text sound trigger | `sound` or `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted text; equivalent to `==text==` with optional color control | `color` |
| `<PlayerName>` | inserts current player username | none |
| `<KeyBind>` | inserts keybinding display name | `id` or `action` |
| `<ItemImage>` | inline item icon | `id` or `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | item tooltip + optional navigation link | `id` or `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command link | `command`, `title`, `close` |
| `<Latex>` | LaTeX math formula; inline in flow context, centered display block in block context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest link with state-aware styling (compat tag, only registered when BetterQuesting is loaded) | `id`, `text`, `show_tooltip` |

Inline markdown also supports action links for sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## Block Tags

| Tag | Purpose | Key attributes |
| --- | --- | --- |
| `<div>` | pass-through block wrapper | none |
| `<details>` | collapsible runtime block | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | directory-style outline with connector lines | `indent`, `gap` |
| `<Row>` | horizontal flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | width-constrained footnote container used by runtime markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of item icons | children must be `<ItemIcon id="..."/>` or `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interactive 3D single-block preview | `id` or `ore`, `scale`, `float`, `perspective`, `nbt` |
| `<FloatingImage>` | floated image block | `src`, `align`, `title`, `width`, `height` |
| `<SubPages>` | navigation child listing | `id`, `alphabetical` |
| `<Category>` | list pages from a category | `name`, `rows` |
| `<Special>` | list built-in MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric block layout view | `width`, `height` |
| `<Mermaid>` | runtime Mermaid graph import/inline | `src`, `width`, `height` |
| `<CsvTable>` | runtime CSV file import table | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered column chart | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar chart | same as `<ColumnChart>` |
| `<LineChart>` | line chart with categorical or numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie chart | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter chart | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curve function graph | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | single-curve shorthand for `<FunctionGraph>` | `expr`, plus all `<FunctionGraph>` panel attributes |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | see [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D guide scene | see [GameScene](GameScene) |
| `<QuestCard>` | block-level BetterQuesting quest summary card (compat tag, only registered when BetterQuesting is loaded) | `id`, `show_desc`, `show_tooltip` |

## Tag Details

### `<a>`

Acts like an HTML-style anchor tag:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` can be relative, rooted, explicit `modid:path`, or HTTP/HTTPS
- `title` becomes the tooltip
- `name` inserts a page anchor target

### `<br>`

GuideNH also supports an MDX break tag with float clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` values:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, And `<sup>`

GuideNH runtime supports a focused subset of lowercase documentation tags for inline use:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

Creates a collapsible runtime block with a summary row. The `<summary>` line supports normal
inline markdown/tag content, and the body can hold ordinary text plus arbitrary block tags such as
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tables, charts, and layout containers.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Hidden-by-default body text with a [normal page link](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

Attributes:

- `open` — starts expanded when present
- `width` — preferred outer width in pixels
- `height` — preferred body viewport height in pixels; overflow becomes scrollable in-game and in site export
- `wrap` — supports the usual block embedding modes such as `square`, `tight`, and `through`
- `align` — `left`, `center`, or `right`; when combined with a floating wrap mode, the whole details block floats

### `<FileTree>`

Renders a directory-style outline with real connector lines drawn from the prefix glyphs on each row. Both Unicode box-drawing (`│ ├ └ ─`) and ASCII (`| +-- \-- ` / four spaces) forms are accepted and may be mixed. Payload text supports the usual inline markdown (links, **bold**, `code`, …), and those links are clickable both in-game and in the built-in site export. The same content can also be written as a fenced ` ```tree ` or ` ```filetree ` block.

````md
<FileTree indent="14" gap="0">
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
</FileTree>
````

Optional per-row icons are introduced by a leading directive on the payload:

- `{:icon=Text}` — short text label (single or double quotes optional)
- `{:iconPng=path/to/file.png}` — PNG asset resolved against the current page
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft item icon. The optional `meta` segment is a damage value (or `*` for a wildcard); an optional trailing `:{snbt}` block carries SNBT to attach to the stack.

````md
```filetree
world
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample asset
```
````

Attributes:

- `indent` — pixels per depth level (default `14`)
- `gap` — extra pixels between rows (default `0`)

### Runtime Blockquotes

Normal markdown blockquotes render at runtime with a left accent line. GitHub alert syntax is supported:

````md
> [!NOTE]
> Alert body
````

GuideNH also supports a runtime-only custom directive on the first quoted line:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Supported directive keys:

- `title`
- `color`
- `icon` for plain text symbols
- `iconItem` for an `ItemStack` id
- `iconPng` for a guide asset png path

Only one icon source should be provided.

### `<Color>`

Use either a symbolic color id or an explicit hex value:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>
````

Rules:

- `id` and `color` are mutually exclusive in practice; provide one
- `color` accepts `#RRGGBB`, `#AARRGGBB`, or `transparent`

### `<Tooltip>`

Creates underlined text that opens a rich content tooltip on hover.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

If `label` is omitted, the trigger text defaults to `tooltip`.

### `<SoundLink>` And Sound Action Links

`<SoundLink>` renders rich inline content that plays a sound when clicked. It does not navigate,
and its custom click sound replaces the normal guide click sound for that click.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound attributes:

- `sound` is a sound event id such as `modid:event.name`
- `src` points at an `.ogg` file; `modid:sounds/machine/start.ogg` becomes `modid:machine.start`
- `volume` defaults to `1.0`
- `pitch` defaults to `1.0`
- `cooldown` is milliseconds between repeated plays, default `250`
- `radius` and `minVolume` control screen-space attenuation when used in scenes

### `<PlayerName>`

Inserts the current Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up a keybinding by id or action and renders the player's current bound key name.

Accepted ids:

- the binding description id, such as `key.jump` or `key.guidenh.open_guide`
- the legacy `category.description` form, such as `key.categories.movement.key.jump`

Example:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments in page content:

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH also ignores explicit `<Comment>` tags:

````md
Visible text. <Comment>This does not render.</Comment> Still visible.
````

### `<ItemImage>`

Shows an inline item icon.

| Attribute | Meaning |
| --- | --- |
| `ore` | ore dictionary name; the first match wins |
| `id` | item reference used when `ore` is absent |
| `scale` | float, default `1` |
| `noTooltip` | truthy string or empty attribute suppresses tooltip (legacy; prefer `showTooltip`) |
| `showTooltip` | boolean, default `true`; `false` suppresses the hover tooltip |
| `showIcon` | boolean, default `true`; `false` hides the item icon graphic |
| `label` | `left` or `right` — shows the item display name as text on the specified side of the icon; omit for no label |
| `format` | format pattern for the label text; supports Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) with optional `%s` placeholder for the item name; default (no attribute) renders the name in italic |
| `yOffset` | integer pixel offset override for the **icon** at scale `1`; does not affect the label text |
| `labelYOffset` | integer pixel offset override for the **label text** at scale `1`; does not affect the icon |

Notes:

- `ore` takes precedence over `id` when both are provided
- if GregTech is installed, the selected ore match is passed through `GTOreDictUnificator.setStack(...)`
- `label` requires at least one of `showIcon` or `label` to produce visible output; setting both `showIcon="false"` and omitting `label` renders nothing
- `format` only applies when `label` is set; if `format` has no `%s`, the literal format text is used as the label

Example:

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
````

### `<ItemLink>`

Creates a text link using the item's display name and item tooltip. If `item_ids` points to a guide page, clicking navigates to it. `ore` can be used to resolve the display stack from the first ore dictionary match instead of a fixed registry id.

| Attribute | Default | Meaning |
| --- | --- | --- |
| `id` | — | item registry id, e.g. `minecraft:compass` or `minecraft:wool:1` |
| `ore` | — | ore-dictionary name; uses the first matching item stack |
| `linksTo` | *(auto)* | overrides the link target; accepts a page id with optional `#anchor`, e.g. `./crafting.md#usage` or `#usage`; when omitted the target is resolved from `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | set to `false` to suppress the hover tooltip; `noTooltip` is a legacy alias |
| `showIcon` | *(none)* | `left` or `right` (or any truthy value → right) — renders the item icon beside the link text; omit to show text only |

Examples:

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
````

### `<CommandLink>`

Sends a chat command when clicked.

| Attribute | Meaning |
| --- | --- |
| `command` | required, must start with `/` |
| `title` | optional tooltip heading |
| `close` | parsed boolean attribute; currently parsed but not used to close the guide |

Example:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` And `<Column>`

Flex-style containers for block content.

| Attribute | Meaning |
| --- | --- |
| `gap` | integer gap between children, default `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | boolean expression, default `false` |
| `width` | integer preferred width; useful for constraining list line width |

Example:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

To constrain the width of normal markdown lists, wrap them in a container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH uses this block tag internally when runtime markdown footnotes are expanded. It can also be written manually if needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders a compact item grid. Children must be raw `<ItemIcon>` elements, which are parsed directly by the grid compiler. Each child can use either `id` or `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders a non-interactive 3D single-block scene. The preview has no scene background, no scene
buttons, no layer controls, and no annotation features, but hovering the block still shows the
selection outline and tooltip. `ore` must resolve to a block item stack.

| Attribute | Meaning |
| --- | --- |
| `id` | block id; supports the normal `modid:block[:meta][:{snbt}]` spelling |
| `ore` | ore dictionary lookup; the first matching block item wins |
| `scale` | camera zoom multiplier, default `1` |
| `float` | legacy flow float support: `left` or `right` |
| `perspective` | `isometric-north-east` (default), `isometric-north-west`, or `up` |
| `nbt` | optional SNBT tile-entity data merged onto any inline SNBT from `id` |

Notes:

- inline SNBT inside `id` is still accepted for compatibility, but `nbt="..."` is the preferred
  authoring form
- when both inline SNBT and `nbt` are present, the `nbt` attribute is merged last and therefore
  overrides conflicting keys
- GuideNH 1.7.10 does not support modern block-state property syntax here, so GuideME-style
  `p:<state>` attributes are intentionally not supported

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" perspective="isometric-north-west" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<FloatingImage>`

See [Images And Assets](Images-And-Assets) for the full behavior.

### `<SubPages>`, `<Category>`, And `<Special>`

See [Navigation](Navigation) for full navigation behavior.

### `<Structure>`

See [Examples](Examples) and [GameScene](GameScene) when deciding whether to use a static structure preview or a full 3D scene.

### `<Mermaid>`

Used for runtime Mermaid content. Current runtime support is focused on `mindmap`, either inline or through a page-relative `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Runtime nodes can embed normal blocks.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` and `height` constrain the runtime viewport box
- inside the viewport, drag pans and the mouse wheel zooms
- quoted Mermaid labels may use rich inline markdown such as `**bold**` and page links
- `<NodeContent id="...">...</NodeContent>` can be added as children of `<Mermaid>` to replace a node body with arbitrary runtime blocks

### `<CsvTable>`

Used to parse a CSV file into a runtime table:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relative to the current page, the same way scene imports and normal asset links do.

Optional attributes:

- `header`
  Defaults to `true`; set `header={false}` to keep the first row unbolded
- `widths`
  Comma-separated integer width hints such as `widths="120,80"`

Examples:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

The related fenced runtime CSV form also supports matching metadata:

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

### `<Latex>`

Renders a LaTeX math formula using jlatexmath. When used inline (inside a paragraph or text flow), it renders as a scaled glyph that expands the line height to fit the formula. When written as its own paragraph (block context), it renders centered as a display-mode formula.

| Attribute | Type | Default | Description |
| --- | --- | --- | --- |
| `formula` | string | *(required)* | LaTeX source string |
| `color` | `#RRGGBB` or `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | float | `1.0` | Display size multiplier applied on top of the automatic line-height scaling |
| `sourceScale` | float | `100.0` | jlatexmath internal render resolution; higher values improve quality at large sizes |
| `tooltip` | string | *(none)* | Plain tooltip text shown on hover |
| `showTooltip` | boolean | `false` | Show the raw LaTeX source as a tooltip on hover |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-only. Vertical alignment within the text line: `baseline` (default) aligns the formula's math baseline with the text baseline; `top` aligns the formula top with the line top; `center` centers it on the text; `bottom` aligns the formula bottom with the text bottom |
| `offsetX` | int | `0` | Horizontal pixel offset applied after alignment (positive = right) |
| `offsetY` | int | `0` | Vertical pixel offset applied after alignment (positive = down) |

Examples:

````md
Inline: <Latex formula="E=mc^2" />

Fraction that expands line height: <Latex formula="\frac{a+b}{c-d}" />

Gold colour: <Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

Scaled up: <Latex formula="\pi" scale="1.5" />

With hover tooltip: <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

Plain custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

Rich tooltip:
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **Gibbs free energy**

  - <Latex formula="\Delta H" />: enthalpy change
  - <Latex formula="T\Delta S" />: entropy term
</Latex>

Bottom-aligned (formula bottom matches text bottom): <Latex formula="\frac{a}{b}" valign="bottom" />

Explicit baseline alignment (same as default): <Latex formula="E=mc^2" valign="baseline" />

Top-aligned with an upward nudge: <Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$formula$$` shorthand

As a convenience you can write `$$formula$$` directly in Markdown text without using the `<Latex>` tag.
All rendering parameters use their defaults (white colour, scale 1.0, no tooltip, baseline-aligned).

- **Inline**: `$$formula$$` embedded inside a paragraph renders as an inline formula.
- **Display**: a paragraph whose entire content is `$$formula$$` (with optional surrounding whitespace) renders as a centred display-mode block.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Notes:

- The formula height is calibrated to the current line text height. Simple formulas render at text height; taller formulas (fractions, summations, integrals, etc.) expand the enclosing line height automatically.
- `valign` only applies to inline formulas. Display-mode (block-level) formulas are always centered horizontally; use `offsetY` to shift them vertically within the block.
- `color` defaults to white (`#FFFFFF`). Use `#AARRGGBB` format for a semi-transparent fill.
- `sourceScale` only affects render sharpness, not the displayed size. Values below `16` are clamped to `16`.
- Tooltip priority is: rich child Markdown content, then `tooltip="..."`, then `showTooltip={true}` raw source fallback.
- Child tooltip content is compiled as regular guide Markdown, so it can include bold text, lists, links, item tags, and nested `<Latex>` formulas.
- The `$$formula$$` shorthand always uses default parameters. Use the `<Latex>` tag for custom colour, scale, alignment or tooltip.

### Scene Runtime Tags

These tags only work inside `<GameScene>` / `<Scene>`:

| Tag | Purpose | Key attributes |
| --- | --- | --- |
| `<ImportStructure>` | import an external SNBT/NBT structure asset | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import a StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | remove already-placed blocks that match a block matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp the same child annotations onto every matching placed block | `id` |

See [GameScene](GameScene) for scene import/removal behavior and [Annotations](Annotations) for annotation template rules.


## Charts

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, and `<ScatterChart>` are interactive chart blocks. All charts share the following common attributes:

| Attribute | Description | Default |
| --- | --- | --- |
| `title` | Chart title | none |
| `width` / `height` | Explicit size | 320 / 200 |
| `background` / `border` | Background and border colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | Title and value-label colors | light grey |
| `legend` | Legend position: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | Value-label position: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | Internal plot legend position: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum internal legend box size | `120` / `64` |
| `cornerLegendBackground` | Internal legend background color | `#AA111922` |

Cartesian charts (Column / Bar / Line / Scatter) additionally accept axis attributes `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` and the matching `yAxis*` set, plus `showXGrid={true}` / `showYGrid={true}` to toggle gridlines.

Children:

* `<Series name="..." color="#..." data="10,20,30"/>` for category-based charts (Column / Bar / categorical Line).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` for numeric X (Line `numericX={true}`, Scatter).
* `<Slice label="..." value="..." color="#..."/>` for `<PieChart>` only.

When `color` is omitted on a `<Series>` or `<Slice>`, GuideNH cycles through a built-in 16-color palette.

`<Series>` and `<Slice>` also accept the following optional icon / tooltip attributes:

* `icon="modid:item"` (same syntax as `<ItemImage>`'s `id`, may include `@meta` and inline NBT JSON) — binds an `ItemStack` to the entry; the legend swatch becomes the item icon and hovering the data point shows the vanilla item tooltip with the chart description appended at the end.
* `iconImage="images/foo.png"` — use a PNG asset as the legend swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form text appended to the tooltip (use `\n` for multi-line).

Example:

```mdx
<PieChart title="Output share">
  <Slice label="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice label="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra attributes: `categories` (X-axis or Y-axis labels, comma separated), `barWidthRatio` (default 0.7). `<BarChart>` puts the categories on the Y-axis and values on the X-axis.

#### Combo extensions

`<ColumnChart>` and `<BarChart>` accept two extra child element types so multiple chart styles can share one plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as a polyline overlay on top of the bars. Each line point sits at the cluster center of the matching category index; the overlay shares the host chart's value axis. You can declare multiple `<LineSeries>` to overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — a small pie chart drawn inside one of the four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of the plot area. Its `<Slice>` children share the same syntax as in `<PieChart>`.

```mdx
<ColumnChart title="Quarterly output" categories="Q1,Q2,Q3,Q4">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice label="Iron" value="225" color="#a0a0a0"/>
    <Slice label="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

Extra attributes: `numericX={true}` to enable a numeric X-axis (children must use `points`); `showPoints={false}` hides point markers. The hovered point is pushed outward by 2px along the curve normal, enlarged, and outlined; the adjacent line segments thicken by 1px.

`<LineChart>` and `<ScatterChart>` can show a compact legend inside the plot area with `cornerLegend="topRight"` or another corner. Entries use existing series names and colors.

### `<PieChart>`

Extra attributes: `startAngle` (default `-90`, i.e. 12 o'clock); `clockwise={false}` to reverse direction. The hovered slice pops outward 4px along its bisector.

### `<ScatterChart>`

Renders points only; `<Series>` must use `points`. The X-axis is always numeric.

## Function Graphs

`<FunctionGraph>` and the single-curve shorthand `<Function>` render an interactive Desmos-style panel. The same panel is also available through a ` ```funcgraph ` fenced code block; see the runtime [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) for a full walkthrough.

Panel attributes (accepted by the container, the shorthand, and the fence header alike):

- `width` / `height` (defaults `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (default `true`)
- `xRange="a..b"` (or `xMin` / `xMax` separately), `xStep` for tick spacing; same for the Y axis
- `quadrants="1,2,3,4"` or `quadrants="all"` to force the visible quadrants; omit to start in quadrant 1 with auto-expansion when sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, and `cornerLegendBackground` show a compact legend inside the plot area using non-empty curve labels

Curve children (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; the expression. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absolute value, `√` / `sqrt` / `∛` / `cbrt`, implicit multiplication, and the constants `pi`, `tau`, `e`, `phi` are supported. Built-in calls cover the standard trig/log/exp/rounding family plus two-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates the expression as `x = f(y)` and rotates the curve.
- `domain="a..b"` (x bounds shorthand) or comma-separated clauses such as `x>=0, x<5`.
- `color`, `label`. Any curve with a non-empty `label` is automatically listed in a legend rendered just below the panel: a small color swatch followed by the label, with entries flowing left-to-right and wrapping onto a new row when the next entry would not fit.
- `pointEveryX="step"` adds generated point markers at regular x intervals on that curve.
- `pointEveryY="step"` adds generated point markers where the curve intersects regular y intervals, using a bounded search.
- `autoPointLabel="none|x|y|xy"` controls generated point labels; default is `none`.
- `autoPointColor="#..."` overrides the generated point color; omitted means inherit the curve color.

Marked points (`<Point>`):

- Explicit: `x="..."` and `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` or `atY="v"` (the runtime bisects on the plot's x-domain to find the matching `x`).
- Optional `color`, `label`.

Interaction: hover a curve to highlight it; press and hold to scrub a point along the curve. The tooltip shows the expression on the first line and `(x, y)` on the second; it stays anchored above the point and flips below when there is no headroom.

## BetterQuesting Compatibility Tags

`<QuestLink>` and `<QuestCard>` are only registered when the BetterQuesting mod is loaded. They are documented in detail on the [Mod Compatibility](Mod-Compatibility) page; the summary below covers the most common usage.

### `<QuestLink>`

Inline link to a BetterQuesting quest. Clicking opens the quest inside the BetterQuesting GUI, unless the quest id is also present in the current guide's `quest_ids` frontmatter — in that case the link navigates to that page instead.

| Attribute | Meaning |
| --- | --- |
| `id` | required BetterQuesting quest id; accepts canonical UUID strings and compact Base64 ids |
| `text` | optional override for the displayed text |
| `show_tooltip` | optional boolean (default `true`); set to `false` to suppress the quest-description tooltip. `showTooltip` is accepted as an alias |

Visibility behavior is decided per player at compile time:

- visible / completed quests render as a clickable link (completed quests are tinted green and append a `✓` mark)
- locked but non-hidden quests still render as clickable quest links so they can open the BetterQuesting quest screen or the indexed guide page
- hidden / secret quests render as a darker italic placeholder using `guidenh.compat.bq.hidden`
- unknown quest ids render as a red placeholder using `guidenh.compat.bq.missing`

Example:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

Block-level summary card for a BetterQuesting quest. Renders the quest title with the same state-aware styling as `<QuestLink>`, plus the quest description as a body paragraph when the quest is visible to the player.

| Attribute | Meaning |
| --- | --- |
| `id` | required BetterQuesting quest id; accepts canonical UUID strings and compact Base64 ids |
| `show_desc` | optional boolean (default `true`); set to `false` to suppress the description body |
| `show_tooltip` | optional boolean (default `true`); set to `false` to suppress the quest-description tooltip on the clickable title. `showTooltip` is accepted as an alias |

The accent color of the card border follows the quest state: green for completed, gray for locked / hidden, red for missing, and the standard link color for visible quests. The title remains clickable for visible, completed, and locked-but-non-hidden quests.

Example:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
