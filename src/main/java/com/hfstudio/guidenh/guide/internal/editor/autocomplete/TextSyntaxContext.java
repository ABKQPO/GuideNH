package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import org.jetbrains.annotations.Nullable;

public class TextSyntaxContext {

    private final SyntaxElementType elementType;
    private final int elementStart;
    private final int elementEnd;
    @Nullable
    private final AutocompleteContext autocomplete;

    public TextSyntaxContext(SyntaxElementType elementType, int elementStart, int elementEnd,
        @Nullable AutocompleteContext autocomplete) {
        this.elementType = elementType;
        this.elementStart = elementStart;
        this.elementEnd = elementEnd;
        this.autocomplete = autocomplete;
    }

    public SyntaxElementType getElementType() {
        return elementType;
    }

    public int getElementStart() {
        return elementStart;
    }

    public int getElementEnd() {
        return elementEnd;
    }

    @Nullable
    public AutocompleteContext getAutocomplete() {
        return autocomplete;
    }

    public boolean shouldAutocomplete() {
        return autocomplete != null;
    }
}
