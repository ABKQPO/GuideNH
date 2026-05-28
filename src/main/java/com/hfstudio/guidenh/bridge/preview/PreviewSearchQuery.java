package com.hfstudio.guidenh.bridge.preview;

import java.util.LinkedHashMap;
import java.util.Map;

public class PreviewSearchQuery {

    private final String capability;
    private final String cursor;
    private final int limit;
    private final String prefix;
    private final Map<String, String> filters;

    public PreviewSearchQuery(String capability, String cursor, int limit, String prefix, Map<String, String> filters) {
        this.capability = capability == null ? "" : capability;
        this.cursor = cursor == null ? "" : cursor;
        this.limit = limit;
        this.prefix = prefix == null ? "" : prefix;
        this.filters = filters == null || filters.isEmpty() ? Map.of() : Map.copyOf(new LinkedHashMap<>(filters));
    }

    public String getCapability() {
        return capability;
    }

    public String getCursor() {
        return cursor;
    }

    public int getLimit() {
        return limit;
    }

    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getFilters() {
        return filters;
    }
}
