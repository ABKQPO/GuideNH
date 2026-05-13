package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import net.minecraft.entity.EntityList;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests entity registry names for &lt;Entity id&gt; attributes. */
public class EntityNameProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("Entity", "id"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    @SuppressWarnings("unchecked")
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (Object obj : EntityList.stringToClassMapping.keySet()) {
            if (results.size() >= limit) break;
            if (obj instanceof String key) {
                if (partial.isEmpty() || key.toLowerCase().contains(partial)) {
                    results.add(new TextCandidate(key));
                }
            }
        }
        return results;
    }
}
