package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;

public abstract class AbstractCollectionSemanticProvider implements SemanticProvider {

    private final String capability;

    public AbstractCollectionSemanticProvider(String capability) {
        this.capability = capability;
    }

    @Override
    public String getCapability() {
        return capability;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        List<Map<String, String>> entries = normalizeEntries(loadEntries());
        List<Map<String, String>> filteredEntries = filterEntries(entries, query.getPrefix());
        int cursor = parseCursor(query.getCursor(), filteredEntries.size());
        int limit = query.getLimit() > 0 ? query.getLimit() : filteredEntries.size();
        int end = Math.min(filteredEntries.size(), cursor + limit);
        String nextCursor = end < filteredEntries.size() ? Integer.toString(end) : null;
        return new SemanticQueryResult(
            capability,
            computeVersion(entries),
            new ArrayList<>(filteredEntries.subList(cursor, end)),
            nextCursor);
    }

    protected abstract List<Map<String, String>> loadEntries();

    protected Map<String, String> createEntry(String id, String label) {
        return createEntry(id, label, null);
    }

    protected Map<String, String> createEntry(String id, String label, String detail) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", id);
        if (label != null && !label.isEmpty()) {
            entry.put("label", label);
        }
        if (detail != null && !detail.isEmpty()) {
            entry.put("detail", detail);
        }
        return entry;
    }

    protected List<Map<String, String>> normalizeEntries(List<Map<String, String>> entries) {
        Map<String, Map<String, String>> deduplicated = new LinkedHashMap<>();
        for (Map<String, String> entry : entries) {
            if (entry == null) {
                continue;
            }
            String id = trimToNull(entry.get("id"));
            if (id == null) {
                continue;
            }

            Map<String, String> normalized = new LinkedHashMap<>();
            normalized.put("id", id);

            String label = trimToNull(entry.get("label"));
            if (label != null) {
                normalized.put("label", label);
            }

            String detail = trimToNull(entry.get("detail"));
            if (detail != null) {
                normalized.put("detail", detail);
            }

            deduplicated.putIfAbsent(id.toLowerCase(Locale.ROOT), normalized);
        }

        List<Map<String, String>> normalizedEntries = new ArrayList<>(deduplicated.values());
        normalizedEntries.sort(Comparator.comparing(entry -> entry.get("id"), String.CASE_INSENSITIVE_ORDER));
        return normalizedEntries;
    }

    protected List<Map<String, String>> filterEntries(List<Map<String, String>> entries, String prefix) {
        String normalizedPrefix = prefix == null ? ""
            : prefix.trim()
                .toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isEmpty()) {
            return entries;
        }

        List<Map<String, String>> filteredEntries = new ArrayList<>();
        for (Map<String, String> entry : entries) {
            if (matchesPrefix(entry, normalizedPrefix)) {
                filteredEntries.add(entry);
            }
        }
        return filteredEntries;
    }

    protected boolean matchesPrefix(Map<String, String> entry, String normalizedPrefix) {
        return startsWithIgnoreCase(entry.get("id"), normalizedPrefix)
            || startsWithIgnoreCase(entry.get("label"), normalizedPrefix)
            || startsWithIgnoreCase(entry.get("detail"), normalizedPrefix);
    }

    protected int computeVersion(List<Map<String, String>> entries) {
        int hash = entries.hashCode();
        if (hash == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(hash) + 1;
    }

    protected int parseCursor(String cursor, int size) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Math.clamp(Integer.parseInt(cursor), 0, size);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean startsWithIgnoreCase(String value, String normalizedPrefix) {
        return value != null && value.toLowerCase(Locale.ROOT)
            .startsWith(normalizedPrefix);
    }

    protected List<Map<String, String>> emptyEntries() {
        return List.of();
    }
}
