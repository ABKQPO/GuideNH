package com.hfstudio.guidenh.guide.internal.localization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;

public class GuidePageLangDumpTask {

    private final Path outDir;

    public GuidePageLangDumpTask(Path outDir) {
        this.outDir = outDir;
    }

    public Result run() throws IOException {
        long startedAt = System.nanoTime();
        List<IResourcePack> resourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        Map<PageDumpKey, String> localizedPages = collectLocalizedPages(resourcePacks);
        Map<LangFileTarget, LinkedHashMap<String, String>> entriesByFile = buildEntries(localizedPages);
        writeEntries(entriesByFile);

        int exportedPageCount = 0;
        var languages = new LinkedHashSet<String>();
        var namespaces = new LinkedHashSet<String>();
        for (Map<String, String> entries : entriesByFile.values()) {
            exportedPageCount += entries.size();
        }
        for (LangFileTarget target : entriesByFile.keySet()) {
            languages.add(target.language());
            namespaces.add(target.namespace());
        }
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return new Result(
            outDir,
            entriesByFile.size(),
            exportedPageCount,
            languages.size(),
            namespaces.size(),
            resourcePacks.size(),
            durationMillis);
    }

    private Map<PageDumpKey, String> collectLocalizedPages(List<IResourcePack> resourcePacks) throws IOException {
        Map<PageDumpKey, String> localizedPages = new LinkedHashMap<>();
        for (IResourcePack resourcePack : resourcePacks) {
            var resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
            if (resourcePackFile == null || !resourcePackFile.exists()) {
                continue;
            }
            if (resourcePackFile.isDirectory()) {
                collectDirectoryLocalizedPages(resourcePackFile.toPath(), localizedPages);
            } else {
                collectZipLocalizedPages(resourcePackFile.toPath(), localizedPages);
            }
        }
        return localizedPages;
    }

    private void collectDirectoryLocalizedPages(Path resourcePackRoot, Map<PageDumpKey, String> localizedPages)
        throws IOException {
        Path assetsRoot = resourcePackRoot.resolve("assets");
        if (!Files.isDirectory(assetsRoot)) {
            return;
        }
        try (var namespaceStream = Files.list(assetsRoot)) {
            for (Path namespaceRoot : (Iterable<Path>) namespaceStream.filter(Files::isDirectory)::iterator) {
                collectDirectoryNamespaceLocalizedPages(namespaceRoot, localizedPages);
            }
        }
    }

    private void collectDirectoryNamespaceLocalizedPages(Path namespaceRoot, Map<PageDumpKey, String> localizedPages)
        throws IOException {
        String namespace = fileName(namespaceRoot);
        try (var folderStream = Files.list(namespaceRoot)) {
            for (Path folderRoot : (Iterable<Path>) folderStream.filter(Files::isDirectory)::iterator) {
                collectDirectoryContentRootLocalizedPages(namespace, folderRoot, localizedPages);
            }
        }
    }

    private void collectDirectoryContentRootLocalizedPages(String namespace, Path folderRoot,
        Map<PageDumpKey, String> localizedPages) throws IOException {
        String contentRootFolder = fileName(folderRoot);
        try (var languageStream = Files.list(folderRoot)) {
            for (Path languageRoot : (Iterable<Path>) languageStream.filter(Files::isDirectory)::iterator) {
                String languageFolder = fileName(languageRoot);
                if (!DataDrivenGuideLoader.isLanguageFolder(languageFolder)) {
                    continue;
                }
                String language = DataDrivenGuideLoader.toLanguageCode(languageFolder);
                collectDirectoryLanguagePages(namespace, contentRootFolder, languageRoot, language, localizedPages);
            }
        }
    }

    private void collectDirectoryLanguagePages(String namespace, String contentRootFolder, Path languageRoot,
        String language, Map<PageDumpKey, String> localizedPages) throws IOException {
        try (var pathStream = Files.walk(languageRoot)) {
            for (Path pageFile : (Iterable<Path>) pathStream.filter(Files::isRegularFile)::iterator) {
                String pageName = fileName(pageFile);
                if (!pageName.endsWith(".md")) {
                    continue;
                }
                String relativePath = normalizeRelativePath(languageRoot.relativize(pageFile));
                localizedPages.put(
                    new PageDumpKey(namespace, contentRootFolder, language, relativePath),
                    Files.readString(pageFile, StandardCharsets.UTF_8));
            }
        }
    }

