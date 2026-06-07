package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightLine;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightToken;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;

public class CodeHighlightHtmlRenderer {

    public String render(CodeHighlightResult result, @Nullable String languageLabel, @Nullable Integer width,
        @Nullable Integer height) {
        String resolvedLanguageLabel = languageLabel != null && !languageLabel.isEmpty() ? languageLabel : "Text";
        StringBuilder html = new StringBuilder(Math.max(256, estimateCapacity(result)));
        html.append("<div class=\"guide-code-block\">")
            .append("<div class=\"guide-code-block-toolbar\">")
            .append("<span class=\"guide-code-block-language\">")
            .append(GuideSiteItemHtml.escapeHtml(resolvedLanguageLabel))
            .append("</span></div>")
            .append("<pre");
        appendPreAttributes(html, width, height);
        html.append("><code");
        appendCodeAttributes(html, result.languageId());
        html.append(">");
        appendLines(html, result);
        html.append("</code></pre></div>");
        return html.toString();
    }

    private void appendPreAttributes(StringBuilder html, @Nullable Integer width, @Nullable Integer height) {
        boolean constrained = width != null || height != null;
        if (!constrained) {
            return;
        }
        html.append(" class=\"guide-code-block-scroll\" style=\"");
        if (width != null) {
            html.append("width:")
                .append(width)
                .append("px;max-width:100%;");
        }
        if (height != null) {
            html.append("height:")
                .append(height)
                .append("px;overflow:auto;");
        }
        html.append("\"");
    }

    private void appendCodeAttributes(StringBuilder html, String languageId) {
        if (languageId == null || languageId.isEmpty()) {
            return;
        }
        html.append(" class=\"language-")
            .append(GuideSiteItemHtml.escapeHtml(languageId))
            .append("\"");
    }

    private void appendLines(StringBuilder html, CodeHighlightResult result) {
        for (int lineIndex = 0; lineIndex < result.lines()
            .size(); lineIndex++) {
            CodeHighlightLine line = result.lines()
                .get(lineIndex);
            appendLine(html, line);
            if (lineIndex < result.lines()
                .size() - 1) {
                html.append('\n');
            }
        }
    }

    private void appendLine(StringBuilder html, CodeHighlightLine line) {
        for (CodeHighlightToken token : line.tokens()) {
            if (token.type() == CodeTokenType.PLAIN) {
                html.append(GuideSiteItemHtml.escapeHtml(token.text()));
                continue;
            }
            html.append("<span class=\"gh-")
                .append(
                    token.type()
                        .name()
                        .toLowerCase(Locale.ROOT))
                .append("\">")
                .append(GuideSiteItemHtml.escapeHtml(token.text()))
                .append("</span>");
        }
    }

    private int estimateCapacity(CodeHighlightResult result) {
        int size = 0;
        for (CodeHighlightLine line : result.lines()) {
            for (CodeHighlightToken token : line.tokens()) {
                size += token.text()
                    .length();
            }
        }
        return size * 2;
    }
}
