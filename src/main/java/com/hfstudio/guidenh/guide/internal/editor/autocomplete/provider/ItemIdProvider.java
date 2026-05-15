package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public class ItemIdProvider implements AutocompleteProvider {

    // Tags whose "id" attribute refers to a Minecraft item registry key
    private static Set<AutocompleteKey> KEYS = buildKeys(
        // item id attributes
        "ItemImage",
        "ItemLink",
        "Recipe",
        "RecipeFor",
        "RecipesFor",
        // recipe filter attributes
        "Recipe",
        "RecipeFor",
        "RecipesFor");

    // Also add these extra keys after buildKeys:
    static {
        Set<AutocompleteKey> allKeys = new HashSet<>(KEYS);
        allKeys.add(AutocompleteKey.forValue("Recipe", "input"));
        allKeys.add(AutocompleteKey.forValue("Recipe", "output"));
        allKeys.add(AutocompleteKey.forValue("RecipeFor", "input"));
        allKeys.add(AutocompleteKey.forValue("RecipeFor", "output"));
        allKeys.add(AutocompleteKey.forValue("RecipesFor", "input"));
        allKeys.add(AutocompleteKey.forValue("RecipesFor", "output"));
        allKeys.add(AutocompleteKey.forValue("ItemIcon", "id"));
        KEYS = Collections.unmodifiableSet(allKeys);
    }

    private static Set<AutocompleteKey> buildKeys(String... tagNames) {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String tag : tagNames) {
            keys.add(AutocompleteKey.forValue(tag, "id"));
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";

        List<AutocompleteCandidate> results = new ArrayList<>();
        if (lower.indexOf(':') < 0) {
            addNamespaceCandidates(results, lower, limit);
        }
        for (Object obj : Item.itemRegistry.getKeys()) {
            if (results.size() >= limit) break;
            if (obj instanceof String key) {
                if (lower.isEmpty() || key.toLowerCase()
                    .contains(lower)) {
                    Item item = (Item) Item.itemRegistry.getObject(key);
                    if (item != null) {
                        results.add(new ItemCandidate(key, new ItemStack(item)));
                    }
                }
            }
        }
        return results;
    }

    private void addNamespaceCandidates(List<AutocompleteCandidate> results, String lower, int limit) {
        Set<String> namespaces = new LinkedHashSet<>();
        for (Object obj : Item.itemRegistry.getKeys()) {
            if (!(obj instanceof String)) continue;
            String key = (String) obj;
            int separator = key.indexOf(':');
            if (separator <= 0) continue;
            String namespace = key.substring(0, separator);
            String namespaceLower = namespace.toLowerCase();
            if (lower.isEmpty() || namespaceLower.startsWith(lower)) {
                namespaces.add(namespace);
            }
        }
        for (String namespace : namespaces) {
            if (results.size() >= limit) break;
            results.add(new TextCandidate(namespace + ":"));
        }
    }
}
