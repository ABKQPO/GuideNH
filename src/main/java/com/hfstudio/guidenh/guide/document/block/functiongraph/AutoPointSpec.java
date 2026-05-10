package com.hfstudio.guidenh.guide.document.block.functiongraph;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record AutoPointSpec(double everyX, double everyY, AutoPointLabelMode labelMode, int color,
    boolean colorInherit) {

    public static final AutoPointSpec NONE = new AutoPointSpec(
        Double.NaN,
        Double.NaN,
        AutoPointLabelMode.NONE,
        0xFFFFFFFF,
        true);

    public AutoPointSpec {
        if (!(everyX > 0d) || !Double.isFinite(everyX)) {
            everyX = Double.NaN;
        }
        if (!(everyY > 0d) || !Double.isFinite(everyY)) {
            everyY = Double.NaN;
        }
        labelMode = labelMode != null ? labelMode : AutoPointLabelMode.NONE;
    }

    public boolean isEnabled() {
        return !Double.isNaN(everyX) || !Double.isNaN(everyY);
    }
}
