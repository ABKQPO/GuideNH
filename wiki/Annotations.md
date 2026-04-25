# Annotations

GuideNH scene annotations are child tags inside `<GameScene>` / `<Scene>`. They render in world space and may contain child markdown/tag content that becomes a rich tooltip.

## General Rules

- annotations only work inside a scene
- child content becomes the tooltip body
- annotations can be hidden with the scene UI toggle
- `alwaysOnTop` draws above scene geometry when supported by the annotation type

## Supported Annotation Tags

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`

GuideNH also registers `<BlockAnnotationTemplate>`, but the current built-in implementation is a no-op stub and does not produce any visible annotation yet.

## `<BlockAnnotation>`

Highlights a single 1x1x1 block volume.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | yes | `x y z` vector |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, or `transparent` |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |

Example:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Highlights the controller block.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Highlights an arbitrary axis-aligned box.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `min` | yes | `x y z` minimum vector |
| `max` | yes | `x y z` maximum vector |
| `color` | no | annotation color |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |

GuideNH automatically swaps min/max coordinates per axis when they are provided in reverse order.

Example:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Half-height area highlight.
</BoxAnnotation>
````

## `<LineAnnotation>`

Draws a line segment in world space.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `from` | yes | `x y z` start vector |
| `to` | yes | `x y z` end vector |
| `color` | no | annotation color |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |

Example:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signal path.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places a screen-facing diamond marker at a world position.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | yes | `x y z` marker position |
| `color` | no | tint color; omitted defaults to bright green |

Example:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## Rich Tooltip Content

Annotation children are compiled as normal GuideNH content, so tooltips may contain:

- markdown paragraphs and headings
- item/block images
- recipes
- nested non-interactive scenes

Example:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## Unsupported Template Tag

`<BlockAnnotationTemplate>` is currently registered but the built-in compiler body is empty. Treat it as reserved for future work.

## Related Pages

- [GameScene](GameScene)
- [Examples](Examples)
