package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests format patterns for &lt;ItemImage format&gt;. */
public class FormatPatternProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("ItemImage", "format"));

    private static final String[] PATTERNS = {
        "%s", "%d", "%.1f", "%.2f", "%s items", "x%d", "%d / %d"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String p : PATTERNS) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || p.toLowerCase().contains(partial)) {
                results.add(new TextCandidate(p));
            }
        }
        return results;
    }
}
