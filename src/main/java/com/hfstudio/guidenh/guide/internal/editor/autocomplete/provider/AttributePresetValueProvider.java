package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;

public class AttributePresetValueProvider implements AutocompleteProvider {

    private static final Map<String, String[]> VALUES = new LinkedHashMap<>();

    static {
        VALUES.put("showIcon", new String[] { "left", "right", "true", "false" });
        VALUES.put("showTooltip", new String[] { "true", "false" });
        VALUES.put("showText", new String[] { "true", "false" });
        VALUES.put("noTooltip", new String[] { "true", "false" });
        VALUES.put("wrap", new String[] { "inline", "square", "tight", "through" });
        VALUES.put("align", new String[] { "left", "center", "right" });
        VALUES.put("float", new String[] { "none", "left", "right" });
        VALUES.put("clear", new String[] { "none", "left", "right", "all" });
        VALUES.put("valign", new String[] { "baseline", "top", "middle", "bottom" });
        VALUES.put("position", new String[] { "topLeft", "topRight", "bottomLeft", "bottomRight", "center" });
        VALUES.put("direction", new String[] { "clockwise", "counterclockwise" });
        VALUES.put("perspective", new String[] { "isometric", "front", "back", "left", "right", "top", "bottom" });
        VALUES.put("facing", new String[] { "north", "south", "west", "east", "up", "down" });
        VALUES.put("rotation", new String[] { "normal", "clockwise_90", "clockwise_180", "counterclockwise_90" });
        VALUES.put("flip", new String[] { "none", "front_back", "left_right" });
        VALUES.put("background", new String[] { "transparent", "checker", "solid" });
        VALUES.put("trigger", new String[] { "click", "hover" });
        VALUES.put("target", new String[] { "self", "blank" });
        VALUES.put("quadrants", new String[] { "all", "positive", "top", "right" });
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return Collections.singleton(AutocompleteKey.forValue("*", "*"));
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof MdxValueContext)) {
            return Collections.emptyList();
        }
        String[] values = VALUES.get(((MdxValueContext) ctx).getAttrName());
        if (values == null) {
            return Collections.emptyList();
        }
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String value : values) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || value.toLowerCase()
                .startsWith(lower)) {
                results.add(new TextCandidate(value));
            }
        }
        return results;
    }
}
