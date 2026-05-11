package com.hfstudio.guidenh.integration.betterquesting;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.client.QuestHoverProvider;

public class BetterQuestingQuestHoverProvider implements QuestHoverProvider {

    @Override
    public boolean isQuestHoverAvailable() {
        return Mods.BetterQuesting.isModLoaded();
    }

    @Override
    public @Nullable UUID currentHoveredQuestId() {
        return BqCompat.getCurrentHoveredQuestUuid();
    }

    @Override
    public @Nullable PageAnchor findQuestHoverPage(MutableGuide guide, UUID questId) {
        try {
            return guide.getIndex(QuestIndex.class)
                .findByUuid(questId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
