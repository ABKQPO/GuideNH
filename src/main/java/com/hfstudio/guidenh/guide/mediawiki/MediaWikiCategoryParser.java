package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

import cpw.mods.fml.common.FMLLog;

public class MediaWikiCategoryParser {

    private static final String FRONTMATTER_KEY = "categories";

    private MediaWikiCategoryParser() {}

    public static List<MediaWikiCategoryReference> parseReferences(ParsedGuidePage page) {
        Object categoriesNode = page.getFrontmatter()
            .additionalProperties()
            .get(FRONTMATTER_KEY);
        if (categoriesNode == null) {
            return List.of();
        }

        List<?> categoryList = normalizeEntries(page, categoriesNode);
        if (categoryList == null) {
            warnMalformedCategories(page, "contains malformed categories frontmatter");
            return List.of();
        }

        Map<String, MediaWikiCategoryReference> categories = new LinkedHashMap<>(categoryList.size());
        for (Object listEntry : categoryList) {
            MediaWikiCategoryReference category = parseReference(page, listEntry);
            if (category != null) {
                categories.putIfAbsent(normalizeCategoryKey(category.categoryName()), category);
            }
        }
        return new ArrayList<>(categories.values());
    }

    private static List<?> normalizeEntries(ParsedGuidePage page, Object categoriesNode) {
        if (categoriesNode instanceof List<?>categoryList) {
            return categoryList;
        }
        if (categoriesNode instanceof String categoryEntry) {
            String trimmed = categoryEntry.trim();
            if (trimmed.isEmpty()) {
                warnMalformedCategories(page, "contains an empty categories frontmatter entry");
                return List.of();
            }
            return List.of(trimmed);
        }
        return null;
    }

    private static MediaWikiCategoryReference parseReference(ParsedGuidePage page, Object listEntry) {
        if (!(listEntry instanceof String rawCategory)) {
            warnMalformedCategories(page, "contains a malformed categories frontmatter entry: " + listEntry);
            return null;
        }

        String trimmed = rawCategory.trim();
        if (trimmed.isEmpty()) {
            warnMalformedCategories(page, "contains an empty categories frontmatter entry");
            return null;
        }

        int separator = trimmed.indexOf('|');
        if (separator < 0) {
            return new MediaWikiCategoryReference(trimmed, null);
        }

        String categoryName = trimmed.substring(0, separator)
            .trim();
        if (categoryName.isEmpty()) {
            warnMalformedCategories(page, "contains a categories entry without a category name: " + rawCategory);
            return null;
        }

        String sortKey = trimmed.substring(separator + 1)
            .trim();
        if (sortKey.isEmpty()) {
            sortKey = null;
        }
        return new MediaWikiCategoryReference(categoryName, sortKey);
    }

    private static void warnMalformedCategories(ParsedGuidePage page, String message) {
        FMLLog.getLogger()
            .warn("[GuideNH] [MediaWikiCategoryParser] Page {} {}", page.getId(), message);
    }

    private static String normalizeCategoryKey(String categoryName) {
        return categoryName.toLowerCase(Locale.ROOT);
    }
}
