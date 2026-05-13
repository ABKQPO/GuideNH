package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import org.jetbrains.annotations.Nullable;

public interface SyntaxContextResolver {
    @Nullable
    TextSyntaxContext resolve(String text, int cursorIndex);
}
