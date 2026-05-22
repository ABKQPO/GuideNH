# Navigation

GuideNH builds its navigation tree from page frontmatter.

In the in-game sidebar, expanded ancestor pages stay pinned at the top while their still-visible
descendants scroll underneath. Multiple expanded ancestor levels can stack at once, and each sticky
row is pushed away only when its entire visible subtree scrolls out, similar to the VSCode file
explorer.

## Navigation Frontmatter

The `navigation` map controls whether a page appears in the guide tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Field Reference

| Field | Description |
| --- | --- |
| `title` | Required display title |
| `parent` | Optional parent page id, resolved like a guide page link |
| `position` | Optional sibling ordering hint |
| `recommend` | Optional home-page recommendation priority; absent means the page is not shown in the Recommended panel |
| `priority` | Optional load priority for same-path page overrides; default `0` |
| `icon` | Optional item icon |
| `icon_texture` | Optional texture icon resolved from guide assets |
| `icon_components` | Parsed but not currently used by built-in rendering |
| `required_mod` | Optional single mod id; page is hidden when this mod is not loaded |
| `required_mods` | Optional list of mod ids; page is hidden unless all listed mods are loaded |

### `navigation.position`

`navigation.position` is an optional integer used to order sibling pages in the navigation tree.

- Missing `position` defaults to `0`.
- Larger values appear earlier.
- If two pages have the same value, they are sorted by title alphabetically.

## Home Page Recommendations

### `navigation.recommend`

`navigation.recommend` is an optional integer used by the home page Recommended panel.

- Pages only appear in the Recommended panel when this field is present.
- `0` is valid.
- Larger values appear earlier.
- If two pages have the same value, they are sorted by title alphabetically.
- The panel works at the `GuidePage` level, so each recommended page entry jumps directly to that page.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Mod Requirements

Use `required_mod` or `required_mods` to make a page conditional on one or more mods being loaded.
When the requirement is not met the page is excluded from the navigation tree and all page indices
(item, category, etc.) so it cannot be found through navigation or search.

```yaml
navigation:
  title: Applied Energistics Integration
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: Multi-Mod Feature
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

Both keys may be combined; the page is only shown when every listed mod is present.

## Load Priority

When several loaded resource packs provide the same guide page path, GuideNH reads the page
frontmatter first and chooses the candidate with the highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Rules:

- missing `priority` is `0`
- values are signed Java integers up to `2147483647`
- higher priority wins
- if priorities are equal, the later processed resource pack entry wins, matching Minecraft resource-pack override order
- priority only decides between candidates for the same page path and language/fallback layer

This is useful when a mod ships a baseline guide page and a pack wants to replace it without relying only on
resource-pack ordering.

## Icon Sources

GuideNH chooses navigation/search icons in this order:

1. `icon_texture` if the texture file loads successfully
2. `icon` if the item exists
3. no icon if neither is usable

Texture icons are read from runtime assets, so relative page-local files such as `test1.png` work.

## Parent And Root Nodes

- Omit `parent` to create a root node.
- Set `parent: index.md` or any other page id to create a child node.
- The parent page must exist in the same guide navigation tree.

`navigation.parent` uses the same namespace rules as Markdown page links:

- `parent: index.md` and `parent: ./index.md` resolve inside the current page namespace.
- `parent: /index.md` resolves from the current page namespace root.
- `parent: gregtech:index.md` or `parent: gregtech:/index.md` explicitly targets another namespace.

Data-driven guides are isolated by namespace. Pages under `assets/guidenh/guidenh/_en_us/...` belong to
`guidenh:guidenh`; pages under `assets/gregtech/guidenh/_en_us/...` belong to `gregtech:guidenh`.
Relative parents and links never fall through to another mod's same-named page.

## Category Pages

Pages can join one or more named categories using frontmatter:

```yaml
categories:
  - basics
  - machines
```

Those categories become queryable through the built-in `<CategoryIndex>` tag.

## Item-Indexed Pages

Pages can register item-to-page mappings using `item_ids`:

```yaml
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - minecraft:iron_ore#crafting
```

These mappings are used by `<ItemLink>`.

An optional `#anchor` suffix scrolls to a specific heading when the link is clicked.
The anchor is formed by lowercasing the heading text and replacing spaces with hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup behavior:

1. exact item + exact meta
2. wildcard-meta fallback if present

## Heading Anchor Links

GuideNH supports heading anchor navigation in Markdown links and `<a>` tags.
Anchors are derived from heading text by lowercasing and replacing spaces with hyphens.

**Same-page anchor:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-page anchor:**

```md
[See Getting Started](./getting-started.md#installation)
[Another guide](other-guide.md#usage)
```

**Absolute path anchor** (using the guide namespace, avoids relative path ambiguity in subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

The `namespace:path` format resolves to the guide page whose id matches `namespace:path`.
This is identical to what relative paths resolve to, but avoids `../` navigation.
The page must exist in the same guide as the link source.

**Named inline anchors** can also be placed with `<a name="...">` in MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating to a link with an anchor scrolls the guide to the target heading or named anchor.

## `<SubPages>`

`<SubPages>` renders links to navigation children.

### Attributes

| Attribute | Type | Default | Meaning |
| --- | --- | --- | --- |
| `id` | page id or empty string | current page | Page whose children should be listed |
| `alphabetical` | boolean expression | `false` | Sort children by title instead of navigation order |

### Examples

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists root navigation nodes.

## `<CategoryIndex>`

`<CategoryIndex>` renders links to every page in a named category.

````md
<CategoryIndex category="machines" />
````

If the category is missing, GuideNH renders an inline error.

## Search Result Titles

Search titles are derived in this order:

1. `navigation.title`
2. first level-1 heading (`# Heading`)
3. raw page id

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
