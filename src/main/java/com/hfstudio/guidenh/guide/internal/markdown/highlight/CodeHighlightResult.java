package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record CodeHighlightResult(String languageId, CodeHighlightMode mode, List<CodeHighlightLine> lines) {

    public boolean isPlain() {
        return mode == CodeHighlightMode.PLAIN;
    }
}
