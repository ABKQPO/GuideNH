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

    /** Pixel cost constants — must match MediaWikiGeneratedListBlock layout. */
    private static final int SECTION_HEADER_HEIGHT = 28; // GAP_TOP(5) + HEADER_HEIGHT(20) + GAP_BOTTOM(3)
    private static final int ROW_HEIGHT = 20;

    public static List<MediaWikiListColumn> planColumns(List<MediaWikiListEntry> entries, int rows) {
        int columnCount = Math.max(1, sanitizeRows(rows));
        var columns = new ArrayList<MediaWikiListColumn>(columnCount);
        if (entries.isEmpty()) {
            for (int index = 0; index < columnCount; index++) {
                columns.add(new MediaWikiListColumn(Collections.emptyList()));
            }
            return columns;
        }

        List<MediaWikiListGroup> groups = buildGroups(entries);
        int[] groupHeights = new int[groups.size()];
        int maxGroupHeight = 0;
        int totalHeight = 0;
        for (int i = 0; i < groups.size(); i++) {
            groupHeights[i] = SECTION_HEADER_HEIGHT + groups.get(i)
                .entries()
                .size() * ROW_HEIGHT;
            maxGroupHeight = Math.max(maxGroupHeight, groupHeights[i]);
            totalHeight += groupHeights[i];
        }

        // Binary search for optimal max column height
        int lo = maxGroupHeight;
        int hi = totalHeight;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            int cols = countColumns(groupHeights, mid);
            if (cols <= columnCount) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }

        // Build columns with the found max height
        int optimalMax = lo;
        int groupIndex = 0;
        for (int col = 0; col < columnCount && groupIndex < groups.size(); col++) {
            var sections = new ArrayList<MediaWikiListSection>();
            if (col == columnCount - 1) {
                while (groupIndex < groups.size()) {
                    MediaWikiListGroup g = groups.get(groupIndex++);
                    sections.add(new MediaWikiListSection(g.key(), new ArrayList<>(g.entries())));
                }
            } else {
                int colHeight = 0;
                while (groupIndex < groups.size()) {
                    MediaWikiListGroup g = groups.get(groupIndex);
                    int gh = groupHeights[groupIndex];
                    // Allow one group per column; otherwise respect the budget
                    if (colHeight > 0 && colHeight + gh > optimalMax) break;
                    sections.add(new MediaWikiListSection(g.key(), new ArrayList<>(g.entries())));
                    colHeight += gh;
                    groupIndex++;
                }
            }
            columns.add(new MediaWikiListColumn(sections));
        }
        return columns;
    }

    private static int countColumns(int[] groupHeights, int maxHeight) {
        int cols = 0;
        int currentHeight = 0;
        for (int gh : groupHeights) {
            if (currentHeight + gh > maxHeight) {
                cols++;
                currentHeight = gh;
            } else {
                currentHeight += gh;
            }
        }
        return currentHeight > 0 ? cols + 1 : cols;
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
