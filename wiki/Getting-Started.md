# Getting Started

This page shows the smallest useful GuideNH runtime guide layout and the first page you can author.

## Minimum Runtime Layout

```text
wiki/resourcepack/
`-- assets/
    `-- <modid>/
        `-- guidenh/
            |-- assets/
            |   `-- example_structure.snbt
            `-- _en_us/
                `-- index.md
```

For the built-in example guide in this repository, that resolves to:

```text
wiki/resourcepack/assets/guidenh/guidenh/
```

## Guide Discovery

GuideNH now discovers pages directly from the resource tree. Any markdown file under
`assets/<modid>/guidenh/_<lang>/...` is part of the guide for `<modid>:guidenh`.
`index.md` is still the conventional start page and the recommended place to begin.

## Fast Local Iteration

If you are editing the built-in example guide in this repository, prefer the live preview workflow documented in
[Live Preview](Live-Preview).

That workflow points GuideNH at:

```text
wiki/resourcepack/assets/guidenh/guidenh/
```

and opens the guide automatically on startup.

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
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
```

Reference them relatively from markdown:

````md
![Example](test1.png)
````

Place shared guide assets under the guide's own `assets/` folder:

```text
wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt
```

Reference them with a rooted guide path:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

## Next Steps

- [Live Preview](Live-Preview)
- [Guide Page Format](Guide-Page-Format)
- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
