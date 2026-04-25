# Examples

GuideNH already ships a runtime example guide in `wiki/resourcepack/`. This page maps the important example files to the feature areas they demonstrate.

## Core Example Pages

| Runtime file | What it demonstrates |
| --- | --- |
| `.../en_us/index.md` | frontmatter, item ids, recipes, item/block images, command links, tooltips, scenes, annotations |
| `.../en_us/markdown.md` | plain markdown features and tables |
| `.../en_us/rendering.md` | block-level rendering and layout behavior |
| `.../en_us/structure.md` | `<Structure>` usage and coordinate format |
| `.../en_us/japanese.md` | navigation child example |
| `.../en_us/subpage.md` | navigation / linking example |

## Asset Examples

| Runtime file | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/test1.png` | page-local image example |
| `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure asset for `<ImportStructure>` |

## Example Snippets

### Frontmatter + Navigation

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  icon_texture: test1.png
```

### Relative Image

````md
![Test Image](test1.png)
````

### Rooted Structure Asset

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### Scene Annotation Tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### Recipe Filter

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## When To Use Which Example

- start with `markdown.md` if you are validating parser basics
- use `index.md` when testing mixed runtime features together
- use `structure.md` when you only need static block layout previews
- use `example_structure.snbt` when you need a reusable imported structure asset

## Recommended Learning Order

1. [Getting Started](Getting-Started)
2. [Guide Page Format](Guide-Page-Format)
3. [Tags Reference](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
