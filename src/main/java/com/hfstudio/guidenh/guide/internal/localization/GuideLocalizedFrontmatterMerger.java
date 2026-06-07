package com.hfstudio.guidenh.guide.internal.localization;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;

public class GuideLocalizedFrontmatterMerger {

    private static final ThreadLocal<Yaml> YAML = ThreadLocal.withInitial(GuideLocalizedFrontmatterMerger::createYaml);

    private GuideLocalizedFrontmatterMerger() {}

    public static String merge(String fallbackSource, String localizedSource) {
        String normalizedFallback = PageCompiler.normalizeLineEndings(fallbackSource);
        String normalizedLocalized = PageCompiler.normalizeLineEndings(localizedSource);
        SourceParts fallbackParts = SourceParts.split(normalizedFallback);
        if (fallbackParts.frontmatter() == null) {
            return normalizedLocalized;
        }

        SourceParts localizedParts = SourceParts.split(normalizedLocalized);
        if (localizedParts.frontmatter() == null) {
            return fallbackParts.withBody(normalizedLocalized);
        }

        Map<String, Object> fallbackFrontmatter = readMap(fallbackParts.frontmatter());
        Map<String, Object> localizedFrontmatter = readMap(localizedParts.frontmatter());
        if (fallbackFrontmatter == null || localizedFrontmatter == null) {
            return normalizedLocalized;
        }

        boolean changed = mergeMissingKeys(fallbackFrontmatter, localizedFrontmatter);
        changed |= mergeNavigation(fallbackFrontmatter, localizedFrontmatter);
        if (!changed) {
            return normalizedLocalized;
        }
        return SourceParts.withFrontmatterAndBody(writeMap(localizedFrontmatter), localizedParts.body());
    }

    private static Yaml createYaml() {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
    }

    private static boolean mergeMissingKeys(Map<String, Object> fallbackFrontmatter,
        Map<String, Object> localizedFrontmatter) {
        boolean changed = false;
        for (var entry : fallbackFrontmatter.entrySet()) {
            if ("navigation".equals(entry.getKey())) {
                continue;
            }
            if (!localizedFrontmatter.containsKey(entry.getKey())) {
                localizedFrontmatter.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean mergeNavigation(Map<String, Object> fallbackFrontmatter,
        Map<String, Object> localizedFrontmatter) {
        Object fallbackNavigation = fallbackFrontmatter.get("navigation");
        if (!(fallbackNavigation instanceof Map<?, ?>fallbackNavigationMap)) {
            return false;
        }

        Object localizedNavigation = localizedFrontmatter.get("navigation");
        if (!(localizedNavigation instanceof Map<?, ?>localizedNavigationMap)) {
            localizedFrontmatter.put("navigation", new LinkedHashMap<>(fallbackNavigationMap));
            return true;
        }

        boolean changed = false;
        Map<Object, Object> writableLocalizedNavigation = ensureWritableNavigation(
            localizedFrontmatter,
            localizedNavigationMap);
        for (var entry : fallbackNavigationMap.entrySet()) {
            if (!writableLocalizedNavigation.containsKey(entry.getKey())) {
                writableLocalizedNavigation.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static Map<Object, Object> ensureWritableNavigation(Map<String, Object> localizedFrontmatter,
        Map<?, ?> localizedNavigationMap) {
        if (localizedNavigationMap instanceof LinkedHashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> writable = (Map<Object, Object>) localizedNavigationMap;
            return writable;
        }

        Map<Object, Object> writable = new LinkedHashMap<>(localizedNavigationMap);
        localizedFrontmatter.put("navigation", writable);
        return writable;
    }

    private static @Nullable Map<String, Object> readMap(String yamlText) {
        Object loaded = YAML.get()
            .load(yamlText);
        if (loaded == null) {
            return new LinkedHashMap<>();
        }
        if (!(loaded instanceof Map<?, ?>loadedMap)) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : loadedMap.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String keyText)) {
                return null;
            }
            result.put(keyText, entry.getValue());
        }
        return result;
    }

    private static String writeMap(Map<String, Object> frontmatter) {
        String dumped = YAML.get()
            .dump(frontmatter);
        return dumped.endsWith("\n") ? dumped.substring(0, dumped.length() - 1) : dumped;
    }

    public record SourceParts(@Nullable String frontmatter, String body) {

        public static SourceParts split(String source) {
            String frontmatter = PageCompiler.extractFrontmatterText(source);
            if (frontmatter == null) {
                return new SourceParts(null, source);
            }

            int bodyStart = findBodyStart(source);
            String body = bodyStart >= 0 ? source.substring(bodyStart) : "";
            return new SourceParts(frontmatter, body);
        }

        public String withBody(String replacementBody) {
            return withFrontmatterAndBody(frontmatter, replacementBody);
        }

        public static String withFrontmatterAndBody(String frontmatter, String body) {
            return "---\n" + frontmatter + "\n---\n" + body;
        }

        private static int findBodyStart(String source) {
            int bodyStart = 4;
            int closingMarker = source.indexOf("\n---\n", bodyStart);
            if (closingMarker >= 0) {
                return closingMarker + 5;
            }
            if (source.endsWith("\n---")) {
                return source.length();
            }
            return -1;
        }
    }
}
