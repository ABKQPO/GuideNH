package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttrType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttributeSpec;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;

public class EnumValueProvider implements AutocompleteProvider {

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String tag : TagAttributeRegistry.getRegisteredTags()) {
            for (AttributeSpec spec : TagAttributeRegistry.get(tag)) {
                if (spec.getType() == AttrType.ENUM) {
                    keys.add(AutocompleteKey.forValue(tag, spec.getName()));
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof MdxValueContext)) return Collections.emptyList();
        MdxValueContext mdx = (MdxValueContext) ctx;

        for (AttributeSpec spec : TagAttributeRegistry.get(mdx.getTagName())) {
            if (!spec.getName()
                .equals(mdx.getAttrName())) continue;
            if (spec.getType() != AttrType.ENUM || spec.getEnumClass() == null) continue;

            String partial = mdx.getPartialText()
                .toLowerCase();
            List<AutocompleteCandidate> results = new ArrayList<>();
            for (Enum<?> e : spec.getEnumClass()
                .getEnumConstants()) {
                if (results.size() >= limit) break;
                String name = e.name();
                if (e instanceof SerializedEnum) {
                    name = ((SerializedEnum) e).getSerializedName();
                }
                if (partial.isEmpty() || name.toLowerCase()
                    .contains(partial)) {
                    results.add(new TextCandidate(name));
                }
            }
            return results;
        }
        return Collections.emptyList();
    }
}
