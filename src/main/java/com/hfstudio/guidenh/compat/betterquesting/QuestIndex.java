package com.hfstudio.guidenh.compat.betterquesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.UniqueIndex;

/**
 * An index of BetterQuesting quest UUIDs to the main guidebook page describing them.
 * <p/>
 * The {@code quest_ids} frontmatter list contains plain UUID strings. When the player views a
 * quest in the BetterQuesting GUI, holding the open-guide hotkey will look up this index to
 * navigate to the appropriate page.
 * <p/>
 * The index itself is independent of BetterQuesting: it only stores {@link UUID} keys and never
 * touches any BQ types. This keeps the class safe to load even when BetterQuesting is absent.
 */
public class QuestIndex extends UniqueIndex<UUID, PageAnchor> {

    public static final Logger LOG = LoggerFactory.getLogger(QuestIndex.class);

    public QuestIndex() {
        super(
            "Quest Index",
            QuestIndex::getQuestAnchors,
            (writer, value) -> writer.value(value.toString()),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Returns the page anchor for the given quest UUID, or {@code null} if no page indexes it.
     */
    @Nullable
    public PageAnchor findByUuid(@Nullable UUID questId) {
        if (questId == null) return null;
        return get(questId);
    }

    /**
     * Convenience overload that parses the UUID string. Invalid UUIDs return {@code null}.
     */
    @Nullable
    public PageAnchor findByUuidString(@Nullable String questId) {
        if (questId == null || questId.isEmpty()) return null;
        UUID parsed;
        try {
            parsed = UUID.fromString(questId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return get(parsed);
    }

    public static List<Pair<UUID, PageAnchor>> getQuestAnchors(ParsedGuidePage page) {
        var questIdsNode = page.getFrontmatter()
            .additionalProperties()
            .get("quest_ids");
        if (questIdsNode == null) {
            return Collections.emptyList();
        }

        if (!(questIdsNode instanceof List<?>questIdList)) {
            LOG.warn("Page {} contains malformed quest_ids frontmatter", page.getId());
            return Collections.emptyList();
        }

        ResourceLocation pageId = page.getId();
        var anchors = new ArrayList<Pair<UUID, PageAnchor>>();

        for (var listEntry : questIdList) {
            if (listEntry instanceof String questIdStr) {
                String trimmed = questIdStr.trim();
                if (trimmed.isEmpty()) {
                    LOG.warn("Page {} contains an empty quest_ids frontmatter entry", pageId);
                    continue;
                }
                UUID parsed;
                try {
                    parsed = UUID.fromString(trimmed);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Page {} contains a malformed quest_ids frontmatter entry: {}", pageId, trimmed);
                    continue;
                }
                anchors.add(Pair.of(parsed, new PageAnchor(pageId, null)));
            } else {
                LOG.warn("Page {} contains a malformed quest_ids frontmatter entry: {}", pageId, listEntry);
            }
        }

        return anchors;
    }
}
