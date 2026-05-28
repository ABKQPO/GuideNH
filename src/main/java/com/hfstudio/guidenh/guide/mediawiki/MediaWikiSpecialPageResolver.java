package com.hfstudio.guidenh.guide.mediawiki;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideLightweightReloadService;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.internal.util.NavigationUtil;

public class MediaWikiSpecialPageResolver {

    private static final long ONE_KILOBYTE = 1024L;
    private static final long ONE_MEGABYTE = 1024L * 1024L;
    private static final DecimalFormat SIZE_DECIMAL_FORMAT = createSizeDecimalFormat();
    private final MediaWikiSpecialContributors contributors = new MediaWikiSpecialContributors();

    public boolean isSupported(String specialName) {
        return normalizeSupportedName(specialName) != null;
    }

    public @Nullable String normalizeSupportedName(String specialName) {
        MediaWikiSpecialDefinition definition = MediaWikiSpecialCatalog.findByName(specialName);
        return definition != null ? definition.name() : null;
    }

    public @Nullable MediaWikiSpecialDefinition findDefinition(String specialName) {
        String normalizedName = normalizeSupportedName(specialName);
        return normalizedName != null ? MediaWikiSpecialCatalog.findByName(normalizedName) : null;
    }

    public MediaWikiSpecialPageResult resolve(MediaWikiListContext context, String specialName,
        MediaWikiSpecialPageQuery query) {
        MediaWikiSpecialDefinition definition = findDefinition(specialName);
        if (definition == null) {
            return MediaWikiSpecialPageModels.info(
                new MediaWikiSpecialDefinition(
                    specialName,
                    "guidenh.mediawiki.special.unsupported",
                    "other",
                    MediaWikiSpecialPageKind.INFO,
                    false,
                    false,
                    MediaWikiSpecialPageQuery.PAGE_SIZE,
                    null),
                GuidebookText.MediaWikiUnsupportedSpecialPage.text(rawSpecialName(specialName)));
        }
        return resolve(context, definition, query);
    }

