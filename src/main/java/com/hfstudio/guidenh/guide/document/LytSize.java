package com.hfstudio.guidenh.guide.document;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record LytSize(int width, int height) {

    public static LytSize empty() {
        return new LytSize(0, 0);
    }

}
