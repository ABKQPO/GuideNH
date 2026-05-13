package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests vector format templates for pos/from/to/min/max attributes. */
public class Vector3Provider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = buildKeys();

    private static Set<AutocompleteKey> buildKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        String[] tags = {"Block", "BlockAnnotation", "BoxAnnotation",
            "DiamondAnnotation", "LineAnnotation", "TextAnnotation",
            "PlaceBlock", "ReplaceBlock", "ImportStructure", "ImportStructureLib"};
        String[] attrs = {"pos", "from", "to", "min", "max", "offsetX", "offsetY", "offsetZ",
            "centerX", "centerY", "centerZ", "headRotation", "leftArmRotation", "rightArmRotation",
            "leftLegRotation", "rightLegRotation", "capeRotation"};
        for (String tag : tags) {
            for (String attr : attrs) {
                keys.add(AutocompleteKey.forValue(tag, attr));
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.singletonList(new TextCandidate("0 0 0"));
    }
}
