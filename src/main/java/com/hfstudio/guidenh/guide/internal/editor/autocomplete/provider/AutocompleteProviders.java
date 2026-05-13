package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAutocompleteContext;

public final class AutocompleteProviders {

    private static final List<AutocompleteProvider> providers = new ArrayList<>();

    private AutocompleteProviders() {}

    public static void register(AutocompleteProvider provider) {
        providers.add(provider);
    }

    public static List<AutocompleteCandidate> query(AutocompleteContext ctx, int limit) {
        if (!(ctx instanceof MdxAutocompleteContext)) return Collections.emptyList();
        MdxAutocompleteContext mdx = (MdxAutocompleteContext) ctx;

        List<AutocompleteCandidate> results = new ArrayList<>();
        for (AutocompleteProvider provider : providers) {
            for (AutocompleteKey key : provider.getSupportedKeys()) {
                if (key.matches(mdx.getTagName(), mdx.getAttributeName())) {
                    results.addAll(provider.provide(ctx, Math.max(0, limit - results.size())));
                    break;
                }
            }
            if (results.size() >= limit) break;
        }
        return results;
    }

    public static void clear() {
        providers.clear();
    }
}
