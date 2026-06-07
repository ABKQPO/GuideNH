package com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightLine;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightToken;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.LanguageTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.TokenizerSupport;

public class JavaLikeTokenizer implements LanguageTokenizer {

    private static final Set<Character> PUNCTUATION = Set.of('(', ')', '{', '}', '[', ']', ';', ',', '.');
    private static final Set<Character> OPERATORS = Set
        .of('+', '-', '*', '/', '%', '=', '&', '|', '!', '<', '>', '?', '^', '~', ':');
    private static final Map<String, Set<String>> KEYWORDS = buildKeywords();

    @Override
    public CodeHighlightResult highlight(String languageId, String codeText, CodeHighlightMode mode) {
        List<String> lines = TokenizerSupport.splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        boolean inBlockComment = false;

        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return new CodeHighlightResult(languageId, mode, result);
        }

        for (String line : lines) {
            List<CodeHighlightToken> tokens = new ArrayList<>();
            int index = 0;

            if (line.isEmpty()) {
                result.add(new CodeHighlightLine(List.of()));
                continue;
            }

            while (index < line.length()) {
                if (inBlockComment) {
                    int commentEnd = line.indexOf("*/", index);
                    if (commentEnd < 0) {
                        TokenizerSupport.appendToken(tokens, line.substring(index), CodeTokenType.COMMENT);
                        index = line.length();
                        break;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, commentEnd + 2), CodeTokenType.COMMENT);
                    index = commentEnd + 2;
                    inBlockComment = false;
                    continue;
                }

                if (startsWith(line, index, "//")) {
                    TokenizerSupport.appendToken(tokens, line.substring(index), CodeTokenType.COMMENT);
                    break;
                }

                if (startsWith(line, index, "/*")) {
                    int commentEnd = line.indexOf("*/", index + 2);
                    if (commentEnd < 0) {
                        TokenizerSupport.appendToken(tokens, line.substring(index), CodeTokenType.COMMENT);
                        inBlockComment = true;
                        break;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, commentEnd + 2), CodeTokenType.COMMENT);
                    index = commentEnd + 2;
                    continue;
                }

