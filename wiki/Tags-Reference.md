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
| `<Color>` | colored inline text | `id` or `color` |
| `<Tooltip>` | rich hover tooltip with markdown/tag children | `label` |
| `<PlayerName>` | inserts current player username | none |
| `<KeyBind>` | inserts keybinding display name | `id` |
| `<ItemImage>` | inline item icon | `id` or `ore`, `scale`, `noTooltip`, `yOffset` |
| `<ItemLink>` | item tooltip + optional navigation link | `id` or `ore` |
| `<CommandLink>` | clickable chat command link | `command`, `title`, `close` |

## Block Tags

| Tag | Purpose | Key attributes |
| --- | --- | --- |
| `<div>` | pass-through block wrapper | none |
| `<Row>` | horizontal flex layout | `gap`, `alignItems`, `fullWidth` |
| `<Column>` | vertical flex layout | `gap`, `alignItems`, `fullWidth` |
| `<ItemGrid>` | compact grid of item icons | children must be `<ItemIcon id="..."/>` or `<ItemIcon ore="..."/>` |
| `<BlockImage>` | block item-form icon | `id` or `ore`, `scale` |
| `<FloatingImage>` | floated image block | `src`, `align`, `title`, `width`, `height` |
| `<SubPages>` | navigation child listing | `id`, `alphabetical` |
| `<CategoryIndex>` | list pages from a category | `category` |
| `<Structure>` | 2.5D isometric block layout view | `width`, `height` |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | see [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D guide scene | see [GameScene](GameScene) |

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

### `<PlayerName>`

Inserts the current Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up a keybinding by id and renders the player's current bound key name.

Accepted ids:

- the binding description id, such as `key.jump` or `key.guidenh.open_guide`
- the legacy `category.description` form, such as `key.categories.movement.key.jump`

Example:

````md
Press <KeyBind id="key.jump" /> to jump.
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

### `<ItemImage>`

Shows an inline item icon.

| Attribute | Meaning |
| --- | --- |
| `ore` | ore dictionary name; the first match wins |
| `id` | item reference used when `ore` is absent |
| `scale` | float, default `1` |
| `noTooltip` | truthy string or empty attribute suppresses tooltip |
| `yOffset` | integer pixel offset override at scale `1` |

Notes:

- `ore` takes precedence over `id` when both are provided
- if GregTech is installed, the selected ore match is passed through `GTOreDictUnificator.setStack(...)`

Example:

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
````

### `<ItemLink>`

Creates a text link using the item's display name and item tooltip. If `item_ids` points to a guide page, clicking navigates to it. `ore` can be used to resolve the display stack from the first ore dictionary match instead of a fixed registry id.

````md
<ItemLink id="minecraft:compass" />
<ItemLink ore="stickWood" />
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

Example:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
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

Renders the item form of a block. `ore` must resolve to a block item stack.

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" />
````

### `<FloatingImage>`

See [Images And Assets](Images-And-Assets) for the full behavior.

### `<SubPages>` And `<CategoryIndex>`

See [Navigation](Navigation) for full navigation behavior.

### `<Structure>`

See [Examples](Examples) and [GameScene](GameScene) when deciding whether to use a static structure preview or a full 3D scene.

### Scene Runtime Tags

These tags only work inside `<GameScene>` / `<Scene>`:

| Tag | Purpose | Key attributes |
| --- | --- | --- |
| `<ImportStructure>` | import an external SNBT/NBT structure asset | `src`, `x`, `y`, `z` |
| `<ImportStructureLib>` | import a StructureLib multiblock by controller id | `controller`, `piece`, `facing`, `rotation`, `flip`, `channel` |
| `<RemoveBlocks>` | remove already-placed blocks that match a block matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp the same child annotations onto every matching placed block | `id` |

See [GameScene](GameScene) for scene import/removal behavior and [Annotations](Annotations) for annotation template rules.
