package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests domain interval templates for domain attributes. */
public class DomainProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = new HashSet<>(Arrays.asList(
        AutocompleteKey.forValue("Function", "domain"),
        AutocompleteKey.forValue("Plot", "domain")
    ));

    private static final String[] DOMAINS = {
        "(-inf, inf)", "[-10, 10]", "(-pi, pi)", "[0, 2*pi)",
        "(-inf, 0]", "[0, inf)", "(-5, 5)", "[-1, 1]"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String d : DOMAINS) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || d.toLowerCase().contains(partial)) {
                results.add(new TextCandidate(d));
            }
        }
        return results;
    }
}
