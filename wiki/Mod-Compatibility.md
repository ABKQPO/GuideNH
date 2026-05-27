# Mod Compatibility

GuideNH ships with conditional integrations for selected mods. Each integration is only activated when its target mod is loaded; when the target is absent, the related tags, indices and key bindings stay inert and the rest of the guide keeps working.

## StructureLib

When StructureLib is loaded, GuideNH can import multiblock previews into `<GameScene>` with `<ImportStructureLib>`. The client command `/exportStructure structureLib` can also export those previews as PNG documentation screenshots. See [Structure Export](Structure-Export) for the full command reference, StructureLib-specific options, and the related `gameScene` export mode.

## BetterQuesting

When [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) is loaded, GuideNH unlocks three features:

1. The `quest_ids` page-frontmatter key starts indexing pages by BetterQuesting quest id.
2. Two new tags become available: `<QuestLink>` (inline) and `<QuestCard>` (block).
3. The standard "open guide" hotkey works while hovering a quest in the BetterQuesting GUI: holding the key looks up the hovered quest id and navigates to the matching guide page.

### Indexing pages by quest id

Add a `quest_ids` list to the frontmatter of any guide page you want associated with one or more quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

The values may be canonical UUID strings or BetterQuesting compact Base64 quest ids. Malformed or empty entries are skipped with a warning in the log.

Compact ids are decoded first, then GuideNH falls back to canonical UUID parsing. This matches BetterQuesting's compact quest-id format such as `AAAAAAAAAAAAAAAAAAAMug==`.

Do not list both encodings for the same quest in one page's `quest_ids`; they normalize to the same internal UUID and would be treated as duplicates.

When a quest id is indexed by a page, both `<QuestLink>` and `<QuestCard>` will route the click to that guide page instead of opening the BetterQuesting quest GUI directly.

### `<QuestLink>` and `<QuestCard>`

Both tags accept a BetterQuesting quest id via `id` and decide their appearance from the player's progress at compile time:

| State | Source | Rendering |
| --- | --- | --- |
| Visible | quest is unlocked but not completed | clickable link, default style |
| Completed | `quest.isComplete(player)` returns true | clickable link, green color, trailing `✓` |
| Locked | quest exists but is not unlocked, visibility ≠ HIDDEN/SECRET | italic gray placeholder, not clickable |
| Hidden | locked plus visibility is HIDDEN or SECRET | italic dark-gray placeholder, no quest details leaked |
| Missing | quest id does not resolve to any quest in the database | italic red placeholder |

Locked but non-hidden quests are also clickable. They use the same navigation target rules as visible and completed quests.

For visible / completed / locked quests, the click target is:

- the indexed guide page if the quest id is present in some page's `quest_ids`
- otherwise BetterQuesting's quest-book quest screen, using BetterQuesting's native parent-screen flow

See [Tags Reference](Tags-Reference#questlink) for attribute tables and inline examples.

### Hidden-quest handling

GuideNH never renders the title or description of a quest whose visibility is `HIDDEN` or `SECRET` while the quest is still locked for the player. The placeholder text is taken from a translation key so packs can localize the wording:

| Translation key | Default (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests can still show their description as a hover tooltip on `<QuestLink>` and on the clickable title inside `<QuestCard>`, because BetterQuesting itself reveals the description on locked quest tooltips. Set `show_tooltip="false"` (or `showTooltip={false}`) to suppress that tooltip. Hidden quests do not expose any tooltip.

> [!NOTE]
> Quest state is resolved at page-compile time using the local player's progress. The compiled page is cached per guide; reopening the guide after completing or unlocking a quest re-evaluates the state.

### Open-guide hotkey integration

The default open-guide hotkey (`G`, configurable under `key.guidenh.open_guide`) gains a second activation path when BetterQuesting is loaded. While the BetterQuesting quest-line GUI is open:

1. Hover over any quest button in the BQ panel
2. Hold the open-guide hotkey

If any registered guide indexes that quest id through `quest_ids`, GuideNH will navigate to the matching page (or open the guide if it's not already open). If no page indexes the hovered id, the hotkey does nothing — it does not fall back to opening the BetterQuesting quest GUI, since BQ already shows that information.

This path is independent of the inventory item-tooltip path, so hovering items in your inventory still routes through the existing item / ore index lookups.

### Behavior when BetterQuesting is absent

- `<QuestLink>` and `<QuestCard>` are not registered, so pages that use them fall back to the standard "unknown tag" error rendering until you remove the tag.
- `quest_ids` frontmatter entries are still parsed and stored under `additionalProperties`, but nothing reads them.
- The hotkey's quest-hover branch becomes a no-op.

This means a guide that targets BetterQuesting can be authored once and silently degrade in environments where BetterQuesting is not installed.
