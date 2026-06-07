package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public class FencedBlockLanguageProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections.singleton(AutocompleteKey.forFenceLanguage());
    private static final String[] LANGUAGES = { "text", "java", "kotlin", "scala", "groovy", "lua", "json", "yaml",
        "xml", "properties", "bash", "sh", "powershell", "markdown", "csv", "mermaid", "tree", "filetree",
        "funcgraph" };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String lang : LANGUAGES) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || lang.toLowerCase()
                .startsWith(lower)) {
                results.add(new TextCandidate(lang));
            }
        }
        return results;
    }
}
