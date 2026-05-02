package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Marker drawn on top of the curves. A point either has explicit {@code (x, y)} coordinates, or it
 * references a {@link FunctionPlot} index plus a known-axis value to be solved for the other axis.
 */
public final class MarkedPoint {

    /** Explicit (x, y). */
    public static final int MODE_EXPLICIT = 0;
    /** Plot lookup with known x. */
    public static final int MODE_PLOT_AT_X = 1;
    /** Plot lookup with known y; requires bisection on the plot domain. */
    public static final int MODE_PLOT_AT_Y = 2;

    private final int mode;
    private final int plotIndex;
    private final double valueA;
    private final double valueB;
    private final int color;
    private final boolean colorInherit;
    private final String label;

    public MarkedPoint(int mode, int plotIndex, double valueA, double valueB, int color, boolean colorInherit,
        String label) {
        this.mode = mode;
        this.plotIndex = plotIndex;
        this.valueA = valueA;
        this.valueB = valueB;
        this.color = color;
        this.colorInherit = colorInherit;
        this.label = label;
    }

    public int getMode() {
        return mode;
    }

    public int getPlotIndex() {
        return plotIndex;
    }

    public double getValueA() {
        return valueA;
    }

    public double getValueB() {
        return valueB;
    }

    public int getColor() {
        return color;
    }

    public boolean isColorInherit() {
        return colorInherit;
    }

    public String getLabel() {
        return label;
    }
}
