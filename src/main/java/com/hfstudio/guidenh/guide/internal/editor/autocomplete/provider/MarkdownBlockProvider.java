package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests block-level markdown syntax templates. Stub — currently no trigger context wired. */
public class MarkdownBlockProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forTag());

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.emptyList(); // TODO: activate when line-start context triggers
    }
}
