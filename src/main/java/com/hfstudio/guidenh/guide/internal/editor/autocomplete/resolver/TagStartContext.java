package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public class TagStartContext implements AutocompleteContext {

    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;
    @Nullable
    private final String parentTagName;

    public TagStartContext(int replaceStart, int replaceEnd, String partialText) {
        this(replaceStart, replaceEnd, partialText, null);
    }

    public TagStartContext(int replaceStart, int replaceEnd, String partialText, @Nullable String parentTagName) {
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
        this.parentTagName = parentTagName;
    }

    @Nullable
    public String getParentTagName() {
        return parentTagName;
    }

    @Override
    public int replaceStart() {
        return replaceStart;
    }

    @Override
    public int replaceEnd() {
        return replaceEnd;
    }

    @Override
    public String getPartialText() {
        return partialText;
    }
}
