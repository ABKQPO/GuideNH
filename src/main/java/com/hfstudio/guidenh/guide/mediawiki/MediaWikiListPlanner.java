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
            for (int index = 0; index < columnCount; index++) {
                columns.add(new MediaWikiListColumn(Collections.emptyList()));
            }
            return columns;
        }

        List<MediaWikiListGroup> groups = buildGroups(entries);
        int[] targetSizes = buildTargetSizes(entries.size(), columnCount);
        int groupIndex = 0;
        int entryOffset = 0;

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            var sections = new ArrayList<MediaWikiListSection>();

            if (columnIndex == columnCount - 1) {
                while (groupIndex < groups.size()) {
                    MediaWikiListGroup group = groups.get(groupIndex);
                    sections.add(
                        createSection(
                            group,
                            entryOffset,
                            group.entries()
                                .size()));
                    groupIndex++;
                    entryOffset = 0;
                }
                columns.add(new MediaWikiListColumn(sections));
                continue;
            }

            int targetSize = targetSizes[columnIndex];
            int currentSize = 0;
            while (groupIndex < groups.size() && currentSize < targetSize) {
                MediaWikiListGroup group = groups.get(groupIndex);
                int remainingCount = group.entries()
                    .size() - entryOffset;
                int remainingCapacity = targetSize - currentSize;
                if (remainingCount <= remainingCapacity) {
                    sections.add(
                        createSection(
                            group,
                            entryOffset,
                            group.entries()
                                .size()));
                    currentSize += remainingCount;
                    groupIndex++;
                    entryOffset = 0;
                    continue;
                }
                if (currentSize == 0 && remainingCapacity > 0) {
                    int endExclusive = entryOffset + remainingCapacity;
                    sections.add(createSection(group, entryOffset, endExclusive));
                    entryOffset = endExclusive;
                    currentSize += remainingCapacity;
                }
                break;
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

    private static int[] buildTargetSizes(int entryCount, int columnCount) {
        int[] targetSizes = new int[columnCount];
        int baseSize = entryCount / columnCount;
        int remainder = entryCount % columnCount;
        for (int index = 0; index < columnCount; index++) {
            targetSizes[index] = baseSize + (index < remainder ? 1 : 0);
        }
        return targetSizes;
    }

    private static MediaWikiListSection createSection(MediaWikiListGroup group, int startInclusive, int endExclusive) {
        return new MediaWikiListSection(
            group.key(),
            new ArrayList<>(
                group.entries()
                    .subList(startInclusive, endExclusive)));
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
