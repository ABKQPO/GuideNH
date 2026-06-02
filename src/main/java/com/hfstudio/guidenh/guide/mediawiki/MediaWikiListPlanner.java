package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.github.bsideup.jabel.Desugar;

public class MediaWikiListPlanner {

    public static final int DEFAULT_ROWS = 3;

    public static final Comparator<MediaWikiListEntry> ENTRY_COMPARATOR = Comparator
        .comparing((MediaWikiListEntry entry) -> normalize(entry.sortKey()))
        .thenComparing(entry -> normalize(entry.title()))
        .thenComparing(
            entry -> entry.pageId()
                .toString());

    private MediaWikiListPlanner() {}

    public static int sanitizeRows(int rows) {
        return rows > 0 ? rows : DEFAULT_ROWS;
    }

    public static List<MediaWikiListEntry> sortEntries(Iterable<MediaWikiListEntry> entries) {
        var sorted = new ArrayList<MediaWikiListEntry>();
        for (MediaWikiListEntry entry : entries) {
            if (entry != null) {
                sorted.add(entry);
            }
        }
        sorted.sort(ENTRY_COMPARATOR);
        return sorted;
    }

    public static List<MediaWikiListGroup> buildGroups(List<MediaWikiListEntry> entries) {
        var groups = new ArrayList<MediaWikiListGroup>();
        var currentEntries = new ArrayList<MediaWikiListEntry>();
        String currentKey = null;

        for (MediaWikiListEntry entry : entries) {
            String groupKey = resolveGroupKey(entry);
            if (!groupKey.equals(currentKey) && !currentEntries.isEmpty()) {
                groups.add(new MediaWikiListGroup(currentKey, new ArrayList<>(currentEntries)));
                currentEntries.clear();
            }
            currentKey = groupKey;
            currentEntries.add(entry);
        }

        if (!currentEntries.isEmpty()) {
            groups.add(new MediaWikiListGroup(currentKey, new ArrayList<>(currentEntries)));
        }
        return groups;
    }

    public static List<MediaWikiListColumn> planColumns(List<MediaWikiListEntry> entries, int rows) {
        int columnCount = Math.max(1, sanitizeRows(rows));
        var columns = new ArrayList<MediaWikiListColumn>(columnCount);
        if (entries.isEmpty()) {
            for (int i = 0; i < columnCount; i++) {
                columns.add(new MediaWikiListColumn(Collections.<MediaWikiListSection>emptyList()));
            }
            return columns;
        }

        int perColumn = (int) Math.ceil((double) entries.size() / columnCount);
        for (int col = 0; col < columnCount; col++) {
            int start = col * perColumn;
            int end = Math.min(start + perColumn, entries.size());
            if (start >= entries.size()) {
                columns.add(new MediaWikiListColumn(Collections.<MediaWikiListSection>emptyList()));
                continue;
            }
            List<MediaWikiListEntry> slice = entries.subList(start, end);
            var sections = new ArrayList<MediaWikiListSection>();
            for (MediaWikiListGroup group : buildGroups(slice)) {
                sections.add(new MediaWikiListSection(group.key(), new ArrayList<>(group.entries())));
            }
            columns.add(new MediaWikiListColumn(sections));
        }
        return columns;
    }

    public static List<List<MediaWikiListEntry>> splitIntoColumns(List<MediaWikiListEntry> entries, int rows) {
        var columns = new ArrayList<List<MediaWikiListEntry>>();
        for (MediaWikiListColumn column : planColumns(entries, rows)) {
            var flattened = new ArrayList<MediaWikiListEntry>();
            for (MediaWikiListSection section : column.sections()) {
                flattened.addAll(section.entries());
            }
            columns.add(flattened);
        }
        return columns;
    }

    public static String resolveGroupKey(MediaWikiListEntry entry) {
        String value = firstSortableValue(entry);
        if (value.isEmpty()) {
            return "#";
        }

        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            if (Character.isDigit(codePoint)) {
                return "0-9";
            }
            if (Character.isLetter(codePoint)) {
                return new String(Character.toChars(Character.toUpperCase(codePoint)));
            }
            if (Character.isIdeographic(codePoint)
                || Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.LATIN) {
                return new String(Character.toChars(codePoint));
            }
        }
        return "#";
    }

    private static String firstSortableValue(MediaWikiListEntry entry) {
        String sortKey = entry.sortKey();
        if (sortKey != null && !sortKey.trim()
            .isEmpty()) {
            return sortKey.trim();
        }
        String title = entry.title();
        return title != null ? title.trim() : "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @Desugar
    public record MediaWikiListGroup(String key, List<MediaWikiListEntry> entries) {}

    @Desugar
    public record MediaWikiListSection(String key, List<MediaWikiListEntry> entries) {}

    @Desugar
    public record MediaWikiListColumn(List<MediaWikiListSection> sections) {}
}
