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

public class JsonTokenizer implements LanguageTokenizer {

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        List<String> lines = TokenizerSupport.splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return new CodeHighlightResult(languageId, mode, result);
        }

        for (String line : lines) {
            List<CodeHighlightToken> tokens = new ArrayList<>();
            int index = 0;
            while (index < line.length()) {
                char current = line.charAt(index);
                if (current == '"') {
                    int end = TokenizerSupport.findQuotedLiteralEnd(line, index + 1, current);
                    int next = TokenizerSupport.skipWhitespace(line, end);
                    CodeTokenType type = next < line.length() && line.charAt(next) == ':'
                        && mode == CodeHighlightMode.FULL ? CodeTokenType.PROPERTY : CodeTokenType.STRING;
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), type);
                    index = end;
                    continue;
                }
                if (Character.isDigit(current)
                    || current == '-' && index + 1 < line.length() && Character.isDigit(line.charAt(index + 1))) {
                    int end = index + 1;
                    while (end < line.length()) {
                        char next = line.charAt(end);
                        if (!Character.isDigit(next) && next != '.'
                            && next != 'e'
                            && next != 'E'
                            && next != '+'
                            && next != '-') {
                            break;
                        }
                        end++;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.NUMBER);
                    index = end;
                    continue;
                }
                if (Character.isLetter(current)) {
                    int end = index + 1;
                    while (end < line.length() && Character.isLetter(line.charAt(end))) {
                        end++;
                    }
                    String token = line.substring(index, end);
                    CodeTokenType type = "true".equals(token) || "false".equals(token) || "null".equals(token)
                        ? CodeTokenType.KEYWORD
                        : CodeTokenType.PLAIN;
                    TokenizerSupport.appendToken(tokens, token, type);
                    index = end;
                    continue;
                }
                if (Character.isWhitespace(current)) {
                    int end = index + 1;
                    while (end < line.length() && Character.isWhitespace(line.charAt(end))) {
                        end++;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.PLAIN);
                    index = end;
                    continue;
                }
                CodeTokenType type = "{}[],:".indexOf(current) >= 0 ? CodeTokenType.PUNCTUATION : CodeTokenType.PLAIN;
                TokenizerSupport.appendToken(tokens, Character.toString(current), type);
                index++;
            }
            result.add(new CodeHighlightLine(List.copyOf(tokens)));
        }
        return new CodeHighlightResult(languageId, mode, result);
    }
}
