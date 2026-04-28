# GuideNH

GuideNH is an in-game guide framework for GTNH-era Minecraft mods. This wiki documents how the project is organized, how the runtime guide format works, and how to author pages, assets, recipes, scenes, and annotations without mixing game-only syntax into the GitHub Wiki itself.

## Start Here

- [Installation](Installation)
- [Getting Started](Getting-Started)
- [Live Preview](Live-Preview)
- [Guide Page Format](Guide-Page-Format)
- [Navigation](Navigation)
- [Search](Search)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
- [Annotations](Annotations)
- [Recipes](Recipes)
- [Localization](Localization)
- [Examples](Examples)
- [FAQ](FAQ)

## Repository Layout

| Path | Purpose |
| --- | --- |
| `wiki/` | Human-facing GitHub Wiki pages such as this one |
| `wiki/resourcepack/` | Runtime guide source tree used by the mod at build time |
| `wiki/resourcepack/assets/guidenh/guidenh/` | Example built-in guide pages and assets |
| `build/resources/main/assets/` | Gradle output where the runtime guide assets are copied for development runs |

## Two Markdown Layers

GuideNH intentionally uses two different authoring layers:

- GitHub Wiki markdown in `wiki/*.md` is for repository documentation and should stay plain GitHub Wiki markdown.
- Runtime guide markdown in `wiki/resourcepack/...` is for the in-game renderer and may use YAML frontmatter plus GuideNH-specific MDX tags such as `<GameScene>`, `<RecipeFor>`, and `<Tooltip>`.

The wiki explains the runtime syntax, but it does not use the runtime tags directly outside fenced code blocks.

## Quick Authoring Checklist

1. Put runtime guide files under `wiki/resourcepack/assets/<modid>/guidenh/`.
2. Add language folders such as `_en_us/` and `_zh_cn/`.
3. Put markdown pages inside those language folders.
4. Declare navigation metadata in frontmatter when you want a page to appear in the sidebar.
5. Use relative asset paths for page-local files and rooted `/...` paths for guide-root assets.
6. For 3D scenes, compose `<GameScene>` with `<ImportStructure>`, `<ImportStructureLib>`, `<RemoveBlocks>`, and `<BlockAnnotationTemplate>` as needed.

## Fast Iteration

For the built-in example guide, use the dedicated live preview flow documented in [Live Preview](Live-Preview). It
launches the client with a development source folder and opens the guide directly on startup.

## Scene Authoring Highlights

- `<ImportStructure>` imports external SNBT/NBT structures into a scene.
- `<ImportStructureLib>` imports StructureLib multiblocks by controller id using GTNH-style `modid:block[:meta]`.
- `<RemoveBlocks>` trims already-placed helper blocks after imports without changing neighboring states.
- `<BlockAnnotationTemplate>` copies the same child annotations onto every matching block that already exists in the scene.
- Interactive scenes can automatically expose layer sliders, StructureLib channel sliders, hatch highlight buttons, and rich hover tooltips when the underlying scene data provides them.

## Runtime Example Sources

The bundled example guide currently lives here:

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/rendering.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/structure.md`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

Those files are the best place to inspect real, running examples while reading this wiki. The two `index.md` pages now include mixed scene samples for `ImportStructureLib`, `RemoveBlocks`, and `BlockAnnotationTemplate`.
