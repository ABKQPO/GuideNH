package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Placement position for data value labels.
 */
public enum ChartLabelPosition {

    NONE,
    INSIDE,
    OUTSIDE,
    ABOVE,
    BELOW,
    CENTER;

    public static ChartLabelPosition fromString(String s, ChartLabelPosition def) {
        if (s == null) {
            return def;
        }
        switch (s.trim()
            .toLowerCase()) {
            case "none":
                return NONE;
            case "inside":
            case "in":
                return INSIDE;
            case "outside":
            case "out":
                return OUTSIDE;
            case "above":
            case "top":
                return ABOVE;
            case "below":
            case "bottom":
                return BELOW;
            case "center":
            case "middle":
                return CENTER;
            default:
                return def;
        }
    }
}
