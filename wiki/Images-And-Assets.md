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
| `sound` | no | sound event played by the whole image |
| `src` | yes | image path; when used on `<SoundArea>`, the sound file path |
| `trigger` | no | `click` by default, or `hover` for hover playback |

### Notes

- giving only one dimension keeps aspect ratio
- giving both dimensions stretches the image
- invalid `align` values render an inline error

### Example

````md
<FloatingImage src="test1.png" align="left" width="64" title="Example" />
````

## `ImageAnnotation`

`<ImageAnnotation>` is a child element of `<FloatingImage>` that attaches a rich-text tooltip (and
an optional colored border) to a rectangular region of the image. Coordinates are specified in
**image pixels** and are automatically proportionally scaled when the image is resized or stretched.

### Attributes

| Attribute | Required | Default | Meaning |
| --- | --- | --- | --- |
| `x` | no | — | left edge of the region in image pixels |
| `y` | no | — | top edge of the region in image pixels |
| `w` | no | — | width of the region in image pixels |
| `h` | no | — | height of the region in image pixels |
| `border` | no | `false` | show a colored border around the region |
| `borderColor` | no | random | border color (`#RRGGBB` or `#AARRGGBB`) |
| `borderThickness` | no | `1` | border thickness in display pixels |
| `sound` | no | none | optional sound event played for this region |
| `src` | no | none | optional sound file path; converted to a sound event id |
| `trigger` | no | `click` | `click` or `hover` |

### Notes

- omitting all four of `x`, `y`, `w`, `h` makes the annotation cover the **whole image**
- if any of the four is present, the remaining omitted ones default to `0` (origin) or `1` (size)
- border is **not shown by default**; add `border` or `border={true}` to enable it
- when `borderColor` is omitted and `border` is enabled, a random fully-opaque color is used
- child MDX content is rendered as the tooltip body and may include any inline/block elements
- later annotations (lower in the list) take hover priority over earlier ones when regions overlap

### Example

Whole-image annotation:

````md
<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation>
    Hover anywhere on the image to see this tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Region annotation with a visible border:

````md
<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    This is the **highlighted region** tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Multiple regions on one image:

````md
<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

Image regions can also play sounds. Use `<SoundArea>` when you only need sound, or put `sound`
directly on `<ImageAnnotation>` when the same region also has a tooltip or border.

````md
<FloatingImage src="test1.png" align="left" width="128" sound="guidenh:image.click">
  <SoundArea x="0" y="0" w="64" h="64" sound="guidenh:image.left" />
  <SoundArea x="64" y="0" w="64" h="64" sound="guidenh:image.right" trigger="hover" />
  <ImageAnnotation x="10" y="10" w="40" h="40" border sound="guidenh:image.note">
    This region has both tooltip content and a click sound.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers the whole image. Region sounds use image-pixel coordinates
and obey the same overlap priority as tooltips: later regions win.

## Content Embedding and Text Wrapping

All block-level tags — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
and any other tag backed by `BlockTagCompiler` — support two optional layout attributes that
provide Word-style content embedding.

| Attribute | Values | Default | Meaning |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | Text-wrapping mode |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Wrap modes

| Mode | Word equivalent | Block-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | In line with text | Default stack (嵌入型) | Sits on the text line |
| `square` | Square | Document-level float; text wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | Same as `square` (紧密型) | Same as `square` |
| `through` | Through | Same as `square` (穿越型) | Same as `square` |
| `top-bottom` | Top and Bottom | Full-width slot; `align` repositions horizontally (上下型) | Line-inline with breaks |
| `behind` | Behind text | Aligned inline slot; renders behind text (衬于文字下方) | Sits on the line |
| `front` | In front of text | Aligned inline slot; renders in front of text (浮于文字上方) | Sits on the line |

### Alignment with floating wrap

For `wrap=square/tight/through`:
- `align=left` (default) — block floats to the **left**; text fills the right side.
- `align=right` — block floats to the **right**; text fills the left side.
- `align=center` — block is centred without floating (no text wrapping).

### Examples

Left-floating image using the new `wrap` attribute:

````md
<FloatingImage src="test1.png" wrap="square" align="left" width="64" />

Paragraph text that flows to the right of the image...
````

Right-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Text that flows to the left of the recipe box...
````

Centred item image (no text wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

Right-aligned item image:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

> **Note** — `<FloatingImage>` also supports its own `align="left/right"` attribute for
> historical reasons. For new content, prefer the universal `wrap` + `align` approach above,
> which works on any block tag.

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

## `BlockImage`

`<BlockImage>` uses the same block-level embedding rules as `<FloatingImage>`, but the visual
content is a transparent 3D single-block preview instead of a bitmap. It is best suited for
showing how a placed block looks in-world while still fitting inline with normal guide prose.

Key behavior:

- transparent background and border
- no scene buttons, no layer slider, no annotation authoring surface
- hover still shows the selected block outline and tooltip
- `scale` changes camera zoom
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, and `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT still works, but the
  standalone `nbt` attribute is easier to read and is preferred

Example:

````md
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## Runtime Example Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
