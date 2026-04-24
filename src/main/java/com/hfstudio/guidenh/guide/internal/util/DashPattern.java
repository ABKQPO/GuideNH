package com.hfstudio.guidenh.guide.internal.util;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record DashPattern(float width, float onLength, float offLength, int color, float animationCycleMs) {

    float length() {
        return onLength + offLength;
    }
}
