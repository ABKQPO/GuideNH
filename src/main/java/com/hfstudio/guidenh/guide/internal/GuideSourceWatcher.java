package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.localization.GuideLocalizedPageSourceResolver;
import com.hfstudio.guidenh.guide.internal.localization.GuidePageLanguageIndex;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

import cpw.mods.fml.common.FMLLog;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

public class GuideSourceWatcher implements AutoCloseable {

    private final String defaultLanguage;

    /**
     * The {@link ResourceLocation} namespace to use for files in the watched folder.
     */
    private final String namespace;
    private final String contentRootFolder;
    private final Path sourceFolder;
    private final GuideDevelopmentSourceLayout sourceLayout;

    // Recursive directory watcher for the guidebook sources.
    @Nullable
    private final DirectoryWatcher sourceWatcher;

    // Queued changes that come in from a separate thread
    @Desugar
    private static class PageLangKey {

        @Nullable
        private final String sourceLang;
        private final ResourceLocation pageId;

        private PageLangKey(@Nullable String sourceLang, ResourceLocation pageId) {
            this.sourceLang = sourceLang;
            this.pageId = pageId;
        }

        @Nullable
        private String sourceLang() {
            return sourceLang;
        }

        private ResourceLocation pageId() {
            return pageId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PageLangKey other)) {
                return false;
            }
            return Objects.equals(sourceLang, other.sourceLang) && Objects.equals(pageId, other.pageId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceLang, pageId);
        }
    }

    @Desugar
    private static class PageSource {

        private final String requestedLanguage;
        private final Path path;
        @Nullable
        private final String localizedSourceOverride;

        private PageSource(String requestedLanguage, Path path, @Nullable String localizedSourceOverride) {
            this.requestedLanguage = requestedLanguage;
            this.path = path;
            this.localizedSourceOverride = localizedSourceOverride;
        }

        private String requestedLanguage() {
            return requestedLanguage;
        }

        private Path path() {
            return path;
        }

        private @Nullable String localizedSourceOverride() {
            return localizedSourceOverride;
        }
    }

    @Desugar
    private static class PageReloadRequest {

        @Nullable
        private final String namespace;
        private final boolean clearLanguageCache;

        private PageReloadRequest(@Nullable String namespace, boolean clearLanguageCache) {
            this.namespace = namespace;
            this.clearLanguageCache = clearLanguageCache;
        }

        @Nullable
        private String namespace() {
            return namespace;
        }

        private boolean clearLanguageCache() {
            return clearLanguageCache;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PageReloadRequest other)) {
                return false;
            }
            return clearLanguageCache == other.clearLanguageCache && Objects.equals(namespace, other.namespace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, clearLanguageCache);
        }
    }

    @Desugar
    private static class ResolvedPageState {

        @Nullable
        private final String requestedLanguage;
        @Nullable
        private final ParsedGuidePage page;
        private final boolean deleted;

        private ResolvedPageState(@Nullable String requestedLanguage, @Nullable ParsedGuidePage page, boolean deleted) {
            this.requestedLanguage = requestedLanguage;
            this.page = page;
            this.deleted = deleted;
        }

        private static ResolvedPageState deleted() {
            return new ResolvedPageState(null, null, true);
        }

        private @Nullable String requestedLanguage() {
            return requestedLanguage;
        }

        private @Nullable ParsedGuidePage page() {
            return page;
        }

        private boolean isDeleted() {
            return deleted;
        }
    }

    private final Map<PageLangKey, ParsedGuidePage> changedPages = new HashMap<>();
    private final Set<PageLangKey> deletedPages = new HashSet<>();
    private final Set<PageReloadRequest> reloadRequests = new HashSet<>();

    private final ExecutorService watchExecutor;

    public GuideSourceWatcher(String namespace, String contentRootFolder, String defaultLanguage, Path sourceFolder) {
        this.namespace = namespace;
        this.contentRootFolder = contentRootFolder;
        this.defaultLanguage = LangUtil.normalizeLanguage(defaultLanguage);
        this.sourceFolder = sourceFolder;
        this.sourceLayout = detectSourceLayout(sourceFolder);
        if (!Files.isDirectory(sourceFolder)) {
            throw new RuntimeException("Cannot find the specified folder with guidebook sources: " + sourceFolder);
        }
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info("[GuideNH] [GuideSourceWatcher] Watching guidebook sources in {}", sourceFolder);
        }

        watchExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("GuideMELiveReloadWatcher%d")
                .build());

        // Watch the folder recursively in a separate thread, queue up any changes and apply them
        // in the client tick.
        DirectoryWatcher watcher;
        try {
            watcher = DirectoryWatcher.builder()
                .path(sourceFolder)
                .fileHashing(false)
                .listener(new Listener())
                .build();
        } catch (IOException e) {
            FMLLog.getLogger()
                .error(
                    "[GuideNH] [GuideSourceWatcher] Failed to watch for changes in the guidebook sources at {}",
                    sourceFolder,
                    e);
            watcher = null;
        }
        sourceWatcher = watcher;

        // Actually process changes in the client tick to prevent race conditions and other crashes
        if (sourceWatcher != null) {
            sourceWatcher.watchAsync(watchExecutor);
        }
    }

    public List<ParsedGuidePage> loadAll() {
        return loadAll(null);
    }

    private List<ParsedGuidePage> loadAll(@Nullable String namespaceFilter) {
        long startedAt = System.nanoTime();
        var currentLanguage = LangUtil.getCurrentLanguage();
        var pageSources = new HashMap<PageLangKey, Path>();
        var pageIds = new LinkedHashSet<ResourceLocation>();

        long stageStartedAt = System.nanoTime();
        try {
            Files.walkFileTree(sourceFolder, new FileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var pageKey = getPageLangKey(file);
                    if (pageKey != null && (namespaceFilter == null || namespaceFilter.equals(
                        pageKey.pageId()
                            .getResourceDomain()))) {
                        pageSources.put(pageKey, file);
                        pageIds.add(pageKey.pageId());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    FMLLog.getLogger()
                        .error("[GuideNH] [GuideSourceWatcher] Failed to list page {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null) {
                        FMLLog.getLogger()
                            .error("[GuideNH] [GuideSourceWatcher] Failed to list all pages in {}", dir, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            FMLLog.getLogger()
                .error("[GuideNH] [GuideSourceWatcher] Failed to list all pages in {}", sourceFolder, e);
        }
        long walkNs = System.nanoTime() - stageStartedAt;

        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideSourceWatcher] Loading {} guidebook pages from {} localized variants",
                    pageIds.size(),
                    pageSources.size());
        }
        stageStartedAt = System.nanoTime();
        Map<String, Map<String, String>> localizedSourcesByNamespace = loadLocalizedSourceOverrides(
            currentLanguage,
            namespaceFilter);
        long localizedOverrideNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var loadedPages = new ArrayList<ParsedGuidePage>(pageIds.size());
        for (var pageId : pageIds) {
            var pageSource = resolvePageSource(pageSources, localizedSourcesByNamespace, pageId, currentLanguage);
            if (pageSource == null) {
                continue;
            }

            try {
                loadedPages.add(
                    GuideLocalizedPageSourceResolver.parse(
                        getSourcePackId(pageId),
                        pageSource.requestedLanguage(),
                        contentRootFolder,
                        pageId,
                        Files.readAllBytes(pageSource.path()),
                        pageSource.localizedSourceOverride()));
            } catch (Exception e) {
                FMLLog.getLogger()
                    .error("[GuideNH] [GuideSourceWatcher] Failed to reload guidebook page {}", pageSource.path(), e);
            }
        }
        long parseNs = System.nanoTime() - stageStartedAt;
        int sourceLanguageCount = countSourceLanguages(pageSources.keySet());
        long totalNs = System.nanoTime() - startedAt;

        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideSourceWatcher] Loaded {} pages from {} with namespaceFilter={} sourceVariants={} sourceLanguages={} localizedNamespaces={} walkNs={} localizedOverrideNs={} parseNs={} totalNs={}",
                    loadedPages.size(),
                    sourceFolder,
                    namespaceFilter != null ? namespaceFilter : "<all>",
                    pageSources.size(),
                    sourceLanguageCount,
                    localizedSourcesByNamespace.size(),
                    walkNs,
                    localizedOverrideNs,
                    parseNs,
                    totalNs);
        }
        return loadedPages;
    }

    public void clearChanges() {
        synchronized (this) {
            changedPages.clear();
            deletedPages.clear();
            reloadRequests.clear();
        }
    }

    public List<GuidePageChange> takeChanges() {
        Set<PageReloadRequest> requests = takeReloadRequests();
        if (!requests.isEmpty()) {
            boolean shouldClearLanguageCache = false;
            for (PageReloadRequest request : requests) {
                if (request.clearLanguageCache()) {
                    shouldClearLanguageCache = true;
                    break;
                }
            }
            if (shouldClearLanguageCache) {
                GuidePageLanguageIndex.clear();
                GuideResourceLanguageIndex.clear();
            }
            for (PageReloadRequest request : requests) {
                queueReloadedPages(loadAll(request.namespace()));
            }
        }

        synchronized (this) {
            return takeQueuedChanges();
        }
    }

    private synchronized Set<PageReloadRequest> takeReloadRequests() {
        if (reloadRequests.isEmpty()) {
            return Set.of();
        }
        Map<String, Boolean> mergedByNamespace = new HashMap<>();
        for (PageReloadRequest request : reloadRequests) {
            String key = request.namespace() != null ? request.namespace() : "";
            mergedByNamespace.merge(key, request.clearLanguageCache(), Boolean::logicalOr);
        }
        reloadRequests.clear();
        Set<PageReloadRequest> requests = new HashSet<>();
        for (var entry : mergedByNamespace.entrySet()) {
            requests.add(
                new PageReloadRequest(
                    entry.getKey()
                        .isEmpty() ? null : entry.getKey(),
                    entry.getValue()));
        }
        return requests;
    }

    private synchronized void queueReloadedPages(List<ParsedGuidePage> pages) {
        if (pages.isEmpty()) {
            return;
        }

        Set<ResourceLocation> reloadedPageIds = new HashSet<>();
        for (ParsedGuidePage page : pages) {
            reloadedPageIds.add(page.getId());
        }
        clearQueuedPageStates(reloadedPageIds);

        for (ParsedGuidePage page : pages) {
            PageLangKey pageKey = new PageLangKey(page.getLanguage(), page.getId());
            changedPages.put(pageKey, page);
            deletedPages.remove(pageKey);
        }
    }

    private List<GuidePageChange> takeQueuedChanges() {
        if (deletedPages.isEmpty() && changedPages.isEmpty()) {
            return List.of();
        }

        var changes = new ArrayList<GuidePageChange>();

        for (var deletedPage : deletedPages) {
            ParsedGuidePage newPage = null;
            changes.add(new GuidePageChange(deletedPage.sourceLang(), deletedPage.pageId(), null, newPage));
        }
        deletedPages.clear();

        for (var entry : changedPages.entrySet()) {
            var pageKey = entry.getKey();
            var changedPage = entry.getValue();
            changes.add(new GuidePageChange(pageKey.sourceLang(), pageKey.pageId(), null, changedPage));
        }
        changedPages.clear();

        return changes;
    }

    public synchronized void close() {
        changedPages.clear();
        deletedPages.clear();
        reloadRequests.clear();
        watchExecutor.shutdownNow();
        try {
            watchExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }

        if (sourceWatcher != null) {
            try {
                sourceWatcher.close();
            } catch (IOException e) {
                FMLLog.getLogger()
                    .error("[GuideNH] [GuideSourceWatcher] Failed to close fileystem watcher for {}", sourceFolder);
            }
        }
    }

    private class Listener implements DirectoryChangeListener {

        @Override
        public void onEvent(DirectoryChangeEvent event) {
            if (event.isDirectory()) {
                return;
            }
            switch (event.eventType()) {
                case CREATE, MODIFY -> pageChanged(event.path());
                case DELETE -> pageDeleted(event.path());
            }
        }

        @Override
        public boolean isWatching() {
            return sourceWatcher != null && !sourceWatcher.isClosed();
        }

        @Override
        public void onException(Exception e) {
            FMLLog.getLogger()
                .error("[GuideNH] [GuideSourceWatcher] Failed watching for changes", e);
        }
    }

    private void pageChanged(Path path) {
        var pageKey = getPageLangKey(path);
        if (pageKey == null) {
            synchronized (this) {
                languageFileChanged(path);
                resourceChanged(path);
            }
            return;
        }
        queueResolvedPageState(pageKey.pageId());
    }

    private void pageDeleted(Path path) {
        var pageKey = getPageLangKey(path);
        if (pageKey == null) {
            synchronized (this) {
                languageFileChanged(path);
                resourceChanged(path);
            }
            return;
        }
        queueResolvedPageState(pageKey.pageId());
    }

    public static boolean isGuideResourcePathForSourcePath(Path sourceFolder, String namespace,
        String contentRootFolder, Path path) {
        GuideDevelopmentSourceLayout sourceLayout = detectSourceLayout(sourceFolder, contentRootFolder);
        return getGuideRelativePath(sourceFolder, sourceLayout, namespace, contentRootFolder, path) != null;
    }

    public static boolean isPagePathForSourcePath(Path sourceFolder, String namespace, String contentRootFolder,
        Path path) {
        return resolvePageIdForSourcePath(sourceFolder, namespace, contentRootFolder, path) != null;
    }

    private synchronized void resourceChanged(Path path) {
        if (getGuideLanguageNamespace(path) != null) {
            return;
        }
        if (!isGuideResourcePath(path)) {
            return;
        }
        reloadRequests.add(new PageReloadRequest(getGuideResourceNamespace(path), false));
    }

    private synchronized void languageFileChanged(Path path) {
        String languageNamespace = getGuideLanguageNamespace(path);
        if (languageNamespace == null) {
            return;
        }
        reloadRequests.add(new PageReloadRequest(languageNamespace, true));
    }

    @Nullable
    public ResourceLocation getPageId(Path path) {
        String relativePath = getGuideRelativePath(sourceFolder, sourceLayout, namespace, contentRootFolder, path);
        return parseGuideRelativePageId(relativePath);
    }

    private boolean isGuideResourcePath(Path path) {
        String relativePath = getGuideRelativePath(sourceFolder, sourceLayout, namespace, contentRootFolder, path);
        return relativePath != null;
    }

    @Nullable
    private String getGuideLanguageNamespace(Path path) {
        Path normalizedSourceFolder = sourceFolder.toAbsolutePath()
            .normalize();
        Path normalizedPath = path.toAbsolutePath()
            .normalize();
        if (!normalizedPath.startsWith(normalizedSourceFolder)) {
            return null;
        }

        Path relativePath = normalizedSourceFolder.relativize(normalizedPath);
        if (sourceLayout == GuideDevelopmentSourceLayout.CONTENT_ROOT) {
            if (relativePath.getNameCount() < 2 || !matchesPathSegment(relativePath, 0, "lang")
                || !isLangFileSegment(relativePath, 1)) {
                return null;
            }
            return namespace;
        }

        if (sourceLayout == GuideDevelopmentSourceLayout.RESOURCE_PACK_ROOT) {
            if (relativePath.getNameCount() < 4 || !matchesPathSegment(relativePath, 0, "assets")
                || !matchesPathSegment(relativePath, 2, "lang")
                || !isLangFileSegment(relativePath, 3)) {
                return null;
            }
            return relativePath.getName(1)
                .toString();
        }

        if (relativePath.getNameCount() < 3 || !matchesPathSegment(relativePath, 1, "lang")
            || !isLangFileSegment(relativePath, 2)) {
            return null;
        }
        return relativePath.getName(0)
            .toString();
    }

    private boolean matchesPathSegment(Path path, int index, String expected) {
        return index >= 0 && index < path.getNameCount()
            && expected.equals(
                path.getName(index)
                    .toString());
    }

    private boolean isLangFileSegment(Path path, int index) {
        if (index < 0 || index >= path.getNameCount()) {
            return false;
        }
        String fileName = path.getName(index)
            .toString();
        return fileName.endsWith(".lang");
    }

    @Nullable
    private String getGuideResourceNamespace(Path path) {
        String relativePath = getGuideRelativePath(sourceFolder, sourceLayout, namespace, contentRootFolder, path);
        if (relativePath == null) {
            return null;
        }
        int namespaceEnd = relativePath.indexOf('/');
        return namespaceEnd > 0 ? relativePath.substring(0, namespaceEnd) : null;
    }

    @Nullable
    public static ResourceLocation resolvePageIdForSourcePath(Path sourceFolder, String namespace,
        String contentRootFolder, Path path) {
        GuideDevelopmentSourceLayout sourceLayout = detectSourceLayout(sourceFolder, contentRootFolder);
        String relativePath = getGuideRelativePath(sourceFolder, sourceLayout, namespace, contentRootFolder, path);
        ResourceLocation pageId = parseGuideRelativePageId(relativePath);
        return pageId != null ? LangUtil.stripLangFromPageId(pageId) : null;
    }

    @Nullable
    private static ResourceLocation parseGuideRelativePageId(@Nullable String relativePath) {
        if (relativePath == null || !relativePath.endsWith(".md")) {
            return null;
        }
        int namespaceEnd = relativePath.indexOf('/');
        if (namespaceEnd <= 0 || namespaceEnd + 1 >= relativePath.length()) {
            return null;
        }
        String sourceNamespace = relativePath.substring(0, namespaceEnd);
        String pagePath = relativePath.substring(namespaceEnd + 1);
        if (!sourceNamespace.matches("[a-z0-9_.-]+") || !pagePath.matches("[a-z0-9_./-]+")) {
            return null;
        }
        return new ResourceLocation(sourceNamespace, pagePath);
    }

    @Nullable
    private PageLangKey getPageLangKey(Path path) {
        var pageId = getPageId(path);
        if (pageId == null) {
            return null;
        }

        var lang = LangUtil.getLangFromPageId(pageId);
        if (lang != null) {
            pageId = LangUtil.stripLangFromPageId(pageId);
        }

        return new PageLangKey(lang, pageId);
    }

    @Nullable
    private PageSource resolvePageSource(Map<PageLangKey, Path> pageSources,
        Map<String, Map<String, String>> localizedSourcesByNamespace, ResourceLocation pageId, String currentLanguage) {
        String localizedSourceOverride = findLocalizedSourceOverride(localizedSourcesByNamespace, pageId);
        var currentPath = pageSources.get(new PageLangKey(currentLanguage, pageId));
        if (currentPath != null) {
            return new PageSource(currentLanguage, currentPath, localizedSourceOverride);
        }

        if (!Objects.equals(currentLanguage, defaultLanguage)) {
            var defaultPath = pageSources.get(new PageLangKey(defaultLanguage, pageId));
            if (defaultPath != null) {
                return new PageSource(currentLanguage, defaultPath, localizedSourceOverride);
            }
        }

        var neutralPath = pageSources.get(new PageLangKey(null, pageId));
        if (neutralPath != null) {
            return new PageSource(currentLanguage, neutralPath, localizedSourceOverride);
        }

        return null;
    }

    private void queueResolvedPageState(ResourceLocation pageId) {
        ResolvedPageState state = resolveQueuedPageState(pageId);
        if (state == null) {
            return;
        }

        synchronized (this) {
            clearQueuedPageState(pageId);
            if (state.isDeleted()) {
                deletedPages.add(new PageLangKey(null, pageId));
                return;
            }
            changedPages.put(new PageLangKey(state.requestedLanguage(), pageId), state.page());
        }
    }

    private void clearQueuedPageState(ResourceLocation pageId) {
        clearQueuedPageStates(Set.of(pageId));
    }

    private void clearQueuedPageStates(Set<ResourceLocation> pageIds) {
        changedPages.keySet()
            .removeIf(pageKey -> pageIds.contains(pageKey.pageId()));
        deletedPages.removeIf(pageKey -> pageIds.contains(pageKey.pageId()));
    }

    private @Nullable ResolvedPageState resolveQueuedPageState(ResourceLocation pageId) {
        PageSource activeSource = resolveActivePageSource(pageId, LangUtil.getCurrentLanguage());
        if (activeSource == null) {
            return ResolvedPageState.deleted();
        }

        try {
            ParsedGuidePage page = GuideLocalizedPageSourceResolver.parse(
                getSourcePackId(pageId),
                activeSource.requestedLanguage(),
                contentRootFolder,
                pageId,
                Files.readAllBytes(activeSource.path()),
                activeSource.localizedSourceOverride());
            return new ResolvedPageState(activeSource.requestedLanguage(), page, false);
        } catch (Exception e) {
            FMLLog.getLogger()
                .error(
                    "[GuideNH] [GuideSourceWatcher] Failed to resolve effective guidebook page {}",
                    activeSource.path(),
                    e);
            return null;
        }
    }

    @Nullable
    private PageSource resolveActivePageSource(ResourceLocation pageId, String currentLanguage) {
        var currentPath = getLocalizedSourcePath(pageId, currentLanguage);
        if (Files.isRegularFile(currentPath)) {
            return new PageSource(currentLanguage, currentPath, loadLocalizedSourceOverride(pageId, currentLanguage));
        }

        if (!Objects.equals(currentLanguage, defaultLanguage)) {
            var defaultPath = getLocalizedSourcePath(pageId, defaultLanguage);
            if (Files.isRegularFile(defaultPath)) {
                return new PageSource(
                    currentLanguage,
                    defaultPath,
                    loadLocalizedSourceOverride(pageId, currentLanguage));
            }
        }

        var neutralPath = getNeutralSourcePath(pageId);
        if (Files.isRegularFile(neutralPath)) {
            return new PageSource(currentLanguage, neutralPath, loadLocalizedSourceOverride(pageId, currentLanguage));
        }

        return null;
    }

    private @Nullable String loadLocalizedSourceOverride(ResourceLocation pageId, String language) {
        Path langFilePath = getLangFilePath(pageId, language);
        if (!Files.isRegularFile(langFilePath)) {
            return null;
        }

        try (InputStream input = Files.newInputStream(langFilePath)) {
            Map<String, String> entries = GuidePageLanguageIndex.readPageKeys(input);
            return entries.get(GuideLocalizedPageSourceResolver.buildLangKey(contentRootFolder, pageId));
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideSourceWatcher] Failed to read localized page lang file {}", langFilePath, e);
            return null;
        }
    }

    private Map<String, Map<String, String>> loadLocalizedSourceOverrides(String language,
        @Nullable String namespaceFilter) {
        Map<String, Map<String, String>> localizedSourcesByNamespace = new HashMap<>();
        for (String candidateNamespace : resolveNamespacesForLocalizedSources(namespaceFilter)) {
            Map<String, String> localizedSources = loadLocalizedSourceOverridesForNamespace(
                candidateNamespace,
                language);
            if (!localizedSources.isEmpty()) {
                localizedSourcesByNamespace.put(candidateNamespace, localizedSources);
            }
        }
        return localizedSourcesByNamespace;
    }

    private Set<String> resolveNamespacesForLocalizedSources(@Nullable String namespaceFilter) {
        if (namespaceFilter != null) {
            return Set.of(namespaceFilter);
        }

        if (sourceLayout == GuideDevelopmentSourceLayout.RESOURCE_PACK_ROOT) {
            Path assetsPath = sourceFolder.resolve("assets");
            if (!Files.isDirectory(assetsPath)) {
                return Set.of();
            }

            Set<String> namespaces = new HashSet<>();
            try (var children = Files.list(assetsPath)) {
                children.filter(Files::isDirectory)
                    .forEach(
                        child -> namespaces.add(
                            child.getFileName()
                                .toString()));
            } catch (IOException e) {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [GuideSourceWatcher] Failed to scan localized source namespaces in {}",
                        assetsPath,
                        e);
            }
            return namespaces;
        }

        return Set.of(namespace);
    }

    private int countSourceLanguages(Set<PageLangKey> pageKeys) {
        Set<String> languages = new HashSet<>();
        for (PageLangKey pageKey : pageKeys) {
            if (pageKey.sourceLang() != null) {
                languages.add(pageKey.sourceLang());
            }
        }
        return languages.size();
    }

    private Map<String, String> loadLocalizedSourceOverridesForNamespace(String sourceNamespace, String language) {
        Path langFilePath = getLangFilePath(sourceNamespace, language);
        if (!Files.isRegularFile(langFilePath)) {
            return Map.of();
        }

        try (InputStream input = Files.newInputStream(langFilePath)) {
            return GuidePageLanguageIndex.readPageKeys(input);
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideSourceWatcher] Failed to read localized page lang file {}", langFilePath, e);
            return Map.of();
        }
    }

    private @Nullable String findLocalizedSourceOverride(Map<String, Map<String, String>> localizedSourcesByNamespace,
        ResourceLocation pageId) {
        Map<String, String> localizedSources = localizedSourcesByNamespace.get(pageId.getResourceDomain());
        if (localizedSources == null || localizedSources.isEmpty()) {
            return null;
        }
        return localizedSources.get(GuideLocalizedPageSourceResolver.buildLangKey(contentRootFolder, pageId));
    }

    private Path getLangFilePath(ResourceLocation pageId, String language) {
        return getLangFilePath(pageId.getResourceDomain(), language);
    }

    private Path getLangFilePath(String sourceNamespace, String language) {
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        return switch (sourceLayout) {
            case CONTENT_ROOT -> sourceFolder.resolve("lang")
                .resolve(normalizedLanguage + ".lang");
            case RESOURCE_PACK_ROOT -> sourceFolder.resolve("assets")
                .resolve(sourceNamespace)
                .resolve("lang")
                .resolve(normalizedLanguage + ".lang");
            case ASSETS_ROOT -> sourceFolder.resolve(sourceNamespace)
                .resolve("lang")
                .resolve(normalizedLanguage + ".lang");
        };
    }

    private Path getLocalizedSourcePath(ResourceLocation pageId, String language) {
        return getSourcePath(pageId, "_" + LangUtil.normalizeLanguage(language) + "/" + pageId.getResourcePath());
    }

    private Path getNeutralSourcePath(ResourceLocation pageId) {
        return getSourcePath(pageId, pageId.getResourcePath());
    }

    private String getSourcePackId(ResourceLocation pageId) {
        return "development:" + pageId.getResourceDomain();
    }

    private Path getSourcePath(ResourceLocation pageId, String contentRelativePath) {
        return switch (sourceLayout) {
            case CONTENT_ROOT -> sourceFolder.resolve(contentRelativePath);
            case RESOURCE_PACK_ROOT -> sourceFolder.resolve("assets")
                .resolve(pageId.getResourceDomain())
                .resolve(contentRootFolder)
                .resolve(contentRelativePath);
            case ASSETS_ROOT -> sourceFolder.resolve(pageId.getResourceDomain())
                .resolve(contentRootFolder)
                .resolve(contentRelativePath);
        };
    }

    @Nullable
    private static String getGuideRelativePath(Path sourceFolder, GuideDevelopmentSourceLayout sourceLayout,
        String namespace, String contentRootFolder, Path path) {
        String relativePath = sourceFolder.relativize(path)
            .toString()
            .replace('\\', '/');
        return switch (sourceLayout) {
            case CONTENT_ROOT -> namespace + "/" + relativePath;
            case RESOURCE_PACK_ROOT -> stripResourcePackPrefix(relativePath, contentRootFolder);
            case ASSETS_ROOT -> stripAssetsPrefix(relativePath, contentRootFolder);
        };
    }

    @Nullable
    private static String stripResourcePackPrefix(String relativePath, String contentRootFolder) {
        String prefix = "assets/";
        if (!relativePath.startsWith(prefix)) {
            return null;
        }
        return stripAssetsPrefix(relativePath.substring(prefix.length()), contentRootFolder);
    }

    @Nullable
    private static String stripAssetsPrefix(String relativePath, String contentRootFolder) {
        int namespaceEnd = relativePath.indexOf('/');
        if (namespaceEnd <= 0) {
            return null;
        }
        String afterNamespace = relativePath.substring(namespaceEnd + 1);
        String guidePrefix = contentRootFolder + "/";
        if (!afterNamespace.startsWith(guidePrefix)) {
            return null;
        }
        return relativePath.substring(0, namespaceEnd + 1) + afterNamespace.substring(guidePrefix.length());
    }

    private GuideDevelopmentSourceLayout detectSourceLayout(Path folder) {
        return detectSourceLayout(folder, contentRootFolder);
    }

    private static GuideDevelopmentSourceLayout detectSourceLayout(Path folder, String contentRootFolder) {
        return GuideDevelopmentSourceLayout.detect(folder, contentRootFolder);
    }
}
