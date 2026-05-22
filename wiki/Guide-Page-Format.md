# Guide Page Format

GuideNH runtime pages are markdown files parsed with:

- standard markdown block and inline syntax
- YAML frontmatter
- GFM tables
- strikethrough
- mark highlight with `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments using `{/* ... */}`
- MDX-style custom tags

## Supported Markdown

GuideNH pages support the common markdown features used in the example guide:

- headings
- paragraphs
- inline emphasis, bold, strike, and code
- inline mark highlight (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), and emphasis dots (`::text::`)
- links and images
- literal autolinks for direct URLs, `www.` hosts, and email addresses
- reference links and reference images
- unordered and ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes such as `[!NOTE]`
- horizontal rules
- fenced code blocks
- indented code blocks
- GFM tables
- footnotes
- lowercase HTML fragments such as `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, and `<details>`
- MDX comments in page text

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` for a live sample page.

## Highlight

Use `==text==` for inline highlighted text. Use `<mark color="#8A6A00">text</mark>` when a custom highlight color is needed. The default mark background is a dark golden yellow chosen to keep white text readable.

## Code Blocks

Runtime code blocks currently support:

- explicit fence languages such as `java`, `lua`, `scala`, `csv`, and `mermaid`
- automatic language inference when the fence language is omitted
- a language label shown above the block
- a top-right copy button in the in-game viewer
- lightweight runtime syntax highlighting for the detected language

Example:

````md
```lua
local value = 42
print(value)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented code blocks are also supported:

````md
    print("indented code")
````

When a fenced block resolves to `mermaid` and the source is a supported `mindmap`, GuideNH renders it as an interactive runtime mindmap instead of a plain code block.

When a fenced block is explicitly marked as `csv`, GuideNH renders it as a runtime table instead of a plain code block. If the fence language is omitted, CSV-shaped text still stays a code block and only uses CSV language detection for labeling/highlighting.

Explicit CSV tables can also provide column width hints:

````md
```csv widths=120,80
name,value
iron,42
gold,17
```
````

Fence metadata also supports `header=false` and quoted width lists:

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

Direct GFM-style literal autolinks are also supported in normal paragraph text:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mermaid Mindmaps

GuideNH runtime Mermaid support is currently focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` blocks
- auto-detected mermaid code fences whose content starts with `mindmap`
- explicit `<Mermaid>...</Mermaid>` tags
- explicit `<Mermaid src="./diagram.mmd" />` imports
- rich inline markdown labels inside Mermaid node text
- optional `<NodeContent id="...">...</NodeContent>` children for arbitrary runtime blocks inside matching nodes
- whole-diagram drag-to-pan interaction in the in-game viewer
- `layout: tidy-tree` frontmatter inside Mermaid source
- common mindmap node shapes such as square, rounded, circle, bang, cloud, and hexagon
- parsed `::icon(...)` and `:::class` metadata

Example:

````md
```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Runtime nodes can mix text, links, and blocks.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams that are not supported at runtime yet still fall back to regular Mermaid-labeled code blocks.

## CSV Table Import

GuideNH also supports runtime CSV file imports through an explicit tag:

````md
<CsvTable src="./markdown-table.csv" />
````

The `src` path resolves relative to the current page, the same way runtime asset links and scene `src` imports do.

Imported CSV tables can also provide width hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You can also write a CSV table inline with an explicit fence:

````md
```csv
name,value
iron,42
gold,17
```
````

## Markdown Table Width Hints

Ordinary GFM markdown tables can also provide runtime column width hints by adding a trailing runtime attribute line immediately after the table:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

This keeps the table itself standard markdown while letting GuideNH apply runtime-only preferred column widths.

## Task Lists, Alerts, And Footnotes

GuideNH runtime also supports several useful GFM-style behaviors:

