package com.hfstudio.guidenh.guide.siteexport.site;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageRegistry;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlighter;

public class GuideSiteCodeBlockRenderer {

    private static final CodeHighlighter DEFAULT_HIGHLIGHTER = new CodeHighlighter();
    private static final CodeHighlightHtmlRenderer DEFAULT_HTML_RENDERER = new CodeHighlightHtmlRenderer();
    private final CodeHighlighter highlighter;
    private final CodeHighlightHtmlRenderer htmlRenderer;

    public GuideSiteCodeBlockRenderer() {
        this(DEFAULT_HIGHLIGHTER, DEFAULT_HTML_RENDERER);
    }

    public GuideSiteCodeBlockRenderer(CodeHighlighter highlighter, CodeHighlightHtmlRenderer htmlRenderer) {
        this.highlighter = highlighter;
        this.htmlRenderer = htmlRenderer;
    }

    public String render(@Nullable String languageFenceName, String codeText, @Nullable Integer width,
        @Nullable Integer height) {
        CodeHighlightResult result = highlighter.highlight(languageFenceName, codeText);
        return htmlRenderer.render(result, resolveLanguageLabel(languageFenceName, result.languageId()), width, height);
    }

    public String render(@Nullable String languageFenceName, String codeText) {
        return render(languageFenceName, codeText, null, null);
    }

    private String resolveLanguageLabel(@Nullable String languageFenceName, @Nullable String resolvedLanguageId) {
        CodeBlockLanguage language = CodeBlockLanguageRegistry.findByFenceName(languageFenceName);
        if (language == null && resolvedLanguageId != null && !resolvedLanguageId.isEmpty()) {
            language = CodeBlockLanguageRegistry.findById(resolvedLanguageId);
        }
        if (language != null) {
            return language.displayName();
        }
        return languageFenceName != null && !languageFenceName.isEmpty() ? languageFenceName : "Text";
    }
}
