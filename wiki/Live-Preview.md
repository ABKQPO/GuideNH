# Live Preview

GuideNH supports a development-mode live preview flow for guide content. When enabled, GuideNH reads pages and assets
directly from a local folder and reloads changes in-game without requiring a full restart.

## What It Is For

Use live preview when you are actively editing runtime guide pages under `wiki/resourcepack/` and want a tight
authoring loop.

This is especially useful for:

- markdown edits
- navigation changes
- page-local images
- guide-root assets such as imported structures
- Ponder JSON files referenced by `<ImportPonder>`

## Supported System Properties

| Property | Meaning |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | local folder used as the development source root |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | optional namespace override for files loaded from that folder |
| `guideme.resourcePacks.sources` | standard resource-pack root to load and watch as an ordered development overlay; may be repeated |
| `guideme.resourcePack.sources` | alias of `guideme.resourcePacks.sources`; may be repeated |
| `guideme.showOnStartup` | optional guide or page to open automatically on the title screen |
| `guideme.validateAtStartup` | optional comma-separated guide ids to validate once on startup |

## GuideNH Repository Example

For the built-in example guide in this repository:

- guide id: `guidenh:guidenh`
- development source root: `wiki/resourcepack/assets/guidenh/guidenh`

The matching system property is:

```text
guideme.guidenh.guidenh.sources=<absolute-path-to-repo>/wiki/resourcepack/assets/guidenh/guidenh
```

GuideNH also supports broader development source roots:

| Source root | Page id mapping |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh` | content-root mode; files map through `sourcesNamespace`, defaulting to `guidenh` |
| `wiki/resourcepack/assets` | assets-root mode; `assets/<modid>/guidenh/...` maps to `<modid>:...` |
| `wiki/resourcepack` | resourcepack-root mode; `assets/<modid>/guidenh/...` maps to `<modid>:...` |

The data-driven guide remains a single merged guide. When multiple namespaces contain the same markdown file name, the
namespace is preserved in the page id, so `assets/gregtech/guidenh/_zh_cn/index.md` and
`assets/appliedenergistics2/guidenh/_zh_cn/index.md` become `gregtech:index.md` and
`appliedenergistics2:index.md` instead of overwriting each other.

## Multi-Pack Live Preview

Use `guideme.resourcePacks.sources` when you want one or more folders to behave like normal resource packs and be
watched for all GuideNH content under `assets/<modid>/guidenh/...`.

```text
-Dguideme.resourcePacks.sources=E:/packs/base
-Dguideme.resourcePacks.sources=E:/packs/override
```

The property is intentionally repeatable. GuideNH reads the JVM input arguments in the order they appear, because Java
system properties would otherwise collapse repeated `-D` keys into a single final value.

Conflict rules are resource-pack-like but deterministic for live preview:

- different mod ids are isolated by namespace, so `gregtech:index.md` and `appliedenergistics2:index.md` are separate pages
- for the same mod id, language, and markdown path, the earlier `guideme.resourcePacks.sources` entry wins
- if the same page also exists in regular loaded resource packs, the development resource-pack roots win

Changing markdown, Ponder JSON, images, or other files under these development resource-pack roots triggers a client
resource reload so the merged guide and scene content refresh together.

## `showOnStartup` Formats

`guideme.showOnStartup` supports these forms:

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

Relative page ids after `!` resolve against the guide namespace, so `index.md` becomes `guidenh:index.md`.

## Performance Notes

GuideNH does not poll guide files every frame.

- development source watching is only registered when at least one guide actually enables development sources
- multi-pack live preview watching is only registered when at least one `guideme.resourcePacks.sources` root exists
- guide watcher processing is throttled to once every `20` client ticks
- non-Markdown guide assets trigger page reparsing, so external files such as Ponder JSON can update live
- startup guide opening and validation run only once after the title screen appears

This keeps the live preview workflow responsive without introducing unnecessary high-frequency client work.

## Recommended Workflow

1. Start the client with `runGuide`.
2. Edit runtime guide files under `wiki/resourcepack/assets/...`.
3. Wait for the throttled watcher to pick up the change.
4. Reopen or revisit the page if you changed navigation structure or startup targets.

## When A Restart Is Still Needed

Live preview helps with content iteration, but a restart or full resource reload can still be needed when:

- you change code rather than guide content
- you change build logic
- you add resources outside the configured development source tree

## Related Pages

- [Installation](Installation)
- [Getting Started](Getting-Started)
- [GameScene](GameScene)
- [FAQ](FAQ)
