# Guide Page Format

GuideNH runtime pages are markdown files parsed with:

- standard markdown block and inline syntax
- YAML frontmatter
- GFM tables
- strikethrough
- MDX-style custom tags

## Supported Markdown

GuideNH pages support the common markdown features used in the example guide:

- headings
- paragraphs
- inline emphasis, bold, strike, and code
- links and images
- unordered and ordered lists
- blockquotes
- horizontal rules
- fenced code blocks
- GFM tables

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` for a live sample page.

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
