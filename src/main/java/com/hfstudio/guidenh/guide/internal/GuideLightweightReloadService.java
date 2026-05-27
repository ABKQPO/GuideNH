package com.hfstudio.guidenh.guide.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.datadriven.GuidePageResourceSelector;
import com.hfstudio.guidenh.guide.internal.localization.GuideLocalizedPageSourceResolver;
import com.hfstudio.guidenh.guide.internal.localization.GuidePageLanguageIndex;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiTranslationStats;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

import cpw.mods.fml.common.FMLLog;

public class GuideLightweightReloadService {

    private GuideLightweightReloadService() {}

    public static void reloadDevelopmentGuides() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        reloadGuides(minecraft.getResourceManager());
    }

    public static void reloadGuides(IResourceManager resourceManager) {
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info("[GuideNH] [GuideLightweightReloadService] Reloading guide data...");
        }
        long startedAt = System.nanoTime();
        var activeResourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        GuidePageTexture.clear();
        GuidePageLanguageIndex.clear();
        GuideResourceLanguageIndex.clear();

        long stageStartedAt = System.nanoTime();
        GuideRegistry.setDataDriven(DataDrivenGuideLoader.load());
        MediaWikiTranslationStats.invalidateCache();
        long dataDrivenLoadNs = System.nanoTime() - stageStartedAt;

        var guidePages = new HashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();
        var pagePathCache = new LinkedHashMap<String, LinkedHashMap<String, LinkedHashSet<String>>>();

        String language = LangUtil.getCurrentLanguage();

        stageStartedAt = System.nanoTime();
        for (var guide : GuideRegistry.getAll()) {
            var pages = loadPages(
                resourceManager,
                guide.getId(),
                guide.getContentRootFolder(),
                guide.getDefaultLanguage(),
                language,
                pagePathCache,
                activeResourcePacks);
            guidePages.put(guide.getId(), pages);
        }
        long pageLoadNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        for (var entry : guidePages.entrySet()) {
            GuideRegistry.updatePages(entry.getKey(), entry.getValue(), false);
        }
        GuideRegistry.invalidateMergedNavigationTree();
        long registryUpdateNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        GuideWarmupPump.clearScheduler();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            guide.resetWarmup();
        }
        long warmupResetNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        try {
            GuideME.getSearch()
                .indexAll();
        } catch (Throwable t) {
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideLightweightReloadService] Failed to reindex search after reload", t);
        }
        long searchIndexNs = System.nanoTime() - stageStartedAt;

        int loadedPageCount = countLoadedPages(guidePages);
        int loadedLanguageCount = countLoadedLanguages(guidePages);
        long totalNs = System.nanoTime() - startedAt;

        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideLightweightReloadService] Guide reload complete, loaded {} guides, {} pages, {} languages in {} ns (dataDrivenLoadNs={}, pageLoadNs={}, registryUpdateNs={}, warmupResetNs={}, searchIndexNs={})",
                    guidePages.size(),
                    loadedPageCount,
                    loadedLanguageCount,
                    totalNs,
                    dataDrivenLoadNs,
                    pageLoadNs,
                    registryUpdateNs,
                    warmupResetNs,
                    searchIndexNs);
        }
    }

    /**
     * Scans the guide folder tree and loads all markdown files under {@code assets/<namespace>/<folder>/_<lang>/...}.
     */
    static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage) {
        return loadPages(
            resourceManager,
            guideId,
            folder,
            defaultLanguage,
            currentLanguage,
            new LinkedHashMap<>(),
            DataDrivenGuideLoader.getActiveResourcePacks());
    }

    static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage,
        Map<String, LinkedHashMap<String, LinkedHashSet<String>>> pagePathCache,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();
        var pagePaths = pagePathsForGuide(guideId, folder, pagePathCache, DataDrivenGuideLoader::discoverPagePaths);
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;
        String sourceNamespace = guideId.getResourceDomain();
        String sourcePack = "resources:" + sourceNamespace;
        int localizedHits = 0;
        int defaultLanguageHits = 0;
        int rawSourceHits = 0;
        int failedLoads = 0;

        for (var pagePath : pagePaths) {
            ResourceLocation pageId = new ResourceLocation(sourceNamespace, pagePath);
            PageLoadResult loadResult = loadPage(
                resourceManager,
                sourcePack,
                sourceNamespace,
                folder,
                defaultLanguage,
                lang,
                pagePath,
                pageId,
                activeResourcePacks);
            ParsedGuidePage parsed = loadResult != null ? loadResult.page() : null;
            if (parsed == null) {
                failedLoads++;
                FMLLog.getLogger()
                    .warn("[GuideNH] [GuideLightweightReloadService] Failed to load guide page {}", pageId);
                continue;
            }
            switch (loadResult.kind()) {
                case LOCALIZED:
                    localizedHits++;
                    break;
                case DEFAULT_LANGUAGE:
                    defaultLanguageHits++;
                    break;
                case RAW_SOURCE:
                    rawSourceHits++;
                    break;
                default:
                    break;
            }
            pages.put(pageId, parsed);
        }

        long totalNs = System.nanoTime() - startedAt;
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideLightweightReloadService] Loaded {} pages for guide {} folder {} requestedLanguage={} defaultLanguage={} discoveredPaths={} localizedHits={} defaultLanguageHits={} rawSourceHits={} failedLoads={} durationNs={}",
                    pages.size(),
                    guideId,
                    folder,
                    lang,
                    defaultLanguage,
                    pagePaths.size(),
                    localizedHits,
                    defaultLanguageHits,
                    rawSourceHits,
                    failedLoads,
                    totalNs);
        }
        return pages;
    }

    static LinkedHashSet<String> pagePathsForGuide(ResourceLocation guideId, String folder,
        Map<String, LinkedHashMap<String, LinkedHashSet<String>>> pagePathCache,
        Function<String, LinkedHashMap<String, LinkedHashSet<String>>> discoverPagePaths) {
        var pathsByNamespace = pagePathCache.computeIfAbsent(folder, discoverPagePaths);
        var pagePaths = pathsByNamespace.get(guideId.getResourceDomain());
        return pagePaths != null ? pagePaths : new LinkedHashSet<>();
    }

    @Nullable
    private static PageLoadResult tryLoadPage(String sourcePack, String requestedLanguage, String sourceLanguage,
        String namespace, String folder, String pagePath, ResourceLocation pageId, LoadKind kind,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        ParsedGuidePage page = tryParsePageCandidate(
            sourcePack,
            requestedLanguage,
            folder,
            pageId,
            new ResourceLocation(namespace, folder + "/_" + sourceLanguage + "/" + pagePath),
            activeResourcePacks);
        return page != null ? new PageLoadResult(page, kind) : null;
    }

    public static @Nullable ParsedGuidePage loadPageForLanguage(ResourceLocation guideId, String folder,
        String requestedLanguage, String sourceLanguage, ResourceLocation pageId) {
        String normalizedRequestedLanguage = LangUtil.normalizeLanguage(requestedLanguage);
        String normalizedSourceLanguage = LangUtil.normalizeLanguage(sourceLanguage);
        String sourcePack = "resources:" + guideId.getResourceDomain();
        PageLoadResult result = tryLoadPage(
            sourcePack,
            normalizedRequestedLanguage,
            normalizedSourceLanguage,
            guideId.getResourceDomain(),
            folder,
            pageId.getResourcePath(),
            pageId,
            LoadKind.LOCALIZED,
            DataDrivenGuideLoader.getActiveResourcePacks());
        return result != null ? result.page() : null;
    }

    public static @Nullable ParsedGuidePage tryLoadNeutralPageForExport(IResourceManager resourceManager,
        String sourcePack, String requestedLanguage, String contentRootFolder, ResourceLocation pageId,
        ResourceLocation sourceId) {
        return tryParsePage(
            resourceManager,
            sourcePack,
            LangUtil.normalizeLanguage(requestedLanguage),
            contentRootFolder,
            pageId,
            sourceId,
            DataDrivenGuideLoader.getActiveResourcePacks());
    }

    @Nullable
    private static ParsedGuidePage tryParsePage(IResourceManager resourceManager, String sourcePack, String language,
        String contentRootFolder, ResourceLocation pageId, ResourceLocation sourceId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        GuidePageResourceSelector.SelectedPageResource selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        byte[] bytes = selected != null ? selected.bytes() : null;
        if (bytes == null) {
            bytes = GuideResourceAccess.readBytes(resourceManager, sourceId);
        }
        if (bytes == null) {
            return null;
        }
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes);
    }

    @Nullable
    private static ParsedGuidePage tryParsePageCandidate(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, Iterable<? extends IResourcePack> activeResourcePacks) {
        GuidePageResourceSelector.SelectedPageResource selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        if (selected == null) {
            return null;
        }
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, selected.bytes());
    }

    @Nullable
    private static ParsedGuidePage parsePageBytes(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, byte[] bytes) {
        try {
            return GuideLocalizedPageSourceResolver.parse(sourcePack, language, contentRootFolder, pageId, bytes);
        } catch (Exception ex) {
            FMLLog.getLogger()
                .error("[GuideNH] [GuideLightweightReloadService] Error parsing page {} from {}", pageId, sourceId, ex);
            return null;
        }
    }

    static @Nullable byte[] selectPageCandidate(ResourceLocation sourceId) {
        return selectPageCandidate(sourceId, DataDrivenGuideLoader.getActiveResourcePacks());
    }

    static @Nullable byte[] selectPageCandidate(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks) {
        GuidePageResourceSelector.SelectedPageResource winner = GuidePageResourceSelector
            .select(sourceId, resourcePacks);
        return winner != null ? winner.bytes() : null;
    }

    static int readLoadPriority(ResourceLocation sourceId, byte[] bytes) {
        return GuidePageResourceSelector.readLoadPriority(sourceId, bytes);
    }

    @Nullable
    private static PageLoadResult loadPage(IResourceManager resourceManager, String sourcePack, String namespace,
        String folder, String defaultLanguage, String requestedLanguage, String pagePath, ResourceLocation pageId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        PageLoadResult localized = tryLoadPage(
            sourcePack,
            requestedLanguage,
            requestedLanguage,
            namespace,
            folder,
            pagePath,
            pageId,
            LoadKind.LOCALIZED,
            activeResourcePacks);
        if (localized != null) {
            return localized;
        }
        if (!requestedLanguage.equals(defaultLanguage)) {
            PageLoadResult fallback = tryLoadPage(
                sourcePack,
                requestedLanguage,
                defaultLanguage,
                namespace,
                folder,
                pagePath,
                pageId,
                LoadKind.DEFAULT_LANGUAGE,
                activeResourcePacks);
            if (fallback != null) {
                return fallback;
            }
        }
        ParsedGuidePage rawPage = tryParsePage(
            resourceManager,
            sourcePack,
            requestedLanguage,
            folder,
            pageId,
            new ResourceLocation(namespace, folder + "/" + pagePath),
            activeResourcePacks);
        return rawPage != null ? new PageLoadResult(rawPage, LoadKind.RAW_SOURCE) : null;
    }

    private static int countLoadedPages(Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> guidePages) {
        int total = 0;
        for (var pages : guidePages.values()) {
            total += pages.size();
        }
        return total;
    }

    private static int countLoadedLanguages(Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> guidePages) {
        var languages = new LinkedHashSet<String>();
        for (var pages : guidePages.values()) {
            for (var parsedPage : pages.values()) {
                languages.add(parsedPage.getLanguage());
            }
        }
        return languages.size();
    }

    private enum LoadKind {
        LOCALIZED,
        DEFAULT_LANGUAGE,
        RAW_SOURCE
    }

    @Desugar
    private record PageLoadResult(ParsedGuidePage page, LoadKind kind) {}
}
