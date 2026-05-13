package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests empty braces for Entity data attributes. */
public class DataProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("Entity", "data"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.singletonList(new TextCandidate("{}"));
    }
}
