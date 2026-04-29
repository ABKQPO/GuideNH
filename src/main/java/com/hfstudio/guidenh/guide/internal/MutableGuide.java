package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuideItemSettings;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

/**
 * Encapsulates a Guide, which consists of a collection of Markdown pages and associated content, loaded from a
 * guide-specific subdirectory of resource packs.
 */
public class MutableGuide implements Guide, GuideDevWatcherPump.TickableGuide {

    public static final Logger LOG = LoggerFactory.getLogger(MutableGuide.class);
    public static final String ACTIVE_CLIENT_WORLD_REQUIRED_MESSAGE = "active client world";

    private final ResourceLocation id;
    private final String defaultNamespace;
    private final String folder;
    private final String defaultLanguage;
    private final ResourceLocation startPage;
    private final Map<ResourceLocation, ParsedGuidePage> developmentPages = new HashMap<>();
    private final Map<Class<?>, PageIndex> indices;
    private NavigationTree navigationTree = new NavigationTree();
    /**
     * These are only loaded for the current language and backfilled by default language pages.
     */
    private Map<ResourceLocation, ParsedGuidePage> pages;
    private final Map<ParsedGuidePage, GuidePage> compiledPages = Collections.synchronizedMap(new WeakHashMap<>());
    private final ExtensionCollection extensions;
    private final boolean availableToOpenHotkey;
    private final GuideItemSettings itemSettings;

    @Nullable
    private final Path developmentSourceFolder;
    @Nullable
    private final String developmentSourceNamespace;

    @Nullable
    private GuideSourceWatcher watcher;

    public MutableGuide(ResourceLocation id, String defaultNamespace, String folder, String defaultLanguage,
        ResourceLocation startPage, @Nullable Path developmentSourceFolder, @Nullable String developmentSourceNamespace,
        Map<Class<?>, PageIndex> indices, ExtensionCollection extensions, boolean availableToOpenHotkey,
        GuideItemSettings itemSettings) {
        this.id = id;
        this.defaultNamespace = defaultNamespace;
        this.folder = folder;
        this.defaultLanguage = defaultLanguage;
        this.startPage = startPage;
        this.developmentSourceFolder = developmentSourceFolder;
        this.developmentSourceNamespace = developmentSourceNamespace;
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
    public ResourceLocation getStartPage() {
        return startPage;
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
            LOG.warn("Can't get page {}. Pages not loaded yet.", id);
            return null;
        }

        return developmentPages.getOrDefault(id, pages.get(id));
    }

    @Override
    @Nullable
    public GuidePage getPage(ResourceLocation id) {
        var parsedPage = getParsedPage(id);
        if (parsedPage == null) {
            return null;
        }

        GuidePage compiledPage;
        synchronized (compiledPages) {
            compiledPage = compiledPages.get(parsedPage);
            if (compiledPage == null) {
                compiledPage = PageCompiler.compile(this, extensions, parsedPage);
                compiledPages.put(parsedPage, compiledPage);
            }
        }
        compiledPage.prepareForDisplay();
        return compiledPage;
    }

