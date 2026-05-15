package com.hfstudio.guidenh.bridge.semantic;

import java.util.Collections;
import java.util.Map;

public class SemanticQuery {

    private final String cursor;
    private final int limit;
    private final String prefix;
    private final Map<String, String> filters;

    public SemanticQuery(String cursor, int limit, String prefix, Map<String, String> filters) {
        this.cursor = cursor == null ? "" : cursor;
        this.limit = limit;
        this.prefix = prefix == null ? "" : prefix;
        this.filters = filters == null ? Collections.emptyMap() : filters;
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