- task lists using `- [ ]` and `- [x]`
- GitHub alert blockquotes such as `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, and `[!CAUTION]`
- footnote references and definitions

Example:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references render as tooltip-style inline markers, and GuideNH appends a compact runtime footnote list near the bottom of the page.

## List Width Customization

Standard markdown lists do not define width controls, but GuideNH runtime containers can constrain them:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

This is currently the recommended way to customize list line width at runtime.

## Reference Links And Images

GuideNH supports CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML Runtime Tags

GuideNH runtime supports a focused subset of lowercase HTML-style tags directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

Other raw HTML fragments still fall back to literal text-style handling instead of browser-grade HTML rendering.

## MDX Comments

GuideNH supports the MDX comment form and ignores it before markdown compilation:

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH reads the first YAML frontmatter block and parses these known keys:

| Key | Type | Meaning |
| --- | --- | --- |
| `navigation` | map | Adds the page to the navigation tree |
| `categories` | list of strings | Adds the page to the category index |
| `item_ids` | list of item references | Makes the page discoverable by `<ItemLink>` |
| `ore_ids` | list of ore dictionary names | Makes the page discoverable by ore-dictionary items (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | list of BetterQuesting quest ids | Makes the page discoverable by `<QuestLink>` / `<QuestCard>` and by the open-guide hotkey when a quest is hovered in the BQ GUI. Accepts canonical UUID strings and BetterQuesting's compact Base64 form. Only consumed when BetterQuesting is loaded. See [Mod Compatibility](Mod-Compatibility) |
| `author` | string | Single author name. Displayed in the bottom bar. |
| `authors` | list of strings or `{name: ...}` maps | Multiple author names. At most two are displayed; additional ones are replaced with `...`. Takes precedence over `author` if both are present. |
| `date` | string or YYYY-MM-DD date | Content creation date. Displayed in the bottom bar. |
| `updated` | string or YYYY-MM-DD date | Last updated date. Displayed in the bottom bar. |
| `zoom` | positive float | Per-page content zoom multiplier (e.g. `1.5` = 150 %). Multiplied with the global `contentZoom` setting in ModConfig. Default `1.0`. |
| any other key | any YAML value | Preserved in `additionalProperties` for extensions or tooling |

### `navigation`

| Field | Required | Type | Notes |
| --- | --- | --- | --- |
| `title` | yes | string | Display name in navigation and search title fallback |
| `parent` | no | page id | Parent page id; omitted means top-level node |
| `position` | no | integer | Sibling sort order; default `0`, larger values appear earlier |
| `priority` | no | integer | Load priority for same-path page overrides; default `0`, higher wins, equal priority lets the later resource pack entry win |
| `icon` | no | item id | Item icon shown in navigation/search. Accepts `modid:name`, `modid:name:meta` (colon-separated damage/subtype), `<modid:name:meta>` (strict form; meta `32767` matches all subtypes), or `modid:name meta` (space-separated, filter-expression style). |
| `icons` | no | list of item ids | List of item icons for animated cycling (one per second). Each entry uses the same syntax as `icon`. When present takes priority over `icon`. |
| `icon_texture` | no | asset path | Texture icon path resolved like any other asset link |
| `icon_textures` | no | list of asset paths | List of texture icons for animated cycling (one per second). When present takes priority over `icon_texture`. |
| `icon_components` | no | map | YAML map applied as NBT data to the navigation icon item. Equivalent of 1.7.10 item NBT (e.g. `display.Name` for custom item names). Applied to the `icon` field item; ignored when `icons` list is used. |

### Example Frontmatter

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book
  # Use meta/damage to select a specific subtype:
  # icon: minecraft:wool:1       (orange wool, colon form)
  # icon: <minecraft:wool:1>     (strict colon form)
  # icon: minecraft:wool 1       (space form, filter-expression style)

  # Cycling icons list — cycles one per second:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4
  #   - minecraft:wool:14

  # Custom NBT on the icon item (only applies to single icon:, not icons:):
  # icon_components:
  #   display:
  #     Name: "My Custom Book"

  icon_texture: test1.png
  # Cycling textures:
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: ExampleAuthor
date: 2024-01-15
updated: 2024-06-01
```

