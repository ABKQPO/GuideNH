package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.util.NavigationUtil;

public class MediaWikiSpecialPageModels {

    private MediaWikiSpecialPageModels() {}

    public static MediaWikiSpecialPageResult empty(MediaWikiSpecialDefinition definition) {
        return new MediaWikiSpecialPageResult(
            definition,
            definition.kind(),
            Collections.<MediaWikiSpecialListEntry>emptyList(),
            Collections.<MediaWikiSpecialGroupedEntry>emptyList(),
            false,
            true);
    }

    public static MediaWikiSpecialPageResult grid(MediaWikiSpecialDefinition definition,
        List<MediaWikiSpecialListEntry> allEntries, MediaWikiSpecialPageQuery query) {
        List<MediaWikiSpecialListEntry> filtered = MediaWikiSpecialSearchSupport
            .filterFlatEntries(allEntries, query.searchText());
        return new MediaWikiSpecialPageResult(
            definition,
            MediaWikiSpecialPageKind.GRID,
            MediaWikiSpecialSearchSupport.limit(filtered, query.visibleCount()),
            Collections.<MediaWikiSpecialGroupedEntry>emptyList(),
            MediaWikiSpecialSearchSupport.hasMore(filtered, query.visibleCount()),
            true);
    }

    public static MediaWikiSpecialPageResult flat(MediaWikiSpecialDefinition definition,
        List<MediaWikiSpecialListEntry> allEntries, MediaWikiSpecialPageQuery query) {
        List<MediaWikiSpecialListEntry> filtered = MediaWikiSpecialSearchSupport
            .filterFlatEntries(allEntries, query.searchText());
        return new MediaWikiSpecialPageResult(
            definition,
            MediaWikiSpecialPageKind.FLAT,
            MediaWikiSpecialSearchSupport.limit(filtered, query.visibleCount()),
            Collections.<MediaWikiSpecialGroupedEntry>emptyList(),
            MediaWikiSpecialSearchSupport.hasMore(filtered, query.visibleCount()),
            true);
    }

    public static MediaWikiSpecialPageResult grouped(MediaWikiSpecialDefinition definition,
        List<MediaWikiSpecialGroupedEntry> allEntries, MediaWikiSpecialPageQuery query) {
        List<MediaWikiSpecialGroupedEntry> filtered = MediaWikiSpecialSearchSupport
            .filterGroupedEntries(allEntries, query.searchText());
        return new MediaWikiSpecialPageResult(
            definition,
            MediaWikiSpecialPageKind.GROUPED,
            Collections.<MediaWikiSpecialListEntry>emptyList(),
            MediaWikiSpecialSearchSupport.limit(filtered, query.visibleCount()),
            MediaWikiSpecialSearchSupport.hasMore(filtered, query.visibleCount()),
            true);
    }

    public static MediaWikiSpecialPageResult info(MediaWikiSpecialDefinition definition, String message) {
        return new MediaWikiSpecialPageResult(
            definition,
            definition.kind(),
            Collections.singletonList(new MediaWikiSpecialListEntry(message, "", message, null, null)),
            Collections.<MediaWikiSpecialGroupedEntry>emptyList(),
            false,
            false);
    }

    public static MediaWikiSpecialPageResult groupIndex(MediaWikiSpecialDefinition definition,
        List<MediaWikiSpecialGroupedEntry> groupedEntries) {
        return new MediaWikiSpecialPageResult(
            definition,
            MediaWikiSpecialPageKind.GROUP_INDEX,
            Collections.<MediaWikiSpecialListEntry>emptyList(),
            groupedEntries,
            false,
            true);
    }

    public static List<MediaWikiSpecialGroupedEntry> groupCatalogEntries(String namespace) {
        ArrayList<MediaWikiSpecialGroupedEntry> grouped = new ArrayList<>();
        for (MediaWikiSpecialGroup group : MediaWikiSpecialCatalog.groups()) {
            ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
            for (MediaWikiSpecialDefinition definition : MediaWikiSpecialCatalog.definitions()) {
                if (MediaWikiSpecialPageIds.SPECIAL_PAGES.equals(definition.name()) || !group.name()
                    .equals(definition.groupName())) {
                    continue;
                }
                entries.add(
                    new MediaWikiSpecialListEntry(
                        MediaWikiPageTitleResolver.resolveSpecialListTitle(definition.name()),
                        "",
                        definition.name() + " " + definition.titleKey(),
                        MediaWikiPageIds.specialPageId(namespace, definition.name()),
                        null,
                        null));
            }
            grouped.add(
                new MediaWikiSpecialGroupedEntry(
                    MediaWikiPageTitleResolver.resolveSpecialGroupTitle(group),
                    "",
                    group.titleKey(),
                    entries));
        }
        return grouped;
    }

    public static List<MediaWikiSpecialListEntry> normalPageEntries(Iterable<ParsedGuidePage> pages) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        if (pages == null) {
            return entries;
        }
        for (ParsedGuidePage page : pages) {
            if (page == null || MediaWikiPageIds.isSyntheticPage(page.getId())) {
                continue;
            }
            entries.add(pageEntry(page.getId(), pageTitle(page), "", pageIcon(page)));
        }
        return entries;
    }

    public static MediaWikiSpecialListEntry pageEntry(ResourceLocation pageId, String title, String searchBlob) {
        return pageEntry(pageId, title, searchBlob, null);
    }

    public static MediaWikiSpecialListEntry pageEntry(ResourceLocation pageId, String title, String searchBlob,
        GuidePageIcon icon) {
        return new MediaWikiSpecialListEntry(
            title,
            pageId != null ? pageId.toString() : "",
            searchBlob == null || searchBlob.isEmpty() ? title : searchBlob,
            pageId,
            null,
            icon);
    }

    private static String pageTitle(ParsedGuidePage page) {
        FrontmatterNavigation navigation = page != null && page.getFrontmatter() != null ? page.getFrontmatter()
            .navigationEntry() : null;
        if (navigation == null || navigation.title() == null
            || navigation.title()
                .trim()
                .isEmpty()) {
            return page != null ? page.getId()
                .toString() : "";
        }
        return navigation.title()
            .trim();
    }

    private static GuidePageIcon pageIcon(ParsedGuidePage page) {
        FrontmatterNavigation navigation = page != null && page.getFrontmatter() != null ? page.getFrontmatter()
            .navigationEntry() : null;
        return navigation != null ? NavigationUtil.createNavigationIcon(page) : null;
    }
}
