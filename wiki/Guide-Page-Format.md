# Guide Page Format

GuideNH runtime pages are markdown files parsed with:

- standard markdown block and inline syntax
- YAML frontmatter
- GFM tables
- strikethrough
- MDX comments using `{/* ... */}`
- MDX-style custom tags

## Supported Markdown

GuideNH pages support the common markdown features used in the example guide:

- headings
- paragraphs
- inline emphasis, bold, strike, and code
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
| any other key | any YAML value | Preserved in `additionalProperties` for extensions or tooling |

### `navigation`

| Field | Required | Type | Notes |
| --- | --- | --- | --- |
| `title` | yes | string | Display name in navigation and search title fallback |
| `parent` | no | page id | Parent page id; omitted means top-level node |
| `position` | no | integer | Sibling sort order; default `0` |
| `icon` | no | item id | Item icon shown in navigation/search when valid |
| `icon_texture` | no | asset path | Texture icon path resolved like any other asset link |
| `icon_components` | no | map | Parsed from frontmatter but currently unused by runtime rendering |

### Example Frontmatter

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  parent: index.md
  position: 10
  icon: minecraft:book
  icon_texture: test1.png
categories:
  - basics
  - examples
```

## Link Resolution

GuideNH resolves ids and paths using these rules:

### Page links

| Input | Meaning |
| --- | --- |
| `subpage.md` | relative to the current page |
| `./subpage.md` | relative to the current page |
| `/assets/example.png` | rooted to the current guide namespace |
| `guidenh:index.md` | explicit `modid:path` resource location |
| `subpage.md#anchor` | page plus anchor fragment |
| `https://example.com` | external HTTP/HTTPS link |

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
