package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests SNBT brace template for NBT-typed attributes. */
public class NbtProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = new HashSet<>(Arrays.asList(
        AutocompleteKey.forValue("Block", "nbt"),
        AutocompleteKey.forValue("PlaceBlock", "nbt"),
        AutocompleteKey.forValue("ReplaceBlock", "from_nbt"),
        AutocompleteKey.forValue("ReplaceBlock", "to_nbt")
    ));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.singletonList(new TextCandidate("{}"));
    }
}