                char current = line.charAt(index);
                if (current == '"' || current == '\'') {
                    int end = TokenizerSupport.findQuotedLiteralEnd(line, index + 1, current);
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.STRING);
                    index = end;
                    continue;
                }

                if (mode == CodeHighlightMode.FULL && current == '@') {
                    int end = index + 1;
                    while (end < line.length() && TokenizerSupport.isIdentifierPart(line.charAt(end))) {
                        end++;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.ANNOTATION);
                    index = end;
                    continue;
                }

                if (Character.isDigit(current)
                    || current == '-' && index + 1 < line.length() && Character.isDigit(line.charAt(index + 1))) {
                    int end = index + 1;
                    while (end < line.length()) {
                        char next = line.charAt(end);
                        if (!Character.isDigit(next) && next != '.' && next != '_' && !Character.isLetter(next)) {
                            break;
                        }
                        end++;
                    }
                    TokenizerSupport.appendToken(tokens, line.substring(index, end), CodeTokenType.NUMBER);
                    index = end;
                    continue;
                }

                if (TokenizerSupport.isIdentifierStart(current)) {
                    int end = index + 1;
                    while (end < line.length() && TokenizerSupport.isIdentifierPart(line.charAt(end))) {
                        end++;
                    }
                    String token = line.substring(index, end);
                    TokenizerSupport.appendToken(tokens, token, classifyToken(languageId, line, token, end, mode));
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

                CodeTokenType type = PUNCTUATION.contains(current) ? CodeTokenType.PUNCTUATION
                    : OPERATORS.contains(current) ? CodeTokenType.OPERATOR : CodeTokenType.PLAIN;
                TokenizerSupport.appendToken(tokens, Character.toString(current), type);
                index++;
            }

            result.add(new CodeHighlightLine(List.copyOf(tokens)));
        }

        return new CodeHighlightResult(languageId, mode, result);
    }

    private CodeTokenType classifyToken(String languageId, String line, String token, int endIndex,
        CodeHighlightMode mode) {
        if (KEYWORDS.getOrDefault(languageId, Set.of())
            .contains(token)) {
            return CodeTokenType.KEYWORD;
        }
        if (mode == CodeHighlightMode.FULL && !token.isEmpty() && Character.isUpperCase(token.charAt(0))) {
            return CodeTokenType.TYPE;
        }
        if (mode == CodeHighlightMode.FULL) {
            int next = TokenizerSupport.skipWhitespace(line, endIndex);
            if (next < line.length() && line.charAt(next) == '(') {
                return CodeTokenType.FUNCTION;
            }
        }
        return CodeTokenType.PLAIN;
    }

    private static boolean startsWith(String line, int index, String expected) {
        return index + expected.length() <= line.length() && line.startsWith(expected, index);
    }

    private static Map<String, Set<String>> buildKeywords() {
        Map<String, Set<String>> keywords = new HashMap<>();
        keywords.put(
            "java",
            setOf(
                "abstract",
                "boolean",
                "break",
                "case",
                "catch",
                "class",
                "continue",
                "default",
                "do",
                "else",
                "enum",
                "extends",
                "final",
                "finally",
                "for",
                "if",
                "implements",
                "import",
                "instanceof",
                "interface",
                "new",
                "package",
                "private",
                "protected",
                "public",
                "return",
                "static",
                "switch",
                "throw",
                "throws",
                "try",
                "void",
                "while"));
        keywords.put(
            "kotlin",
            setOf(
                "as",
                "break",
                "class",
                "continue",
                "data",
                "do",
                "else",
                "false",
                "for",
                "fun",
                "if",
                "in",
                "interface",
                "is",
                "null",
                "object",
                "package",
                "return",
                "sealed",
                "super",
                "this",
                "throw",
                "true",
                "try",
                "typealias",
                "val",
                "var",
                "when",
                "while"));
        keywords.put(
            "scala",
            setOf(
                "case",
                "class",
                "def",
                "do",
                "else",
                "enum",
                "extends",
                "false",
                "for",
                "given",
                "if",
                "import",
                "match",
                "new",
                "object",
                "override",
                "package",
                "return",
                "then",
                "trait",
                "true",
                "type",
                "using",
                "val",
                "var",
                "while",
                "yield"));
        keywords.put(
            "groovy",
            setOf(
                "as",
                "break",
                "case",
                "catch",
                "class",
                "continue",
                "def",
                "do",
                "else",
                "enum",
                "extends",
                "false",
                "finally",
                "for",
                "if",
                "implements",
                "import",
                "in",
                "interface",
                "new",
                "null",
                "package",
                "return",
                "static",
                "switch",
                "throw",
                "trait",
                "true",
                "try",
                "while"));
        keywords.put(
            "javascript",
            setOf(
                "async",
                "await",
                "break",
                "case",
                "catch",
                "class",
                "const",
                "continue",
                "default",
                "delete",
                "do",
                "else",
                "export",
                "extends",
                "false",
                "finally",
                "for",
                "function",
                "if",
                "import",
                "in",
                "instanceof",
                "let",
                "new",
                "null",
                "return",
                "static",
                "super",
                "switch",
                "this",
                "throw",
                "true",
                "try",
                "typeof",
                "var",
                "void",
                "while",
                "yield"));
        keywords.put(
            "typescript",
            setOf(
                "abstract",
                "any",
                "as",
                "async",
                "await",
                "break",
                "case",
                "catch",
                "class",
                "const",
                "constructor",
                "continue",
                "declare",
                "default",
                "do",
                "else",
                "enum",
                "export",
                "extends",
                "false",
                "finally",
                "for",
                "function",
                "if",
                "implements",
                "import",
                "in",
                "infer",
                "instanceof",
                "interface",
                "keyof",
                "let",
                "module",
                "namespace",
                "never",
                "new",
                "null",
                "private",
                "protected",
                "public",
                "readonly",
                "return",
                "static",
                "super",
                "switch",
                "this",
                "throw",
                "true",
                "try",
                "type",
                "typeof",
                "undefined",
                "unknown",
                "var",
                "void",
                "while"));
        return keywords;
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(List.of(values));
    }
}
