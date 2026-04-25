# Localization

GuideNH supports localized guide pages and localized guide assets.

## Folder Layout

Runtime localization is folder-based:

```text
wiki/resourcepack/assets/<namespace>/guides/<guide_namespace>/<guide_id>/
├─ _manifest.json
├─ en_us/
│  └─ index.md
└─ zh_cn/
   └─ index.md
```

## Page Lookup Order

For each manifest entry, GuideNH tries:

1. `<current language>/<page>`
2. `<page>` without a language folder
3. if the current language is not the guide default language, `<default language>/<page>`
4. the language-neutral `<page>` path again as the final fallback

In practice, you should keep page files in language folders and rely on the default language as the main fallback.

## Asset Lookup Order

Guide assets use a slightly richer fallback order:

1. `<current language>/<path>`
2. `_<current language>/<path>`
3. `<default language>/<path>`
4. `<path>`

This makes it possible to localize images or texture-like assets when needed.

## Search And Language

Search documents store both the raw Minecraft language and the analyzer language used for Lucene. If the current Minecraft language is not mapped to a known analyzer, search falls back to English tokenization.

## Ignore Translation Config

GuideNH exposes a client configuration option named `Ignore Guide Translations`. When enabled, the original guide language is used regardless of the current UI language.

## Authoring Advice

- always keep one fully complete default language
- translate pages first, then translate assets only when text is embedded in the asset
- use the same manifest page list across languages
- avoid language-specific asset filenames when a rooted shared asset would do

## Example

```text
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/index.md
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/zh_cn/index.md
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/en_us/test1.png
wiki/resourcepack/assets/guidenh/guides/guidenh/guidenh/zh_cn/test1.png
```

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
