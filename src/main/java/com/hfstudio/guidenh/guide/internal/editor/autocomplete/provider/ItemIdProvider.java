package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        // Frontmatter wildcards
        allKeys.add(AutocompleteKey.forValue("*", "item_ids"));
        allKeys.add(AutocompleteKey.forValue("*", "icon"));
        KEYS = Collections.unmodifiableSet(allKeys);
    }

    private List<String> cachedSortedKeys = Collections.emptyList();
    private int cachedRegistryKeyCount = -1;

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
        List<String> keys = getSortedRegistryKeys();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String candidate : collectCandidateTexts(keys, partial, limit, false)) {
            if (candidate.endsWith(":")) {
                results.add(new TextCandidate(candidate));
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(candidate);
            if (item != null) {
                results.add(new ItemCandidate(candidate, new ItemStack(item)));
            }
        }
        return results;
    }

    private List<String> getSortedRegistryKeys() {
        Set<?> keysView = Item.itemRegistry.getKeys();
        if (cachedRegistryKeyCount == keysView.size()) {
            return cachedSortedKeys;
        }
        List<String> keys = new ArrayList<>();
        for (Object obj : keysView) {
            if (obj instanceof String key) {
                keys.add(key);
            }
        }
        cachedSortedKeys = sortedKeys(keys);
        cachedRegistryKeyCount = keysView.size();
        return cachedSortedKeys;
    }

    public static List<String> collectCandidateTexts(List<String> keys, String partial, int limit) {
        return collectCandidateTexts(keys, partial, limit, true);
    }

    private static List<String> collectCandidateTexts(List<String> keys, String partial, int limit, boolean sortKeys) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit <= 0) {
            return Collections.emptyList();
        }
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<String> candidates = new ArrayList<>(Math.min(safeLimit, keys.size()));
        List<String> sortedKeys = sortKeys ? sortedKeys(keys) : keys;
        if (lower.indexOf(':') < 0) {
            addNamespaceCandidateTexts(candidates, sortedKeys, lower, safeLimit);
        }
        addItemCandidateTexts(candidates, sortedKeys, lower, safeLimit);
        return candidates;
    }

    private static void addNamespaceCandidateTexts(List<String> results, List<String> sortedKeys, String lower,
        int limit) {
        Set<String> namespaces = new LinkedHashSet<>();
        for (String key : sortedKeys) {
            int separator = key.indexOf(':');
            if (separator <= 0) continue;
            String namespace = key.substring(0, separator);
            String namespaceLower = namespace.toLowerCase(Locale.ROOT);
            if (lower.isEmpty() || namespaceLower.startsWith(lower)) {
                namespaces.add(namespace);
            }
        }
        for (String namespace : namespaces) {
            if (results.size() >= limit) break;
            results.add(namespace + ":");
        }
    }

    private static void addItemCandidateTexts(List<String> results, List<String> sortedKeys, String lower, int limit) {
        for (String key : sortedKeys) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || key.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(key);
            }
        }
    }

    private static List<String> sortedKeys(List<String> keys) {
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(
            Comparator.comparing((String key) -> key.toLowerCase(Locale.ROOT))
                .thenComparing(Comparator.naturalOrder()));
        return sorted;
    }
}
