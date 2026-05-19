package com.hfstudio.guidenh.bridge.semantic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;

public class SemanticQueryFactory {

    private final BridgeProtocolLimits limits;

    public SemanticQueryFactory(BridgeProtocolLimits limits) {
        this.limits = limits;
    }

    public String readCapability(JsonObject payload) {
        return readString(payload, "capability", "");
    }

    public SemanticQuery fromPayload(JsonObject payload) {
        int requestedLimit = readInt(payload, "limit", limits.getMaxPageSize());
        int limit = Math.max(0, Math.min(requestedLimit, limits.getMaxPageSize()));
        return new SemanticQuery(
            readString(payload, "cursor", ""),
            limit,
            readString(payload, "prefix", ""),
            readFilters(payload));
    }

    private Map<String, String> readFilters(JsonObject payload) {
        if (payload == null || !payload.has("filters")
            || !payload.get("filters")
                .isJsonObject()) {
            return Collections.emptyMap();
        }

        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : payload.getAsJsonObject("filters")
            .entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) {
                filters.put(entry.getKey(), value.getAsString());
            }
        }
        return filters;
    }

    private String readString(JsonObject payload, String name, String defaultValue) {
        if (payload == null || !payload.has(name)) {
            return defaultValue;
        }
        JsonElement value = payload.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : defaultValue;
    }

    private int readInt(JsonObject payload, String name, int defaultValue) {
        if (payload == null || !payload.has(name)) {
            return defaultValue;
        }
        JsonElement value = payload.get(name);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
