package com.hfstudio.guidenh.integration.api.client;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

public interface QuestHoverProvider {

    boolean isQuestHoverAvailable();

    @Nullable
    UUID currentHoveredQuestId();

    @Nullable
    PageAnchor findQuestHoverPage(MutableGuide guide, UUID questId);
}
