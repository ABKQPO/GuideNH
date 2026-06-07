package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;

/** Suggests valid YAML frontmatter top-level keys. */
public class FrontmatterKeyProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections.singleton(AutocompleteKey.forValue("*", "fm_key"));

    private static final String[] KEYS_LIST = { "navigation", "item_ids", "ore_ids", "quest_ids", "categories",
        "authors", "author", "date", "updated", "zoom" };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof FrontmatterContext)) return Collections.emptyList();
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String key : KEYS_LIST) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || key.toLowerCase()
                .contains(partial)) {
                results.add(new TextCandidate(key));
            }
        }
        return results;
    }
}
