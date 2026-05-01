package com.hfstudio.guidenh.compat.betterquesting;

/**
 * Per-player display classification of a BetterQuesting quest, decided by {@link BqHelpers}.
 * Kept free of any BetterQuesting type references so other modules can reference it without
 * triggering BQ class loading when BQ is absent.
 */
public enum QuestState {
    /** The quest exists, is unlocked for the player and not yet completed. */
    VISIBLE,
    /** The quest exists and the player has completed it. */
    COMPLETED,
    /** The quest exists but is not unlocked; visibility allows showing it as a locked placeholder. */
    LOCKED,
    /** The quest exists and is hidden from the player (visibility == HIDDEN/SECRET while locked). */
    HIDDEN,
    /** The UUID does not resolve to any known quest in the database. */
    MISSING
}
