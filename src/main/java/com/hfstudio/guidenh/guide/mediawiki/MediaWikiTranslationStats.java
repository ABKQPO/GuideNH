package com.hfstudio.guidenh.guide.mediawiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;

public class MediaWikiTranslationStats {

    private static final Object CACHE_LOCK = new Object();
    private static volatile TranslationCacheEntry cachedEntry;

    private MediaWikiTranslationStats() {}

    public static TranslationSnapshot scan(Guide guide) {
        if (guide == null) {
            return new TranslationSnapshot(
                Collections.<String>emptyList(),
                Collections.<String, Set<String>>emptyMap(),
                0);
        }
        String cacheKey = cacheKey(guide);
        long currentRevision = computeRevision();
        TranslationCacheEntry cacheEntry = cachedEntry;
        if (cacheEntry != null && cacheEntry.matches(cacheKey, currentRevision)) {
            return cacheEntry.snapshot();
        }
        synchronized (CACHE_LOCK) {
            cacheEntry = cachedEntry;
            if (cacheEntry != null && cacheEntry.matches(cacheKey, currentRevision)) {
                return cacheEntry.snapshot();
            }
            TranslationSnapshot snapshot = buildSnapshot(guide);
            cachedEntry = new TranslationCacheEntry(cacheKey, currentRevision, snapshot);
            return snapshot;
        }
    }

    public static void invalidateCache() {
        synchronized (CACHE_LOCK) {
            cachedEntry = null;
        }
    }

    private static TranslationSnapshot buildSnapshot(Guide guide) {
        Iterable<Guide> guides = resolveGuides(guide);
        LinkedHashMap<String, LinkedHashSet<String>> pagePathsByLanguage = new LinkedHashMap<>();
        LinkedHashSet<String> discoveredLanguages = new LinkedHashSet<>();

        for (IResourcePack resourcePack : DataDrivenGuideLoader.getActiveResourcePacks()) {
            File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
            if (resourcePackFile == null || !resourcePackFile.exists()) {
                continue;
            }
            for (Guide sourceGuide : guides) {
                if (resourcePackFile.isDirectory()) {
                    scanFolderPagesByLanguage(sourceGuide, resourcePackFile, pagePathsByLanguage, discoveredLanguages);
                } else {
                    scanZipPagesByLanguage(sourceGuide, resourcePackFile, pagePathsByLanguage, discoveredLanguages);
                }
            }
        }

        TreeSet<String> sortedLanguages = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sortedLanguages.addAll(discoveredLanguages);
        sortedLanguages.addAll(pagePathsByLanguage.keySet());
        LinkedHashSet<String> languages = new LinkedHashSet<>(sortedLanguages);
        LinkedHashMap<String, Set<String>> resolvedPagePathsByLanguage = new LinkedHashMap<>();
        for (String language : languages) {
            resolvedPagePathsByLanguage.put(
                language,
                new LinkedHashSet<String>(pagePathsByLanguage.getOrDefault(language, new LinkedHashSet<String>())));
        }

        LinkedHashSet<String> allSourcePages = new LinkedHashSet<>();
        for (Set<String> value : resolvedPagePathsByLanguage.values()) {
            allSourcePages.addAll(value);
        }
        return new TranslationSnapshot(new ArrayList<>(languages), resolvedPagePathsByLanguage, allSourcePages.size());
    }

    private static Iterable<Guide> resolveGuides(Guide guide) {
        if (guide instanceof MediaWikiGuideAggregator aggregator) {
            return new ArrayList<Guide>(aggregator.getComponentGuides());
        }
        return Collections.singletonList(guide);
    }

    private static String cacheKey(Guide guide) {
        ResourceLocation guideId = guide.getId();
        return (guideId != null ? guideId.toString() : "guide") + "|"
            + guide.getDefaultNamespace()
            + "|"
            + guide.getContentRootFolder();
    }

    private static long computeRevision() {
        long revision = 1L;
        for (IResourcePack resourcePack : DataDrivenGuideLoader.getActiveResourcePacks()) {
            File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
            if (resourcePackFile == null || !resourcePackFile.exists()) {
                continue;
            }
            revision = 31L * revision + resourcePackFile.getAbsolutePath()
                .hashCode();
            revision = 31L * revision + resourcePackFile.lastModified();
            revision = 31L * revision + resourcePackFile.length();
        }
        return revision;
    }

