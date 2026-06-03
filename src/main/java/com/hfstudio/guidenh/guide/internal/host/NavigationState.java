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
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreenRoute;
import com.hfstudio.guidenh.guide.internal.GuideScreenViewState;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;

public class NavigationState {

    @Nullable
    private ResourceLocation currentGuideId;
    @Nullable
    private PageAnchor currentAnchor;

    private final Deque<GuideScreenViewState> backStack = new ArrayDeque<>();

    @Nullable
    private GuideScreenViewState lastContentViewState;
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

    @Nullable
    public ResourceLocation currentGuideId() {
        return currentGuideId;
    }

    @Nullable
    public PageAnchor currentAnchor() {
        return currentAnchor;
    }

    public void pushHistory(GuideScreenViewState state) {
        backStack.push(state);
    }

    @Nullable
    public GuideScreenViewState popHistory() {
        return backStack.pollFirst();
    }

    public Deque<GuideScreenViewState> backStack() {
        return backStack;
    }

    public void rememberContentState(@Nullable GuideScreenViewState state) {
        lastContentViewState = state;
    }

    @Nullable
    public GuideScreenViewState recallLastContentState() {
        return lastContentViewState;
    }

    public void rememberNavBarState(ResourceLocation guideId, GuideNavBarState state) {
        if (state != null) navBarStates.put(guideId, state);
    }

    @Nullable
    public GuideNavBarState recallNavBarState(ResourceLocation guideId) {
        return navBarStates.get(guideId);
    }

    public boolean isBookmarked(ResourceLocation pageId) {
        return bookmarks.contains(pageId);
    }

    public void toggleBookmark(ResourceLocation pageId) {
        if (!bookmarks.remove(pageId)) {
            bookmarks.add(pageId);
        }
    }

    public Set<ResourceLocation> bookmarks() {
        return bookmarks;
    }

    public void recordHomeHistory(ResourceLocation guideId, ResourceLocation pageId) {
        homeHistory.add(0, new HomeHistoryEntry(guideId, pageId));
    }

    public List<HomeHistoryEntry> homeHistory() {
        return homeHistory;
    }

    public GuideNavBarState recallNavigationState() {
        GuideNavBarState currentGuideState = currentGuideId != null ? navBarStates.get(currentGuideId) : null;
        return currentGuideState != null ? currentGuideState : GuideNavBarState.defaultState();
    }

    @Nullable
    public GuideScreenViewState consumeValidLastContentState() {
        GuideScreenViewState state = lastContentViewState;
        if (state == null) return null;
        if (!isValidContentRoute(state.route())) {
            lastContentViewState = null;
            return null;
        }
        return state;
    }

    public boolean isRememberable(@Nullable GuideScreenViewState state) {
        if (state == null) return false;
        GuideScreenRoute route = state.route();
        if (route == null || !route.isContent()) return false;
        PageAnchor anchor = route.anchor();
        return anchor != null && isSupportedContentAnchor(anchor) && isValidContentRoute(route);
    }

    public static boolean isSupportedContentAnchor(@Nullable PageAnchor anchor) {
        return anchor != null;
    }

    public boolean isValidContentRoute(@Nullable GuideScreenRoute route) {
        if (route == null || !route.isContent()) return false;
        ResourceLocation guideId = route.guideId();
        PageAnchor anchor = route.anchor();
        if (guideId == null || anchor == null) return false;
        MutableGuide guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(anchor.pageId());
    }

    public void clear() {
        backStack.clear();
        lastContentViewState = null;
        navBarStates.clear();
        bookmarks.clear();
        homeHistory.clear();
    }
}
