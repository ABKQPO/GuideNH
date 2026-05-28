package com.hfstudio.guidenh.guide.internal.home;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideBookmarkState;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class HomePageDataBuilder {

    private final HomePageSummaryExtractor summaryExtractor = new HomePageSummaryExtractor();
    @Nullable
    private NavigationTree cachedNavigationTree;
    @Nullable
    private String cachedLanguage;
    private int cachedBookmarkVersion = Integer.MIN_VALUE;
    private int cachedHistoryVersion = Integer.MIN_VALUE;
    private int cachedRecommendedLimit = Integer.MIN_VALUE;
    private int cachedBookmarkedLimit = Integer.MIN_VALUE;
    private int cachedHistoryLimit = Integer.MIN_VALUE;
    @Nullable
    private HomePageSections cachedSections;

    public HomePageSections build(GuideBookmarkState bookmarkState, GuideScreenHomeHistory history) {
        NavigationTree navigationTree = GuideRegistry.getMergedNavigationTree();
        String language = LangUtil.getCurrentLanguage();
        int bookmarkVersion = bookmarkState.version();
        int historyVersion = history.version();
        int recommendedLimit = ModConfig.clampPositiveHomeLimit(ModConfig.ui.homeRecommendedPageLimit, 30);
        int bookmarkedLimit = ModConfig.clampPositiveHomeLimit(ModConfig.ui.homeBookmarkedPageLimit, 10);
        int historyLimit = ModConfig.clampPositiveHomeLimit(ModConfig.ui.homeHistoryPageLimit, 10);

        if (cachedSections != null && cachedNavigationTree == navigationTree
            && language.equals(cachedLanguage)
            && cachedBookmarkVersion == bookmarkVersion
            && cachedHistoryVersion == historyVersion
            && cachedRecommendedLimit == recommendedLimit
            && cachedBookmarkedLimit == bookmarkedLimit
            && cachedHistoryLimit == historyLimit) {
            return cachedSections;
        }

        PageLookup pageLookup = buildPageLookup();
        HomePageSections sections = new HomePageSections(
            new HomePageSection(
                HomePageSection.Kind.RECOMMENDED,
                GuidebookText.HomeRecommended.text(),
                GuidebookText.HomeEmpty.text(),
                buildRecommendedEntries(navigationTree, pageLookup, recommendedLimit)),
            new HomePageSection(
                HomePageSection.Kind.BOOKMARKS,
                GuidebookText.Bookmarks.text(),
                GuidebookText.HomeEmpty.text(),
                buildBookmarkedEntries(bookmarkState, navigationTree, pageLookup, bookmarkedLimit)),
            new HomePageSection(
                HomePageSection.Kind.HISTORY,
                GuidebookText.HomeHistory.text(),
                GuidebookText.HomeEmpty.text(),
                buildHistoryEntries(history, navigationTree, pageLookup, historyLimit)));
        cachedNavigationTree = navigationTree;
        cachedLanguage = language;
        cachedBookmarkVersion = bookmarkVersion;
        cachedHistoryVersion = historyVersion;
        cachedRecommendedLimit = recommendedLimit;
        cachedBookmarkedLimit = bookmarkedLimit;
        cachedHistoryLimit = historyLimit;
        cachedSections = sections;
        return sections;
    }

    private List<HomePageEntry> buildRecommendedEntries(NavigationTree navigationTree, PageLookup pageLookup,
        int limit) {
        List<HomePageEntry> entries = new ArrayList<>();
        for (PageReference pageReference : pageLookup.pages()) {
            ParsedGuidePage page = pageReference.page();
            FrontmatterNavigation navigation = page.getFrontmatter()
                .navigationEntry();
            if (navigation == null || navigation.recommend() == Frontmatter.NAVIGATION_RECOMMEND_ABSENT) {
                continue;
            }
            HomePageEntry entry = toEntry(pageReference.guide(), page.getId(), navigation.recommend(), navigationTree);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries.sort(
            Comparator.comparingInt(HomePageEntry::recommend)
                .reversed()
                .thenComparing(HomePageEntry::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                    entry -> entry.pageId()
                        .toString()));
        return limit(entries, limit);
    }

    private List<HomePageEntry> buildBookmarkedEntries(GuideBookmarkState bookmarkState, NavigationTree navigationTree,
        PageLookup pageLookup, int limit) {
        List<HomePageEntry> entries = new ArrayList<>();
        for (ResourceLocation pageId : bookmarkState.getBookmarksView()) {
            HomePageEntry entry = toEntry(
                pageLookup.guideByPageId()
                    .get(pageId),
                pageId,
                Frontmatter.NAVIGATION_RECOMMEND_ABSENT,
                navigationTree);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return limit(entries, limit);
    }

    private List<HomePageEntry> buildHistoryEntries(GuideScreenHomeHistory history, NavigationTree navigationTree,
        PageLookup pageLookup, int limit) {
        List<HomePageEntry> entries = new ArrayList<>();
        for (GuideScreenHomeHistory.Entry historyEntry : history.snapshot()) {
            HomePageEntry entry = toEntry(
                pageLookup.guideByGuideId()
                    .get(historyEntry.guideId()),
                historyEntry.pageId(),
                Frontmatter.NAVIGATION_RECOMMEND_ABSENT,
                navigationTree);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return limit(entries, limit);
    }

    @Nullable
    private HomePageEntry toEntry(@Nullable MutableGuide guide, ResourceLocation pageId, int recommend,
        NavigationTree navigationTree) {
        if (guide == null || pageId == null || !guide.pageExists(pageId)) {
            return null;
        }

        ParsedGuidePage page = guide.getParsedPage(pageId);
        if (page == null) {
            return null;
        }

        NavigationNode node = resolveNavigationNode(navigationTree, guide, pageId);
        GuidePageIcon icon = node != null ? node.icon() : null;
        String title = resolveTitle(page, pageId, node);
        return new HomePageEntry(
            guide.getId(),
            pageId,
            PageAnchor.page(pageId),
            title,
            summaryExtractor.extract(page),
            icon,
            recommend);
    }

    private String resolveTitle(ParsedGuidePage page, ResourceLocation pageId, @Nullable NavigationNode node) {
        if (node != null && node.title() != null
            && !node.title()
                .trim()
                .isEmpty()) {
            return node.title();
        }
        String headingText = summaryExtractor.extractHeadingText(page);
        if (!headingText.isEmpty()) {
            return headingText;
        }
        return pageId.toString();
    }

    @Nullable
    private NavigationNode resolveNavigationNode(NavigationTree navigationTree, MutableGuide guide,
        ResourceLocation pageId) {
        NavigationNode node = navigationTree.getNodeById(pageId);
        if (node == null || node.guideId() == null
            || node.guideId()
                .equals(guide.getId())) {
            return node;
        }
        return null;
    }

    private PageLookup buildPageLookup() {
        Map<ResourceLocation, MutableGuide> guideByPageId = new HashMap<>();
        Map<ResourceLocation, MutableGuide> guideByGuideId = new HashMap<>();
        Set<ResourceLocation> ambiguousPageIds = new HashSet<>();
        List<PageReference> pages = new ArrayList<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            guideByGuideId.put(guide.getId(), guide);
            for (ParsedGuidePage page : guide.getPages()) {
                if (!ambiguousPageIds.contains(page.getId())) {
                    MutableGuide existingGuide = guideByPageId.putIfAbsent(page.getId(), guide);
                    if (existingGuide != null && existingGuide != guide) {
                        guideByPageId.remove(page.getId());
                        ambiguousPageIds.add(page.getId());
                    }
                }
                pages.add(new PageReference(guide, page));
            }
        }
        return new PageLookup(guideByGuideId, guideByPageId, pages);
    }

    private List<HomePageEntry> limit(List<HomePageEntry> entries, int limit) {
        if (entries.size() <= limit) {
            return entries;
        }
        return new ArrayList<>(entries.subList(0, limit));
    }

    public static class HomePageSections {

        private final HomePageSection recommended;
        private final HomePageSection bookmarks;
        private final HomePageSection history;

        public HomePageSections(HomePageSection recommended, HomePageSection bookmarks, HomePageSection history) {
            this.recommended = recommended;
            this.bookmarks = bookmarks;
            this.history = history;
        }

        public HomePageSection recommended() {
            return recommended;
        }

        public HomePageSection bookmarks() {
            return bookmarks;
        }

        public HomePageSection history() {
            return history;
        }
    }

    private static class PageLookup {

        private final Map<ResourceLocation, MutableGuide> guideByGuideId;
        private final Map<ResourceLocation, MutableGuide> guideByPageId;
        private final List<PageReference> pages;

        private PageLookup(Map<ResourceLocation, MutableGuide> guideByGuideId,
            Map<ResourceLocation, MutableGuide> guideByPageId, List<PageReference> pages) {
            this.guideByGuideId = guideByGuideId;
            this.guideByPageId = guideByPageId;
            this.pages = pages;
        }

        private Map<ResourceLocation, MutableGuide> guideByGuideId() {
            return guideByGuideId;
        }

        private Map<ResourceLocation, MutableGuide> guideByPageId() {
            return guideByPageId;
        }

        private List<PageReference> pages() {
            return pages;
        }
    }

    private static class PageReference {

        private final MutableGuide guide;
        private final ParsedGuidePage page;

        private PageReference(MutableGuide guide, ParsedGuidePage page) {
            this.guide = guide;
            this.page = page;
        }

        private MutableGuide guide() {
            return guide;
        }

        private ParsedGuidePage page() {
            return page;
        }
    }
}
