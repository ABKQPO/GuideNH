package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttrType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttributeSpec;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;

public class BooleanValueProvider implements AutocompleteProvider {

    private static final String[] VALUES = { "true", "false" };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String tag : TagAttributeRegistry.getRegisteredTags()) {
            for (AttributeSpec spec : TagAttributeRegistry.get(tag)) {
                if (spec.getType() == AttrType.BOOLEAN) {
                    keys.add(AutocompleteKey.forValue(tag, spec.getName()));
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String value : VALUES) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || value.startsWith(lower)) {
                results.add(new TextCandidate(value));
            }
        }
        return results;
    }
}
