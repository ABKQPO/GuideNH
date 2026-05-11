package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class CodeBlockLanguageDetector {

    private static final CodeBlockLanguage PLAIN_TEXT = new CodeBlockLanguage("text", "Text");

    protected CodeBlockLanguageDetector() {}

    public static CodeBlockLanguage detect(@Nullable String explicitFenceLanguage, String codeText) {
        CodeBlockLanguage explicit = CodeBlockLanguageRegistry.findByFenceName(explicitFenceLanguage);
        if (explicit != null) {
            return explicit;
        }

        String text = codeText != null ? codeText : "";
        String lower = text.toLowerCase(Locale.ROOT);
        if (looksLikeLua(lower)) {
            return require("lua");
        }
        if (looksLikeScala(lower)) {
            return require("scala");
        }
        if (looksLikeJava(lower)) {
            return require("java");
        }
        if (looksLikeKotlin(lower)) {
            return require("kotlin");
        }
        if (looksLikeMarkdown(text)) {
            return require("markdown");
        }
        if (looksLikeJson(lower)) {
            return require("json");
        }
        if (looksLikeYaml(lower)) {
            return require("yaml");
        }
        if (looksLikeXml(lower)) {
            return require("xml");
        }
        if (looksLikeProperties(lower)) {
            return require("properties");
        }
        if (looksLikeBash(lower)) {
            return require("bash");
        }
        if (looksLikePowerShell(lower)) {
            return require("powershell");
        }
        if (looksLikeCsv(text)) {
            return require("csv");
        }
        if (looksLikeMermaid(lower)) {
            return require("mermaid");
        }
        return PLAIN_TEXT;
    }

    private static CodeBlockLanguage require(String fenceName) {
        CodeBlockLanguage language = CodeBlockLanguageRegistry.findByFenceName(fenceName);
        return language != null ? language : PLAIN_TEXT;
    }

    private static boolean looksLikeLua(String lower) {
        return lower.contains("local ") || lower.contains("function ") && lower.contains(" end")
            || lower.contains("print(")
            || lower.contains("then\n")
            || lower.contains("elseif ");
    }

    private static boolean looksLikeScala(String lower) {
        return lower.contains("object ") && lower.contains(" extends app") || lower.contains("case class ")
            || lower.contains("println(")
            || lower.contains("def ") && lower.contains(": ");
    }

    private static boolean looksLikeJava(String lower) {
        return lower.contains("public class ") || lower.contains("public static void main")
            || lower.contains("system.out.println(")
            || lower.contains("private static ");
    }

    private static boolean looksLikeKotlin(String lower) {
        return lower.contains("fun main(") || lower.contains("val ")
            || lower.contains("var ")
            || lower.contains("println(") && lower.contains(": string");
    }

    private static boolean looksLikeMarkdown(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith("#") || trimmed.startsWith("- ")
            || trimmed.startsWith("* ")
            || trimmed.startsWith("> ")
            || trimmed.startsWith("```");
    }

    private static boolean looksLikeJson(String lower) {
        String trimmed = lower.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    private static boolean looksLikeYaml(String lower) {
        return lower.contains(": ") && !lower.contains("{")
            && !lower.contains(";")
            && (lower.contains("\n- ") || lower.contains("\n  "));
    }

    private static boolean looksLikeXml(String lower) {
        String trimmed = lower.trim();
        return trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.contains("</");
    }

    private static boolean looksLikeProperties(String lower) {
        return lower.contains("=") && !lower.contains("{") && !lower.contains(";");
    }

    private static boolean looksLikeBash(String lower) {
        return lower.startsWith("#!/bin/bash") || lower.startsWith("#!/usr/bin/env bash")
            || lower.contains("echo ")
            || lower.contains(" fi")
            || lower.contains("then");
    }

    private static boolean looksLikePowerShell(String lower) {
        return lower.contains("$env:") || lower.contains("write-host")
            || lower.contains("get-childitem")
            || lower.contains("where-object");
    }

    private static boolean looksLikeCsv(String text) {
        int firstLineEnd = findLineEnd(text, 0);
        if (firstLineEnd >= text.length()) {
            return false;
        }
        int columns = countCsvColumns(text, 0, firstLineEnd);
        if (columns < 2) {
            return false;
        }
        int lineStart = skipLineBreak(text, firstLineEnd);
        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(text, lineStart);
            if (hasText(text, lineStart, lineEnd) && countCsvColumns(text, lineStart, lineEnd) == columns) {
                return true;
            }
            lineStart = skipLineBreak(text, lineEnd);
        }
        return false;
    }

    private static int countCsvColumns(String text, int start, int end) {
        boolean inQuotes = false;
        int count = 1;
        for (int i = start; i < end; i++) {
            char current = text.charAt(i);
            if (current == '"') {
                if (inQuotes && i + 1 < end && text.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (current == ',' && !inQuotes) {
                count++;
            }
        }
        return count;
    }

    private static int findLineEnd(String text, int start) {
        int index = start;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (ch == '\n' || ch == '\r') {
                return index;
            }
            index++;
        }
        return text.length();
    }

    private static int skipLineBreak(String text, int offset) {
        if (offset < text.length() && text.charAt(offset) == '\r') {
            offset++;
        }
        if (offset < text.length() && text.charAt(offset) == '\n') {
            offset++;
        }
        return offset;
    }

    private static boolean hasText(String text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeMermaid(String lower) {
        String trimmed = lower.trim();
        return trimmed.startsWith("graph ") || trimmed.startsWith("flowchart ") || trimmed.startsWith("mindmap");
    }
}