    private static void scanFolderPagesByLanguage(Guide guide, File resourcePackRoot,
        Map<String, LinkedHashSet<String>> pagePathsByLanguage, Set<String> discoveredLanguages) {
        File guideRoot = new File(
            new File(new File(resourcePackRoot, "assets"), guide.getDefaultNamespace()),
            guide.getContentRootFolder());
        File[] languageDirs = guideRoot.listFiles(File::isDirectory);
        if (languageDirs == null) {
            return;
        }
        for (File languageDir : languageDirs) {
            String folder = languageDir.getName();
            if (!folder.startsWith("_")) {
                continue;
            }
            String language = folder.substring(1);
            discoveredLanguages.add(language);
            LinkedHashSet<String> pagePaths = pagePathsByLanguage
                .computeIfAbsent(language, ignored -> new LinkedHashSet<String>());
            DataDrivenGuideLoader.collectMarkdownPaths(languageDir, "", pagePaths);
        }
    }

    private static void scanZipPagesByLanguage(Guide guide, File resourcePackFile,
        Map<String, LinkedHashSet<String>> pagePathsByLanguage, Set<String> discoveredLanguages) {
        String prefix = "assets/" + guide.getDefaultNamespace() + "/" + guide.getContentRootFolder() + "/";
        try (ZipFile zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) {
                    continue;
                }
                String relative = path.substring(prefix.length());
                int slashIndex = relative.indexOf('/');
                if (slashIndex <= 0) {
                    continue;
                }
                String folder = relative.substring(0, slashIndex);
                if (!folder.startsWith("_")) {
                    continue;
                }
                String language = folder.substring(1);
                discoveredLanguages.add(language);
                String pagePath = relative.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) {
                    pagePathsByLanguage.computeIfAbsent(language, ignored -> new LinkedHashSet<String>())
                        .add(pagePath);
                }
            }
        } catch (IOException ignored) {}
    }

    @Desugar
    public record TranslationSnapshot(List<String> languages, Map<String, Set<String>> pagePathsByLanguage,
        int sourcePageCount) {

        public Set<String> pagePathsForLanguage(String language) {
            Set<String> pages = pagePathsByLanguage != null ? pagePathsByLanguage.get(language) : null;
            return pages != null ? pages : Collections.<String>emptySet();
        }

        public int translatedSourcePageCount() {
            if (pagePathsByLanguage == null || pagePathsByLanguage.isEmpty()) {
                return 0;
            }
            LinkedHashMap<String, Integer> appearances = new LinkedHashMap<>();
            for (Set<String> pages : pagePathsByLanguage.values()) {
                for (String page : pages) {
                    appearances.put(page, appearances.getOrDefault(page, 0) + 1);
                }
            }
            int translated = 0;
            for (int count : appearances.values()) {
                if (count > 1) {
                    translated++;
                }
            }
            return translated;
        }

        public int fullyTranslatedSourcePageCount() {
            if (pagePathsByLanguage == null || pagePathsByLanguage.isEmpty()) {
                return 0;
            }
            int expected = pagePathsByLanguage.size();
            LinkedHashMap<String, Integer> appearances = new LinkedHashMap<>();
            for (Set<String> pages : pagePathsByLanguage.values()) {
                for (String page : pages) {
                    appearances.put(page, appearances.getOrDefault(page, 0) + 1);
                }
            }
            int translated = 0;
            for (int count : appearances.values()) {
                if (count >= expected) {
                    translated++;
                }
            }
            return translated;
        }

        public int pageCountForLanguage(String language) {
            Set<String> pages = pagePathsByLanguage != null ? pagePathsByLanguage.get(language) : null;
            return pages != null ? pages.size() : 0;
        }

        public List<String> languagesForPagePath(String pagePath) {
            if (pagePath == null || pagePath.isEmpty()
                || pagePathsByLanguage == null
                || pagePathsByLanguage.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList<String> matchingLanguages = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : pagePathsByLanguage.entrySet()) {
                if (entry.getValue() != null && entry.getValue()
                    .contains(pagePath)) {
                    matchingLanguages.add(entry.getKey());
                }
            }
            matchingLanguages.sort(String.CASE_INSENSITIVE_ORDER);
            return matchingLanguages;
        }

        public Set<String> allPagePaths() {
            if (pagePathsByLanguage == null || pagePathsByLanguage.isEmpty()) {
                return Collections.emptySet();
            }
            LinkedHashSet<String> pagePaths = new LinkedHashSet<>();
            for (Set<String> pages : pagePathsByLanguage.values()) {
                if (pages != null) {
                    pagePaths.addAll(pages);
                }
            }
            return pagePaths;
        }
    }

    @Desugar
    private record TranslationCacheEntry(String cacheKey, long revision, TranslationSnapshot snapshot) {

        private boolean matches(String candidateKey, long candidateRevision) {
            return cacheKey.equals(candidateKey) && revision == candidateRevision;
        }
    }
}
