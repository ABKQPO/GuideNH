package com.hfstudio.guidenh.guide.mediawiki;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import net.minecraft.util.ResourceLocation;

public class MediaWikiPageIds {

    public static final String CATEGORY_TITLE_PREFIX = "Category:";
    public static final String SPECIAL_TITLE_PREFIX = "Special:";
    public static final String SPECIAL_ALL_PAGES = MediaWikiSpecialPageIds.ALL_PAGES;
    public static final String SPECIAL_CATEGORIES = MediaWikiSpecialPageIds.CATEGORIES;

    private static final String SYNTHETIC_ROOT = "__mediawiki/";
    private static final String CATEGORY_ROOT = SYNTHETIC_ROOT + "category/";
    private static final String SPECIAL_ROOT = SYNTHETIC_ROOT + "special/";

    private MediaWikiPageIds() {}

    public static ResourceLocation categoryPageId(String namespace, String categoryName) {
        return new ResourceLocation(namespace, CATEGORY_ROOT + encodeName(categoryName) + ".md");
    }

    public static ResourceLocation specialPageId(String namespace, String specialName) {
        return new ResourceLocation(namespace, SPECIAL_ROOT + specialName.toLowerCase(Locale.ROOT) + ".md");
    }

    public static boolean isSyntheticPage(ResourceLocation pageId) {
        return pageId != null && pageId.getResourcePath()
            .startsWith(SYNTHETIC_ROOT);
    }

    public static boolean isCategoryPage(ResourceLocation pageId) {
        return pageId != null && pageId.getResourcePath()
            .startsWith(CATEGORY_ROOT);
    }

    public static boolean isSpecialPage(ResourceLocation pageId) {
        return pageId != null && pageId.getResourcePath()
            .startsWith(SPECIAL_ROOT);
    }

    public static boolean isSpecialPagesIndex(ResourceLocation pageId) {
        return pageId != null && specialPageName(pageId) != null
            && MediaWikiSpecialPageIds.SPECIAL_PAGES.equalsIgnoreCase(specialPageName(pageId));
    }

    public static String specialPageName(ResourceLocation pageId) {
        if (!isSpecialPage(pageId)) {
            return null;
        }
        String path = pageId.getResourcePath();
        int start = SPECIAL_ROOT.length();
        if (path.length() <= start || !path.endsWith(".md")) {
            return null;
        }
        return path.substring(start, path.length() - 3);
    }

    public static String toCategoryTitle(String categoryName) {
        return CATEGORY_TITLE_PREFIX + categoryName;
    }

    public static String toSpecialTitle(String specialName) {
        return SPECIAL_TITLE_PREFIX + specialName;
    }

    private static String encodeName(String value) {
        String trimmed = value == null ? "" : value.trim();
        StringBuilder builder = new StringBuilder(trimmed.length() + 24);
        boolean previousDash = false;
        for (int index = 0; index < trimmed.length(); index++) {
            char ch = trimmed.charAt(index);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(Character.toLowerCase(ch));
                previousDash = false;
            } else if (!previousDash) {
                builder.append('-');
                previousDash = true;
            }
        }
        if (builder.length() == 0) {
            builder.append("category");
        }
        builder.append("--")
            .append(toHex(trimmed));
        return builder.toString();
    }

    private static String toHex(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            int unsigned = current & 0xFF;
            if (unsigned < 0x10) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(unsigned));
        }
        return builder.toString();
    }
}
