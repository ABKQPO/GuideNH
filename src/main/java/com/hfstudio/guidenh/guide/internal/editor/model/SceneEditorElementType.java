package com.hfstudio.guidenh.guide.internal.editor.model;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public enum SceneEditorElementType {

    BLOCK("BlockAnnotation", GuidebookText.SceneEditorElementBlock, "guidenh:textures/guide/buttons.png", 'B',
        0xFF9FC6FF),
    BOX("BoxAnnotation", GuidebookText.SceneEditorElementBox, "guidenh:textures/guide/buttons.png", 'O', 0xFFFFC07A),
    LINE("LineAnnotation", GuidebookText.SceneEditorElementLine, "guidenh:textures/guide/buttons.png", 'L', 0xFF9FFFB0),
    DIAMOND("DiamondAnnotation", GuidebookText.SceneEditorElementDiamond, "guidenh:textures/guide/diamond.png", 'D',
        0xFFFFE16A);

    private final String tagName;
    private final GuidebookText textKey;
    private final String iconPngPath;
    private final char fallbackGlyph;
    private final int accentColor;

    SceneEditorElementType(String tagName, GuidebookText textKey, String iconPngPath, char fallbackGlyph,
        int accentColor) {
        this.tagName = tagName;
        this.textKey = textKey;
        this.iconPngPath = iconPngPath;
        this.fallbackGlyph = fallbackGlyph;
        this.accentColor = accentColor;
    }

    public String getTagName() {
        return tagName;
    }

    public String getDisplayText() {
        return textKey.text();
    }

    public GuidebookText getTextKey() {
        return textKey;
    }

    public String getIconPngPath() {
        return iconPngPath;
    }

    public char getFallbackGlyph() {
        return fallbackGlyph;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
