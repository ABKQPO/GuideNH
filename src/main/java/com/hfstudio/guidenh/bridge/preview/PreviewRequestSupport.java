package com.hfstudio.guidenh.bridge.preview;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PreviewRequestSupport {

    protected PreviewRequestSupport() {}

    public static String requireString(JsonObject payload, String name) {
        String value = readOptionalString(payload, name, "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    public static String readOptionalString(JsonObject payload, String name, String defaultValue) {
        if (payload == null || !payload.has(name)) {
            return defaultValue;
        }
        JsonElement value = payload.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return defaultValue;
        }
        String text = value.getAsString();
        return text == null ? defaultValue : text;
    }

    public static int readBoundedInt(JsonObject payload, String name, int defaultValue, int minValue, int maxValue) {
        int value = defaultValue;
        if (payload != null && payload.has(name)) {
            JsonElement rawValue = payload.get(name);
            if (rawValue != null && rawValue.isJsonPrimitive()) {
                try {
                    value = rawValue.getAsInt();
                } catch (NumberFormatException ignored) {
                    value = defaultValue;
                }
            }
        }
        return Math.max(minValue, Math.min(value, maxValue));
    }

    public static Map<String, String> readStringMap(JsonObject payload, String name) {
        if (payload == null || !payload.has(name)
            || !payload.get(name)
                .isJsonObject()) {
            return Collections.emptyMap();
        }
        Map<String, String> values = new LinkedHashMap<>();
        JsonObject object = payload.getAsJsonObject(name);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) {
                values.put(entry.getKey(), value.getAsString());
            }
        }
        return values;
    }
}
