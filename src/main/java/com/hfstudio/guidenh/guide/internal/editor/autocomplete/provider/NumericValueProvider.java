package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;

/** Suggests common numeric values for INT/FLOAT attributes. */
public class NumericValueProvider implements AutocompleteProvider {

    private static final Map<String, String[]> SUGGESTIONS = new LinkedHashMap<>();
    static {
        SUGGESTIONS.put("width", new String[] { "100", "200", "256", "300", "400", "512" });
        SUGGESTIONS.put("height", new String[] { "100", "200", "256", "300", "400" });
        SUGGESTIONS.put("scale", new String[] { "0.5", "1.0", "1.5", "2.0", "3.0" });
        SUGGESTIONS.put("zoom", new String[] { "0.5", "1.0", "1.5", "2.0", "3.0" });
        SUGGESTIONS.put("gap", new String[] { "0", "5", "10", "15", "20" });
        SUGGESTIONS.put("x", new String[] { "0" });
        SUGGESTIONS.put("y", new String[] { "0" });
        SUGGESTIONS.put("z", new String[] { "0" });
        SUGGESTIONS.put("startAngle", new String[] { "-90", "0", "90", "180" });
        SUGGESTIONS.put("barWidthRatio", new String[] { "0.5", "0.7", "0.9" });
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String attr : SUGGESTIONS.keySet()) {
            keys.add(AutocompleteKey.forValue("*", attr));
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof MdxValueContext)) return Collections.emptyList();
        String attrName = ((MdxValueContext) ctx).getAttrName();
        String[] vals = SUGGESTIONS.get(attrName);
        if (vals == null) return Collections.emptyList();

        String partial = ctx.getPartialText();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String v : vals) {
            if (partial.isEmpty() || v.startsWith(partial)) {
                results.add(new TextCandidate(v));
            }
            if (results.size() >= limit) break;
        }
        return results;
    }
}
