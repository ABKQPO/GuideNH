package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record CodeHighlightLine(List<CodeHighlightToken> tokens) {}
