package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public final class CodeBlockLanguageRegistry {

    private static final Map<String, CodeBlockLanguage> BY_FENCE_NAME = buildFenceMap();

    private CodeBlockLanguageRegistry() {}

    public static @Nullable CodeBlockLanguage findByFenceName(@Nullable String fenceName) {
        if (fenceName == null || fenceName.isEmpty()) {
            return null;
        }
        return BY_FENCE_NAME.get(fenceName.toLowerCase(Locale.ROOT));
    }

    private static Map<String, CodeBlockLanguage> buildFenceMap() {
        Map<String, CodeBlockLanguage> result = new HashMap<>();
        register(result, new CodeBlockLanguage("text", "Text"), "text", "plain", "plaintext", "txt");
        register(result, new CodeBlockLanguage("java", "Java"), "java");
        register(result, new CodeBlockLanguage("kotlin", "Kotlin"), "kt", "kotlin", "kts");
        register(result, new CodeBlockLanguage("scala", "Scala"), "scala", "sc");
        register(result, new CodeBlockLanguage("groovy", "Groovy"), "groovy", "gradle");
        register(result, new CodeBlockLanguage("lua", "Lua"), "lua");
        register(result, new CodeBlockLanguage("json", "JSON"), "json");
        register(result, new CodeBlockLanguage("yaml", "YAML"), "yaml", "yml");
        register(result, new CodeBlockLanguage("xml", "XML"), "xml");
        register(result, new CodeBlockLanguage("properties", "Properties"), "properties");
        register(result, new CodeBlockLanguage("bash", "Bash"), "bash", "sh", "shell");
        register(result, new CodeBlockLanguage("powershell", "PowerShell"), "powershell", "ps1", "pwsh");
        register(result, new CodeBlockLanguage("markdown", "Markdown"), "markdown", "md");
        register(result, new CodeBlockLanguage("csv", "CSV"), "csv");
        register(result, new CodeBlockLanguage("mermaid", "Mermaid"), "mermaid");
        return Collections.unmodifiableMap(result);
    }

    private static void register(Map<String, CodeBlockLanguage> result, CodeBlockLanguage language, String... aliases) {
        Arrays.stream(aliases)
            .forEach(alias -> result.put(alias, language));
    }
}
