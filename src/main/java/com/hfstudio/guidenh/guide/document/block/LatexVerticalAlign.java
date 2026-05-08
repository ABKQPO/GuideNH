package com.hfstudio.guidenh.guide.document.block;

/**
 * Vertical alignment mode for inline {@link LytLatexBlock} relative to the surrounding text line.
 * Determines how the formula is positioned vertically when placed inside a text flow.
 */
public enum LatexVerticalAlign {

    /**
     * Formula top aligns with the top of the text line.
     * The formula may extend below the normal text bottom when it is taller than the text.
     */
    TOP,

    /**
     * Formula is vertically centered on the surrounding text line height.
     * This is the default; tall formulas grow symmetrically around the text mid-line.
     */
    CENTER,

    /**
     * Formula bottom aligns with the bottom of the text line.
     * Tall formulas extend above the normal text top.
     */
    BOTTOM;

    /**
     * Parses a string value (case-insensitive) and returns the matching enum constant,
     * or {@link #CENTER} if the value is not recognised.
     *
     * @param value the raw attribute string, may be {@code null}
     * @return the corresponding {@link LatexVerticalAlign} constant
     */
    public static LatexVerticalAlign parse(String value) {
        if (value == null) {
            return CENTER;
        }
        return switch (value.trim()
            .toLowerCase()) {
            case "top" -> TOP;
            case "bottom" -> BOTTOM;
            default -> CENTER;
        };
    }
}
