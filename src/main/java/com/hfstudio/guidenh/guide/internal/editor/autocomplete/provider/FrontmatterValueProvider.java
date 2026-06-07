package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;

/** Dispatches frontmatter value completion based on the current key. */
public class FrontmatterValueProvider implements AutocompleteProvider {

    private static final Map<String, String[]> HINTS = new LinkedHashMap<>();
    static {
        HINTS.put(
            "navigation",
            new String[] { "\n  title:", "\n  parent:", "\n  position:", "\n  icon:", "\n  icon_texture:" });
        // TODO: integrate with BetterQuesting for dynamic quest UUID lookup
        HINTS.put("quest_ids", new String[] { "\n  - 00000000-0000-0000-0000-000000000000" });
    }

    private static final Set<AutocompleteKey> KEYS = buildKeys();

    private static Set<AutocompleteKey> buildKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String k : HINTS.keySet()) {
            keys.add(AutocompleteKey.forValue("*", k));
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof FrontmatterContext)) return Collections.emptyList();
        FrontmatterContext fmc = (FrontmatterContext) ctx;
        String[] suggestions = HINTS.getOrDefault(fmc.getKey(), new String[0]);
        String partial = fmc.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String s : suggestions) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || s.toLowerCase()
                .contains(partial)) {
                results.add(new TextCandidate(s));
            }
        }
        return results;
    }
}
