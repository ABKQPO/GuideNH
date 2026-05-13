package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttributeSpec;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttrType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;

/** Suggests true/false for boolean-typed attributes. */
public class BooleanProvider implements AutocompleteProvider {

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
        return Arrays.asList(new TextCandidate("true"), new TextCandidate("false"));
    }
}
