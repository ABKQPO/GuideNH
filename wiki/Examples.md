# Examples

GuideNH already ships a runtime example guide in `wiki/resourcepack/`. This page maps the important example files to the feature areas they demonstrate.

## Core Example Pages

| Runtime file | What it demonstrates |
| --- | --- |
| `.../_en_us/index.md` | frontmatter, item ids, recipes, item/block images, command links, tooltips, scenes, annotations, `ImportStructureLib`, `RemoveBlocks`, `BlockAnnotationTemplate`, home-page recommendation example |
| `.../_en_us/markdown.md` | plain markdown features, tables, and a `recommend: 0` example |
| `.../_en_us/rendering.md` | block-level rendering and layout behavior |
| `.../_en_us/scene-blocks.md` | static and interactive block scene examples |
| `.../_en_us/japanese.md` | navigation child example |
| `.../_en_us/navigation-guide.md` | navigation, linking, and high-priority recommendation example |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | second namespace example for cross-guide links and isolated same-name pages |

## Asset Examples

| Runtime file | Purpose |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | page-local image example |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | rooted shared structure asset for `<ImportStructure>` and `<RemoveBlocks>` |

## Example Snippets

### Frontmatter + Navigation

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### Home Recommendation

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

```yaml
navigation:
  title: Navigation & Index
  parent: index.md
  recommend: 5
```

### Relative Image

````md
![Test Image](test1.png)
````

### Namespaced Guide Links

````md
[Same namespace](guide.md)
[Same namespace from root](/guide.md)
[Other namespace](gregtech:guide.md)
[Other namespace from root](gregtech:/guide.md)
````

### Rooted Structure Asset

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib Scene Import

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="hatch" value="1" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

The child tags set StructureLib defaults for the scene and are restored when the scene view is reset.

### Imported Structure Cleanup

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
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
- use `index.md` when testing mixed runtime features together, including StructureLib tooltip, hatch-highlight, and cleanup behavior
- use `scene-blocks.md` when you only need static block layout previews
- use `example_structure.snbt` when you need a reusable imported structure asset

## Recommended Learning Order

1. [Getting Started](Getting-Started)
2. [Guide Page Format](Guide-Page-Format)
3. [Tags Reference](Tags-Reference)
4. [GameScene](GameScene)
5. [Annotations](Annotations)
6. [Recipes](Recipes)
