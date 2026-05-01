package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Cartesian chart axis configuration. All numeric fields use boxed types; {@code null} means auto.
 */
public class ChartAxisOptions {

    private String label;
    private Double min;
    private Double max;
    private Double step;
    private String tickFormat;
    private String unit;
    private boolean gridVisible;
    private int gridColor = 0x33FFFFFF;
    private int axisColor = 0xFF7A7A7A;
    private int labelColor = 0xFFCCCCCC;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getStep() {
        return step;
    }

    public void setStep(Double step) {
        this.step = step;
    }

    public String getTickFormat() {
        return tickFormat;
    }

    public void setTickFormat(String tickFormat) {
        this.tickFormat = tickFormat;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
    }

    public int getGridColor() {
        return gridColor;
    }

    public void setGridColor(int gridColor) {
        this.gridColor = gridColor;
    }

    public int getAxisColor() {
        return axisColor;
    }

    public void setAxisColor(int axisColor) {
        this.axisColor = axisColor;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    /**
     * Format a numeric value using the configured tickFormat and unit; when tickFormat is unspecified,
     * automatically choose between integer / one-decimal formatting.
     */
    public String formatTick(double value) {
        String text;
        if (tickFormat != null && !tickFormat.isEmpty()) {
            try {
                text = String.format(tickFormat, value);
            } catch (java.util.IllegalFormatException ex) {
                text = defaultFormat(value);
            }
        } else {
            text = defaultFormat(value);
        }
        if (unit != null && !unit.isEmpty()) {
            text = text + unit;
        }
        return text;
    }

    private static String defaultFormat(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }
}