    private void collectZipLocalizedPages(Path resourcePackFile, Map<PageDumpKey, String> localizedPages)
        throws IOException {
        try (ZipFile zip = new ZipFile(resourcePackFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                PageDumpKey key = parseLocalizedZipEntry(entry.getName());
                if (key == null) {
                    continue;
                }
                localizedPages.put(
                    key,
                    new String(
                        zip.getInputStream(entry)
                            .readAllBytes(),
                        StandardCharsets.UTF_8));
            }
        }
    }

    private PageDumpKey parseLocalizedZipEntry(String path) {
        if (!path.startsWith("assets/")) {
            return null;
        }
        String[] segments = path.split("/");
        if (segments.length < 5) {
            return null;
        }
        String namespace = segments[1];
        String contentRootFolder = segments[2];
        String languageFolder = segments[3];
        if (!DataDrivenGuideLoader.isLanguageFolder(languageFolder)) {
            return null;
        }
        String pagePath = joinSegments(segments, 4);
        if (!pagePath.endsWith(".md")) {
            return null;
        }
        return new PageDumpKey(
            namespace,
            contentRootFolder,
            DataDrivenGuideLoader.toLanguageCode(languageFolder),
            pagePath);
    }

    private Map<LangFileTarget, LinkedHashMap<String, String>> buildEntries(Map<PageDumpKey, String> localizedPages) {
        Map<LangFileTarget, LinkedHashMap<String, String>> entriesByFile = new LinkedHashMap<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            addGuideEntries(guide, localizedPages, entriesByFile);
        }
        return entriesByFile;
    }

    private void addGuideEntries(MutableGuide guide, Map<PageDumpKey, String> localizedPages,
        Map<LangFileTarget, LinkedHashMap<String, String>> entriesByFile) {
        String namespace = guide.getId()
            .getResourceDomain();
        String contentRootFolder = guide.getContentRootFolder();
        for (var entry : localizedPages.entrySet()) {
            PageDumpKey key = entry.getKey();
            if (!namespace.equals(key.namespace()) || !contentRootFolder.equals(key.contentRootFolder())) {
                continue;
            }
            ResourceLocation pageId = new ResourceLocation(namespace, key.pagePath());
            String langKey = GuideLocalizedPageSourceResolver.buildLangKey(contentRootFolder, pageId);
            LangFileTarget target = new LangFileTarget(namespace, key.language());
            entriesByFile.computeIfAbsent(target, ignored -> new LinkedHashMap<>())
                .put(langKey, escapeLangValue(entry.getValue()));
        }
    }

    private void writeEntries(Map<LangFileTarget, LinkedHashMap<String, String>> entriesByFile) throws IOException {
        for (var entry : entriesByFile.entrySet()) {
            Path target = outDir.resolve("assets")
                .resolve(
                    entry.getKey()
                        .namespace())
                .resolve("lang")
                .resolve(
                    entry.getKey()
                        .language() + ".lang");
            Files.createDirectories(target.getParent());

            List<String> lines = new ArrayList<>();
            var sortedKeys = new ArrayList<>(
                entry.getValue()
                    .keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                lines.add(
                    key + "="
                        + entry.getValue()
                            .get(key));
            }
            Files.write(target, lines, StandardCharsets.UTF_8);
        }
    }

    private String escapeLangValue(String value) {
        String normalized = value.replace("\r\n", "\n")
            .replace("\r", "\n");
        StringBuilder builder = new StringBuilder(normalized.length() + 32);
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                default:
                    builder.append(ch);
                    break;
            }
        }
        return builder.toString();
    }

    private String fileName(Path path) {
        return path.getFileName() != null ? path.getFileName()
            .toString() : "";
    }

    private String normalizeRelativePath(Path path) {
        return path.toString()
            .replace('\\', '/');
    }

    private String joinSegments(String[] segments, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < segments.length; i++) {
            if (i > startIndex) {
                builder.append('/');
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    @Desugar
    public record Result(Path outDir, int writtenLanguageFileCount, int exportedPageCount, int languageCount,
        int namespaceCount, int resourcePackCount, long durationMillis) {}

    @Desugar
    private record LangFileTarget(String namespace, String language) {}

    @Desugar
    private record PageDumpKey(String namespace, String contentRootFolder, String language, String pagePath) {}
}
