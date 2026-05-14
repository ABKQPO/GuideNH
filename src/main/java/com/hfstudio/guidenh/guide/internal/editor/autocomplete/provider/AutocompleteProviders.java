package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FenceLanguageContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.FrontmatterContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;

public class AutocompleteProviders {

    private static final List<AutocompleteProvider> providers = new ArrayList<>();
    private static final Map<String, List<AutocompleteProvider>> providersByKey = new HashMap<>();

    private AutocompleteProviders() {}

    public static void register(AutocompleteProvider provider) {
        providers.add(provider);
        for (AutocompleteKey key : provider.getSupportedKeys()) {
            providersByKey
                .computeIfAbsent(
                    toLookupKey(key.getType(), key.getTagName(), key.getAttrName()),
                    ignored -> new ArrayList<>())
                .add(provider);
        }
    }

    public static List<AutocompleteCandidate> query(AutocompleteContext ctx, int limit) {
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (AutocompleteProvider provider : resolveProviders(ctx)) {
            results.addAll(provider.provide(ctx, Math.max(0, limit - results.size())));
            if (results.size() >= limit) break;
        }
        return results;
    }

    private static List<AutocompleteProvider> resolveProviders(AutocompleteContext ctx) {
        Set<AutocompleteProvider> matched = new LinkedHashSet<>();
        if (ctx instanceof MdxValueContext) {
            MdxValueContext mdx = (MdxValueContext) ctx;
            addValueProviders(matched, mdx.getTagName(), mdx.getAttrName());
            return preserveRegistrationOrder(matched);
        }
        if (ctx instanceof MdxAttrNameContext) {
            MdxAttrNameContext mdx = (MdxAttrNameContext) ctx;
            addProviders(matched, AutocompleteKey.MatchType.ATTR_NAME, mdx.getTagName(), null);
            addProviders(matched, AutocompleteKey.MatchType.ATTR_NAME, "*", null);
            return preserveRegistrationOrder(matched);
        }
        if (ctx instanceof TagStartContext) {
            addProviders(matched, AutocompleteKey.MatchType.TAG_NAME, null, null);
            return preserveRegistrationOrder(matched);
        }
        if (ctx instanceof FenceLanguageContext) {
            addProviders(matched, AutocompleteKey.MatchType.FENCE_LANGUAGE, null, null);
            return preserveRegistrationOrder(matched);
        }
        if (ctx instanceof FrontmatterContext) {
            FrontmatterContext fmc = (FrontmatterContext) ctx;
            String attr = fmc.isValue() ? fmc.getKey() : "fm_key";
            addValueProviders(matched, "*", attr);
            return preserveRegistrationOrder(matched);
        }
        return providers;
    }

    private static List<AutocompleteProvider> preserveRegistrationOrder(Set<AutocompleteProvider> matched) {
        List<AutocompleteProvider> ordered = new ArrayList<>();
        for (AutocompleteProvider provider : providers) {
            if (matched.contains(provider)) {
                ordered.add(provider);
            }
        }
        return ordered;
    }

    private static void addValueProviders(Set<AutocompleteProvider> matched, String tagName, String attrName) {
        addProviders(matched, AutocompleteKey.MatchType.ATTR_VALUE, tagName, attrName);
        addProviders(matched, AutocompleteKey.MatchType.ATTR_VALUE, tagName, "*");
        addProviders(matched, AutocompleteKey.MatchType.ATTR_VALUE, "*", attrName);
        addProviders(matched, AutocompleteKey.MatchType.ATTR_VALUE, "*", "*");
    }

    private static void addProviders(Set<AutocompleteProvider> matched, AutocompleteKey.MatchType type, String tagName,
        String attrName) {
        List<AutocompleteProvider> indexed = providersByKey.get(toLookupKey(type, tagName, attrName));
        if (indexed != null) {
            matched.addAll(indexed);
        }
    }

    private static String toLookupKey(AutocompleteKey.MatchType type, String tagName, String attrName) {
        return type.name() + '\u0001'
            + (tagName != null ? tagName : "")
            + '\u0001'
            + (attrName != null ? attrName : "");
    }

    public static void clear() {
        providers.clear();
        providersByKey.clear();
    }
}
