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

public class ShellTokenizer implements LanguageTokenizer {

    private static final Set<String> BASH_KEYWORDS = Set
        .of("if", "then", "else", "fi", "for", "do", "done", "case", "esac", "function", "while", "in");
    private static final Set<String> POWERSHELL_KEYWORDS = Set
        .of("function", "param", "if", "else", "foreach", "switch", "return", "trap", "throw", "while");

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        List<String> lines = TokenizerSupport.splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return new CodeHighlightResult(languageId, mode, result);
        }
        for (String line : lines) {
            result.add(new CodeHighlightLine(List.copyOf(tokenizeLine(languageId, line, mode))));
        }
        return new CodeHighlightResult(languageId, mode, result);
    }

    private List<CodeHighlightToken> tokenizeLine(String languageId, String line, CodeHighlightMode mode) {
        List<CodeHighlightToken> tokens = new ArrayList<>();
        int commentIndex = TokenizerSupport.findCommentStartOutsideQuotes(line, '#');
        int endLimit = commentIndex >= 0 ? commentIndex : line.length();
        int index = 0;

        while (index < endLimit) {
            char current = line.charAt(index);
            if (current == '"' || current == '\'') {
                int end = TokenizerSupport.findQuotedLiteralEnd(line, index + 1, current);
                TokenizerSupport
                    .appendToken(tokens, line.substring(index, Math.min(end, endLimit)), CodeTokenType.STRING);
                index = end;
                continue;
            }
            if (current == '$') {
                int end = index + 1;
                while (end < endLimit) {
                    char next = line.charAt(end);
                    if (!TokenizerSupport.isIdentifierPart(next) && next != ':' && next != '{' && next != '}') {
                        break;
                    }
                    end++;
                }
                TokenizerSupport.appendToken(
                    tokens,
                    line.substring(index, end),
                    mode == CodeHighlightMode.FULL ? CodeTokenType.PROPERTY : CodeTokenType.PLAIN);
                index = end;
                continue;
            }
            if (Character.isDigit(current)) {
                int end = index + 1;
                while (end < endLimit && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.')) {
                    end++;
                }
                TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.NUMBER);
                index = end;
                continue;
            }
            if (TokenizerSupport.isIdentifierStart(current)) {
                int end = index + 1;
                while (end < endLimit) {
                    char next = line.charAt(end);
                    if (!TokenizerSupport.isIdentifierPart(next) && next != '-') {
                        break;
                    }
                    end++;
                }
                String token = line.substring(index, end);
                CodeTokenType type = keywordsFor(languageId).contains(token.toLowerCase()) ? CodeTokenType.KEYWORD
                    : mode == CodeHighlightMode.FULL && token.contains("-") ? CodeTokenType.FUNCTION
                        : CodeTokenType.PLAIN;
                TokenizerSupport.appendToken(tokens, token, type);
                index = end;
                continue;
            }
            if (Character.isWhitespace(current)) {
                int end = index + 1;
                while (end < endLimit && Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
                TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.PLAIN);
                index = end;
                continue;
            }
            CodeTokenType type = "|&;(){}[],:".indexOf(current) >= 0 ? CodeTokenType.PUNCTUATION
                : "=+-*/%!<>".indexOf(current) >= 0 ? CodeTokenType.OPERATOR : CodeTokenType.PLAIN;
            TokenizerSupport.appendToken(tokens, Character.toString(current), type);
            index++;
        }

        if (commentIndex >= 0) {
            TokenizerSupport.appendToken(tokens, line.substring(commentIndex), CodeTokenType.COMMENT);
        }
        return tokens;
    }

    private Set<String> keywordsFor(String languageId) {
        return "powershell".equals(languageId) ? POWERSHELL_KEYWORDS : BASH_KEYWORDS;
    }
}
