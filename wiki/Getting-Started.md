# Getting Started

This page shows the smallest useful GuideNH runtime guide layout and the first page you can author.

## Minimum Runtime Layout

```text
wiki/resourcepack/
├─ pack.mcmeta
├─ pack.png
└─ assets/
   └─ <namespace>/
      └─ guides/
         └─ <guide_namespace>/
            └─ <guide_id>/
               ├─ _manifest.json
               └─ en_us/
                  └─ index.md
```

For the built-in example guide in this repository, that resolves to:

```text
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/
```

## Minimum Manifest

```json
{
  "namespace": "guidenh",
  "pages": [
    "index.md"
  ]
}
```

The `pages` array lists runtime page files relative to the language folder.

## First Page

````md
---
navigation:
  title: Root
---

# Start Page

Welcome to GuideNH.

[Next Page](subpage.md)
````

## Adding Navigation Metadata

The smallest useful frontmatter for navigation is:

```yaml
navigation:
  title: Root
```

Without navigation frontmatter, the page can still exist and be linked to directly, but it will not automatically appear in the guide navigation tree.

## Adding Assets

Place page-local assets next to the page file:

```text
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/test1.png
```

Reference them relatively from markdown:

````md
![Example](test1.png)
````

Place shared guide assets under the guide's own `assets/` folder:

```text
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/assets/example_structure.snbt
```

Reference them with a rooted guide path:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

## Next Steps

- [Guide Page Format](Guide-Page-Format)
- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