    public MediaWikiSpecialPageResult resolve(MediaWikiListContext context, MediaWikiSpecialDefinition definition,
        MediaWikiSpecialPageQuery query) {
        MediaWikiSpecialPageQuery effectiveQuery = query != null ? query : MediaWikiSpecialPageQuery.DEFAULT;
        return switch (definition.name()) {
            case MediaWikiSpecialPageIds.SPECIAL_PAGES -> buildSpecialPagesIndex(context, definition, effectiveQuery);
            case MediaWikiSpecialPageIds.ALL_PAGES -> MediaWikiSpecialPageModels
                .grid(definition, buildNormalPageEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.ALL_PAGES_WITH_PREFIX -> MediaWikiSpecialPageModels
                .grid(definition, buildAllPagesWithPrefixEntries(context, effectiveQuery), effectiveQuery);
            case MediaWikiSpecialPageIds.CATEGORIES -> MediaWikiSpecialPageModels
                .grid(definition, buildCategoryEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.CATEGORY_TREE -> MediaWikiSpecialPageModels
                .grouped(definition, buildCategoryTreeGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.LONG_PAGES -> MediaWikiSpecialPageModels
                .flat(definition, buildSizedPageEntries(context, true), effectiveQuery);
            case MediaWikiSpecialPageIds.SHORT_PAGES -> MediaWikiSpecialPageModels
                .flat(definition, buildSizedPageEntries(context, false), effectiveQuery);
            case MediaWikiSpecialPageIds.UNCATEGORIZED_PAGES -> MediaWikiSpecialPageModels
                .grid(definition, buildUncategorizedPageEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.PAGES_WITH_PAGE_PROPERTY -> MediaWikiSpecialPageModels
                .grid(definition, buildPagesWithProperties(context), effectiveQuery);
            case MediaWikiSpecialPageIds.PAGES_WITH_BADGES -> MediaWikiSpecialPageModels
                .grid(definition, buildPagesWithBadges(context), effectiveQuery);
            case MediaWikiSpecialPageIds.PAGES_NOT_CONNECTED_TO_ITEMS -> MediaWikiSpecialPageModels
                .grid(definition, buildPagesNotConnectedToItems(context), effectiveQuery);
            case MediaWikiSpecialPageIds.DOUBLE_REDIRECTS -> MediaWikiSpecialPageModels
                .grouped(definition, buildDoubleRedirectGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.DISAMBIGUATION_PAGES -> MediaWikiSpecialPageModels
                .grouped(definition, buildDisambiguationGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.PAGES_LINKING_TO_DISAMBIGUATION_PAGES -> MediaWikiSpecialPageModels
                .grouped(definition, buildPagesLinkingToDisambiguation(context), effectiveQuery);
            case MediaWikiSpecialPageIds.EXTERNAL_LINKS_SEARCH -> MediaWikiSpecialPageModels
                .grouped(definition, buildExternalLinkGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.LINT_ERRORS -> MediaWikiSpecialPageModels
                .grouped(definition, buildLintIssueGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.OVERRIDDEN_PAGES -> MediaWikiSpecialPageModels
                .grouped(definition, buildOverrideGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.UNUSED_FILES -> MediaWikiSpecialPageModels
                .flat(definition, buildUnusedFileEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.FILE_LIST -> MediaWikiSpecialPageModels
                .flat(definition, buildFileListEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.GLOBAL_FILE_USAGE -> MediaWikiSpecialPageModels
                .grouped(definition, buildGlobalFileUsageGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.MEDIA_STATISTICS -> MediaWikiSpecialPageModels
                .flat(definition, buildMediaStatisticsEntries(context), effectiveQuery);
            case MediaWikiSpecialPageIds.DOWNLOAD_GUIDENH_EXTENSION -> MediaWikiSpecialPageModels.info(definition, "");
            case MediaWikiSpecialPageIds.ALL_TRANSLATIONS -> MediaWikiSpecialPageModels
                .grouped(definition, buildAllTranslations(context, effectiveQuery), effectiveQuery);
            case MediaWikiSpecialPageIds.LANGUAGE_STATISTICS -> MediaWikiSpecialPageModels
                .grouped(definition, buildLanguageStatisticsGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.PAGE_TRANSLATION -> MediaWikiSpecialPageModels
                .grouped(definition, buildPageTranslationGroups(context, effectiveQuery), effectiveQuery);
            case MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS -> MediaWikiSpecialPageModels
                .grouped(definition, buildLanguageSearchGroups(context, effectiveQuery), effectiveQuery);
            case MediaWikiSpecialPageIds.TRANSLATION_STATISTICS -> MediaWikiSpecialPageModels
                .grouped(definition, buildLanguageStatisticsGroups(context), effectiveQuery);
            case MediaWikiSpecialPageIds.CONTRIBUTE -> MediaWikiSpecialPageModels
                .flat(definition, buildContributorEntries(context), effectiveQuery);
            default -> MediaWikiSpecialPageModels
                .info(definition, GuidebookText.MediaWikiSpecialPageNotImplemented.text());
        };
    }

    private MediaWikiSpecialPageResult buildSpecialPagesIndex(MediaWikiListContext context,
        MediaWikiSpecialDefinition definition, MediaWikiSpecialPageQuery query) {
        List<MediaWikiSpecialGroupedEntry> groupedEntries = MediaWikiSpecialSearchSupport.filterGroupedEntries(
            MediaWikiSpecialPageModels.groupCatalogEntries(
                context.guide()
                    .getDefaultNamespace()),
            query.searchText());
        return MediaWikiSpecialPageModels.groupIndex(definition, groupedEntries);
    }

    private List<MediaWikiSpecialListEntry> buildNormalPageEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            entries.add(pageEntry(page, resolvePageTitle(context, page), page.getLanguage() + " " + page.getId()));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildCategoryEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (String categoryName : context.categoryIndex()
            .getCategoryNames()) {
            ResourceLocation pageId = MediaWikiPageIds.categoryPageId(
                context.guide()
                    .getDefaultNamespace(),
                categoryName);
            String title = MediaWikiPageIds.toCategoryTitle(categoryName);
            int pageCount = context.categoryIndex()
                .getMembers(categoryName)
                .size();
            entries.add(
                new MediaWikiSpecialListEntry(
                    title,
                    GuidebookText.MediaWikiPagesCount.text(pageCount),
                    title,
                    pageId,
                    null,
                    null));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildAllPagesWithPrefixEntries(MediaWikiListContext context,
        MediaWikiSpecialPageQuery query) {
        String normalizedPrefix = normalizePagePrefix(query);
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            String title = resolvePageTitle(context, page);
            String pageIdText = page.getId()
                .toString();
            if (!normalizedPrefix.isEmpty() && !matchesPrefix(title, pageIdText, normalizedPrefix)) {
                continue;
            }
            entries.add(pageEntry(page, title, title + " " + pageIdText + " " + page.getLanguage()));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialGroupedEntry> buildCategoryTreeGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (String categoryName : context.categoryIndex()
            .getCategoryNames()) {
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (MediaWikiCategoryMember member : context.categoryIndex()
                .getMembers(categoryName)) {
                ParsedGuidePage page = context.specialDataIndex()
                    .normalPagesById()
                    .get(
                        member.pageAnchor()
                            .pageId());
                if (page == null) {
                    continue;
                }
                String title = resolvePageTitle(context, page);
                children.add(pageEntry(page, title, categoryName + " " + title + " " + page.getId()));
            }
            children.sort(this::compareEntries);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    MediaWikiPageIds.toCategoryTitle(categoryName),
                    GuidebookText.MediaWikiPagesCount.text(children.size()),
                    categoryName,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialListEntry> buildSizedPageEntries(MediaWikiListContext context, boolean descending) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Long> entry : context.specialDataIndex()
            .pageSizesById()
            .entrySet()) {
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            long size = entry.getValue() != null ? entry.getValue() : 0L;
            String title = resolvePageTitle(context, page);
            entries.add(
                new MediaWikiSpecialListEntry(
                    title,
                    formatBytesForDisplay(size),
                    title + " " + size,
                    page.getId(),
                    null,
                    pageIcon(page)));
        }
        entries.sort((left, right) -> {
            long leftSize = parseSize(left.subtitle());
            long rightSize = parseSize(right.subtitle());
            int sizeComparison = descending ? Long.compare(rightSize, leftSize) : Long.compare(leftSize, rightSize);
            return sizeComparison != 0 ? sizeComparison : compareEntries(left, right);
        });
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildUncategorizedPageEntries(MediaWikiListContext context) {
        Set<ResourceLocation> categorizedPageIds = new HashSet<>();
        for (String categoryName : context.categoryIndex()
            .getCategoryNames()) {
            for (MediaWikiCategoryMember member : context.categoryIndex()
                .getMembers(categoryName)) {
                categorizedPageIds.add(
                    member.pageAnchor()
                        .pageId());
            }
        }
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            if (!categorizedPageIds.contains(page.getId())) {
                entries.add(
                    pageEntry(
                        page,
                        resolvePageTitle(context, page),
                        page.getId()
                            .toString()));
            }
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildPagesWithProperties(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Set<String>> entry : context.specialDataIndex()
            .pagePropertiesById()
            .entrySet()) {
            Set<String> properties = entry.getValue();
            if (properties == null || properties.isEmpty()) {
                continue;
            }
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            String subtitle = String.join(", ", properties);
            entries.add(
                new MediaWikiSpecialListEntry(
                    resolvePageTitle(context, page),
                    subtitle,
                    resolvePageTitle(context, page) + " " + subtitle,
                    page.getId(),
                    null,
                    pageIcon(page)));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildPagesWithBadges(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            if (page.getFrontmatter() == null || page.getFrontmatter()
                .navigationEntry() == null) {
                continue;
            }
            if (!hasBadges(page)) {
                continue;
            }
            entries.add(
                pageEntry(
                    page,
                    resolvePageTitle(context, page),
                    page.getId()
                        .toString()));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildPagesNotConnectedToItems(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            if (!hasGuideConnection(page)) {
                entries.add(
                    pageEntry(
                        page,
                        resolvePageTitle(context, page),
                        page.getId()
                            .toString()));
            }
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialGroupedEntry> buildDoubleRedirectGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            List<MediaWikiSpecialListEntry> bindings = buildPageBindingEntries(page);
            if (!bindings.isEmpty()) {
                String title = resolvePageTitle(context, page);
                groups.add(
                    new MediaWikiSpecialGroupedEntry(
                        title,
                        page.getId()
                            .toString(),
                        title,
                        bindings));
            }
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildDisambiguationGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : context.specialDataIndex()
            .ambiguousItemBindings()
            .entrySet()) {
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (ResourceLocation pageId : entry.getValue()) {
                ParsedGuidePage page = context.specialDataIndex()
                    .normalPagesById()
                    .get(pageId);
                if (page == null) {
                    continue;
                }
                children.add(pageEntry(pageId, resolvePageTitle(context, page), entry.getKey() + " " + page.getId()));
            }
            groups.add(new MediaWikiSpecialGroupedEntry(entry.getKey(), "", entry.getKey(), children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildPagesLinkingToDisambiguation(MediaWikiListContext context) {
        Map<ResourceLocation, LinkedHashSet<String>> bindingsByPage = new LinkedHashMap<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : context.specialDataIndex()
            .ambiguousItemBindings()
            .entrySet()) {
            List<ResourceLocation> boundPageIds = entry.getValue();
            if (boundPageIds == null || boundPageIds.size() < 2) {
                continue;
            }
            for (ResourceLocation pageId : boundPageIds) {
                if (pageId == null) {
                    continue;
                }
                bindingsByPage.computeIfAbsent(pageId, ignored -> new LinkedHashSet<>())
                    .add(entry.getKey());
            }
        }
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, LinkedHashSet<String>> entry : bindingsByPage.entrySet()) {
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (String binding : entry.getValue()) {
                children.add(new MediaWikiSpecialListEntry(binding, "", binding, entry.getKey(), null, pageIcon(page)));
            }
            children.sort(this::compareEntries);
            String title = resolvePageTitle(context, page);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    title,
                    entry.getKey()
                        .toString(),
                    title + " " + String.join(" ", entry.getValue()),
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildExternalLinkGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<String>> entry : context.specialDataIndex()
            .externalLinksByPage()
            .entrySet()) {
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (String link : entry.getValue()) {
                children.add(new MediaWikiSpecialListEntry(link, "", link, page.getId(), null, pageIcon(page)));
            }
            String title = resolvePageTitle(context, page);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    title,
                    page.getId()
                        .toString(),
                    title,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildLintIssueGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<MediaWikiSpecialLintIssue>> entry : context.specialDataIndex()
            .lintIssuesByPage()
            .entrySet()) {
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (MediaWikiSpecialLintIssue issue : entry.getValue()) {
                String lineTitle = issue.lineNumber() != null
                    ? GuidebookText.MediaWikiLineNumber.text(issue.lineNumber())
                    : GuidebookText.MediaWikiLineUnknown.text();
                String message = issue.message() != null ? issue.message() : "";
                children.add(
                    new MediaWikiSpecialListEntry(
                        lineTitle,
                        message,
                        lineTitle + " " + message,
                        page.getId(),
                        issue.lineNumber(),
                        pageIcon(page)));
            }
            String title = resolvePageTitle(context, page);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    title,
                    page.getId()
                        .toString(),
                    title,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildOverrideGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<MediaWikiSpecialOverrideEntry>> entry : context.specialDataIndex()
            .overridesByPage()
            .entrySet()) {
            ParsedGuidePage page = context.specialDataIndex()
                .normalPagesById()
                .get(entry.getKey());
            if (page == null) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (MediaWikiSpecialOverrideEntry override : entry.getValue()) {
                String title = override.priority() + " | " + override.sourcePack() + " | " + override.language();
                String searchBlob = title + " " + override.sourceId();
                children.add(
                    new MediaWikiSpecialListEntry(
                        title,
                        override.sourceId(),
                        searchBlob,
                        page.getId(),
                        null,
                        pageIcon(page)));
            }
            String title = resolvePageTitle(context, page);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    title,
                    page.getId()
                        .toString(),
                    title,
                    children));
        }
        groups.sort((left, right) -> compareOverrideGroups(left, right));
        return groups;
    }

    private List<MediaWikiSpecialListEntry> buildFileListEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            String title = page.getId()
                .toString();
            entries.add(
                new MediaWikiSpecialListEntry(title, page.getSourcePack(), title, page.getId(), null, pageIcon(page)));
        }
        for (Map.Entry<ResourceLocation, Long> entry : context.specialDataIndex()
            .assetSizesById()
            .entrySet()) {
            entries.add(
                new MediaWikiSpecialListEntry(
                    entry.getKey()
                        .toString(),
                    formatBytesForDisplay(entry.getValue() != null ? entry.getValue() : 0L),
                    entry.getKey() + " " + entry.getValue(),
                    null,
                    null,
                    null));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildMediaStatisticsEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Long> entry : context.specialDataIndex()
            .pageSizesById()
            .entrySet()) {
            String title = entry.getKey()
                .toString();
            long size = entry.getValue() != null ? entry.getValue() : 0L;
            entries.add(
                new MediaWikiSpecialListEntry(
                    title,
                    buildMediaStatisticsSubtitle(entry.getKey(), size, true),
                    title + " " + size,
                    entry.getKey(),
                    null,
                    null));
        }
        for (Map.Entry<ResourceLocation, Long> entry : context.specialDataIndex()
            .assetSizesById()
            .entrySet()) {
            String title = entry.getKey()
                .toString();
            long size = entry.getValue() != null ? entry.getValue() : 0L;
            entries.add(
                new MediaWikiSpecialListEntry(
                    title,
                    buildMediaStatisticsSubtitle(entry.getKey(), size, false),
                    title + " " + size,
                    null,
                    null,
                    null));
        }
        entries.sort((left, right) -> {
            int sizeComparison = Long.compare(parseLeadingSize(right.subtitle()), parseLeadingSize(left.subtitle()));
            return sizeComparison != 0 ? sizeComparison : compareEntries(left, right);
        });
        return entries;
    }

    private List<MediaWikiSpecialListEntry> buildUnusedFileEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (String assetKey : context.specialDataIndex()
            .unusedFiles()) {
            entries.add(new MediaWikiSpecialListEntry(assetKey, "", assetKey, null, null, null));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private List<MediaWikiSpecialGroupedEntry> buildGlobalFileUsageGroups(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : context.specialDataIndex()
            .fileUsageByPath()
            .entrySet()) {
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (ResourceLocation pageId : entry.getValue()) {
                ParsedGuidePage page = context.specialDataIndex()
                    .normalPagesById()
                    .get(pageId);
                if (page == null) {
                    continue;
                }
                children.add(pageEntry(page, resolvePageTitle(context, page), entry.getKey() + " " + page.getId()));
            }
            children.sort(this::compareEntries);
            String summary = GuidebookText.MediaWikiPagesCount.text(children.size());
            groups.add(new MediaWikiSpecialGroupedEntry(entry.getKey(), summary, entry.getKey(), children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildAllTranslations(MediaWikiListContext context,
        MediaWikiSpecialPageQuery query) {
        String pageQuery = firstNonBlank(query.parameter(MediaWikiSpecialPageQuery.PARAM_PAGE), query.searchText());
        String normalizedQuery = MediaWikiSpecialSearchSupport.normalize(pageQuery);
        MediaWikiTranslationStats.TranslationSnapshot snapshot = MediaWikiTranslationStats.scan(context.guide());
        if (normalizedQuery.isEmpty()) {
            return buildAllTranslationsFast(context, snapshot);
        }
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (ResourceLocation sourcePageId : collectTranslationSourcePageIds(context, snapshot)) {
            ParsedGuidePage sourcePage = findSourcePage(
                context,
                sourcePageId,
                context.specialDataIndex()
                    .translationsBySourcePage()
                    .get(sourcePageId),
                snapshot);
            String sourceTitle = sourcePage != null ? resolvePageTitle(context, sourcePage) : sourcePageId.toString();
            if (!normalizedQuery.isEmpty() && !matchesPageTranslationSelector(
                context,
                sourcePageId,
                sourcePage,
                sourceTitle,
                normalizedQuery,
                snapshot)) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = buildTranslationVariantEntries(
                context,
                sourcePageId,
                sourceTitle,
                snapshot);
            if (children.isEmpty()) {
                continue;
            }
            children.sort(this::compareEntries);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    sourceTitle,
                    sourcePageId.toString(),
                    sourceTitle + " " + sourcePageId,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildAllTranslationsFast(MediaWikiListContext context,
        MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (ResourceLocation sourcePageId : collectTranslationSourcePageIds(context, snapshot)) {
            ParsedGuidePage sourcePage = findSourcePageWithoutLoading(
                context,
                sourcePageId,
                context.specialDataIndex()
                    .translationsBySourcePage()
                    .get(sourcePageId));
            String sourceTitle = sourcePage != null ? resolvePageTitle(context, sourcePage) : sourcePageId.toString();
            ArrayList<MediaWikiSpecialListEntry> children = buildFastTranslationVariantEntries(
                context,
                sourcePageId,
                sourceTitle,
                snapshot);
            if (children.isEmpty()) {
                continue;
            }
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    sourceTitle,
                    sourcePageId.toString(),
                    sourceTitle + " " + sourcePageId,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildPageTranslationGroups(MediaWikiListContext context,
        MediaWikiSpecialPageQuery query) {
        String pageSelector = firstNonBlank(query.parameter(MediaWikiSpecialPageQuery.PARAM_PAGE), query.searchText());
        if (pageSelector == null) {
            return List.of();
        }
        String normalizedSelector = MediaWikiSpecialSearchSupport.normalize(pageSelector);
        MediaWikiTranslationStats.TranslationSnapshot snapshot = MediaWikiTranslationStats.scan(context.guide());
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (ResourceLocation sourcePageId : collectTranslationSourcePageIds(context, snapshot)) {
            ParsedGuidePage sourcePage = findSourcePage(
                context,
                sourcePageId,
                context.specialDataIndex()
                    .translationsBySourcePage()
                    .get(sourcePageId),
                snapshot);
            String sourceTitle = sourcePage != null ? resolvePageTitle(context, sourcePage) : sourcePageId.toString();
            if (!matchesPageTranslationSelector(
                context,
                sourcePageId,
                sourcePage,
                sourceTitle,
                normalizedSelector,
                snapshot)) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = buildTranslationVariantEntries(
                context,
                sourcePageId,
                sourceTitle,
                snapshot);
            if (children.isEmpty()) {
                continue;
            }
            children.sort(this::compareEntries);
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    sourceTitle,
                    sourcePageId.toString(),
                    sourceTitle + " " + sourcePageId,
                    children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildLanguageSearchGroups(MediaWikiListContext context,
        MediaWikiSpecialPageQuery query) {
        String explicitLanguage = firstNonBlank(query.parameter(MediaWikiSpecialPageQuery.PARAM_LANGUAGE), null);
        String rawQuery = firstNonBlank(query.parameter(MediaWikiSpecialPageQuery.PARAM_PAGE), query.searchText());
        LanguageScopedQuery languageQuery = parseLanguageScopedQuery(context, rawQuery);
        if (explicitLanguage != null) {
            languageQuery = new LanguageScopedQuery(explicitLanguage, languageQuery.pageQuery());
        }
        MediaWikiTranslationStats.TranslationSnapshot snapshot = MediaWikiTranslationStats.scan(context.guide());
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (String language : snapshot.languages()) {
            if (languageQuery.languageFilter() != null && !language.equalsIgnoreCase(languageQuery.languageFilter())) {
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
            for (String pagePath : snapshot.pagePathsForLanguage(language)) {
                ResourceLocation sourcePageId = new ResourceLocation(
                    context.guide()
                        .getDefaultNamespace(),
                    pagePath);
                ParsedGuidePage page = loadTranslatedPage(context, sourcePageId, language);
                if (page != null) {
                    String title = resolvePageTitle(context, page);
                    children.add(pageEntry(page, title, language + " " + page.getId() + " " + title));
                }
            }
            if (languageQuery.pageQuery() != null && !languageQuery.pageQuery()
                .isEmpty()) {
                children = new ArrayList<>(
                    MediaWikiSpecialSearchSupport.filterFlatEntries(children, languageQuery.pageQuery()));
            }
            if (children.isEmpty() && languageQuery.languageFilter() != null) {
                continue;
            }
            children.sort(this::compareEntries);
            groups.add(new MediaWikiSpecialGroupedEntry(language, "", language, children));
        }
        groups.sort(Comparator.comparing(MediaWikiSpecialGroupedEntry::title, String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    private List<MediaWikiSpecialGroupedEntry> buildLanguageStatisticsGroups(MediaWikiListContext context) {
        MediaWikiTranslationStats.TranslationSnapshot snapshot = MediaWikiTranslationStats.scan(context.guide());
        String currentLanguage = LangUtil.getCurrentLanguage();
        Set<String> baselinePages = snapshot.pagePathsForLanguage(currentLanguage);
        int baselinePageCount = baselinePages.size();
        ArrayList<MediaWikiSpecialGroupedEntry> groups = new ArrayList<>();
        for (String language : snapshot.languages()) {
            List<String> languagePages = sanitizeLanguagePages(snapshot.pagePathsForLanguage(language));
            int languagePageCount = languagePages.size();
            int percentage = baselinePageCount <= 0 ? 0 : Math.round(languagePageCount * 100f / baselinePageCount);
            ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
            for (String pagePath : languagePages) {
                ResourceLocation sourcePageId = new ResourceLocation(
                    context.guide()
                        .getDefaultNamespace(),
                    pagePath);
                ParsedGuidePage page = loadTranslatedPage(context, sourcePageId, language);
                String title = page != null ? resolvePageTitle(context, page) : pagePath;
                entries.add(
                    new MediaWikiSpecialListEntry(
                        title,
                        pagePath,
                        language + " " + percentage + " " + title + " " + pagePath,
                        page != null ? page.getId() : sourcePageId,
                        null,
                        pageIcon(page)));
            }
            groups.add(
                new MediaWikiSpecialGroupedEntry(
                    GuidebookText.MediaWikiLanguageCoverageLine
                        .text(language, percentage, languagePageCount, baselinePageCount),
                    GuidebookText.MediaWikiLanguageCoverageSummary.text(
                        snapshot.languages()
                            .size(),
                        baselinePageCount),
                    language,
                    entries));
        }
        if (groups.isEmpty()) {
            groups.add(new MediaWikiSpecialGroupedEntry(GuidebookText.MediaWikiNoPages.text(), "", "", List.of()));
        }
        return groups;
    }

    private List<ResourceLocation> collectTranslationSourcePageIds(MediaWikiListContext context,
        MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        LinkedHashSet<ResourceLocation> sourcePageIds = new LinkedHashSet<>(
            context.specialDataIndex()
                .translationsBySourcePage()
                .keySet());
        for (String pagePath : snapshot.allPagePaths()) {
            if (pagePath == null || pagePath.trim()
                .isEmpty()) {
                continue;
            }
            sourcePageIds.add(
                new ResourceLocation(
                    context.guide()
                        .getDefaultNamespace(),
                    pagePath));
        }
        return new ArrayList<>(sourcePageIds);
    }

    private List<String> sanitizeLanguagePages(Set<String> pages) {
        ArrayList<String> sanitized = new ArrayList<>();
        if (pages == null) {
            return sanitized;
        }
        for (String page : pages) {
            if (page == null) {
                continue;
            }
            String normalized = page.replace("\r", "")
                .replace("\n", "")
                .trim();
            if (!normalized.isEmpty()) {
                sanitized.add(normalized);
            }
        }
        sanitized.sort(String.CASE_INSENSITIVE_ORDER);
        return sanitized;
    }

    private String joinWrappedLanguagePages(List<String> pages, int maxPerLine) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < pages.size(); index += maxPerLine) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            StringJoiner joiner = new StringJoiner(", ");
            int endExclusive = Math.min(pages.size(), index + maxPerLine);
            for (int pageIndex = index; pageIndex < endExclusive; pageIndex++) {
                joiner.add(pages.get(pageIndex));
            }
            builder.append(joiner);
        }
        return builder.toString();
    }

    private List<MediaWikiSpecialListEntry> buildContributorEntries(MediaWikiListContext context) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        for (MediaWikiSpecialContributors.ContributorEntry contributor : contributors.load(context.guide())) {
            String name = contributors.resolveName(contributor);
            String role = contributors.resolveRole(contributor);
            String link = contributor.link() != null ? contributor.link() : "";
            entries
                .add(new MediaWikiSpecialListEntry(name, role, name + " " + role + " " + link, null, null, null, link));
        }
        entries.sort(this::compareEntries);
        return entries;
    }

    private Set<String> collectLanguages(MediaWikiListContext context) {
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        for (ParsedGuidePage page : context.specialDataIndex()
            .normalPagesById()
            .values()) {
            languages.add(page.getLanguage());
        }
        return languages;
    }

    private boolean hasBadges(ParsedGuidePage page) {
        if (page.getFrontmatter() == null || page.getFrontmatter()
            .navigationEntry() == null) {
            return false;
        }
        return page.getFrontmatter()
            .navigationEntry()
            .iconItemId() != null
            || page.getFrontmatter()
                .navigationEntry()
                .iconTextureId() != null
            || page.getFrontmatter()
                .navigationEntry()
                .iconEntries() != null
                && !page.getFrontmatter()
                    .navigationEntry()
                    .iconEntries()
                    .isEmpty()
            || page.getFrontmatter()
                .navigationEntry()
                .iconTextureEntries() != null
                && !page.getFrontmatter()
                    .navigationEntry()
                    .iconTextureEntries()
                    .isEmpty();
    }

    private boolean hasGuideConnection(ParsedGuidePage page) {
        if (page.getFrontmatter() == null) {
            return false;
        }
        Map<String, Object> properties = page.getFrontmatter()
            .additionalProperties();
        return hasNonEmptyList(properties, "item_ids") || hasNonEmptyList(properties, "ore_ids")
            || hasNonEmptyList(properties, "quest_ids")
            || hasNonEmptyList(properties, "item_id")
            || hasNonEmptyList(properties, "ore_id")
            || hasNonEmptyList(properties, "quest_id");
    }

    private boolean hasNonEmptyList(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        if (value instanceof List<?>values) {
            return !values.isEmpty();
        }
        if (value instanceof String text) {
            return !text.trim()
                .isEmpty();
        }
        return false;
    }

    private List<MediaWikiSpecialListEntry> buildPageBindingEntries(ParsedGuidePage page) {
        ArrayList<MediaWikiSpecialListEntry> entries = new ArrayList<>();
        appendBindingEntries(entries, page, "item_ids", "item");
        appendBindingEntries(entries, page, "ore_ids", "ore");
        appendBindingEntries(entries, page, "quest_ids", "quest");
        appendSingleBindingEntry(entries, page, "item_id", "item");
        appendSingleBindingEntry(entries, page, "ore_id", "ore");
        appendSingleBindingEntry(entries, page, "quest_id", "quest");
        return entries;
    }

    private void appendBindingEntries(List<MediaWikiSpecialListEntry> entries, ParsedGuidePage page, String key,
        String label) {
        if (page.getFrontmatter() == null) {
            return;
        }
        Object value = page.getFrontmatter()
            .additionalProperties()
            .get(key);
        if (!(value instanceof List<?>values)) {
            return;
        }
        for (Object entry : values) {
            if (entry instanceof String text && !text.trim()
                .isEmpty()) {
                String trimmed = text.trim();
                entries.add(
                    new MediaWikiSpecialListEntry(
                        trimmed,
                        label,
                        label + " " + trimmed,
                        page.getId(),
                        null,
                        pageIcon(page)));
            }
        }
    }

    private void appendSingleBindingEntry(List<MediaWikiSpecialListEntry> entries, ParsedGuidePage page, String key,
        String label) {
        if (page.getFrontmatter() == null) {
            return;
        }
        Object value = page.getFrontmatter()
            .additionalProperties()
            .get(key);
        if (!(value instanceof String text) || text.trim()
            .isEmpty()) {
            return;
        }
        String trimmed = text.trim();
        entries.add(
            new MediaWikiSpecialListEntry(trimmed, label, label + " " + trimmed, page.getId(), null, pageIcon(page)));
    }

    private MediaWikiSpecialListEntry pageEntry(ResourceLocation pageId, String title, String searchBlob) {
        return new MediaWikiSpecialListEntry(title, pageId.toString(), searchBlob, pageId, null, null);
    }

    private MediaWikiSpecialListEntry pageEntry(ParsedGuidePage page, String title, String searchBlob) {
        return new MediaWikiSpecialListEntry(
            title,
            page.getId()
                .toString(),
            searchBlob,
            page.getId(),
            null,
            pageIcon(page));
    }

    private String resolvePageTitle(MediaWikiListContext context, ParsedGuidePage page) {
        return MediaWikiPageTitleResolver.resolvePageTitle(context.guide(), page);
    }

    private ParsedGuidePage findSourcePage(MediaWikiListContext context, ResourceLocation sourcePageId,
        List<ResourceLocation> variants, MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        ParsedGuidePage loaded = findSourcePageWithoutLoading(context, sourcePageId, variants);
        if (loaded != null) {
            return loaded;
        }
        List<String> languages = snapshot
            .languagesForPagePath(sourcePageId != null ? sourcePageId.getResourcePath() : "");
        String currentLanguage = LangUtil.getCurrentLanguage();
        ParsedGuidePage fallback = null;
        for (String language : languages) {
            ParsedGuidePage page = loadTranslatedPage(context, sourcePageId, language);
            if (page == null) {
                continue;
            }
            if (currentLanguage.equalsIgnoreCase(page.getLanguage())) {
                return page;
            }
            if (fallback == null) {
                fallback = page;
            }
        }
        return fallback;
    }

    private ParsedGuidePage findSourcePageWithoutLoading(MediaWikiListContext context, ResourceLocation sourcePageId,
        List<ResourceLocation> variants) {
        ParsedGuidePage sourcePage = context.specialDataIndex()
            .normalPagesById()
            .get(sourcePageId);
        if (sourcePage != null) {
            return sourcePage;
        }
        if (variants == null) {
            return null;
        }
        String currentLanguage = LangUtil.getCurrentLanguage();
        ParsedGuidePage fallback = null;
        for (ResourceLocation variantId : variants) {
            ParsedGuidePage variant = context.specialDataIndex()
                .normalPagesById()
                .get(variantId);
            if (variant == null) {
                continue;
            }
            if (currentLanguage.equalsIgnoreCase(variant.getLanguage())) {
                return variant;
            }
            if (fallback == null) {
                fallback = variant;
            }
        }
        return fallback;
    }

    private boolean matchesSourceTranslationQuery(String normalizedQuery, ResourceLocation sourcePageId,
        ParsedGuidePage sourcePage, String sourceTitle) {
        String sourcePageText = sourcePageId != null ? sourcePageId.toString() : "";
        String sourceLanguage = sourcePage != null ? sourcePage.getLanguage() : "";
        String blob = MediaWikiSpecialSearchSupport
            .normalize(sourceTitle + " " + sourcePageText + " " + sourceLanguage);
        return blob.contains(normalizedQuery);
    }

    private boolean matchesPageTranslationSelector(MediaWikiListContext context, ResourceLocation sourcePageId,
        ParsedGuidePage sourcePage, String sourceTitle, String normalizedSelector,
        MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        if (normalizedSelector == null || normalizedSelector.isEmpty()) {
            return true;
        }
        if (matchesSourceTranslationQuery(normalizedSelector, sourcePageId, sourcePage, sourceTitle)) {
            return true;
        }
        for (ParsedGuidePage variant : loadTranslationVariants(context, sourcePageId, snapshot)) {
            if (variant == null) {
                continue;
            }
            String blob = MediaWikiSpecialSearchSupport
                .normalize(variant.getId() + " " + variant.getLanguage() + " " + resolvePageTitle(context, variant));
            if (blob.contains(normalizedSelector)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<MediaWikiSpecialListEntry> buildTranslationVariantEntries(MediaWikiListContext context,
        ResourceLocation sourcePageId, String sourceTitle, MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>();
        for (ParsedGuidePage page : loadTranslationVariants(context, sourcePageId, snapshot)) {
            String title = resolvePageTitle(context, page);
            children.add(
                new MediaWikiSpecialListEntry(
                    page.getLanguage(),
                    title,
                    sourceTitle + " " + page.getLanguage() + " " + page.getId() + " " + title,
                    page.getId(),
                    null,
                    pageIcon(page)));
        }
        return children;
    }

    private ArrayList<MediaWikiSpecialListEntry> buildFastTranslationVariantEntries(MediaWikiListContext context,
        ResourceLocation sourcePageId, String sourceTitle, MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        LinkedHashMap<String, MediaWikiSpecialListEntry> entriesByLanguage = new LinkedHashMap<>();
        for (ResourceLocation variantId : context.specialDataIndex()
            .translationsBySourcePage()
            .getOrDefault(sourcePageId, List.of())) {
            ParsedGuidePage variant = context.specialDataIndex()
                .normalPagesById()
                .get(variantId);
            if (variant == null) {
                continue;
            }
            String title = resolvePageTitle(context, variant);
            entriesByLanguage.putIfAbsent(
                variant.getLanguage(),
                new MediaWikiSpecialListEntry(
                    variant.getLanguage(),
                    title,
                    sourceTitle + " " + variant.getLanguage() + " " + variant.getId() + " " + title,
                    variant.getId(),
                    null,
                    pageIcon(variant)));
        }
        for (String language : snapshot.languagesForPagePath(sourcePageId.getResourcePath())) {
            entriesByLanguage.computeIfAbsent(
                language,
                ignored -> new MediaWikiSpecialListEntry(
                    language,
                    sourceTitle,
                    sourceTitle + " " + language + " " + sourcePageId,
                    sourcePageId,
                    null,
                    null));
        }
        ArrayList<MediaWikiSpecialListEntry> children = new ArrayList<>(entriesByLanguage.values());
        children.sort(this::compareEntries);
        return children;
    }

    private List<ParsedGuidePage> loadTranslationVariants(MediaWikiListContext context, ResourceLocation sourcePageId,
        MediaWikiTranslationStats.TranslationSnapshot snapshot) {
        LinkedHashMap<String, ParsedGuidePage> pagesByLanguage = new LinkedHashMap<>();
        for (ResourceLocation variantId : context.specialDataIndex()
            .translationsBySourcePage()
            .getOrDefault(sourcePageId, List.of())) {
            ParsedGuidePage variant = context.specialDataIndex()
                .normalPagesById()
                .get(variantId);
            if (variant != null) {
                pagesByLanguage.putIfAbsent(variant.getLanguage(), variant);
            }
        }
        for (String language : snapshot.languagesForPagePath(sourcePageId.getResourcePath())) {
            pagesByLanguage.computeIfAbsent(language, ignored -> loadTranslatedPage(context, sourcePageId, language));
        }
        ArrayList<ParsedGuidePage> pages = new ArrayList<>();
        for (ParsedGuidePage page : pagesByLanguage.values()) {
            if (page != null) {
                pages.add(page);
            }
        }
        pages.sort(Comparator.comparing(ParsedGuidePage::getLanguage, String.CASE_INSENSITIVE_ORDER));
        return pages;
    }

    private ParsedGuidePage loadTranslatedPage(MediaWikiListContext context, ResourceLocation pageId, String language) {
        if (!(context.guide() instanceof MediaWikiGuideAggregator aggregator)) {
            return null;
        }
        MutableGuide ownerGuide = aggregator.findOwnerGuide(pageId);
        if (ownerGuide == null) {
            return null;
        }
        return GuideLightweightReloadService
            .loadPageForLanguage(ownerGuide.getId(), ownerGuide.getContentRootFolder(), language, language, pageId);
    }

    private boolean matchesPrefix(String title, String pageIdText, String normalizedPrefix) {
        String normalizedTitle = MediaWikiSpecialSearchSupport.normalize(title);
        String normalizedPageId = MediaWikiSpecialSearchSupport.normalize(pageIdText);
        return normalizedTitle.startsWith(normalizedPrefix) || normalizedPageId.startsWith(normalizedPrefix);
    }

    private String normalizePagePrefix(MediaWikiSpecialPageQuery query) {
        String prefix = query.parameter(MediaWikiSpecialPageQuery.PARAM_PREFIX);
        if (prefix == null || prefix.trim()
            .isEmpty()) {
            prefix = query.searchText();
        }
        return MediaWikiSpecialSearchSupport.normalize(prefix);
    }

    private @Nullable String firstNonBlank(@Nullable String first, @Nullable String second) {
        if (first != null && !first.trim()
            .isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim()
            .isEmpty()) {
            return second.trim();
        }
        return null;
    }

    private String rawSpecialName(@Nullable String specialName) {
        return specialName != null && !specialName.trim()
            .isEmpty() ? specialName.trim() : MediaWikiPageIds.SPECIAL_TITLE_PREFIX;
    }

    private LanguageScopedQuery parseLanguageScopedQuery(MediaWikiListContext context, String rawQuery) {
        String normalized = MediaWikiSpecialSearchSupport.normalize(rawQuery);
        if (normalized.isEmpty()) {
            return new LanguageScopedQuery(null, "");
        }
        int separator = normalized.indexOf(':');
        if (separator > 0) {
            String candidateLanguage = normalized.substring(0, separator)
                .trim();
            if (containsLanguage(context, candidateLanguage)) {
                return new LanguageScopedQuery(
                    candidateLanguage,
                    normalized.substring(separator + 1)
                        .trim());
            }
        }
        for (String language : collectLanguages(context)) {
            String normalizedLanguage = MediaWikiSpecialSearchSupport.normalize(language);
            if (normalized.equals(normalizedLanguage) || normalized.startsWith(normalizedLanguage + " ")) {
                return new LanguageScopedQuery(
                    language,
                    normalized.length() > normalizedLanguage.length()
                        ? normalized.substring(normalizedLanguage.length())
                            .trim()
                        : "");
            }
        }
        return new LanguageScopedQuery(null, normalized);
    }

    private boolean containsLanguage(MediaWikiListContext context, String language) {
        for (String candidate : collectLanguages(context)) {
            if (candidate.equalsIgnoreCase(language)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable GuidePageIcon pageIcon(ParsedGuidePage page) {
        return page != null && page.getFrontmatter() != null
            && page.getFrontmatter()
                .navigationEntry() != null ? NavigationUtil.createNavigationIcon(page) : null;
    }

    private int compareEntries(MediaWikiSpecialListEntry left, MediaWikiSpecialListEntry right) {
        String leftSort = normalizeSortValue(left.title(), left.subtitle());
        String rightSort = normalizeSortValue(right.title(), right.subtitle());
        int titleComparison = leftSort.compareTo(rightSort);
        if (titleComparison != 0) {
            return titleComparison;
        }
        return String.valueOf(left.subtitle())
            .compareToIgnoreCase(String.valueOf(right.subtitle()));
    }

    private int compareOverrideGroups(MediaWikiSpecialGroupedEntry left, MediaWikiSpecialGroupedEntry right) {
        int leftPriority = parseLeadingPriority(left.children());
        int rightPriority = parseLeadingPriority(right.children());
        int priorityCompare = Integer.compare(rightPriority, leftPriority);
        return priorityCompare != 0 ? priorityCompare
            : left.title()
                .compareToIgnoreCase(right.title());
    }

    private int parseLeadingPriority(List<MediaWikiSpecialListEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        String text = entries.get(0)
            .title();
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int separator = text.indexOf('|');
        String numeric = separator >= 0 ? text.substring(0, separator)
            .trim() : text.trim();
        try {
            return Integer.parseInt(numeric);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String normalizeSortValue(@Nullable String title, @Nullable String subtitle) {
        String value = title != null && !title.trim()
            .isEmpty() ? title : subtitle;
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private long parseSize(@Nullable String subtitle) {
        return parseLeadingSize(subtitle);
    }

    private long parseLeadingSize(@Nullable String subtitle) {
        if (subtitle == null) {
            return 0L;
        }
        int separator = subtitle.indexOf(' ');
        String numeric = separator >= 0 ? subtitle.substring(0, separator) : subtitle;
        String unit = separator >= 0 ? subtitle.substring(separator + 1)
            .trim() : "";
        try {
            double value = Double.parseDouble(numeric);
            if (unit.startsWith("MB")) {
                return Math.round(value * ONE_MEGABYTE);
            }
            if (unit.startsWith("KB")) {
                return Math.round(value * ONE_KILOBYTE);
            }
            if (unit.startsWith("B")) {
                return Math.round(value);
            }
            return Math.round(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String buildMediaStatisticsSubtitle(ResourceLocation resourceId, long size, boolean pageFile) {
        String extension = resolveFileExtension(resourceId);
        String type = extension.isEmpty() ? (pageFile ? "page" : "file") : extension;
        return formatBytesForDisplay(size) + " | " + type;
    }

    private String formatBytesForDisplay(long sizeBytes) {
        long safeBytes = Math.max(0L, sizeBytes);
        if (safeBytes >= ONE_MEGABYTE) {
            return formatDecimalSize((double) safeBytes / (double) ONE_MEGABYTE) + " MB";
        }
        return formatDecimalSize((double) safeBytes / (double) ONE_KILOBYTE) + " KB";
    }

    private String formatDecimalSize(double value) {
        synchronized (SIZE_DECIMAL_FORMAT) {
            return SIZE_DECIMAL_FORMAT.format(value);
        }
    }

    private static DecimalFormat createSizeDecimalFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        format.setGroupingUsed(false);
        return format;
    }

    private String resolveFileExtension(@Nullable ResourceLocation resourceId) {
        if (resourceId == null || resourceId.getResourcePath() == null) {
            return "";
        }
        String path = resourceId.getResourcePath();
        int separator = path.lastIndexOf('.');
        if (separator < 0 || separator >= path.length() - 1) {
            return "";
        }
        return path.substring(separator + 1)
            .toLowerCase(Locale.ROOT);
    }

    @Desugar
    private record LanguageScopedQuery(@Nullable String languageFilter, String pageQuery) {}
}
