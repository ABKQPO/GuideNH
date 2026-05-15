package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxUtils;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

public class WordBoundaryResolver implements SyntaxContextResolver {

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty() || cursorIndex < 0 || cursorIndex > text.length()) {
            return null;
        }

        if (!SyntaxUtils.isWordChar(text.charAt(Math.min(cursorIndex, text.length() - 1)))) {
            return new TextSyntaxContext(SyntaxElementType.OTHER, cursorIndex, cursorIndex, null);
        }

        return SyntaxUtils.resolveWord(text, cursorIndex);
    }
}
