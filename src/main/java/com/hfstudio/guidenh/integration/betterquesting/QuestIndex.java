package com.hfstudio.guidenh.integration.betterquesting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.UniqueIndex;

import cpw.mods.fml.common.FMLLog;

/**
 * An index of BetterQuesting quest ids to the main guidebook page describing them.
 * <p/>
 * The {@code quest_ids} frontmatter list accepts both canonical UUID strings and BetterQuesting's
 * compact Base64 quest-id encoding. When the player views a quest in the BetterQuesting GUI,
 * holding the open-guide hotkey will look up this index to navigate to the appropriate page.
 * <p/>
 * The index itself is independent of BetterQuesting: it only stores {@link UUID} keys and never
 * touches any BQ types. This keeps the class safe to load even when BetterQuesting is absent.
 */
public class QuestIndex extends UniqueIndex<UUID, PageAnchor> {

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
     * Convenience overload that parses supported BetterQuesting quest-id formats.
     */
    @Nullable
    public PageAnchor findByUuidString(@Nullable String questId) {
        UUID parsed = QuestIdParser.parse(questId);
        if (parsed == null) {
            return null;
        }
        return get(parsed);
    }

    public static List<Pair<UUID, PageAnchor>> getQuestAnchors(ParsedGuidePage page) {
        var questIdsNode = page.getFrontmatter()
            .additionalProperties()
            .get("quest_ids");
        if (questIdsNode == null) {
            return List.of();
        }

        if (!(questIdsNode instanceof List<?>questIdList)) {
            FMLLog.getLogger()
                .warn("[GuideNH] [QuestIndex] Page {} contains malformed quest_ids frontmatter", page.getId());
            return List.of();
        }

        ResourceLocation pageId = page.getId();
        var anchors = new ArrayList<Pair<UUID, PageAnchor>>();

        for (var listEntry : questIdList) {
            if (listEntry instanceof String questIdStr) {
                String trimmed = questIdStr.trim();
                if (trimmed.isEmpty()) {
                    FMLLog.getLogger()
                        .warn("[GuideNH] [QuestIndex] Page {} contains an empty quest_ids frontmatter entry", pageId);
                    continue;
                }
                UUID parsed = QuestIdParser.parse(trimmed);
                if (parsed == null) {
                    FMLLog.getLogger()
                        .warn(
                            "[GuideNH] [QuestIndex] Page {} contains a malformed quest_ids frontmatter entry: {}",
                            pageId,
                            trimmed);
                    continue;
                }
                anchors.add(Pair.of(parsed, new PageAnchor(pageId, null)));
            } else {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [QuestIndex] Page {} contains a malformed quest_ids frontmatter entry: {}",
                        pageId,
                        listEntry);
            }
        }

        return anchors;
    }
}
