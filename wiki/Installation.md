# Installation

This page covers the repository layout and the development flow for the built-in tutorial guide shipped by GuideNH.

## Runtime Guide Source

The runtime guide is authored from `wiki/resourcepack/`, not from `src/main/resources/assets/guidenh/guides/...`.

Required top-level files:

- `wiki/resourcepack/pack.mcmeta`
- `wiki/resourcepack/pack.png`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/_manifest.json`

The example guide content then continues inside language folders such as:

- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/`
- `wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/zh_cn/`

## Build Output

During packaging, the project produces a standard resource-pack zip and embeds it into the mod jar at:

`src/main/resources/assets/guidenh/resourcepacks/guidenh_tutorial_guide_resource_pack.zip`

That zip contains:

- `pack.mcmeta`
- `pack.png`
- `assets/guidenh/guides/guidenh/guidenh/...`

## Development Loop

1. Edit runtime pages and assets under `wiki/resourcepack/`.
2. Edit human documentation under `wiki/*.md`.
3. Package the runtime guide resource pack.
4. Reload or restart the client and open the guide.

## What Not To Do

- Do not put in-game MDX tags directly into the GitHub Wiki pages unless they are inside fenced code blocks.
- Do not add new built-in guide source files under `src/main/resources/assets/guidenh/guides/...`.
- Do not treat `src/main/resources/assets/guidenh/resourcepacks/` as authored content; it is build output.

## Related Pages

- [Getting Started](Getting-Started)
- [Guide Page Format](Guide-Page-Format)
- [Examples](Examples)
