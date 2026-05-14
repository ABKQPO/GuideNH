package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

/** Chains multiple resolvers; first non-null result wins. */
public class CompositeResolver implements SyntaxContextResolver {

    private final List<SyntaxContextResolver> chain;

    public CompositeResolver(SyntaxContextResolver... resolvers) {
        this.chain = Arrays.asList(resolvers);
    }

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        for (SyntaxContextResolver r : chain) {
            TextSyntaxContext ctx = r.resolve(text, cursorIndex);
            if (ctx != null) return ctx;
        }
        return null;
    }
}
