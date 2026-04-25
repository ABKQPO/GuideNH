# Search

GuideNH maintains an in-memory Lucene index for guide search.

## What Gets Indexed

Each page contributes:

- a title
- full searchable body text
- guide id
- page id
- language metadata

### Title Source

GuideNH chooses the title in this order:

1. `frontmatter.navigation.title`
2. the first `# Heading`
3. the page resource id

### Body Source

The searchable body is built from parsed markdown and from custom tags that explicitly contribute index text.

Examples:

- normal markdown paragraphs are indexed
- headings are indexed
- tooltip labels and link titles may be indexed
- command link titles are indexed
- search can match body text, not only titles

## Result Layout

The in-game search page currently shows:

- the page icon when one exists
- the page title in blue
- the page resource path aligned on the right
- a highlighted text snippet from the body

When the query is empty, the page shows a centered prompt message. When nothing matches, it shows a centered no-results message.

## Language Behavior

Search analyzers are chosen from the current Minecraft language when possible. Unknown languages fall back to English tokenization.

## Query Scope

By default, guide search returns up to 25 matches. The search page can also be restricted to a single guide internally when needed.

## Authoring Tips For Better Results

- Put the real human-readable page name in `navigation.title`.
- Start major pages with a meaningful `#` heading.
- Include important nouns in the body, not only in images or icons.
- Prefer stable item names and machine names in text if you want users to find the page.

## Example Search Targets

The built-in example guide is a good search smoke-test because it includes:

- markdown syntax examples
- recipe pages
- structure previews
- image assets
- 3D scene examples

See [Examples](Examples) for the exact runtime files.
