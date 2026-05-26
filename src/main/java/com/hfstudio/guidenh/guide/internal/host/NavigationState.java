package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideScreenViewState;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;

public class NavigationState {

    @Nullable private ResourceLocation currentGuideId;
    @Nullable private PageAnchor currentAnchor;

    private final Deque<GuideScreenViewState> backStack = new ArrayDeque<>();

    @Nullable private GuideScreenViewState lastContentViewState;
    private final Map<ResourceLocation, GuideNavBarState> navBarStates = new LinkedHashMap<>();

    private final Set<ResourceLocation> bookmarks = new LinkedHashSet<>();

    private final List<HomeHistoryEntry> homeHistory = new ArrayList<>();

    public static class HomeHistoryEntry {
        public final ResourceLocation guideId;
        public final ResourceLocation pageId;
        public HomeHistoryEntry(ResourceLocation guideId, ResourceLocation pageId) {
            this.guideId = guideId;
            this.pageId = pageId;
        }
    }

    public void setCurrent(ResourceLocation guideId, PageAnchor anchor) {
        this.currentGuideId = guideId;
        this.currentAnchor = anchor;
    }

    @Nullable public ResourceLocation currentGuideId() { return currentGuideId; }
    @Nullable public PageAnchor currentAnchor() { return currentAnchor; }

    public void pushHistory(GuideScreenViewState state) { backStack.push(state); }
    @Nullable public GuideScreenViewState popHistory() { return backStack.pollFirst(); }
    public Deque<GuideScreenViewState> backStack() { return backStack; }

    public void rememberContentState(@Nullable GuideScreenViewState state) { lastContentViewState = state; }
    @Nullable public GuideScreenViewState recallLastContentState() { return lastContentViewState; }

    public void rememberNavBarState(ResourceLocation guideId, GuideNavBarState state) {
        if (state != null) navBarStates.put(guideId, state);
    }
    @Nullable public GuideNavBarState recallNavBarState(ResourceLocation guideId) {
        return navBarStates.get(guideId);
    }

    public boolean isBookmarked(ResourceLocation pageId) { return bookmarks.contains(pageId); }
    public void toggleBookmark(ResourceLocation pageId) {
        if (!bookmarks.remove(pageId)) { bookmarks.add(pageId); }
    }
    public Set<ResourceLocation> bookmarks() { return bookmarks; }

    public void recordHomeHistory(ResourceLocation guideId, ResourceLocation pageId) {
        homeHistory.add(0, new HomeHistoryEntry(guideId, pageId));
    }
    public List<HomeHistoryEntry> homeHistory() { return homeHistory; }

    public void clear() {
        backStack.clear();
        lastContentViewState = null;
        navBarStates.clear();
        bookmarks.clear();
        homeHistory.clear();
    }
}
