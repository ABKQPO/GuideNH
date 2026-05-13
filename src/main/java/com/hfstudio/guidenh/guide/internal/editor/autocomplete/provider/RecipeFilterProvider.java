package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests item IDs for Recipe input/output filters, with DNF format hint. */
public class RecipeFilterProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = new HashSet<>(Arrays.asList(
        AutocompleteKey.forValue("Recipe", "input"),
        AutocompleteKey.forValue("Recipe", "output"),
        AutocompleteKey.forValue("RecipesFor", "input"),
        AutocompleteKey.forValue("RecipesFor", "output")
    ));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (Object obj : Item.itemRegistry.getKeys()) {
            if (results.size() >= limit) break;
            if (obj instanceof String key) {
                if (partial.isEmpty() || key.toLowerCase().contains(partial)) {
                    Item item = (Item) Item.itemRegistry.getObject(key);
                    if (item != null) {
                        results.add(new ItemCandidate(key, new ItemStack(item)));
                    }
                }
            }
        }
        return results;
    }
}
