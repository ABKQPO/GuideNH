# Navigation

GuideNH builds its navigation tree from page frontmatter.

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
| `parent` | Optional parent page id |
| `position` | Optional sibling ordering hint |
| `icon` | Optional item icon |
| `icon_texture` | Optional texture icon resolved from guide assets |
| `icon_components` | Parsed but not currently used by built-in rendering |

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
```

These mappings are used by `<ItemLink>`.

Lookup behavior:

1. exact item + exact meta
2. wildcard-meta fallback if present

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
