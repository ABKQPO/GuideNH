package com.hfstudio.guidenh.bridge.preview;

import java.util.List;

public class PreviewSearchResult {

    private final String capability;
    private final int version;
    private final List<PreviewSearchEntry> entries;
    private final String nextCursor;

    public PreviewSearchResult(String capability, int version, List<PreviewSearchEntry> entries, String nextCursor) {
        this.capability = capability == null ? "" : capability;
        this.version = version;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.nextCursor = nextCursor;
    }

    public static PreviewSearchResult page(String capability, List<PreviewSearchEntry> entries, String cursor,
        int limit) {
        List<PreviewSearchEntry> safeEntries = entries == null ? List.of() : entries;
        int start = parseCursor(cursor, safeEntries.size());
        int safeLimit = limit > 0 ? limit : safeEntries.size();
        int end = Math.min(safeEntries.size(), start + safeLimit);
        String nextCursor = end < safeEntries.size() ? Integer.toString(end) : null;
        return new PreviewSearchResult(
            capability,
            computeVersion(safeEntries),
            List.copyOf(safeEntries.subList(start, end)),
            nextCursor);
    }

    public String getCapability() {
        return capability;
    }

    public int getVersion() {
        return version;
    }

    public List<PreviewSearchEntry> getEntries() {
        return entries;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    private static int parseCursor(String cursor, int size) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(Integer.parseInt(cursor), size));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int computeVersion(List<PreviewSearchEntry> entries) {
        int hash = entries.hashCode();
        if (hash == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(hash) + 1;
    }
}
