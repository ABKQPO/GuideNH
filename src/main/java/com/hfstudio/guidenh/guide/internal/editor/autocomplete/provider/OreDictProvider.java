package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests OreDictionary names for "ore" attributes. */
public class OreDictProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(AutocompleteKey.forValue("*", "ore"), AutocompleteKey.forValue("*", "ore_ids"))));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String name : OreDictionary.getOreNames()) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || name.toLowerCase()
                .contains(partial)) {
                results.add(new TextCandidate(name));
            }
        }
        return results;
    }
}