For BetterQuesting integration, `quest_ids` accepts either of these formats:

- canonical UUID strings such as `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids such as `AAAAAAAAAAAAAAAAAAAMug==`

Do not list both forms for the same quest in one page's `quest_ids`; they normalize to the same internal UUID and would be treated as duplicates.

When any of `author`, `authors`, `date`, or `updated` is present, GuideNH shows a
bottom bar in the guide screen (matching the top toolbar style) with right-aligned
text like: *Content from MyMod, Author ExampleAuthor, Date 2024-01-15, Updated 2024-06-01*.

Multiple authors example:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
Or with structured entries:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

The `zoom` key lets you enlarge or shrink the content of a single page without
affecting any other page. The value is a positive float treated as a multiplier:

| Example value | Effect |
| --- | --- |
| `1.0` (default) | Normal size |
| `1.5` | 150 % — content 50 % larger |
| `0.75` | 75 % — content 25 % smaller |

The per-page zoom is multiplied with the global **contentZoom** slider in
ModConfig → GuideNH → UI. This lets server packs set a sensible baseline while
still allowing individual pages to fine-tune layout for narrow or wide content.

Example: set this page to display at 150 % of the base zoom:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

Page layout is recomputed at the zoom-adjusted width, so text wrapping and all
block geometry remain correct at any zoom level.

## Link Resolution

GuideNH resolves ids and paths using these rules:

### Page links

| Input | Meaning |
| --- | --- |
| `subpage.md` | relative to the current page, in the current page namespace |
| `./subpage.md` | relative to the current page, in the current page namespace |
| `/guide.md` | rooted to the current page namespace, equivalent to `currentmod:guide.md` |
| `gregtech:guide.md` | explicit namespace; opens `gregtech:guidenh` when the current guide path is `guidenh` |
| `gregtech:/guide.md` | explicit namespace plus rooted path, normalized to `gregtech:guide.md` |
| `subpage.md#anchor` | page plus anchor fragment |
| `guidenh:other.md#anchor` | explicit `modid:path#anchor` |
| `https://example.com` | external HTTP/HTTPS link |

Page links are isolated by namespace. A link written from `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves to `guidenh:guide.md`; the same text in
`assets/gregtech/guidenh/_en_us/index.md` resolves to `gregtech:guide.md`. If that page is missing in the
current namespace, GuideNH reports it as a broken link instead of falling back to another mod's page.

Explicit `modid:path` links can cross from one mod's data-driven guide to another. The guide id is derived from
the target page namespace and the current guide path, so a link from `guidenh:guidenh` to `gregtech:guide.md`
opens page `gregtech:guide.md` in guide `gregtech:guidenh`.

Anchor fragments scroll the guide to a heading whose text lowercased and spaces replaced with hyphens
matches the fragment (e.g. `#crafting-recipe` scrolls to `## Crafting Recipe`), or to a `<a name="...">` anchor.

### Asset links

Assets use the same resolution rules as links. For example:

- `test1.png` resolves relative to the current page file.
- `/assets/example_structure.snbt` resolves to the guide's asset root.
- `guidenh:textures/gui/example.png` resolves as an explicit resource location.

## Item Reference Syntax

Several tags accept item references with extended syntax:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Rules:

- omitted `meta` defaults to `0`
- `*`, `32767`, or uppercase tokens like `ANY` become wildcard meta
- an SNBT tail starts at the first `{` and is parsed as item NBT

Examples:

```text
minecraft:diamond
minecraft:wool:14
minecraft:wool:*
minecraft:written_book:0:{title:TestBook,author:GuideNH}
```

## Error Handling

If a page fails to parse, GuideNH creates an error page instead of crashing the guide. Invalid tags, ids, and attributes are reported inline as guide-rendered error text.

## Related Pages

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
