package com.hfstudio.guidenh.integration.betterquesting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Per-player display classification of a BetterQuesting quest, decided by {@link BqHelpers}.
 * Kept free of any BetterQuesting type references so other modules can reference it without
 * triggering BQ class loading when BQ is absent.
 */
public class QuestState {

    private static final Map<String, QuestState> STATES = new LinkedHashMap<>();

    /** The quest exists, is shown by BetterQuesting for the player, and is not yet completed. */
    public static final QuestState VISIBLE = register("guidenh:betterquesting_visible");
    /** The quest exists and the player has completed it. */
    public static final QuestState COMPLETED = register("guidenh:betterquesting_completed");
    /** The quest exists but is not unlocked; visibility allows showing it as a locked placeholder. */
    public static final QuestState LOCKED = register("guidenh:betterquesting_locked");
    /** The quest exists and is hidden from the player (visibility == HIDDEN/SECRET while locked). */
    public static final QuestState HIDDEN = register("guidenh:betterquesting_hidden");
    /** The UUID does not resolve to any known quest in the database. */
    public static final QuestState MISSING = register("guidenh:betterquesting_missing");

    private final String id;

    public QuestState(String id) {
        this.id = normalizeId(id);
    }

    public static synchronized QuestState register(String id) {
        String normalizedId = normalizeId(id);
        QuestState existing = STATES.get(normalizedId);
        if (existing != null) {
            return existing;
        }
        QuestState state = new QuestState(normalizedId);
        STATES.put(normalizedId, state);
        return state;
    }

    @Nullable
    public static synchronized QuestState state(@Nullable String id) {
        String normalizedId = normalizeIdOrNull(id);
        return normalizedId != null ? STATES.get(normalizedId) : null;
    }

    public static synchronized Map<String, QuestState> states() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(STATES));
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    public static String normalizeId(String id) {
        String normalizedId = normalizeIdOrNull(id);
        if (normalizedId == null) {
            throw new IllegalArgumentException("id");
        }
        return normalizedId;
    }

    @Nullable
    public static String normalizeIdOrNull(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
