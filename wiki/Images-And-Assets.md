# Images And Assets

GuideNH supports both normal markdown images and several runtime-specific visual elements.

## Asset Resolution Rules

Guide assets resolve with the same rules used by page links.

| Path form | Example | Meaning |
| --- | --- | --- |
| relative | `test1.png` | relative to the current page file |
| rooted | `/assets/example_structure.snbt` | relative to the current guide root |
| explicit resource id | `guidenh:textures/gui/example.png` | absolute `modid:path` lookup |

## Markdown Images

Normal markdown images are supported:

````md
![Example](test1.png)
````

GuideNH resolves the path and loads the binary asset from the guide content root.

## `FloatingImage`

`<FloatingImage>` is the GuideNH-specific tag for float-left / float-right image layout.

### Attributes

| Attribute | Required | Meaning |
| --- | --- | --- |
| `src` | yes | image path |
| `align` | no | `left` or `right`, default `left` |
| `title` | no | tooltip/title text |
| `width` | no | explicit width in pixels |
| `height` | no | explicit height in pixels |

### Notes

- giving only one dimension keeps aspect ratio
- giving both dimensions stretches the image
- invalid `align` values render an inline error

### Example

````md
<FloatingImage src="test1.png" align="left" width="64" title="Example" />
````

## Navigation Texture Icons

Frontmatter can use `icon_texture` to show a texture instead of an item in navigation/search:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

The file must decode as an image. The path is resolved like any other guide asset path.

## Non-Image Assets

GuideNH pages may also reference non-image runtime assets, especially structure files, for example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

These assets are loaded through the same guide asset pipeline but are consumed by custom tags rather than rendered directly as images.

## Best Practices

- keep page-local images near the page that uses them
- keep reusable files under the guide root `assets/` folder
- prefer rooted `/assets/...` paths for shared files referenced by multiple pages
- use texture icons only for real image assets

## Runtime Example Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
