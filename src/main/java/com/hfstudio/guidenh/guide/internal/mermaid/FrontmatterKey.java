package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDirection;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutMode;

public enum FrontmatterKey {

    DIRECTION("direction", FlowchartDirection::fromString),
    LAYOUT("layout", FlowchartLayoutMode::fromConfigValue);

    private final String key;
    private final Function<String, Object> parser;

    FrontmatterKey(String key, Function<String, Object> parser) {
        this.key = key;
        this.parser = parser;
    }

    public String key() {
        return key;
    }

    @Nullable
    public Object parse(String value) {
        return parser.apply(value);
    }

    private static final Map<String, FrontmatterKey> KEY_MAP = buildKeyMap();

    private static Map<String, FrontmatterKey> buildKeyMap() {
        Map<String, FrontmatterKey> map = new LinkedHashMap<>();
        for (FrontmatterKey fk : values()) {
            map.put(fk.key.toLowerCase(Locale.ROOT), fk);
        }
        return map;
    }

    @Nullable
    public static FrontmatterKey byKey(String key) {
        if (key == null) return null;
        return KEY_MAP.get(
            key.trim()
                .toLowerCase(Locale.ROOT));
    }
}
