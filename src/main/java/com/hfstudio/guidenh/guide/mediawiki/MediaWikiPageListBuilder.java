package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;
import com.hfstudio.guidenh.guide.internal.util.NavigationUtil;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;

public class MediaWikiPageListBuilder {

    private MediaWikiPageListBuilder() {}

    public static List<MediaWikiListEntry> buildAllPages(MediaWikiListContext context) {
        var entries = new ArrayList<MediaWikiListEntry>();
        for (ParsedGuidePage page : context.pages()) {
            if (MediaWikiPageIds.isSyntheticPage(page.getId())) {
                continue;
            }
            entries.add(buildPageEntry(context, page, null));
        }
        return MediaWikiListPlanner.sortEntries(entries);
    }

    public static List<MediaWikiListEntry> buildCategories(MediaWikiListContext context) {
        var entries = new ArrayList<MediaWikiListEntry>();
        for (String categoryName : context.categoryIndex()
            .getCategoryNames()) {
            ResourceLocation pageId = MediaWikiPageIds.categoryPageId(
                context.guide()
                    .getDefaultNamespace(),
                categoryName);
            entries.add(
                new MediaWikiListEntry(pageId, MediaWikiPageIds.toCategoryTitle(categoryName), null, categoryName));
        }
        return MediaWikiListPlanner.sortEntries(entries);
    }

    public static List<MediaWikiListEntry> buildCategoryMembers(MediaWikiListContext context, String categoryName) {
        Set<ResourceLocation> allowedPageIds = context.pageIds();
        var entries = new ArrayList<MediaWikiListEntry>();
        for (MediaWikiCategoryMember member : context.categoryIndex()
            .getMembers(categoryName)) {
            ResourceLocation pageId = member.pageAnchor()
                .pageId();
            if (!allowedPageIds.contains(pageId)) {
                continue;
            }

            ParsedGuidePage page = context.getParsedPage(
                member.pageAnchor()
                    .pageId());
            if (page == null || MediaWikiPageIds.isSyntheticPage(page.getId())) {
                continue;
            }
            entries.add(buildPageEntry(context, page, member.sortKey()));
        }
        return MediaWikiListPlanner.sortEntries(entries);
    }

    private static MediaWikiListEntry buildPageEntry(MediaWikiListContext context, ParsedGuidePage page,
        @Nullable String sortKey) {
        String title = GuideSearch.getPageTitle(context.guide(), page);
        GuidePageIcon icon = resolveIcon(context, page);
        return new MediaWikiListEntry(page.getId(), title, icon, sortKey != null ? sortKey : title);
    }

    @Nullable
    private static GuidePageIcon resolveIcon(MediaWikiListContext context, ParsedGuidePage page) {
        NavigationNode node = context.navigationTree()
            .getNodeById(page.getId());
        if (node != null && node.icon() != null) {
            return node.icon();
        }
        return NavigationUtil.createNavigationIcon(page, context.guide());
    }
}
