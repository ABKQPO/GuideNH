# FAQ

## Where should I edit the built-in runtime guide?

Under `wiki/resourcepack/`. The old loose source tree under `src/main/resources/assets/...` is no longer the authored source of truth for runtime guide pages.

## Why does this repository have both wiki pages and runtime markdown pages?

The files under `wiki/*.md` are human-facing GitHub Wiki documentation. The files under `wiki/resourcepack/...` are the in-game guide pages consumed by GuideNH itself.

## Can I use the same markdown in both places?

Not directly. Runtime guide markdown may contain GuideNH-specific MDX tags that GitHub Wiki does not understand. Keep runtime syntax inside the runtime guide tree and show it in the wiki only inside fenced code blocks.

## How do I preview guide edits quickly?

Use the dedicated live preview run tasks documented in [Live Preview](Live-Preview). In this repository, `runGuide`
launches the client with the built-in example guide source folder already wired in and opens the guide on startup.
