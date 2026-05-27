package com.hfstudio.guidenh.guide.internal;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;
import com.hfstudio.guidenh.guide.internal.search.GuideItemLinksPage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchPage;

public class GuideScreenMemory {

    @Nullable
    private static GuideScreenViewState lastContentViewState;
    @Nullable
    private static GuideNavBarState navigationState;

    private GuideScreenMemory() {}

    public static void clear() {
        lastContentViewState = null;
        navigationState = null;
    }

    private static void clearContentState() {
        lastContentViewState = null;
    }

    public static void rememberContentState(@Nullable GuideScreenViewState state) {
        if (!isRememberable(state)) {
            return;
        }
        lastContentViewState = state;
    }

    @Nullable
    public static GuideScreenViewState recallLastContentState() {
        return lastContentViewState;
    }

    @Nullable
    public static GuideScreenViewState consumeValidLastContentState() {
        GuideScreenViewState state = lastContentViewState;
        if (state == null) {
            return null;
        }
        if (!isValidContentRoute(state.route())) {
            clearContentState();
            return null;
        }
        return state;
    }

    public static void rememberNavigationState(@Nullable GuideNavBarState state) {
        navigationState = state;
    }

    public static GuideNavBarState recallNavigationState() {
        return navigationState != null ? navigationState : GuideNavBarState.defaultState();
    }

    public static boolean isRememberable(@Nullable GuideScreenViewState state) {
        if (state == null) {
            return false;
        }
        GuideScreenRoute route = state.route();
        if (route == null || !route.isContent()) {
            return false;
        }
        PageAnchor anchor = route.anchor();
        return anchor != null && isSupportedContentAnchor(anchor) && isValidContentRoute(route);
    }

    public static boolean isSupportedContentAnchor(@Nullable PageAnchor anchor) {
        return anchor != null && !GuideSearchPage.isSearchAnchor(anchor)
            && !GuideItemLinksPage.isItemLinksAnchor(anchor);
    }

    public static boolean isValidContentRoute(@Nullable GuideScreenRoute route) {
        if (route == null || !route.isContent()) {
            return false;
        }
        ResourceLocation guideId = route.guideId();
        PageAnchor anchor = route.anchor();
        if (guideId == null || anchor == null) {
            return false;
        }
        MutableGuide guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(anchor.pageId());
    }
}
