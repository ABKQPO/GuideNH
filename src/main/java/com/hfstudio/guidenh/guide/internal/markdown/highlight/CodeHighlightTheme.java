package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.EnumMap;
import java.util.Map;

public class CodeHighlightTheme {

    public static final CodeHighlightTheme GITHUB_DARK_DEFAULT = githubDarkDefault();

    private final int backgroundArgb;
    private final int toolbarBackgroundArgb;
    private final int borderArgb;
    private final int scrollbarTrackArgb;
    private final int scrollbarThumbArgb;
    private final int scrollbarThumbActiveArgb;
    private final int toolbarTextArgb;
    private final Map<CodeTokenType, Integer> tokenColors;

    public CodeHighlightTheme(int backgroundArgb, int toolbarBackgroundArgb, int borderArgb, int scrollbarTrackArgb,
        int scrollbarThumbArgb, int scrollbarThumbActiveArgb, int toolbarTextArgb,
        Map<CodeTokenType, Integer> tokenColors) {
        this.backgroundArgb = backgroundArgb;
        this.toolbarBackgroundArgb = toolbarBackgroundArgb;
        this.borderArgb = borderArgb;
        this.scrollbarTrackArgb = scrollbarTrackArgb;
        this.scrollbarThumbArgb = scrollbarThumbArgb;
        this.scrollbarThumbActiveArgb = scrollbarThumbActiveArgb;
        this.toolbarTextArgb = toolbarTextArgb;
        this.tokenColors = Map.copyOf(tokenColors);
    }

    public int backgroundArgb() {
        return backgroundArgb;
    }

    public int toolbarBackgroundArgb() {
        return toolbarBackgroundArgb;
    }

    public int borderArgb() {
        return borderArgb;
    }

    public int scrollbarTrackArgb() {
        return scrollbarTrackArgb;
    }

    public int scrollbarThumbArgb() {
        return scrollbarThumbArgb;
    }

    public int scrollbarThumbActiveArgb() {
        return scrollbarThumbActiveArgb;
    }

    public int toolbarTextArgb() {
        return toolbarTextArgb;
    }

    public int colorOf(CodeTokenType type) {
        return tokenColors.getOrDefault(type, tokenColors.get(CodeTokenType.PLAIN));
    }

    private static CodeHighlightTheme githubDarkDefault() {
        Map<CodeTokenType, Integer> colors = new EnumMap<>(CodeTokenType.class);
        colors.put(CodeTokenType.PLAIN, 0xFFE6EDF3);
        colors.put(CodeTokenType.KEYWORD, 0xFFFF7B72);
        colors.put(CodeTokenType.STRING, 0xFFA5D6FF);
        colors.put(CodeTokenType.NUMBER, 0xFF79C0FF);
        colors.put(CodeTokenType.COMMENT, 0xFF8B949E);
        colors.put(CodeTokenType.OPERATOR, 0xFFFF7B72);
        colors.put(CodeTokenType.PUNCTUATION, 0xFFE6EDF3);
        colors.put(CodeTokenType.TYPE, 0xFF7EE787);
        colors.put(CodeTokenType.FUNCTION, 0xFFD2A8FF);
        colors.put(CodeTokenType.ANNOTATION, 0xFFFFA657);
        colors.put(CodeTokenType.PROPERTY, 0xFF79C0FF);
        return new CodeHighlightTheme(
            0xFF0D1117,
            0xFF161B22,
            0xFF30363D,
            0x4D6E7681,
            0x80768496,
            0xCC768496,
            0xFF8B949E,
            colors);
    }
}
