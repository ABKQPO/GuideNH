package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;

/** Dispatches frontmatter value completion based on the current key.
 *  For now returns empty — full dispatch requires wiring to other providers. */
public class FrontmatterValueProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("*", "fm_value"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof FrontmatterContext)) return Collections.emptyList();
        FrontmatterContext fmc = (FrontmatterContext) ctx;
        String key = fmc.getKey();
        String partial = fmc.getPartialText().toLowerCase();

        // TODO: dispatch by key to specialized providers
        // "parent" → PageReferenceProvider
        // "icon", "item_ids" → ItemIdProvider
        // "ore_ids" → OreDictProvider
        // "quest_ids" → QuestIdProvider
        // "position" → numeric values

        if ("navigation".equals(key) || "authors".equals(key) || "author".equals(key)) {
            return Collections.emptyList(); // navigation is a map, authors need names
        }
        return Collections.emptyList();
    }
}
