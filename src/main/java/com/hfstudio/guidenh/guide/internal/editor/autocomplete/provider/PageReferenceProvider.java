package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests guide page paths for href, linksTo, and SubPages id attributes.
 *  Requires a PageCollection reference — for now returns empty; enable after wiring. */
public class PageReferenceProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = buildKeys();

    private static Set<AutocompleteKey> buildKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        keys.add(AutocompleteKey.forValue("a", "href"));
        keys.add(AutocompleteKey.forValue("SubPages", "id"));
        keys.add(AutocompleteKey.forValue("ItemLink", "linksTo"));
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        // TODO: wire PageCollection reference to enumerate page paths
        return Collections.emptyList();
    }
}
