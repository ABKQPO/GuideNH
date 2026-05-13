package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

public interface AutocompleteContext {
    int replaceStart();
    int replaceEnd();
    String getPartialText();
}
