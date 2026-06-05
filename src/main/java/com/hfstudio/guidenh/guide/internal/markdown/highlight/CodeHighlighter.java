package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageDetector;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageRegistry;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.JavaLikeTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.JsonTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.LuaTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.MarkdownTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.PlainTextTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.PropertiesTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.ShellTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.XmlTokenizer;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.tokenizer.YamlTokenizer;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class CodeHighlighter {

    public static final int FULL_MODE_MAX_BYTES = 16 * 1024;
    public static final int FULL_MODE_MAX_LINES = 400;
    public static final int FAST_MODE_MAX_BYTES = 64 * 1024;
    public static final int FAST_MODE_MAX_LINES = 1600;
    private static final LanguageTokenizer PLAIN_TEXT_TOKENIZER = new PlainTextTokenizer();
    private static final Map<String, LanguageTokenizer> TOKENIZERS = buildTokenizers(PLAIN_TEXT_TOKENIZER);

    public CodeHighlighter() {}

    public CodeHighlightResult highlight(@Nullable String explicitFenceLanguage, @Nullable String codeText) {
        String safeCodeText = GuideStringLines.normalizeLineEndings(codeText != null ? codeText : "");
        String languageId = resolveLanguageId(explicitFenceLanguage, safeCodeText);
        CodeHighlightMode mode = selectMode(safeCodeText);
        LanguageTokenizer tokenizer = TOKENIZERS.getOrDefault(languageId, PLAIN_TEXT_TOKENIZER);
        try {
            return tokenizer.highlight(languageId, safeCodeText, mode);
        } catch (RuntimeException e) {
            GuideDebugLog.error("[GuideNH] [CodeHighlighter] Failed to highlight language {}", languageId, e);
            return PLAIN_TEXT_TOKENIZER.highlight(languageId, safeCodeText, CodeHighlightMode.PLAIN);
        }
    }

    private String resolveLanguageId(@Nullable String explicitFenceLanguage, String safeCodeText) {
        String normalized = CodeBlockLanguageRegistry.normalizeFenceLanguage(explicitFenceLanguage);
        if (normalized != null) {
            return normalized;
        }
        CodeBlockLanguage detected = CodeBlockLanguageDetector.detect(null, safeCodeText);
        return detected != null ? detected.id() : "text";
    }

    private CodeHighlightMode selectMode(String safeCodeText) {
        int length = safeCodeText.length();
        int lines = countLines(safeCodeText);
        if (length > FAST_MODE_MAX_BYTES || lines > FAST_MODE_MAX_LINES) {
            return CodeHighlightMode.PLAIN;
        }
        if (length > FULL_MODE_MAX_BYTES || lines > FULL_MODE_MAX_LINES) {
            return CodeHighlightMode.FAST;
        }
        return CodeHighlightMode.FULL;
    }

    private int countLines(String safeCodeText) {
        if (safeCodeText.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int index = 0; index < safeCodeText.length(); index++) {
            if (safeCodeText.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private static Map<String, LanguageTokenizer> buildTokenizers(LanguageTokenizer plainTextTokenizer) {
        Map<String, LanguageTokenizer> result = new HashMap<>();
        LanguageTokenizer javaLike = new JavaLikeTokenizer();
        LanguageTokenizer json = new JsonTokenizer();
        LanguageTokenizer yaml = new YamlTokenizer();
        LanguageTokenizer xml = new XmlTokenizer();
        LanguageTokenizer properties = new PropertiesTokenizer();
        LanguageTokenizer shell = new ShellTokenizer();
        LanguageTokenizer markdown = new MarkdownTokenizer();
        LanguageTokenizer lua = new LuaTokenizer();
        LanguageTokenizer plain = plainTextTokenizer;

        result.put("text", plain);
        result.put("java", javaLike);
        result.put("kotlin", javaLike);
        result.put("scala", javaLike);
        result.put("groovy", javaLike);
        result.put("json", json);
        result.put("yaml", yaml);
        result.put("xml", xml);
        result.put("properties", properties);
        result.put("bash", shell);
        result.put("powershell", shell);
        result.put("markdown", markdown);
        result.put("lua", lua);
        result.put("javascript", javaLike);
        result.put("typescript", javaLike);
        return Map.copyOf(result);
    }
}
