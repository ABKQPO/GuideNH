package com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightLine;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightToken;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.LanguageTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.TokenizerSupport;

public class YamlTokenizer implements LanguageTokenizer {

    private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "yes", "no", "on", "off");

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        List<String> lines = TokenizerSupport.splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return new CodeHighlightResult(languageId, mode, result);
        }

        for (String line : lines) {
            result.add(new CodeHighlightLine(List.copyOf(tokenizeLine(line, mode))));
        }
        return new CodeHighlightResult(languageId, mode, result);
    }

    private List<CodeHighlightToken> tokenizeLine(String line, CodeHighlightMode mode) {
        if (line.isEmpty()) {
            return List.of();
        }

        List<CodeHighlightToken> tokens = new ArrayList<>();
        int commentStart = TokenizerSupport.findCommentStartOutsideQuotes(line, '#');
        String content = commentStart >= 0 ? line.substring(0, commentStart) : line;
        String comment = commentStart >= 0 ? line.substring(commentStart) : "";

        int index = 0;
        while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
            index++;
        }
        if (index > 0) {
            TokenizerSupport.appendToken(tokens, content.substring(0, index), CodeTokenType.PLAIN);
        }
        if (index < content.length() && content.charAt(index) == '-') {
            TokenizerSupport.appendToken(tokens, "-", CodeTokenType.PUNCTUATION);
            index++;
            while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
                index++;
            }
        }

        int colonIndex = content.indexOf(':', index);
        if (colonIndex > index) {
            String key = content.substring(index, colonIndex)
                .trim();
            int keyOffset = content.indexOf(key, index);
            if (keyOffset > index) {
                TokenizerSupport.appendToken(tokens, content.substring(index, keyOffset), CodeTokenType.PLAIN);
            }
            if (!key.isEmpty()) {
                TokenizerSupport.appendToken(
                    tokens,
                    key,
                    mode == CodeHighlightMode.FULL ? CodeTokenType.PROPERTY : CodeTokenType.PLAIN);
            }
            TokenizerSupport.appendToken(tokens, ":", CodeTokenType.PUNCTUATION);
            appendValueTokens(tokens, content.substring(colonIndex + 1));
        } else if (index < content.length()) {
            appendValueTokens(tokens, content.substring(index));
        }

        if (!comment.isEmpty()) {
            TokenizerSupport.appendToken(tokens, comment, CodeTokenType.COMMENT);
        }
        return tokens;
    }

    private void appendValueTokens(List<CodeHighlightToken> tokens, String value) {
        if (value.isEmpty()) {
            return;
        }
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        if (index > 0) {
            TokenizerSupport.appendToken(tokens, value.substring(0, index), CodeTokenType.PLAIN);
        }
        if (index >= value.length()) {
            return;
        }
        String trimmed = value.substring(index)
            .trim();
        if (trimmed.isEmpty()) {
            TokenizerSupport.appendToken(tokens, value.substring(index), CodeTokenType.PLAIN);
            return;
        }
        if (trimmed.startsWith("\"") || trimmed.startsWith("'")) {
            TokenizerSupport.appendToken(tokens, value.substring(index), CodeTokenType.STRING);
            return;
        }
        if (KEYWORDS.contains(trimmed.toLowerCase())) {
            TokenizerSupport.appendToken(tokens, value.substring(index), CodeTokenType.KEYWORD);
            return;
        }
        if (isNumber(trimmed)) {
            TokenizerSupport.appendToken(tokens, value.substring(index), CodeTokenType.NUMBER);
            return;
        }
        TokenizerSupport.appendToken(tokens, value.substring(index), CodeTokenType.PLAIN);
    }

    private boolean isNumber(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!Character.isDigit(current) && current != '.' && current != '-' && current != '+') {
                return false;
            }
        }
        return true;
    }
}
