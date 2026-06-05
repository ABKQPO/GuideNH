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

public class XmlTokenizer implements LanguageTokenizer {

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
        List<CodeHighlightToken> tokens = new ArrayList<>();
        int index = 0;
        boolean insideTag = false;
        while (index < line.length()) {
            char current = line.charAt(index);
            if (!insideTag && current == '<') {
                insideTag = true;
                TokenizerSupport.appendToken(tokens, "<", CodeTokenType.PUNCTUATION);
                index++;
                if (index < line.length() && line.charAt(index) == '/') {
                    TokenizerSupport.appendToken(tokens, "/", CodeTokenType.PUNCTUATION);
                    index++;
                }
                int nameStart = index;
                while (index < line.length()
                    && (TokenizerSupport.isIdentifierPart(line.charAt(index)) || line.charAt(index) == ':'
                        || line.charAt(index) == '-')) {
                    index++;
                }
                if (index > nameStart) {
                    TokenizerSupport.appendToken(tokens, line.substring(nameStart, index), CodeTokenType.TYPE);
                }
                continue;
            }
            if (insideTag && (current == '"' || current == '\'')) {
                int end = TokenizerSupport.findQuotedLiteralEnd(line, index + 1, current);
                TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.STRING);
                index = end;
                continue;
            }
            if (insideTag && current == '=') {
                TokenizerSupport.appendToken(tokens, "=", CodeTokenType.OPERATOR);
                index++;
                continue;
            }
            if (insideTag && current == '>') {
                TokenizerSupport.appendToken(tokens, ">", CodeTokenType.PUNCTUATION);
                insideTag = false;
                index++;
                continue;
            }
            if (insideTag && current == '/' && index + 1 < line.length() && line.charAt(index + 1) == '>') {
                TokenizerSupport.appendToken(tokens, "/>", CodeTokenType.PUNCTUATION);
                insideTag = false;
                index += 2;
                continue;
            }
            if (insideTag && TokenizerSupport.isIdentifierStart(current)) {
                int end = index + 1;
                while (end < line.length()
                    && (TokenizerSupport.isIdentifierPart(line.charAt(end)) || line.charAt(end) == ':'
                        || line.charAt(end) == '-')) {
                    end++;
                }
                CodeTokenType type = mode == CodeHighlightMode.FULL ? CodeTokenType.PROPERTY : CodeTokenType.PLAIN;
                TokenizerSupport.appendToken(tokens, line.substring(index, end), type);
                index = end;
                continue;
            }
            int end = index + 1;
            while (end < line.length()) {
                char next = line.charAt(end);
                if (next == '<' || insideTag && (next == '"' || next == '\''
                    || next == '='
                    || next == '>'
                    || next == '/' && end + 1 < line.length() && line.charAt(end + 1) == '>'
                    || TokenizerSupport.isIdentifierStart(next))) {
                    break;
                }
                end++;
            }
            TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.PLAIN);
            index = end;
        }
        return tokens;
    }
}
