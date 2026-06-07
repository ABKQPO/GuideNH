---
navigation:
  title: Page Lang Keys
  parent: markdown.md
  position: 60
categories:
  - markdown
  - localization
---

# Page Lang Key Fallback

This is the physical markdown fallback file.

If `guidenh.page.guidenh.guidenh.page-lang-key` exists in the active language `.lang` file, GuideNH replaces this
entire page source with that localized markdown value before parsing.

When that localized value has frontmatter, missing fields are inherited from this physical fallback page. This lets a
translation keep its own title while still receiving structural metadata such as home-page recommendations from the
fallback file.

If the key is missing or empty, GuideNH falls back to this file content.
