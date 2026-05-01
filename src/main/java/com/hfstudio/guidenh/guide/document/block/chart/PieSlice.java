package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * A single slice of a pie chart.
 */
public class PieSlice {

    private final String label;
    private final double value;
    private final int color;
    private ChartIcon icon;
    private String tooltipExtra;

    public PieSlice(String label, double value, int color) {
        this.label = label != null ? label : "";
        this.value = value;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public double getValue() {
        return value;
    }

    public int getColor() {
        return color;
    }

    public ChartIcon getIcon() {
        return icon;
    }

    public void setIcon(ChartIcon icon) {
        this.icon = icon;
    }

    public String getTooltipExtra() {
        return tooltipExtra;
    }

    public void setTooltipExtra(String tooltipExtra) {
        this.tooltipExtra = tooltipExtra;
    }
}
