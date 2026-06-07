package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests Block registry names for block-related tag "id" attributes. */
public class BlockIdProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = buildKeys("BlockImage", "PlaceBlock", "RemoveBlocks", "Block");

    private static Set<AutocompleteKey> buildKeys(String... tagNames) {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String tag : tagNames) {
            keys.add(AutocompleteKey.forValue(tag, "id"));
        }
        // Also match ReplaceBlock's "from" and "to" attributes
        keys.add(AutocompleteKey.forValue("ReplaceBlock", "from"));
        keys.add(AutocompleteKey.forValue("ReplaceBlock", "to"));
        keys.add(AutocompleteKey.forValue("BlockAnnotationTemplate", "id"));
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (Object obj : Block.blockRegistry.getKeys()) {
            if (results.size() >= limit) break;
            if (obj instanceof String key) {
                if (partial.isEmpty() || key.toLowerCase()
                    .contains(partial)) {
                    Block block = (Block) Block.blockRegistry.getObject(key);
                    ItemStack stack = block != null ? new ItemStack(Item.getItemFromBlock(block)) : null;
                    if (stack != null && stack.getItem() != null) {
                        results.add(new RegistryCandidate(key, stack));
                    } else {
                        results.add(new TextCandidate(key));
                    }
                }
            }
        }
        return results;
    }
}
