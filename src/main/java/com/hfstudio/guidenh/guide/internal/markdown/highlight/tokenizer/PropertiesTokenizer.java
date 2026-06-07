package com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightLine;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightToken;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.LanguageTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.TokenizerSupport;

public class PropertiesTokenizer implements LanguageTokenizer {

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
        String trimmed = line.trim();
        if (trimmed.startsWith("#") || trimmed.startsWith("!") || trimmed.startsWith(";")) {
            return List.of(new CodeHighlightToken(line, CodeTokenType.COMMENT));
        }

        int separator = findSeparator(line);
        if (separator < 0) {
            return List.of(new CodeHighlightToken(line, CodeTokenType.PLAIN));
        }

        List<CodeHighlightToken> tokens = new ArrayList<>();
        if (separator > 0) {
            TokenizerSupport.appendToken(
                tokens,
                line.substring(0, separator),
                mode == CodeHighlightMode.FULL ? CodeTokenType.PROPERTY : CodeTokenType.PLAIN);
        }
        TokenizerSupport.appendToken(tokens, Character.toString(line.charAt(separator)), CodeTokenType.PUNCTUATION);
        if (separator + 1 < line.length()) {
            TokenizerSupport.appendToken(tokens, line.substring(separator + 1), CodeTokenType.PLAIN);
        }
        return tokens;
    }

    private int findSeparator(String line) {
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '=' || current == ':') {
                return index;
            }
        }
        return -1;
    }
}
