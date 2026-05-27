package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class MediaWikiSpecialCatalog {

    private static final int DEFAULT_VISIBLE_COUNT = 60;
    private static final int SINGLE_ROW_VISIBLE_COUNT = 1;

    private static final List<MediaWikiSpecialGroup> GROUPS;
    private static final List<MediaWikiSpecialDefinition> DEFINITIONS;

    static {
        ArrayList<MediaWikiSpecialGroup> groups = new ArrayList<>();
        groups.add(new MediaWikiSpecialGroup("maintenance", "guidenh.mediawiki.special.group.maintenance", 0));
        groups.add(new MediaWikiSpecialGroup("lists", "guidenh.mediawiki.special.group.lists", 1));
        groups.add(new MediaWikiSpecialGroup("media", "guidenh.mediawiki.special.group.media", 2));
        groups.add(new MediaWikiSpecialGroup("developer", "guidenh.mediawiki.special.group.developer", 3));
        groups.add(new MediaWikiSpecialGroup("translation", "guidenh.mediawiki.special.group.translation", 4));
        groups.add(new MediaWikiSpecialGroup("other", "guidenh.mediawiki.special.group.other", 5));
        GROUPS = Collections.unmodifiableList(groups);

        ArrayList<MediaWikiSpecialDefinition> definitions = new ArrayList<>();
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.SPECIAL_PAGES,
                "guidenh.mediawiki.special.specialpages",
                "other",
                MediaWikiSpecialPageKind.GROUP_INDEX,
                true,
                Integer.MAX_VALUE,
                null));

        definitions.add(
            definition(
                MediaWikiSpecialPageIds.DOUBLE_REDIRECTS,
                "guidenh.mediawiki.special.doubleredirects",
                "maintenance",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.LINT_ERRORS,
                "guidenh.mediawiki.special.linterrors",
                "maintenance",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.LONG_PAGES,
                "guidenh.mediawiki.special.longpages",
                "maintenance",
                MediaWikiSpecialPageKind.FLAT));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.SHORT_PAGES,
                "guidenh.mediawiki.special.shortpages",
                "maintenance",
                MediaWikiSpecialPageKind.FLAT));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.UNCATEGORIZED_PAGES,
                "guidenh.mediawiki.special.uncategorizedpages",
                "maintenance",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.PAGES_NOT_CONNECTED_TO_ITEMS,
                "guidenh.mediawiki.special.pagesnotconnectedtoitems",
                "maintenance",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.UNUSED_FILES,
                "guidenh.mediawiki.special.unusedfiles",
                "maintenance",
                MediaWikiSpecialPageKind.FLAT));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.OVERRIDDEN_PAGES,
                "guidenh.mediawiki.special.overriddenpages",
                "maintenance",
                MediaWikiSpecialPageKind.GROUPED));

        definitions.add(
            definition(
                MediaWikiSpecialPageIds.ALL_PAGES,
                "guidenh.mediawiki.special.allpages",
                "lists",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.ALL_PAGES_WITH_PREFIX,
                "guidenh.mediawiki.special.allpageswithprefix",
                "lists",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.CATEGORIES,
                "guidenh.mediawiki.special.categories",
                "lists",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.CATEGORY_TREE,
                "guidenh.mediawiki.special.categorytree",
                "lists",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.DISAMBIGUATION_PAGES,
                "guidenh.mediawiki.special.disambiguationpages",
                "lists",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.EXTERNAL_LINKS_SEARCH,
                "guidenh.mediawiki.special.externallinkssearch",
                "lists",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.PAGES_LINKING_TO_DISAMBIGUATION_PAGES,
                "guidenh.mediawiki.special.pageslinkingtodisambiguationpages",
                "lists",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.PAGES_WITH_PAGE_PROPERTY,
                "guidenh.mediawiki.special.pageswithpageproperty",
                "lists",
                MediaWikiSpecialPageKind.GRID));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.PAGES_WITH_BADGES,
                "guidenh.mediawiki.special.pageswithbadges",
                "lists",
                MediaWikiSpecialPageKind.GRID));

        definitions.add(
            definition(
                MediaWikiSpecialPageIds.FILE_LIST,
                "guidenh.mediawiki.special.filelist",
                "media",
                MediaWikiSpecialPageKind.FLAT));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.GLOBAL_FILE_USAGE,
                "guidenh.mediawiki.special.globalfileusage",
                "media",
                MediaWikiSpecialPageKind.GROUPED));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.MEDIA_STATISTICS,
                "guidenh.mediawiki.special.mediastatistics",
                "media",
                MediaWikiSpecialPageKind.FLAT));

        definitions.add(
            definition(
                MediaWikiSpecialPageIds.DOWNLOAD_GUIDENH_EXTENSION,
                "guidenh.mediawiki.special.downloadguidenhextension",
                "developer",
                MediaWikiSpecialPageKind.ACTION,
                false,
                DEFAULT_VISIBLE_COUNT,
                "https://github.com/ABKQPO/GuideNH-VSC"));

        definitions.add(
            definition(
                MediaWikiSpecialPageIds.ALL_TRANSLATIONS,
                "guidenh.mediawiki.special.alltranslations",
                "translation",
                MediaWikiSpecialPageKind.GROUPED,
                false,
                SINGLE_ROW_VISIBLE_COUNT,
                null));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.LANGUAGE_STATISTICS,
                "guidenh.mediawiki.special.languagestatistics",
                "translation",
                MediaWikiSpecialPageKind.GROUPED,
                false,
                SINGLE_ROW_VISIBLE_COUNT,
                null));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.PAGE_TRANSLATION,
                "guidenh.mediawiki.special.pagetranslation",
                "translation",
                MediaWikiSpecialPageKind.GROUPED,
                false,
                SINGLE_ROW_VISIBLE_COUNT,
                null));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS,
                "guidenh.mediawiki.special.searchtranslations",
                "translation",
                MediaWikiSpecialPageKind.GROUPED,
                false,
                SINGLE_ROW_VISIBLE_COUNT,
                null));
        definitions.add(
            definition(
                MediaWikiSpecialPageIds.CONTRIBUTE,
                "guidenh.mediawiki.special.contribute",
                "other",
                MediaWikiSpecialPageKind.FLAT));

        DEFINITIONS = Collections.unmodifiableList(definitions);
    }

    private MediaWikiSpecialCatalog() {}

    public static List<MediaWikiSpecialGroup> groups() {
        return GROUPS;
    }

    public static List<MediaWikiSpecialDefinition> definitions() {
        return DEFINITIONS;
    }

    public static boolean isSupported(String name) {
        return findByName(name) != null;
    }

    public static @Nullable MediaWikiSpecialDefinition findByName(String name) {
        if (name == null) {
            return null;
        }
        for (MediaWikiSpecialDefinition definition : DEFINITIONS) {
            if (definition.name()
                .equalsIgnoreCase(name.trim())) {
                return definition;
            }
        }
        return null;
    }

    private static MediaWikiSpecialDefinition definition(String name, String titleKey, String groupName,
        MediaWikiSpecialPageKind kind) {
        return definition(name, titleKey, groupName, kind, false, DEFAULT_VISIBLE_COUNT, null);
    }

    private static MediaWikiSpecialDefinition definition(String name, String titleKey, String groupName,
        MediaWikiSpecialPageKind kind, boolean showsAllByDefault, int defaultVisibleCount,
        @Nullable String externalUrl) {
        return new MediaWikiSpecialDefinition(
            name,
            titleKey,
            groupName,
            kind,
            false,
            showsAllByDefault,
            defaultVisibleCount,
            externalUrl);
    }
}
