package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

// TODO: re-enable when fence-block context resolver is implemented.
// Currently forTag() causes these suggestions to appear in the tag name popup.

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
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String lang : LANGUAGES) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || lang.toLowerCase().startsWith(partial)) {
                results.add(new TextCandidate(lang));
            }
        }
        return results;
    }
}
