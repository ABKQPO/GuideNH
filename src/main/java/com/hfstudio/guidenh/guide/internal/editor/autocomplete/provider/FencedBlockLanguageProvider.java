package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests language identifiers after ``` for fenced code blocks. */
public class FencedBlockLanguageProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forTag());

    private static final String[] LANGUAGES = {
        "java", "python", "javascript", "json", "yaml", "xml",
        "sh", "bash", "text", "funcgraph", "mermaid"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.emptyList(); // TODO: activate when ``` context is detected
    }
}
