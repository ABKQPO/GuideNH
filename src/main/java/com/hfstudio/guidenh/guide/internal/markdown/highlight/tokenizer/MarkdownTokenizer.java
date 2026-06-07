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

public class MarkdownTokenizer implements LanguageTokenizer {

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        List<String> lines = TokenizerSupport.splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return new CodeHighlightResult(languageId, mode, result);
        }
        for (String line : lines) {
            result.add(new CodeHighlightLine(List.copyOf(tokenizeLine(line))));
        }
        return new CodeHighlightResult(languageId, mode, result);
    }

    private List<CodeHighlightToken> tokenizeLine(String line) {
        if (line.isEmpty()) {
            return List.of();
        }
        List<CodeHighlightToken> tokens = new ArrayList<>();
        int index = 0;
        if (line.startsWith("#")) {
            while (index < line.length() && line.charAt(index) == '#') {
                index++;
            }
            TokenizerSupport.appendToken(tokens, line.substring(0, index), CodeTokenType.KEYWORD);
        } else if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("> ")) {
            TokenizerSupport.appendToken(tokens, line.substring(0, 1), CodeTokenType.PUNCTUATION);
            index = 1;
        }
        while (index < line.length()) {
            char current = line.charAt(index);
            if (current == '`') {
                int end = line.indexOf('`', index + 1);
                end = end >= 0 ? end + 1 : line.length();
                TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.STRING);
                index = end;
                continue;
            }
            if ("*_~[]()".indexOf(current) >= 0) {
                TokenizerSupport.appendToken(tokens, Character.toString(current), CodeTokenType.PUNCTUATION);
                index++;
                continue;
            }
            int end = index + 1;
            while (end < line.length() && "`*_~[]()".indexOf(line.charAt(end)) < 0) {
                end++;
            }
            TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.PLAIN);
            index = end;
        }
        return tokens;
    }
}
