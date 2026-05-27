package com.hfstudio.guidenh.integration.betterquesting;

import org.jetbrains.annotations.Nullable;

/**
 * A snapshot of a quest's display attributes for the current player, produced by {@link BqHelpers}.
 * This class does not reference any BetterQuesting types directly so it is safe to load when BQ
 * is absent.
 */
public class QuestDisplay {

    private final QuestState state;
    @Nullable
    private final String name;
    @Nullable
    private final String description;

    public QuestDisplay(QuestState state, @Nullable String name, @Nullable String description) {
        this.state = state;
        this.name = name;
        this.description = description;
    }

    public QuestState getState() {
        return state;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }
}
