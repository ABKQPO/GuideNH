package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttributeSpec;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext;

/** Suggests valid attribute names for the current MDX tag. */
public class AttributeNameProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forAttr("*"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    //
    // TODO: Currently disabled because the resolver layer (MdxSyntaxResolver) does not
    // reliably distinguish attribute-name positions from plain text. When the parser
    // supports error-recovery or the resolver can accurately detect tag-internal
    // whitespace without false positives, set this to true.
    //
    private static volatile boolean enabled = false;

    public static void setEnabled(boolean value) { enabled = value; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!enabled) return Collections.emptyList();
        if (!(ctx instanceof MdxAttrNameContext)) return Collections.emptyList();
        MdxAttrNameContext mdx = (MdxAttrNameContext) ctx;

        List<AttributeSpec> specs = TagAttributeRegistry.get(mdx.getTagName());
        String partial = mdx.getPartialText().toLowerCase();

        List<AutocompleteCandidate> results = new ArrayList<>();
        for (AttributeSpec spec : specs) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || spec.getName().toLowerCase().contains(partial)) {
                results.add(new TextCandidate(spec.getName()));
            }
        }
        return results;
    }
}
