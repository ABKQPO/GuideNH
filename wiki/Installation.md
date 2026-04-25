# Installation

This page covers the repository layout and the development flow for the built-in tutorial guide shipped by GuideNH.

## Runtime Guide Source

The runtime guide is authored from `wiki/resourcepack/`, not from `src/main/resources/assets/...`.

The built-in example guide now lives directly under:

- `wiki/resourcepack/assets/guidenh/guidenh/`

The example guide content then continues inside language folders such as:

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/`
- `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/`

## Build Output

During `processResources`, the project copies everything under `wiki/resourcepack/assets/` directly into the mod jar resource tree. The bundled guide therefore ships inside the jar as normal mod resources such as:

- `assets/guidenh/guidenh/_en_us/index.md`
- `assets/guidenh/guidenh/_zh_cn/index.md`
- `assets/guidenh/guidenh/assets/example_structure.snbt`

## Development Loop

1. Edit runtime pages and assets under `wiki/resourcepack/`.
2. Edit human documentation under `wiki/*.md`.
3. Rebuild resources or rerun the client so the updated guide assets are copied onto the classpath.
4. Reload or restart the client and open the guide.

## What Not To Do

- Do not put in-game MDX tags directly into the GitHub Wiki pages unless they are inside fenced code blocks.
- Do not add new built-in guide source files directly under `src/main/resources/assets/...`; author them from `wiki/resourcepack/assets/...`.
- Do not reintroduce `_manifest.json` or resource-pack zip wrapping for runtime guide pages; the loader now scans the resource tree directly.

## Related Pages

- [Getting Started](Getting-Started)
- [Guide Page Format](Guide-Page-Format)
- [Examples](Examples)