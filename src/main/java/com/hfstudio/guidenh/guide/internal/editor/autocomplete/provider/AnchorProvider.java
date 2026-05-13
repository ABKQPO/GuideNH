package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests heading anchors for &lt;a href="#..."&gt; attributes.
 *  Requires current document context — for now returns empty; enable after wiring. */
public class AnchorProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("a", "href"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        if (partial == null || !partial.startsWith("#")) return Collections.emptyList();
        // TODO: parse current document for headings and anchor names
        return Collections.emptyList();
    }
}
