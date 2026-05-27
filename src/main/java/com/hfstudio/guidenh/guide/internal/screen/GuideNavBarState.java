package com.hfstudio.guidenh.guide.internal.screen;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

public class GuideNavBarState {

    private static final GuideNavBarState DEFAULT_STATE = new GuideNavBarState(true, Collections.emptySet());

    private final boolean bookmarkGroupExpanded;
    private final Set<ResourceLocation> expandedPageIds;

    public GuideNavBarState(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds) {
        this.bookmarkGroupExpanded = bookmarkGroupExpanded;
        this.expandedPageIds = Collections.unmodifiableSet(
            expandedPageIds == null ? new LinkedHashSet<ResourceLocation>()
                : new LinkedHashSet<ResourceLocation>(expandedPageIds));
    }

    public static GuideNavBarState create(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds) {
        return new GuideNavBarState(bookmarkGroupExpanded, expandedPageIds);
    }

    public static GuideNavBarState defaultState() {
        return DEFAULT_STATE;
    }

    public boolean bookmarkGroupExpanded() {
        return bookmarkGroupExpanded;
    }

    public Set<ResourceLocation> expandedPageIds() {
        return expandedPageIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GuideNavBarState other)) {
            return false;
        }
        return bookmarkGroupExpanded == other.bookmarkGroupExpanded && expandedPageIds.equals(other.expandedPageIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkGroupExpanded, expandedPageIds);
    }
}
