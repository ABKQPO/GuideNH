# Localization

GuideNH supports localized guide pages and localized guide assets.

## Folder Layout

Runtime localization is folder-based:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

Language folders are recognized only in the underscored form. Plain folders such as `en_us/` and `zh_cn/` are no longer treated as localization roots.

## Page Lookup Order

For each requested page id, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` if the current language page is missing
3. `<page>` without a language folder

Guide pages only fall back to the guide's `defaultLanguage`. Auto-discovered resource-pack guides still default that value to `en_us`, so another language is not promoted into a fallback language just because it exists.

## Page Lang Key Overrides

Guide pages may also replace their full markdown source from a `.lang` key, but only when the physical page file
already exists. The file remains the existence gate and the fallback source.

- GuideNH first resolves the page file with the normal language fallback order
- after a file has been found, GuideNH looks for a page-localized `.lang` value for the requested language
- if that key exists and is non-empty, its full value becomes the page markdown source before parsing
- if the key is missing or empty, GuideNH falls back to the resolved file content

The key format is:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Example:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

Path separators become `.` in the key. Literal dots inside a path segment are escaped so they do not collide with
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

Other non-alphanumeric characters are escaped with the same `_x<hex>_` pattern.

Inside the `.lang` value, literal `\n` and `\r` sequences are converted to real line endings before markdown parsing,
so the translation value can contain a full page including frontmatter, headings, lists, and MDX tags.

Authoring rules:

- write the page as one physical `.lang` line for that key
- use literal `\n` inside the value when you want a markdown line break
- do not insert real line breaks into the `.lang` value itself, because Forge reads `.lang` files line by line
- do not write `\\n` unless you intentionally want the final markdown source to contain the literal characters `\n`

GuideNH does not synthesize pages from `.lang` alone. A real page file must still exist.

## Key Length

GuideNH does not impose an extra character limit on these page keys. On Minecraft 1.7.10 / Forge, the backing language
data is effectively a string-property map, so the practical limits are normal memory usage and maintainability rather
than a dedicated hard cap. Shorter page paths still make keys easier to author and review.

## Authoring Advice

- set `defaultLanguage` deliberately when you want a non-English fallback language for a guide
- add a shared language-neutral page only when cross-language fallback is actually intended
- translate pages first, then translate assets only when text is embedded in the asset
- avoid language-specific asset filenames when a rooted shared asset would do

## Asset Lookup Order

Guide assets use a slightly richer fallback order:

1. `_<current language>/<path>`
2. if the current language is not the guide default language, `_<default language>/<path>`
3. `<path>`

This makes it possible to localize images or texture-like assets when needed.

## Search And Language

Search documents store both the raw Minecraft language and the analyzer language used for Lucene. If the current Minecraft language is not mapped to a known analyzer, search falls back to English tokenization.

## Ignore Translation Config

GuideNH does not expose a global "ignore translations" switch. If you want a guide to fall back to a non-English language, set that guide's `defaultLanguage` explicitly in code.

## Example

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
