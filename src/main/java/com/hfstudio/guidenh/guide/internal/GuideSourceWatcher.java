package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bsideup.jabel.Desugar;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

class GuideSourceWatcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSourceWatcher.class);

    private final String defaultLanguage;

    /**
     * The {@link ResourceLocation} namespace to use for files in the watched folder.
     */
    private final String namespace;
    /**
     * The ID of the resource pack to use as the source pack.
     */
    private final String sourcePackId;

    private final Path sourceFolder;

    // Recursive directory watcher for the guidebook sources.
    @Nullable
    private final DirectoryWatcher sourceWatcher;

    // Queued changes that come in from a separate thread
    @Desugar
    private record PageLangKey(String sourceLang, ResourceLocation pageId) {}

    @Desugar
    private record PageSource(String language, Path path) {}

    private final Map<PageLangKey, ParsedGuidePage> changedPages = new HashMap<>();
    private final Set<PageLangKey> deletedPages = new HashSet<>();

    private final ExecutorService watchExecutor;

    public GuideSourceWatcher(String namespace, String defaultLanguage, Path sourceFolder) {
        this.namespace = namespace;
        this.sourcePackId = "development:" + namespace;
        this.defaultLanguage = LangUtil.normalizeLanguage(defaultLanguage);
        this.sourceFolder = sourceFolder;
        if (!Files.isDirectory(sourceFolder)) {
            throw new RuntimeException("Cannot find the specified folder with guidebook sources: " + sourceFolder);
        }
        LOG.info("Watching guidebook sources in {}", sourceFolder);

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
            LOG.error("Failed to watch for changes in the guidebook sources at {}", sourceFolder, e);
            watcher = null;
        }
        sourceWatcher = watcher;

        // Actually process changes in the client tick to prevent race conditions and other crashes
        if (sourceWatcher != null) {
            sourceWatcher.watchAsync(watchExecutor);
        }
    }

    public List<ParsedGuidePage> loadAll() {
        var stopwatch = Stopwatch.createStarted();
        var currentLanguage = LangUtil.getCurrentLanguage();
        var pageSources = new HashMap<PageLangKey, Path>();
        var pageIds = new LinkedHashSet<ResourceLocation>();

        try {
            Files.walkFileTree(sourceFolder, new FileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var pageKey = getPageLangKey(file);
                    if (pageKey != null) {
                        pageSources.put(pageKey, file);
                        pageIds.add(pageKey.pageId());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOG.error("Failed to list page {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null) {
                        LOG.error("Failed to list all pages in {}", dir, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error("Failed to list all pages in {}", sourceFolder, e);
        }

        LOG.info("Loading {} guidebook pages from {} localized variants", pageIds.size(), pageSources.size());
        var loadedPages = new ArrayList<ParsedGuidePage>(pageIds.size());
        for (var pageId : pageIds) {
            var pageSource = resolvePageSource(pageSources, pageId, currentLanguage);
            if (pageSource == null) {
                continue;
            }

            try (var in = Files.newInputStream(pageSource.path())) {
                loadedPages.add(PageCompiler.parse(sourcePackId, pageSource.language(), pageId, in));
            } catch (Exception e) {
                LOG.error("Failed to reload guidebook page {}", pageSource.path(), e);
            }
        }

        LOG.info("Loaded {} pages from {} in {}", loadedPages.size(), sourceFolder, stopwatch);
        return loadedPages;
    }

    public synchronized void clearChanges() {
        changedPages.clear();
        deletedPages.clear();
    }

    public synchronized List<GuidePageChange> takeChanges() {

        if (deletedPages.isEmpty() && changedPages.isEmpty()) {
            return Collections.emptyList();
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
        watchExecutor.shutdown();

        if (sourceWatcher != null) {
            try {
                sourceWatcher.close();
            } catch (IOException e) {
                LOG.error("Failed to close fileystem watcher for {}", sourceFolder);
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
            LOG.error("Failed watching for changes", e);
        }
    }

    // Only call while holding the lock!
    private synchronized void pageChanged(Path path) {
        var pageKey = getPageLangKey(path);
        if (pageKey == null) {
            return; // Probably not a page
        }

        var language = pageKey.sourceLang() != null ? pageKey.sourceLang() : defaultLanguage;

        // If it was previously deleted in the same change-set, undelete it
        deletedPages.remove(pageKey);

        try (var in = Files.newInputStream(path)) {
            var page = PageCompiler.parse(sourcePackId, language, pageKey.pageId(), in);
            changedPages.put(pageKey, page);
        } catch (Exception e) {
            LOG.error("Failed to reload guidebook page {}", path, e);
        }
    }

    // Only call while holding the lock!
    private synchronized void pageDeleted(Path path) {
        var pageKey = getPageLangKey(path);
        if (pageKey == null) {
            return; // Probably not a page
        }

        var fallbackPage = loadFallbackPage(pageKey, path);
        if (fallbackPage != null) {
            changedPages.put(pageKey, fallbackPage);
            deletedPages.remove(pageKey);
            return;
        }

        // If it was previously changed in the same change-set, remove the change
        changedPages.remove(pageKey);
        deletedPages.add(pageKey);
    }

    @Nullable
    private ResourceLocation getPageId(Path path) {
        var relativePath = sourceFolder.relativize(path);
        var relativePathStr = relativePath.toString()
            .replace('\\', '/');
        if (!relativePathStr.endsWith(".md")) {
            return null;
        }
        if (!relativePathStr.matches("[a-z0-9_./-]+")) {
            return null;
        }
        return new ResourceLocation(namespace, relativePathStr);
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
    private PageSource resolvePageSource(Map<PageLangKey, Path> pageSources, ResourceLocation pageId,
        String currentLanguage) {
        var currentPath = pageSources.get(new PageLangKey(currentLanguage, pageId));
        if (currentPath != null) {
            return new PageSource(currentLanguage, currentPath);
        }

        if (!Objects.equals(currentLanguage, defaultLanguage)) {
            var defaultPath = pageSources.get(new PageLangKey(defaultLanguage, pageId));
            if (defaultPath != null) {
                return new PageSource(defaultLanguage, defaultPath);
            }
        }

        var neutralPath = pageSources.get(new PageLangKey(null, pageId));
        if (neutralPath != null) {
            return new PageSource(defaultLanguage, neutralPath);
        }

        return null;
    }

    @Nullable
    private ParsedGuidePage loadFallbackPage(PageLangKey pageKey, Path removedPath) {
        var fallbackSource = resolveFallbackSource(pageKey, removedPath);
        if (fallbackSource == null) {
            return null;
        }

        try (var in = Files.newInputStream(fallbackSource.path())) {
            return PageCompiler.parse(sourcePackId, fallbackSource.language(), pageKey.pageId(), in);
        } catch (Exception e) {
            LOG.error("Failed to load fallback guidebook page {}", fallbackSource.path(), e);
            return null;
        }
    }

    @Nullable
    private PageSource resolveFallbackSource(PageLangKey pageKey, Path removedPath) {
        var defaultPath = getLocalizedSourcePath(pageKey.pageId(), defaultLanguage);
        if (!defaultPath.equals(removedPath) && Files.isRegularFile(defaultPath)) {
            return new PageSource(defaultLanguage, defaultPath);
        }

        var neutralPath = getNeutralSourcePath(pageKey.pageId());
        if (!neutralPath.equals(removedPath) && Files.isRegularFile(neutralPath)) {
            return new PageSource(defaultLanguage, neutralPath);
        }

        return null;
    }

    private Path getLocalizedSourcePath(ResourceLocation pageId, String language) {
        return sourceFolder.resolve("_" + LangUtil.normalizeLanguage(language) + "/" + pageId.getResourcePath());
    }

    private Path getNeutralSourcePath(ResourceLocation pageId) {
        return sourceFolder.resolve(pageId.getResourcePath());
    }
}
