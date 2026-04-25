# FAQ

## Where do runtime guide files live now?

Under `wiki/resourcepack/`. The old loose source tree under `src/main/resources/assets/guidenh/guides/...` is no longer the authored source of truth.

## Why are there two markdown systems?

Because they solve different problems:

- GitHub Wiki markdown documents the mod for repository readers
- runtime markdown drives the in-game guide renderer and supports GuideNH-specific tags

## What makes a page appear in navigation?

Add `navigation.title` in frontmatter. Without it, the page exists but does not become a navigation node automatically.

## How do I link to another page?

Use normal markdown links or the `<a>` tag:

- `subpage.md`
- `./subpage.md`
- `guidenh:index.md`
- `subpage.md#anchor`

## How do I reference assets?

- use relative paths for page-local files such as `test1.png`
- use rooted `/assets/...` paths for shared guide assets such as `/assets/example_structure.snbt`

## Does `icon_texture` accept non-image files?

Path resolution works the same way as other guide assets, but the loaded asset still has to decode as an image. A structure file such as `.snbt` is not a valid navigation icon texture.

## How do item ids with metadata work?

GuideNH accepts:

- `modid:name`
- `modid:name:meta`
- `modid:name:*`
- `modid:name:meta:{snbt}`

Wildcard meta values include `*`, `32767`, and uppercase tokens such as `ANY`.

## Why does my recipe tag show fallback text?

Usually one of these is true:

- the item id is wrong
- the optional handler filter removed every candidate
- the required mod/recipe handler is not loaded
- there is genuinely no matching recipe

## Can I put annotations anywhere?

No. Annotation tags only work inside `<GameScene>` / `<Scene>`.

## Is `<Entity>` supported?

Not visually yet. The tag is parsed, but the built-in compiler only logs that entity rendering is not implemented.

## Is `<BlockAnnotationTemplate>` supported?

It is registered, but the current built-in implementation is a no-op placeholder.
