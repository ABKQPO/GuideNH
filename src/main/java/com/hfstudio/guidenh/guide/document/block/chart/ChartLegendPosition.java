package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Legend placement position.
 */
public enum ChartLegendPosition {

    NONE,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT;

    public static ChartLegendPosition fromString(String s, ChartLegendPosition def) {
        if (s == null) {
            return def;
        }
        switch (s.trim()
            .toLowerCase()) {
            case "none":
            case "off":
            case "false":
                return NONE;
            case "top":
                return TOP;
            case "bottom":
                return BOTTOM;
            case "left":
                return LEFT;
            case "right":
                return RIGHT;
            default:
                return def;
        }
    }
}
