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

## Supported System Properties

| Property | Meaning |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | local folder used as the development source root |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | optional namespace override for files loaded from that folder |
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

## `showOnStartup` Formats

`guideme.showOnStartup` supports these forms:

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

Relative page ids after `!` resolve against the guide namespace, so `index.md` becomes `guidenh:index.md`.

## Gradle Run Tasks In This Repository

This repository now provides dedicated live preview tasks:

- `runGuide`
- `runGuide17`
- `runGuide21`
- `runGuide25`

They inherit the normal client run configuration and add:

- `guideme.guidenh.guidenh.sources`
- `guideme.showOnStartup=guidenh:guidenh!index.md`

Typical usage:

```text
.\gradlew.bat runGuide
```

## Performance Notes

GuideNH does not poll guide files every frame.

- development source watching is only registered when at least one guide actually enables development sources
- guide watcher processing is throttled to once every `20` client ticks
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
