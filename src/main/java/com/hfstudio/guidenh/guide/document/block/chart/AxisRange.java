package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Utility for automatic axis range and tick calculation.
 */
public final class AxisRange {

    public final double min;
    public final double max;
    public final double step;

    public AxisRange(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /**
     * Computes a "nice number" axis range from the data range; explicit min/max/step take precedence.
     */
    public static AxisRange compute(Double explicitMin, Double explicitMax, Double explicitStep, double dataMin,
        double dataMax) {
        double dMin = dataMin;
        double dMax = dataMax;
        if (!(dMin <= dMax)) {
            dMin = 0d;
            dMax = 1d;
        }
        if (Math.abs(dMax - dMin) < 1e-9) {
            if (Math.abs(dMin) < 1e-9) {
                dMin = 0d;
                dMax = 1d;
            } else {
                double pad = Math.abs(dMin) * 0.5;
                dMin = dMin - pad;
                dMax = dMax + pad;
            }
        }
        double range = niceNumber(dMax - dMin, false);
        double step;
        if (explicitStep != null && explicitStep > 0) {
            step = explicitStep;
        } else {
            step = niceNumber(range / 5d, true);
        }
        double niceMin = explicitMin != null ? explicitMin : Math.floor(dMin / step) * step;
        double niceMax = explicitMax != null ? explicitMax : Math.ceil(dMax / step) * step;
        if (niceMax <= niceMin) {
            niceMax = niceMin + step;
        }
        return new AxisRange(niceMin, niceMax, step);
    }

    private static double niceNumber(double value, boolean round) {
        if (value <= 0d) {
            return 1d;
        }
        double exp = Math.floor(Math.log10(value));
        double frac = value / Math.pow(10d, exp);
        double nf;
        if (round) {
            if (frac < 1.5) {
                nf = 1d;
            } else if (frac < 3d) {
                nf = 2d;
            } else if (frac < 7d) {
                nf = 5d;
            } else {
                nf = 10d;
            }
        } else {
            if (frac <= 1d) {
                nf = 1d;
            } else if (frac <= 2d) {
                nf = 2d;
            } else if (frac <= 5d) {
                nf = 5d;
            } else {
                nf = 10d;
            }
        }
        return nf * Math.pow(10d, exp);
    }

    public double normalize(double v) {
        double span = max - min;
        if (Math.abs(span) < 1e-12) {
            return 0d;
        }
        return (v - min) / span;
    }
}
