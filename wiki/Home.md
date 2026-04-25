# GuideNH

GuideNH is an in-game guide framework for GTNH-era Minecraft mods. This wiki documents how the project is organized, how the runtime guide format works, and how to author pages, assets, recipes, scenes, and annotations without mixing game-only syntax into the GitHub Wiki itself.

## Start Here

- [Installation](Installation)
- [Getting Started](Getting-Started)
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
| `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/` | Example built-in guide pages and assets |
| `src/main/resources/assets/guidenh/resourcepacks/` | Embedded resource-pack zip output included in the jar |

## Two Markdown Layers

GuideNH intentionally uses two different authoring layers:

- GitHub Wiki markdown in `wiki/*.md` is for repository documentation and should stay plain GitHub Wiki markdown.
- Runtime guide markdown in `wiki/resourcepack/...` is for the in-game renderer and may use YAML frontmatter plus GuideNH-specific MDX tags such as `<GameScene>`, `<RecipeFor>`, and `<Tooltip>`.

The wiki explains the runtime syntax, but it does not use the runtime tags directly outside fenced code blocks.

## Quick Authoring Checklist

1. Put runtime guide files under `wiki/resourcepack/assets/<namespace>/guides/<guide_namespace>/<guide_id>/`.
2. Keep `pack.mcmeta`, `pack.png`, and `_manifest.json` present.
3. Add language folders such as `en_us/` and `zh_cn/`.
4. Declare navigation metadata in frontmatter when you want a page to appear in the sidebar.
5. Use relative asset paths for page-local files and rooted `/...` paths for guide-root assets.

## Runtime Example Sources

The bundled example guide currently lives here:

- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/index.md`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/markdown.md`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/rendering.md`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/structure.md`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/assets/example_structure.snbt`

Those files are the best place to inspect real, running examples while reading this wiki.
