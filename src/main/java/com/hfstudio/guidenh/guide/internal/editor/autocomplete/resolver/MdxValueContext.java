package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Replaces the old MdxAutocompleteContext. Carries tag name, attribute name, and replacement range. */
public class MdxValueContext implements AutocompleteContext {

    private final String tagName;
    private final String attrName;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;
    private final char missingValueTerminator;

    public MdxValueContext(String tagName, String attrName, int replaceStart, int replaceEnd, String partialText) {
        this(tagName, attrName, replaceStart, replaceEnd, partialText, '\0');
    }

    public MdxValueContext(String tagName, String attrName, int replaceStart, int replaceEnd, String partialText,
        char missingValueTerminator) {
        this.tagName = tagName;
        this.attrName = attrName;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
        this.missingValueTerminator = missingValueTerminator;
    }

    public String getTagName() {
        return tagName;
    }

    public String getAttrName() {
        return attrName;
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

    public char getMissingValueTerminator() {
        return missingValueTerminator;
    }
}
