package com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer;

import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.LanguageTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.TokenizerSupport;

public class PlainTextTokenizer implements LanguageTokenizer {

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        return new CodeHighlightResult(languageId, mode, TokenizerSupport.plainLines(codeText));
    }
}
