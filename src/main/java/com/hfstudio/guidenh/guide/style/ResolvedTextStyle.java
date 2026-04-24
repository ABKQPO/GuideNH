package com.hfstudio.guidenh.guide.style;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.color.ColorValue;

/**
 * Represents the styling of text for rendering.
 */
@Desugar
public record ResolvedTextStyle(float fontScale, boolean bold, boolean italic, boolean underlined,
    boolean strikethrough, boolean obfuscated, String font, ColorValue color, WhiteSpaceMode whiteSpace,
    TextAlignment alignment, boolean dropShadow) {}
