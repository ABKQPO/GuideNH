package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public final class AutocompleteProviders {

    private static final List<AutocompleteProvider> providers = new ArrayList<>();

    private AutocompleteProviders() {}

    public static void register(AutocompleteProvider provider) {
        providers.add(provider);
    }

    public static List<AutocompleteCandidate> query(AutocompleteContext ctx, int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (AutocompleteProvider provider : providers) {
            for (AutocompleteKey key : provider.getSupportedKeys()) {
                if (matchesContext(key, ctx)) {
                    results.addAll(provider.provide(ctx, Math.max(0, limit - results.size())));
                    break;
                }
            }
            if (results.size() >= limit) break;
        }
        return results;
    }

    private static boolean matchesContext(AutocompleteKey key, AutocompleteContext ctx) {
        if (ctx instanceof com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext) {
            com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext mdx =
                (com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext) ctx;
            return key.matches(AutocompleteKey.MatchType.ATTR_VALUE, mdx.getTagName(), mdx.getAttrName());
        }
        if (ctx instanceof com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext) {
            com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext mdx =
                (com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext) ctx;
            return key.matches(AutocompleteKey.MatchType.ATTR_NAME, mdx.getTagName(), null);
        }
        if (ctx instanceof com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext) {
            return key.matches(AutocompleteKey.MatchType.TAG_NAME, null, null);
        }
        if (ctx instanceof com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext) {
            com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext fmc =
                (com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext) ctx;
            String attr = fmc.isValue() ? "fm_value" : "fm_key";
            return key.matches(AutocompleteKey.MatchType.ATTR_VALUE, "*", attr);
        }
        return false;
    }

    public static void clear() {
        providers.clear();
    }
}
