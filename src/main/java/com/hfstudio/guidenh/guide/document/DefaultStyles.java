package com.hfstudio.guidenh.guide.document;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public final class DefaultStyles {

    private DefaultStyles() {}

    private static final String UNIFORM_FONT = null;

    /**
     * The base style everything else is based on.
     */
    public static final ResolvedTextStyle BASE_STYLE = new ResolvedTextStyle(
        1,
        false,
        false,
        false,
        false,
        false,
        UNIFORM_FONT,
        SymbolicColor.BODY_TEXT,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false);

    public static final TextStyle BODY_TEXT = TextStyle.builder()
        .font(UNIFORM_FONT)
        .color(SymbolicColor.BODY_TEXT)
        .build();

    public static final TextStyle ERROR_TEXT = TextStyle.builder()
        .color(SymbolicColor.ERROR_TEXT)
        .build();

    public static final TextStyle CRAFTING_RECIPE_TYPE = TextStyle.builder()
        .font(UNIFORM_FONT)
        .color(SymbolicColor.CRAFTING_RECIPE_TYPE)
        .build();

    public static final TextStyle HEADING1 = TextStyle.builder()
        .fontScale(1.3f)
        .bold(true)
        .font(null)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING2 = TextStyle.builder()
        .fontScale(1.1f)
        .font(null)
        .build();
    public static final TextStyle HEADING3 = TextStyle.builder()
        .fontScale(1f)
        .font(null)
        .build();
    public static final TextStyle HEADING4 = TextStyle.builder()
        .fontScale(1.1f)
        .bold(true)
        .font(UNIFORM_FONT)
        .build();
    public static final TextStyle HEADING5 = TextStyle.builder()
        .fontScale(1f)
        .bold(true)
        .font(UNIFORM_FONT)
        .build();
    public static final TextStyle HEADING6 = TextStyle.builder()
        .fontScale(1f)
        .font(UNIFORM_FONT)
        .build();

    public static final TextStyle SEARCH_RESULT_HIGHLIGHT = TextStyle.builder()
        .bold(true)
        .underlined(true)
        .build();
}
