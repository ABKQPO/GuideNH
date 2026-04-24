package com.hfstudio.guidenh.guide.document;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record LytPoint(float x, float y) {

    public LytPoint add(float dx, float dy) {
        return new LytPoint(x + dx, y + dy);
    }

    public LytPoint add(int dx, int dy) {
        return new LytPoint(x + dx, y + dy);
    }
}
