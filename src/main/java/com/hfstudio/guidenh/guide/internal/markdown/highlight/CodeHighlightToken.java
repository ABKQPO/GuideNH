package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record CodeHighlightToken(String text, CodeTokenType type) {}
