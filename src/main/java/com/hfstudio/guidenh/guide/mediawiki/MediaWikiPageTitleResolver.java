package com.hfstudio.guidenh.guide.mediawiki;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

public class MediaWikiPageTitleResolver {

    private MediaWikiPageTitleResolver() {}

    public static String resolvePageTitle(Guide guide, ParsedGuidePage page) {
        if (page == null) {
            return "";
        }

        String syntheticTitle = resolveSyntheticTitle(page.getId());
        if (syntheticTitle != null) {
            return syntheticTitle;
        }

        FrontmatterNavigation navigationEntry = page.getFrontmatter()
            .navigationEntry();
        if (navigationEntry != null && navigationEntry.title() != null
            && !navigationEntry.title()
                .trim()
                .isEmpty()) {
            return navigationEntry.title();
        }

        return GuideTitleHeadings.resolveHeading1Title(guide, page);
    }

    public static @Nullable String resolveSyntheticTitle(@Nullable ResourceLocation pageId) {
        if (pageId == null) {
            return null;
        }
        if (MediaWikiPageIds.isCategoryPage(pageId)) {
            return MediaWikiPageIds.toCategoryTitle(resolveCategoryName(pageId));
        }
        if (MediaWikiPageIds.isSpecialPage(pageId)) {
            return resolveSpecialTitle(MediaWikiPageIds.specialPageName(pageId));
        }
        return null;
    }

    public static String resolveSpecialTitle(@Nullable String specialName) {
        MediaWikiSpecialDefinition definition = MediaWikiSpecialCatalog.findByName(specialName);
        if (definition != null) {
            String localized = StatCollector.translateToLocal(definition.titleKey());
            if (localized != null && !localized.equals(definition.titleKey())) {
                return MediaWikiPageIds.SPECIAL_TITLE_PREFIX + localized;
            }
        }
        return MediaWikiPageIds.toSpecialTitle(specialName != null ? specialName : "");
    }

    public static String resolveSpecialListTitle(@Nullable String specialName) {
        MediaWikiSpecialDefinition definition = MediaWikiSpecialCatalog.findByName(specialName);
        if (definition != null) {
            String localized = StatCollector.translateToLocal(definition.titleKey());
            if (localized != null && !localized.equals(definition.titleKey())) {
                return localized;
            }
        }
        return specialName != null ? specialName : "";
    }

    public static String resolveSpecialGroupTitle(MediaWikiSpecialGroup group) {
        if (group == null) {
            return "";
        }
        String localized = StatCollector.translateToLocal(group.titleKey());
        return localized != null && !localized.equals(group.titleKey()) ? localized : group.name();
    }

    private static String resolveCategoryName(ResourceLocation pageId) {
        String path = pageId.getResourcePath();
        int separator = path.lastIndexOf("--");
        if (separator < 0 || !path.endsWith(".md")) {
            return pageId.toString();
        }
        String hex = path.substring(separator + 2, path.length() - 3);
        return MediaWikiTitleCodec.decodeHex(hex);
    }
}
