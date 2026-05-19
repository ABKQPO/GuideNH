package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;

public class StaticSemanticProvider implements SemanticProvider {

    private final String capability;
    private final List<Map<String, String>> entries;

    public StaticSemanticProvider(String capability, List<Map<String, String>> entries) {
        this.capability = capability;
        this.entries = entries;
    }

    @Override
    public String getCapability() {
        return capability;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        List<Map<String, String>> filtered = new ArrayList<>();
        String prefix = query.getPrefix();
        for (Map<String, String> entry : entries) {
            String id = entry.get("id");
            if (prefix.isEmpty() || id != null && id.startsWith(prefix)) {
                filtered.add(entry);
            }
        }
        int limit = Math.max(0, query.getLimit());
        int end = limit == 0 ? filtered.size() : Math.min(filtered.size(), limit);
        return new SemanticQueryResult(
            capability,
            1,
            filtered.subList(0, end),
            end < filtered.size() ? String.valueOf(end) : null);
    }
}
