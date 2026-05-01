package com.hfstudio.guidenh.compat.betterquesting;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.betterquesting.compiler.QuestCardCompiler;
import com.hfstudio.guidenh.compat.betterquesting.compiler.QuestLinkCompiler;
import com.hfstudio.guidenh.guide.GuideBuilder;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;

/**
 * BetterQuesting integration entry point.
 * <p/>
 * This class never references BetterQuesting types directly; all such access is funneled
 * through {@link BqHelpers}. That keeps {@code BqCompat} safe to load even when BetterQuesting
 * is missing from the classpath, so it can be statically referenced from the rest of the mod.
 */
public final class BqCompat {

    @Nullable
    private static volatile UUID currentHoveredQuestUuid;

    private BqCompat() {}

    /**
     * Attaches the {@link QuestIndex} to the given guide builder. Safe to call when BQ is
     * absent because {@link QuestIndex} only depends on standard library types.
     */
    public static void attachQuestIndex(GuideBuilder builder) {
        builder.index(new QuestIndex());
    }

    /**
     * Appends BetterQuesting-aware tag compilers to the given mutable list of tag compilers.
     * The compilers themselves do not reference BQ types; they delegate runtime work to
     * {@link BqHelpers}.
     */
    public static void appendCompilers(List<TagCompiler> compilers) {
        compilers.add(new QuestLinkCompiler());
        compilers.add(new QuestCardCompiler());
    }

    /**
     * Returns the UUID of the BetterQuesting quest currently under the mouse cursor in the BQ
     * quest line GUI, or {@code null} if no quest is hovered. Updated by the BQ-targeted mixin
     * and consumed by the open-guide hotkey handler.
     */
    @Nullable
    public static UUID getCurrentHoveredQuestUuid() {
        return currentHoveredQuestUuid;
    }

    /**
     * Sets the UUID of the BetterQuesting quest currently under the mouse cursor. Intended for
     * use by the BQ-targeted mixin only. Pass {@code null} to clear.
     */
    public static void setCurrentHoveredQuestUuid(@Nullable UUID uuid) {
        currentHoveredQuestUuid = uuid;
    }
}
