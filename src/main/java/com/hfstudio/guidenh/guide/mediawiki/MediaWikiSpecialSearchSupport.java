package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MediaWikiSpecialSearchSupport {

    private MediaWikiSpecialSearchSupport() {}

    public static List<MediaWikiSpecialListEntry> filterFlatEntries(List<MediaWikiSpecialListEntry> entries,
        String rawQuery) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        String query = normalize(rawQuery);
        if (query.isEmpty()) {
            return entries;
        }

        ArrayList<MediaWikiSpecialListEntry> filtered = new ArrayList<>();
        for (MediaWikiSpecialListEntry entry : entries) {
            if (entry != null && normalize(entry.searchBlob()).contains(query)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static List<MediaWikiSpecialGroupedEntry> filterGroupedEntries(List<MediaWikiSpecialGroupedEntry> entries,
        String rawQuery) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        String query = normalize(rawQuery);
        if (query.isEmpty()) {
            return entries;
        }

        ArrayList<MediaWikiSpecialGroupedEntry> filtered = new ArrayList<>();
        for (MediaWikiSpecialGroupedEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (normalize(entry.searchBlob()).contains(query)) {
                filtered.add(entry);
                continue;
            }
            ArrayList<MediaWikiSpecialListEntry> matchingChildren = new ArrayList<>();
            for (MediaWikiSpecialListEntry child : entry.children()) {
                if (child != null && normalize(child.searchBlob()).contains(query)) {
                    matchingChildren.add(child);
                }
            }
            if (!matchingChildren.isEmpty()) {
                filtered.add(
                    new MediaWikiSpecialGroupedEntry(
                        entry.title(),
                        entry.summary(),
                        entry.searchBlob(),
                        matchingChildren));
            }
        }
        return filtered;
    }

    public static <T> List<T> limit(List<T> entries, int visibleCount) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        if (visibleCount >= entries.size()) {
            return entries;
        }
        return new ArrayList<>(entries.subList(0, Math.max(0, visibleCount)));
    }

    public static boolean hasMore(List<?> entries, int visibleCount) {
        return entries != null && visibleCount < entries.size();
    }

    public static String normalize(String value) {
        return value == null ? ""
            : value.toLowerCase(Locale.ROOT)
                .trim();
    }
}
