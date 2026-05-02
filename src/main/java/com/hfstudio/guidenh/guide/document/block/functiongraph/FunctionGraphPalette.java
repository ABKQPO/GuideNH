package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Deterministic palette used when {@link FunctionPlot} authors omit an explicit colour. Colours are
 * picked by plot index so re-rendering the same page does not flicker.
 */
public final class FunctionGraphPalette {

    private static final int[] COLORS = new int[] { 0xFFE15759, 0xFF4E79A7, 0xFF59A14F, 0xFFF28E2B, 0xFF76B7B2,
        0xFFB07AA1, 0xFFEDC948, 0xFF9C755F, 0xFFFF9DA7, 0xFF1F77B4, 0xFFFF7F0E, 0xFF2CA02C, 0xFFD62728, 0xFF9467BD };

    private FunctionGraphPalette() {}

    public static int color(int index) {
        int n = COLORS.length;
        int i = ((index % n) + n) % n;
        return COLORS[i];
    }
}
