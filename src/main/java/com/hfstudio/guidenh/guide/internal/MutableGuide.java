package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuideItemSettings;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.home.GuideScreenHomeHistory;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiGuideAggregator;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContextProvider;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageIds;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialDataIndex;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialDataIndexer;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageRefreshController;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSyntheticPageFactory;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSyntheticPageFactory.SyntheticSourceSnapshot;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;

import cpw.mods.fml.common.FMLLog;

/**
 * Encapsulates a Guide, which consists of a collection of Markdown pages and associated content, loaded from a
 * guide-specific subdirectory of resource packs.
 */
public class MutableGuide
    implements Guide, GuideDevWatcherPump.TickableGuide, MediaWikiListContextProvider, AutoCloseable {

    public static final String ACTIVE_CLIENT_WORLD_REQUIRED_MESSAGE = "active client world";
    private static final int MAX_STRONG_RUNTIME_PAGES = 48;
    private static final long DEVELOPMENT_VALIDATION_INTERVAL_TICKS = 10L;
    private static final long NORMAL_HEAVY_PAGE_WARMUP_DELAY_TICKS = 8L;

    private final ResourceLocation id;
    private final String defaultNamespace;
    private final String folder;
    private final String defaultLanguage;
    private final Map<ResourceLocation, ParsedGuidePage> developmentPages = new HashMap<>();
    private final Map<ResourceLocation, GuidePageFailure> pageFailures = new HashMap<>();
    private final Map<Class<?>, PageIndex> indices;
    private NavigationTree navigationTree = new NavigationTree();
    /**
     * These are only loaded for the current language and optionally supplemented by language-neutral pages.
     */
    private Map<ResourceLocation, ParsedGuidePage> pages;
    private Map<ResourceLocation, ParsedGuidePage> syntheticPages = Map.of();
    private final Map<ResourceLocation, SyntheticSourceSnapshot> syntheticSourceCache = new HashMap<>();
    @Nullable
    private MediaWikiListContext mediaWikiListContext;
    @Nullable
    private volatile MediaWikiListContext fallbackMediaWikiListContext;
    @Nullable
    private MediaWikiSpecialDataIndex mediaWikiSpecialDataIndex;
    private volatile long mediaWikiListContextRevision = Long.MIN_VALUE;
    private volatile long mediaWikiSpecialDataIndexRevision = Long.MIN_VALUE;
    private volatile long fallbackMediaWikiListContextRevision = Long.MIN_VALUE;
    private volatile long requestedMediaWikiWarmupRevision = Long.MIN_VALUE;
    private final MediaWikiSpecialPageRefreshController mediaWikiRefreshController = new MediaWikiSpecialPageRefreshController();
    private final Map<ParsedGuidePage, GuidePage> compiledPagesWeak = Collections.synchronizedMap(new WeakHashMap<>());
    private final LinkedHashMap<ResourceLocation, GuidePage> compiledPagesStrong = new LinkedHashMap<>(
        64,
        0.75f,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<ResourceLocation, GuidePage> eldest) {
            return size() > MAX_STRONG_RUNTIME_PAGES;
        }
    };
    private final ExtensionCollection extensions;
    private final boolean availableToOpenHotkey;
    private final GuideItemSettings itemSettings;
    private final GuideDevelopmentSourceLayout developmentSourceLayout;

    @Nullable
    private final Path developmentSourceFolder;
    @Nullable
    private final String developmentSourceNamespace;

    @Nullable
    private GuideSourceWatcher watcher;
    private long nextValidationTick;
    private long lastSchedulerTick;

    public MutableGuide(ResourceLocation id, String defaultNamespace, String folder, String defaultLanguage,
        @Nullable Path developmentSourceFolder, @Nullable String developmentSourceNamespace,
        Map<Class<?>, PageIndex> indices, ExtensionCollection extensions, boolean availableToOpenHotkey,
        GuideItemSettings itemSettings) {
        this.id = id;
        this.defaultNamespace = defaultNamespace;
        this.folder = folder;
        this.defaultLanguage = defaultLanguage;
        this.developmentSourceFolder = developmentSourceFolder;
        this.developmentSourceNamespace = developmentSourceNamespace;
        this.developmentSourceLayout = detectDevelopmentSourceLayout(developmentSourceFolder);
        this.indices = indices;
        this.extensions = extensions;
        this.availableToOpenHotkey = availableToOpenHotkey;
        this.itemSettings = itemSettings;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    /**
     * The resource pack subfolder that is the content root for this guide.
     */
    @Override
    public String getContentRootFolder() {
        return folder;
    }

    public static ResourceLocation resolveTranslatedAssetId(ResourceLocation assetId, String language) {
        if (assetId.getResourcePath()
            .startsWith("assets/")) {
            return assetId;
        }
        return LangUtil.getTranslatedAsset(assetId, language);
    }

    public static ResourceLocation resolveGuideAssetId(ResourceLocation assetId, String folder) {
        String path = assetId.getResourcePath();
        if (path.startsWith("assets/") || path.startsWith(folder + "/")) {
            return assetId;
        }
        return new ResourceLocation(assetId.getResourceDomain(), folder + "/" + path);
    }

    @Override
    public <T extends PageIndex> T getIndex(Class<T> indexClass) {
        var index = indices.get(indexClass);
        if (index == null) {
            throw new IllegalArgumentException("No index of type " + indexClass + " is registered with this guide.");
        }
        return indexClass.cast(index);
    }

    @Override
    @Nullable
    public ParsedGuidePage getParsedPage(ResourceLocation id) {
        if (pages == null) {
            FMLLog.getLogger()
                .warn("[GuideNH] [MutableGuide] Can't get page {}. Pages not loaded yet.", id);
            return null;
        }

        ParsedGuidePage developmentPage = developmentPages.get(id);
        if (developmentPage != null) {
            return developmentPage;
        }

        ParsedGuidePage syntheticPage = syntheticPages.get(id);
        if (syntheticPage != null) {
            return syntheticPage;
        }

        return pages.get(id);
    }

    @Override
    @Nullable
    public GuidePage getPage(ResourceLocation id) {
        ParsedGuidePage parsedPage = getParsedPage(id);
        if (parsedPage == null) {
            return null;
        }

        GuidePage compiledPage;
        try {
            synchronized (compiledPagesWeak) {
                compiledPage = compiledPagesStrong.get(id);
                if (compiledPage == null) {
                    compiledPage = compiledPagesWeak.get(parsedPage);
                }
                if (compiledPage == null) {
                    compiledPage = PageCompiler.compile(this, extensions, parsedPage);
                }
                compiledPagesWeak.put(parsedPage, compiledPage);
                compiledPagesStrong.put(id, compiledPage);
            }
            clearCompileFailure(id);
        } catch (Throwable t) {
            recordCompileFailure(id, buildCompileFailureText(id, t));
            FMLLog.getLogger()
                .error("[GuideNH] [MutableGuide] Failed to compile guide page {}", id, t);
            compiledPage = buildFailurePage(parsedPage, pageFailures.get(id));
        }
        compiledPage.prepareForDisplay();
        return compiledPage;
    }

    @Override
    public Collection<ParsedGuidePage> getPages() {
        if (pages == null) {
            throw new IllegalStateException("Pages are not loaded yet.");
        }

        if (developmentPages.isEmpty() && syntheticPages.isEmpty()) {
            return pages.values();
        }

        var allPages = new LinkedHashMap<>(pages);
        allPages.putAll(developmentPages);
        allPages.putAll(syntheticPages);
        return allPages.values();
    }

    @Override
    public byte[] loadAsset(ResourceLocation id) {
        var language = LangUtil.getCurrentLanguage();
        var result = loadAssetInternal(LangUtil.getTranslatedAsset(id, language));
        if (result != null) return result;

        if (!Objects.equals(language, defaultLanguage)) {
            result = loadAssetInternal(LangUtil.getTranslatedAsset(id, defaultLanguage));
            if (result != null) return result;
        }

        return loadAssetInternal(id);
    }

    private byte @Nullable [] loadAssetInternal(ResourceLocation id) {
        // Also load assets from the development sources folder.
        if (canLoadDevelopmentSource(id)) {
            var path = resolveDevelopmentSourcePath(id);
            try {
                return Files.readAllBytes(path);
            } catch (NoSuchFileException ignored) {} catch (IOException e) {
                FMLLog.getLogger()
                    .error("[GuideNH] [MutableGuide] Failed to open guidebook asset {}", path);
                return null;
            }
        }

        // Transform id such that the path is prefixed with the source folder for the guidebook assets
        id = new ResourceLocation(id.getResourceDomain(), folder + "/" + id.getResourcePath());

        var bytes = GuideResourceAccess.readBytes(
            Minecraft.getMinecraft()
                .getResourceManager(),
            id);
        if (bytes != null) {
            return bytes;
        }

        FMLLog.getLogger()
            .error("[GuideNH] [MutableGuide] Failed to open guidebook asset {}", id);
        return null;
    }

    @Override
    public NavigationTree getNavigationTree() {
        return navigationTree;
    }

    @Override
    public boolean pageExists(ResourceLocation pageId) {
        return developmentPages.containsKey(pageId) || syntheticPages.containsKey(pageId)
            || pages != null && pages.containsKey(pageId);
    }

    @Override
    public boolean isPageFailed(ResourceLocation pageId) {
        return pageFailures.containsKey(pageId);
    }

    public Map<ResourceLocation, GuidePageFailureView> getPageFailureViews() {
        LinkedHashMap<ResourceLocation, GuidePageFailureView> snapshot = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, GuidePageFailure> entry : pageFailures.entrySet()) {
            GuidePageFailure failure = entry.getValue();
            if (failure == null) {
                continue;
            }
            snapshot.put(
                entry.getKey(),
                new GuidePageFailureView(failure.headingText, failure.errorText, failure.parseFailure));
        }
        return Map.copyOf(snapshot);
    }

    /**
     * Returns the on-disk path for a given guidebook resource (i.e. page, asset) if development mode is enabled and the
     * resource exists in the development source folder.
     *
     * @return null if development mode is not enabled or the resource doesn't exist in the development sources.
     */
    @Nullable
    public Path getDevelopmentSourcePath(ResourceLocation id) {
        if (canLoadDevelopmentSource(id)) {
            var path = resolveDevelopmentSourcePath(id);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    public Path getDevelopmentSourceFolder() {
        return developmentSourceFolder;
    }

    @Override
    public ExtensionCollection getExtensions() {
        return extensions;
    }

    /**
     * @return True if this guide should be considered for use in the global open guide hotkey.
     */
    public boolean isAvailableToOpenHotkey() {
        return availableToOpenHotkey;
    }

    public void watchDevelopmentSources() {
        if (watcher != null) {
            return;
        }

        watcher = new GuideSourceWatcher(developmentSourceNamespace, folder, defaultLanguage, developmentSourceFolder);
        Runtime.getRuntime()
            .addShutdownHook(new Thread(watcher::close));
    }

    @Override
    public boolean hasDevelopmentSources() {
        return watcher != null;
    }

    @Override
    public synchronized void close() {
        int developmentPageCount = developmentPages.size();
        int syntheticPageCount = syntheticPages.size();
        int failureCount = pageFailures.size();
        int weakCompiledCount;
        int strongCompiledCount;
        synchronized (compiledPagesWeak) {
            weakCompiledCount = compiledPagesWeak.size();
            strongCompiledCount = compiledPagesStrong.size();
        }
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
        mediaWikiRefreshController.close();
        developmentPages.clear();
        pageFailures.clear();
        syntheticPages = Map.of();
        syntheticSourceCache.clear();
        mediaWikiListContext = null;
        fallbackMediaWikiListContext = null;
        mediaWikiSpecialDataIndex = null;
        fallbackMediaWikiListContextRevision = Long.MIN_VALUE;
        requestedMediaWikiWarmupRevision = Long.MIN_VALUE;
        synchronized (compiledPagesWeak) {
            compiledPagesWeak.clear();
            compiledPagesStrong.clear();
        }
        warmupPageQueue.clear();
        queuedWarmupPages.clear();
        prioritizedWarmupPages.clear();
        scheduledWarmupPages.clear();
        validationPageQueue.clear();
        queuedValidationPages.clear();
        scheduledValidationPages.clear();
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [MutableGuide] Closed guide {} and cleared caches developmentPages={}, syntheticPages={}, failures={}, compiledWeak={}, compiledStrong={}",
                    id,
                    developmentPageCount,
                    syntheticPageCount,
                    failureCount,
                    weakCompiledCount,
                    strongCompiledCount);
        }
    }

    private final Deque<ResourceLocation> warmupPageQueue = new ArrayDeque<>();
    private final HashSet<ResourceLocation> queuedWarmupPages = new HashSet<>();
    private final HashSet<ResourceLocation> prioritizedWarmupPages = new HashSet<>();
    private final HashSet<ResourceLocation> scheduledWarmupPages = new HashSet<>();
    private final Deque<ResourceLocation> validationPageQueue = new ArrayDeque<>();
    private final HashSet<ResourceLocation> queuedValidationPages = new HashSet<>();
    private final HashSet<ResourceLocation> scheduledValidationPages = new HashSet<>();

    public void resetWarmup() {
        rebuildWarmupQueue();
    }

    public void tick() {
        if (pages == null || watcher == null) {
            return; // Do nothing while pages haven't been loaded yet
        }

        var changes = watcher.takeChanges();
        if (!changes.isEmpty()) {
            applyChanges(changes);
        }
    }

    @Override
    public void tickDevelopmentSources() {
        tick();
    }

    private void applyChanges(List<GuidePageChange> changes) {
        invalidateMediaWikiDerivedCaches();
        var initialPages = new HashMap<>(developmentPages);
        var deduplicatedChanges = new ArrayList<GuidePageChange>(changes.size());
        var seenPageIds = new LinkedHashSet<ResourceLocation>();
        for (int i = changes.size() - 1; i >= 0; i--) {
            var change = changes.get(i);
            if (change == null || !seenPageIds.add(change.pageId())) {
                continue;
            }
            deduplicatedChanges.addFirst(change);
        }

        // Enrich each change with the previous page data while we process them
        for (int i = 0; i < deduplicatedChanges.size(); i++) {
            var change = deduplicatedChanges.get(i);
            var pageId = change.pageId();

            var newPage = change.newPage();
            if (newPage != null) {
                developmentPages.put(pageId, newPage);
            } else {
                developmentPages.remove(pageId);
            }
            removeCompiledPage(pageId);
            prioritizeWarmupPage(pageId);
            queueValidationPage(pageId);

            deduplicatedChanges
                .set(i, new GuidePageChange(change.language(), pageId, initialPages.get(pageId), newPage));
        }

        // Allow indices to rebuild
        var allPages = new ArrayList<>(getSourceParsedPages().values());
        allPages.removeIf(
            p -> !NavigationTree.areModRequirementsMet(
                p.getFrontmatter()
                    .navigationEntry()));
        for (var index : indices.values()) {
            if (index.supportsUpdate()) {
                index.update(allPages, deduplicatedChanges);
            } else {
                index.rebuild(allPages);
            }
        }

        rebuildSyntheticPages();
        // Rebuild navigation
        this.navigationTree = buildNavigation();
        GuideRegistry.invalidateMergedNavigationTree();
        refreshPageFailures();
        requestValidationRefresh(0L);

        // Reload the current page if it has been changed
        var guideScreen = GuideScreen.current();
        if (guideScreen != null) {
            var currentId = guideScreen.getCurrentPageId();
            if (currentId != null) {
                boolean shouldReloadCurrentPage = MediaWikiPageIds.isSyntheticPage(currentId);
                if (!shouldReloadCurrentPage) {
                    for (var change : deduplicatedChanges) {
                        if (currentId.equals(change.pageId())) {
                            shouldReloadCurrentPage = true;
                            break;
                        }
                    }
                }
                if (shouldReloadCurrentPage) {
                    guideScreen.reloadPage();
                }
            }
        }
    }

    private NavigationTree buildNavigation() {
        return NavigationTree.build(this, getSourceParsedPages().values());
    }

    public void validateAll() {
        // Iterate and compile all pages to warn about errors on startup
        for (var entry : developmentPages.entrySet()) {
            if (ModConfig.debug.enableDebugMode) {
                FMLLog.getLogger()
                    .info("[GuideNH] [MutableGuide] Compiling {}", entry.getKey());
            }
            getPage(entry.getKey());
        }
    }

    public void rebuildIndices() {
        var allPages = new ArrayList<>(getSourceParsedPages().values());
        allPages.removeIf(
            p -> !NavigationTree.areModRequirementsMet(
                p.getFrontmatter()
                    .navigationEntry()));
        for (var index : indices.values()) {
            index.rebuild(allPages);
        }
    }

    public void setPages(Map<ResourceLocation, ParsedGuidePage> pages) {
        setPages(pages, true);
    }

    public void setPages(Map<ResourceLocation, ParsedGuidePage> pages, boolean invalidateMergedNavigationTree) {
        this.pages = Map.copyOf(new HashMap<>(pages));
        this.syntheticPages = Map.of();
        invalidateMediaWikiDerivedCaches();
        synchronized (compiledPagesWeak) {
            compiledPagesWeak.clear();
            compiledPagesStrong.clear();
        }
        scheduledWarmupPages.clear();
        scheduledValidationPages.clear();
        validationPageQueue.clear();
        queuedValidationPages.clear();
        nextValidationTick = 0L;

        if (watcher != null) {
            watcher.clearChanges(); // Since we'll load them all now, ignore all changes up to now
            developmentPages.clear();
            for (var page : watcher.loadAll()) {
                developmentPages.put(page.getId(), page);
            }
        }

        rebuildIndices();
        rebuildSyntheticPages();
        navigationTree = buildNavigation();
        if (invalidateMergedNavigationTree) {
            GuideRegistry.invalidateMergedNavigationTree();
        }
        refreshPageFailures();
        rebuildWarmupQueue();
        // Do not eagerly compile pages here. Some packs register or rewrite recipes
        // during FMLLoadComplete, after the initial resource reload has already parsed the guide.
        // Deferring compilation until first display avoids caching stale "missing recipe" error blocks.

        var guideScreen = GuideScreen.current();
        if (guideScreen != null && guideScreen.isShowingGuide(getId())) {
            guideScreen.reloadPage();
        }
    }

    public void applyEditorPage(ParsedGuidePage parsedPage) {
        stageEditorPage(parsedPage);
        rebuildEditorNavigationStateWithoutValidation();
    }

    public void stageEditorPage(ParsedGuidePage parsedPage) {
        if (parsedPage == null) {
            return;
        }
        ResourceLocation pageId = parsedPage.getId();
        developmentPages.put(pageId, parsedPage);
        invalidateMediaWikiDerivedCaches();
        removeCompiledPage(pageId);
        prioritizeWarmupPage(pageId);
        queueValidationPage(pageId);
        requestValidationRefresh(0L);
        if (parsedPage.hasParseFailure()) {
            recordParseFailure(parsedPage);
        } else {
            clearCompileFailure(pageId);
            clearParseFailure(pageId);
        }
    }

    private void removeCompiledPage(ResourceLocation pageId) {
        synchronized (compiledPagesWeak) {
            compiledPagesStrong.remove(pageId);
            compiledPagesWeak.keySet()
                .removeIf(page -> page != null && pageId.equals(page.getId()));
        }
        scheduledWarmupPages.remove(pageId);
        prioritizedWarmupPages.remove(pageId);
        queuedWarmupPages.remove(pageId);
        warmupPageQueue.remove(pageId);
        scheduledValidationPages.remove(pageId);
        queuedValidationPages.remove(pageId);
        validationPageQueue.remove(pageId);
    }

    private void rebuildWarmupQueue() {
        warmupPageQueue.clear();
        queuedWarmupPages.clear();
        prioritizedWarmupPages.clear();
        queueWarmupPage(resolveRememberedWarmupPageId(), true);
        queueWarmupBookmarkedPages();
        queueWarmupHistoryPages();
        ResourceLocation firstNavigationPage = resolveWarmupPageId();
        if (firstNavigationPage != null) {
            queueWarmupPage(firstNavigationPage, true);
        }
        if (pages != null) {
            for (ParsedGuidePage parsedPage : pages.values()) {
                queueWarmupPage(parsedPage.getId());
            }
        }
        for (ParsedGuidePage parsedPage : developmentPages.values()) {
            queueWarmupPage(parsedPage.getId());
        }
    }

    private void prioritizeWarmupPage(ResourceLocation pageId) {
        queueWarmupPage(pageId, true);
    }

    @Nullable
    private ResourceLocation pollWarmupCandidate() {
        while (!warmupPageQueue.isEmpty()) {
            ResourceLocation pageId = warmupPageQueue.poll();
            if (pageId == null) {
                return null;
            }
            queuedWarmupPages.remove(pageId);
            if (!compiledPageExists(pageId) && getParsedPage(pageId) != null) {
                return pageId;
            }
        }
        return null;
    }

    private void queueWarmupPage(@Nullable ResourceLocation pageId) {
        queueWarmupPage(pageId, false);
    }

    private void queueWarmupPage(@Nullable ResourceLocation pageId, boolean highPriority) {
        if (pageId == null || compiledPageExists(pageId)) {
            return;
        }
        if (highPriority) {
            prioritizedWarmupPages.add(pageId);
        }
        if (!queuedWarmupPages.add(pageId)) {
            if (highPriority) {
                warmupPageQueue.remove(pageId);
                warmupPageQueue.offerFirst(pageId);
            }
            return;
        }
        if (highPriority) {
            warmupPageQueue.offerFirst(pageId);
        } else {
            warmupPageQueue.offerLast(pageId);
        }
    }

    @Nullable
    private ResourceLocation resolveRememberedWarmupPageId() {
        GuideScreenViewState state = GuideScreenMemory.recallLastContentState();
        if (state == null || state.route() == null
            || state.route()
                .guideId() == null
            || !id.equals(
                state.route()
                    .guideId())) {
            return null;
        }
        PageAnchor anchor = state.route()
            .anchor();
        return anchor != null ? anchor.pageId() : null;
    }

    private void queueWarmupBookmarkedPages() {
        for (ResourceLocation pageId : GuideBookmarkState.getSharedInstance()
            .getBookmarksView()) {
            if (pageExists(pageId)) {
                queueWarmupPage(pageId, true);
            }
        }
    }

    private void queueWarmupHistoryPages() {
        for (GuideScreenHomeHistory.Entry entry : GuideScreenHomeHistory.shared()
            .snapshot()) {
            if (id.equals(entry.guideId()) && pageExists(entry.pageId())) {
                queueWarmupPage(entry.pageId(), true);
            }
        }
    }

    private boolean compiledPageExists(ResourceLocation pageId) {
        ParsedGuidePage parsedPage = getParsedPage(pageId);
        if (parsedPage == null) {
            return false;
        }
        synchronized (compiledPagesWeak) {
            return compiledPagesStrong.containsKey(pageId) || compiledPagesWeak.containsKey(parsedPage);
        }
    }

    public void populateWarmupScheduler(GuideWarmupScheduler scheduler, long nowTick) {
        if (pages == null) {
            return;
        }
        lastSchedulerTick = nowTick;
        GuideScreen guideScreen = GuideScreen.current();
        if (guideScreen != null && guideScreen.isShowingGuide(id)) {
            prioritizeWarmupPage(guideScreen.getCurrentPageId());
        }
        if (Minecraft.getMinecraft()
            .getNetHandler() != null) {
            flushWarmupItems(scheduler);
        }
        if (shouldUseDevelopmentValidation()) {
            flushValidationItems(scheduler);
        }
    }

    public boolean processWarmupWorkItem(GuideWarmupWorkItem item, long nowTick) {
        return switch (item.kind()) {
            case HIGH_PRIORITY_PAGE, NORMAL_PAGE -> processPageWarmup(item, nowTick);
            case DEV_VALIDATION -> processDevelopmentValidation(item, nowTick);
        };
    }

    private void flushWarmupItems(GuideWarmupScheduler scheduler) {
        ResourceLocation pageId;
        while ((pageId = pollWarmupCandidate()) != null) {
            if (!scheduledWarmupPages.add(pageId)) {
                continue;
            }
            GuideWarmupWorkItem.Kind kind = prioritizedWarmupPages.remove(pageId)
                ? GuideWarmupWorkItem.Kind.HIGH_PRIORITY_PAGE
                : GuideWarmupWorkItem.Kind.NORMAL_PAGE;
            scheduler.enqueue(new GuideWarmupWorkItem(id, pageId, kind));
        }
    }

    private void flushValidationItems(GuideWarmupScheduler scheduler) {
        while (!validationPageQueue.isEmpty()) {
            ResourceLocation pageId = validationPageQueue.pollFirst();
            if (pageId == null) {
                return;
            }
            queuedValidationPages.remove(pageId);
            if (scheduledValidationPages.add(pageId)) {
                GuideWarmupWorkItem item = new GuideWarmupWorkItem(id, pageId, GuideWarmupWorkItem.Kind.DEV_VALIDATION);
                item.setNextEligibleTick(nextValidationTick);
                scheduler.enqueue(item);
            }
        }
    }

    public void rebuildEditorNavigationState() {
        rebuildIndices();
        invalidateMediaWikiDerivedCaches();
        rebuildSyntheticPages();
        navigationTree = buildNavigation();
        GuideRegistry.invalidateMergedNavigationTree();
        refreshPageFailures();
    }

    public void rebuildEditorNavigationStateWithoutValidation() {
        rebuildIndices();
        invalidateMediaWikiDerivedCaches();
        rebuildSyntheticPages();
        navigationTree = buildNavigation();
        GuideRegistry.invalidateMergedNavigationTree();
    }

    public GuideItemSettings getItemSettings() {
        return itemSettings;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    @Nullable
    private ResourceLocation resolveWarmupPageId() {
        if (navigationTree != null) {
            ResourceLocation pageId = findFirstNavigationPageId(navigationTree.getRootNodes());
            if (pageId != null) {
                return pageId;
            }
        }
        return pages != null && !pages.isEmpty() ? pages.keySet()
            .iterator()
            .next() : null;
    }

    @Nullable
    private ResourceLocation findFirstNavigationPageId(List<NavigationNode> nodes) {
        for (var node : nodes) {
            if (node.hasPage() && node.pageId() != null) {
                return node.pageId();
            }
            ResourceLocation childPageId = findFirstNavigationPageId(node.children());
            if (childPageId != null) {
                return childPageId;
            }
        }
        return null;
    }

    private boolean canLoadDevelopmentSource(ResourceLocation id) {
        if (developmentSourceFolder == null) {
            return false;
        }
        return developmentSourceLayout != GuideDevelopmentSourceLayout.CONTENT_ROOT || id.getResourceDomain()
            .equals(developmentSourceNamespace);
    }

    private Path resolveDevelopmentSourcePath(ResourceLocation id) {
        return switch (developmentSourceLayout) {
            case CONTENT_ROOT -> developmentSourceFolder.resolve(id.getResourcePath());
            case RESOURCE_PACK_ROOT -> developmentSourceFolder.resolve("assets")
                .resolve(id.getResourceDomain())
                .resolve(folder)
                .resolve(id.getResourcePath());
            case ASSETS_ROOT -> developmentSourceFolder.resolve(id.getResourceDomain())
                .resolve(folder)
                .resolve(id.getResourcePath());
        };
    }

    private GuideDevelopmentSourceLayout detectDevelopmentSourceLayout(@Nullable Path sourceFolder) {
        if (sourceFolder == null) {
            return GuideDevelopmentSourceLayout.CONTENT_ROOT;
        }
        return GuideDevelopmentSourceLayout.detect(sourceFolder, folder);
    }

    private boolean processPageWarmup(GuideWarmupWorkItem item, long nowTick) {
        ResourceLocation pageId = item.pageId();
        ParsedGuidePage parsedPage = getParsedPage(pageId);
        if (parsedPage == null) {
            scheduledWarmupPages.remove(pageId);
            return true;
        }

        try {
            if (item.stepIndex() == 0 && SceneTagCompiler.likelyHasHeavySceneWork(parsedPage)) {
                item.advanceStep();
                item.setNextEligibleTick(
                    nowTick + (item.kind() == GuideWarmupWorkItem.Kind.HIGH_PRIORITY_PAGE ? 1L
                        : NORMAL_HEAVY_PAGE_WARMUP_DELAY_TICKS));
                return false;
            }
            if (!warmPage(pageId, parsedPage)) {
                item.setNextEligibleTick(nowTick + 1L);
                return false;
            }
        } catch (Throwable t) {
            scheduledWarmupPages.remove(pageId);
            if (!isDeferrableWarmPageFailure(t)) {
                FMLLog.getLogger()
                    .error("[GuideNH] [MutableGuide] Failed to pre-warm guide page {}", pageId, t);
            }
            return true;
        }

        scheduledWarmupPages.remove(pageId);
        return true;
    }

    private boolean warmPage(ResourceLocation pageId, ParsedGuidePage parsedPage) {
        synchronized (compiledPagesWeak) {
            GuidePage compiledPage = compiledPagesStrong.get(pageId);
            if (compiledPage == null) {
                compiledPage = compiledPagesWeak.get(parsedPage);
            }
            if (compiledPage != null) {
                compiledPagesStrong.put(pageId, compiledPage);
                return true;
            }
            try {
                compiledPage = PageCompiler.compile(this, extensions, parsedPage);
            } catch (RuntimeException e) {
                if (isDeferrableWarmPageFailure(e)) {
                    FMLLog.getLogger()
                        .debug(
                            "[GuideNH] [MutableGuide] Deferring warm compilation for page {} until an active client world is available",
                            pageId);
                    return false;
                }
                throw e;
            }
            compiledPagesWeak.put(parsedPage, compiledPage);
            compiledPagesStrong.put(pageId, compiledPage);
            clearCompileFailure(pageId);
            return true;
        }
    }

    private boolean processDevelopmentValidation(GuideWarmupWorkItem item, long nowTick) {
        ResourceLocation pageId = item.pageId();
        if (!shouldUseDevelopmentValidation()) {
            scheduledValidationPages.remove(pageId);
            return true;
        }
        if (nowTick < nextValidationTick) {
            item.setNextEligibleTick(nextValidationTick);
            return false;
        }

        try {
            ParsedGuidePage parsedPage = getParsedPage(pageId);
            if (parsedPage == null) {
                return true;
            }
            if (parsedPage.hasParseFailure()) {
                recordParseFailure(parsedPage);
                return true;
            }
            PageCompiler.compile(this, extensions, parsedPage);
            clearCompileFailure(pageId);
        } catch (Throwable t) {
            if (!isDeferrableWarmPageFailure(t)) {
                recordCompileFailure(pageId, buildCompileFailureText(pageId, t));
                FMLLog.getLogger()
                    .error("[GuideNH] [MutableGuide] Failed to validate guide page {}", pageId, t);
            }
        } finally {
            scheduledValidationPages.remove(pageId);
        }
        return true;
    }

    private void queueValidationPage(@Nullable ResourceLocation pageId) {
        if (pageId == null || !queuedValidationPages.add(pageId) || scheduledValidationPages.contains(pageId)) {
            return;
        }
        validationPageQueue.offerLast(pageId);
    }

    private void queueValidationForAllPages() {
        for (ParsedGuidePage parsedPage : getAllParsedPages().values()) {
            if (!parsedPage.hasParseFailure()) {
                queueValidationPage(parsedPage.getId());
            }
        }
    }

    private void requestValidationRefresh(long nowTick) {
        long referenceTick = nowTick > 0L ? nowTick : lastSchedulerTick;
        long requestedTick = referenceTick + DEVELOPMENT_VALIDATION_INTERVAL_TICKS;
        if (nextValidationTick == 0L) {
            nextValidationTick = requestedTick;
        } else {
            nextValidationTick = Math.min(nextValidationTick, requestedTick);
        }
    }

    private boolean shouldUseDevelopmentValidation() {
        return watcher != null || isGuideEditorRuntimeActive();
    }

    private boolean isGuideEditorRuntimeActive() {
        GuideScreen guideScreen = GuideScreen.current();
        return guideScreen != null && guideScreen.isShowingGuide(id) && guideScreen.isEditorActive();
    }

    static boolean isDeferrableWarmPageFailure(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if ((current instanceof IllegalStateException || current instanceof IllegalArgumentException)
                && current.getMessage() != null
                && current.getMessage()
                    .contains(ACTIVE_CLIENT_WORLD_REQUIRED_MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    private void refreshPageFailures() {
        Map<ResourceLocation, ParsedGuidePage> allParsedPages = getAllParsedPages();
        pageFailures.keySet()
            .removeIf(pageId -> !allParsedPages.containsKey(pageId));
        for (ParsedGuidePage parsedPage : allParsedPages.values()) {
            if (parsedPage.hasParseFailure()) {
                recordParseFailure(parsedPage);
            } else {
                clearCompileFailure(parsedPage.getId());
                clearParseFailure(parsedPage.getId());
            }
        }
        if (shouldUseDevelopmentValidation()) {
            requestValidationRefresh(0L);
            queueValidationForAllPages();
        }
    }

    private Map<ResourceLocation, ParsedGuidePage> getAllParsedPages() {
        var allPages = new LinkedHashMap<ResourceLocation, ParsedGuidePage>();
        if (pages != null) {
            allPages.putAll(pages);
        }
        allPages.putAll(developmentPages);
        allPages.putAll(syntheticPages);
        return allPages;
    }

    private Map<ResourceLocation, ParsedGuidePage> getSourceParsedPages() {
        var allPages = new LinkedHashMap<ResourceLocation, ParsedGuidePage>();
        if (pages != null) {
            allPages.putAll(pages);
        }
        allPages.putAll(developmentPages);
        allPages.keySet()
            .removeIf(MediaWikiPageIds::isSyntheticPage);
        return allPages;
    }

    private void rebuildSyntheticPages() {
        if (pages == null) {
            syntheticPages = Map.of();
            syntheticSourceCache.clear();
            invalidateMediaWikiDerivedCaches();
            return;
        }

        long startNanos = System.nanoTime();
        var previousSyntheticIds = new HashSet<>(syntheticPages.keySet());
        CategoryIndex categoryIndex = getIndex(CategoryIndex.class);
        Map<ResourceLocation, ParsedGuidePage> rebuiltPages = MediaWikiSyntheticPageFactory.buildPages(
            this,
            getSourceParsedPages().values(),
            categoryIndex,
            syntheticSourceCache,
            this::parseSyntheticPage);
        syntheticPages = Map.copyOf(rebuiltPages);
        previousSyntheticIds.addAll(syntheticPages.keySet());
        for (ResourceLocation syntheticPageId : previousSyntheticIds) {
            removeCompiledPage(syntheticPageId);
        }
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [MutableGuide] Rebuilt {} synthetic pages in {} ms for guide {}",
                    syntheticPages.size(),
                    nanosToMillis(System.nanoTime() - startNanos),
                    id);
        }
    }

    private void invalidateMediaWikiDerivedCaches() {
        long nextRevision = mediaWikiRefreshController.invalidate();
        mediaWikiListContext = null;
        fallbackMediaWikiListContext = null;
        mediaWikiSpecialDataIndex = null;
        mediaWikiListContextRevision = nextRevision;
        mediaWikiSpecialDataIndexRevision = nextRevision;
        fallbackMediaWikiListContextRevision = nextRevision;
        requestedMediaWikiWarmupRevision = Long.MIN_VALUE;
    }

    @Override
    public @Nullable MediaWikiListContext getMediaWikiListContext() {
        long currentRevision = mediaWikiRefreshController.currentRevision();
        MediaWikiListContext cached = mediaWikiListContext;
        if (cached != null && mediaWikiListContextRevision == currentRevision) {
            return cached;
        }
        if (pages == null) {
            return cached;
        }
        requestMediaWikiDerivedCacheWarmup(currentRevision);
        if (cached != null) {
            return cached;
        }
        return getOrCreateFallbackMediaWikiListContext(currentRevision);
    }

    private void requestMediaWikiDerivedCacheWarmup(long revision) {
        if (pages == null) {
            return;
        }
        if (requestedMediaWikiWarmupRevision == revision) {
            return;
        }
        requestedMediaWikiWarmupRevision = revision;
        MediaWikiGuideAggregator aggregatedGuide = MediaWikiGuideAggregator.create(this);
        CategoryIndex categoryIndex = aggregatedGuide.getIndex(CategoryIndex.class);
        Map<ResourceLocation, ParsedGuidePage> pagesSnapshot = new LinkedHashMap<>();
        for (ParsedGuidePage page : aggregatedGuide.getPages()) {
            if (page != null) {
                pagesSnapshot.put(page.getId(), page);
            }
        }
        NavigationTree navigationSnapshot = aggregatedGuide.getNavigationTree();
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [MutableGuide] Scheduling MediaWiki cache warmup for guide {} revision {} with {} pages",
                    id,
                    revision,
                    pagesSnapshot.size());
        }
        mediaWikiRefreshController.requestRefresh(revision, () -> {
            try {
                long startNanos = System.nanoTime();
                MediaWikiSpecialDataIndex specialDataIndex = new MediaWikiSpecialDataIndexer()
                    .build(aggregatedGuide, pagesSnapshot.values(), categoryIndex);
                MediaWikiListContext listContext = MediaWikiListContext.create(
                    aggregatedGuide,
                    pagesSnapshot.values(),
                    navigationSnapshot,
                    categoryIndex,
                    specialDataIndex);
                synchronized (this) {
                    if (!mediaWikiRefreshController.isCurrent(revision)) {
                        return;
                    }
                    mediaWikiSpecialDataIndex = specialDataIndex;
                    mediaWikiSpecialDataIndexRevision = revision;
                    mediaWikiListContext = listContext;
                    mediaWikiListContextRevision = revision;
                    fallbackMediaWikiListContext = listContext;
                    fallbackMediaWikiListContextRevision = revision;
                }
                if (ModConfig.debug.enableDebugMode) {
                    FMLLog.getLogger()
                        .info(
                            "[GuideNH] [MutableGuide] Warmed MediaWiki caches asynchronously in {} ms for guide {} revision {} with {} pages",
                            nanosToMillis(System.nanoTime() - startNanos),
                            id,
                            revision,
                            pagesSnapshot.size());
                }
            } catch (Throwable t) {
                synchronized (this) {
                    if (requestedMediaWikiWarmupRevision == revision) {
                        requestedMediaWikiWarmupRevision = Long.MIN_VALUE;
                    }
                }
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [MutableGuide] Failed to warm MediaWiki caches asynchronously for guide {} revision {}",
                        id,
                        revision,
                        t);
            }
        });
    }

    private ParsedGuidePage parseSyntheticPage(ResourceLocation pageId, String sourcePack, String language,
        String source) {
        return PageCompiler.parse(sourcePack, language, pageId, source);
    }

    private MediaWikiListContext getOrCreateFallbackMediaWikiListContext(long revision) {
        MediaWikiListContext cached = fallbackMediaWikiListContext;
        if (cached != null && fallbackMediaWikiListContextRevision == revision) {
            return cached;
        }
        synchronized (this) {
            cached = fallbackMediaWikiListContext;
            if (cached != null && fallbackMediaWikiListContextRevision == revision) {
                return cached;
            }
            MediaWikiListContext created = createFallbackMediaWikiListContext();
            fallbackMediaWikiListContext = created;
            fallbackMediaWikiListContextRevision = revision;
            return created;
        }
    }

    private MediaWikiListContext createFallbackMediaWikiListContext() {
        MediaWikiGuideAggregator aggregatedGuide = MediaWikiGuideAggregator.create(this);
        CategoryIndex categoryIndex = aggregatedGuide.getIndex(CategoryIndex.class);
        return MediaWikiListContext.create(
            aggregatedGuide,
            aggregatedGuide.getPages(),
            aggregatedGuide.getNavigationTree(),
            categoryIndex,
            MediaWikiSpecialDataIndex.empty());
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private void recordParseFailure(ParsedGuidePage parsedPage) {
        var parseFailureMessage = parsedPage.getParseFailureMessage();
        if (parseFailureMessage == null || parseFailureMessage.isEmpty()) {
            return;
        }

        pageFailures.put(parsedPage.getId(), GuidePageFailure.parse(parseFailureMessage));
    }

    private void recordCompileFailure(ResourceLocation pageId, String errorText) {
        pageFailures.put(pageId, GuidePageFailure.compile(errorText));
    }

    private void clearCompileFailure(ResourceLocation pageId) {
        var failure = pageFailures.get(pageId);
        if (failure != null && !failure.parseFailure) {
            pageFailures.remove(pageId);
        }
    }

    private void clearParseFailure(ResourceLocation pageId) {
        var failure = pageFailures.get(pageId);
        if (failure != null && failure.parseFailure) {
            pageFailures.remove(pageId);
        }
    }

    private String buildCompileFailureText(ResourceLocation pageId, Throwable throwable) {
        var writer = new StringWriter();
        var printer = new PrintWriter(writer);
        printer.printf("Failed to compile guide page %s%n%n", pageId);
        throwable.printStackTrace(printer);
        printer.flush();
        return writer.toString();
    }

    private GuidePage buildFailurePage(ParsedGuidePage parsedPage, @Nullable GuidePageFailure failure) {
        var effectiveFailure = failure != null ? failure : GuidePageFailure.compile("Unknown guide page failure");
        return PageCompiler.buildErrorGuidePage(
            this,
            extensions,
            parsedPage.getSourcePack(),
            parsedPage.getId(),
            parsedPage.getSource(),
            effectiveFailure.headingText,
            effectiveFailure.errorText);
    }

    private static class GuidePageFailure {

        private final String headingText;
        private final String errorText;
        private final boolean parseFailure;

        private GuidePageFailure(String headingText, String errorText, boolean parseFailure) {
            this.headingText = headingText;
            this.errorText = errorText;
            this.parseFailure = parseFailure;
        }

        private static GuidePageFailure parse(String errorText) {
            return new GuidePageFailure("PARSING ERROR", errorText, true);
        }

        private static GuidePageFailure compile(String errorText) {
            return new GuidePageFailure("COMPILATION ERROR", errorText, false);
        }
    }

    @Desugar
    public record GuidePageFailureView(String headingText, String errorText, boolean parseFailure) {}
}
