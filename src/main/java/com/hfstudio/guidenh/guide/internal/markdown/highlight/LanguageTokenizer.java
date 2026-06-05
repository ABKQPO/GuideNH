package com.hfstudio.guidenh.guide.internal.markdown.highlight;

public interface LanguageTokenizer {

    CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode);
}
