package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public interface AutocompleteProvider {

    Set<AutocompleteKey> getSupportedKeys();

    List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit);
}