    @Override
    public Collection<ParsedGuidePage> getPages() {
        if (pages == null) {
            throw new IllegalStateException("Pages are not loaded yet.");
        }

        if (developmentPages.isEmpty()) {
            return this.pages.values();
        }

        var pages = new LinkedHashMap<>(this.pages);
        pages.putAll(developmentPages);
        return pages.values();
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

    private @Nullable byte[] loadAssetInternal(ResourceLocation id) {
        // Also load images from the development sources folder, if it exists and contains the asset namespace
        if (developmentSourceFolder != null && id.getResourceDomain()
            .equals(developmentSourceNamespace)) {
            var path = developmentSourceFolder.resolve(id.getResourcePath());
            try {
                return Files.readAllBytes(path);
            } catch (NoSuchFileException ignored) {} catch (IOException e) {
                LOG.error("Failed to open guidebook asset {}", path);
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

        LOG.error("Failed to open guidebook asset {}", id);
        return null;
    }

    @Override
    public NavigationTree getNavigationTree() {
        return navigationTree;
    }

    @Override
    public boolean pageExists(ResourceLocation pageId) {
        return developmentPages.containsKey(pageId) || pages != null && pages.containsKey(pageId);
    }

    /**
     * Returns the on-disk path for a given guidebook resource (i.e. page, asset) if development mode is enabled and the
     * resource exists in the development source folder.
     *
     * @return null if development mode is not enabled or the resource doesn't exist in the development sources.
     */
    @Nullable
    public Path getDevelopmentSourcePath(ResourceLocation id) {
        if (developmentSourceFolder != null && id.getResourceDomain()
            .equals(developmentSourceNamespace)) {
            var path = developmentSourceFolder.resolve(id.getResourcePath());
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

        watcher = new GuideSourceWatcher(developmentSourceNamespace, defaultLanguage, developmentSourceFolder);
        Runtime.getRuntime()
            .addShutdownHook(new Thread(watcher::close));
    }

    @Override
    public boolean hasDevelopmentSources() {
        return watcher != null;
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

    private boolean isForDefaultLanguage(GuidePageChange change) {
        return change.language() == null || defaultLanguage.equals(change.language());
    }

    private void applyChanges(List<GuidePageChange> changes) {
        var initialPages = new HashMap<>(developmentPages);

        var currentLanguage = LangUtil.getCurrentLanguage();
        // Remove redundant changes
        for (int i = changes.size() - 1; i >= 0; i--) {
            var change = changes.get(i);
            if (change == null) {
                continue;
            }

            // Delete changes for languages that aren't relevant
            if (!isForDefaultLanguage(change) && !currentLanguage.equals(change.language())) {
                changes.set(i, null);
            } else {
                for (int j = 0; j < i; j++) {
                    var prevChange = changes.get(j);
                    if (prevChange != null && prevChange.pageId()
                        .equals(change.pageId())) {
                        changes.set(j, null);
                    }
                }
            }
        }
        changes.removeIf(Objects::isNull); // remove erased changes

        // Enrich each change with the previous page data while we process them
        for (int i = 0; i < changes.size(); i++) {
            var change = changes.get(i);
            var pageId = change.pageId();

            // Handle language changes
            var currentPage = developmentPages.get(pageId);
            if (currentPage != null && !defaultLanguage.equals(currentPage.getLanguage())
                && isForDefaultLanguage(change)) {
                changes.set(i, null);
                continue;
            }

            var newPage = change.newPage();
            if (newPage != null) {
                developmentPages.put(pageId, newPage);
            } else {
                developmentPages.remove(pageId);
            }

            changes.set(i, new GuidePageChange(change.language(), pageId, initialPages.get(pageId), newPage));
        }
        changes.removeIf(Objects::isNull); // Remove changes not relevant for the current language

        // Allow indices to rebuild
        var allPages = new ArrayList<ParsedGuidePage>(pages.size() + developmentPages.size());
        allPages.addAll(pages.values());
        allPages.addAll(developmentPages.values());
        for (var index : indices.values()) {
            if (index.supportsUpdate()) {
                index.update(allPages, changes);
            } else {
                index.rebuild(allPages);
            }
        }

        // Rebuild navigation
        this.navigationTree = buildNavigation();

        // Reload the current page if it has been changed
        var guideScreen = GuideScreen.current();
        if (guideScreen != null) {
            var currentId = guideScreen.getCurrentPageId();
            for (var change : changes) {
                if (currentId.equals(change.pageId())) {
                    guideScreen.reloadPage();
                    break;
                }
            }
        }
    }

    private NavigationTree buildNavigation() {
        if (developmentPages.isEmpty()) {
            return NavigationTree.build(this, pages.values());
        } else {
            var allPages = new HashMap<>(pages);
            allPages.putAll(developmentPages);
            return NavigationTree.build(this, allPages.values());
        }
    }

    public void validateAll() {
        // Iterate and compile all pages to warn about errors on startup
        for (var entry : developmentPages.entrySet()) {
            LOG.info("Compiling {}", entry.getKey());
            getPage(entry.getKey());
        }
    }

    public void rebuildIndices() {
        var allPages = new ArrayList<>(getPages());
        for (var index : indices.values()) {
            index.rebuild(allPages);
        }
    }

    public void setPages(Map<ResourceLocation, ParsedGuidePage> pages) {
        this.pages = Collections.unmodifiableMap(new HashMap<>(pages));
        compiledPages.clear();

        if (watcher != null) {
            watcher.clearChanges(); // Since we'll load them all now, ignore all changes up to now

            for (var page : watcher.loadAll()) {
                developmentPages.put(page.getId(), page);
            }
        }

        rebuildIndices();
        navigationTree = buildNavigation();
        warmPage(startPage);

        var guideScreen = GuideScreen.current();
        if (guideScreen != null && guideScreen.isShowingGuide(getId())) {
            guideScreen.reloadPage();
        }
    }

    public GuideItemSettings getItemSettings() {
        return itemSettings;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    private void warmPage(ResourceLocation pageId) {
        var parsedPage = getParsedPage(pageId);
        if (parsedPage == null) {
            return;
        }
        synchronized (compiledPages) {
            if (!compiledPages.containsKey(parsedPage)) {
                try {
                    compiledPages.put(parsedPage, PageCompiler.compile(this, extensions, parsedPage));
                } catch (RuntimeException e) {
                    if (isDeferrableWarmPageFailure(e)) {
                        LOG.debug(
                            "Deferring warm compilation for page {} until an active client world is available",
                            pageId);
                        return;
                    }
                    throw e;
                }
            }
        }
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
}
