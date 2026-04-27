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

GuideNH also supports `<BlockAnnotationTemplate>`, which applies its child annotations to every already-placed matching block in the current scene.

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

## `<BlockAnnotationTemplate>`

Use it when you want to stamp the same annotation onto every matching block.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block matcher in `modid:block[:meta]` form |

Rules:

- the template only sees blocks that already exist when it is parsed
- place it after `<Block>`, `<ImportStructure>`, or `<ImportStructureLib>` tags that should feed it
- child annotations use local coordinates relative to each matched block

Example:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Template-generated tooltip
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Related Pages

- [GameScene](GameScene)
- [Examples](Examples)
