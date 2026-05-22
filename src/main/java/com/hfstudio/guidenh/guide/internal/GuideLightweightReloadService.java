package com.hfstudio.guidenh.guide.internal;

import java.nio.charset.StandardCharsets;
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
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.localization.GuideLocalizedPageSourceResolver;
import com.hfstudio.guidenh.guide.internal.localization.GuidePageLanguageIndex;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
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
        FMLLog.getLogger()
            .info("[GuideNH] [GuideLightweightReloadService] Reloading guide data...");
        long startedAt = System.nanoTime();
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        GuidePageTexture.clear();
        GuidePageLanguageIndex.clear();

        long stageStartedAt = System.nanoTime();
        GuideRegistry.setDataDriven(DataDrivenGuideLoader.load());
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
                pagePathCache);
            guidePages.put(guide.getId(), pages);
        }
        long pageLoadNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        for (var entry : guidePages.entrySet()) {
            GuideRegistry.updatePages(entry.getKey(), entry.getValue());
        }
        long registryUpdateNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
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

    /**
     * Scans the guide folder tree and loads all markdown files under {@code assets/<namespace>/<folder>/_<lang>/...}.
     */
    static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage) {
        return loadPages(resourceManager, guideId, folder, defaultLanguage, currentLanguage, new LinkedHashMap<>());
    }

    static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage,
        Map<String, LinkedHashMap<String, LinkedHashSet<String>>> pagePathCache) {
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

            ParsedGuidePage parsed = tryLoadPage(sourcePack, lang, lang, sourceNamespace, folder, pagePath, pageId);
            if (parsed != null) {
                localizedHits++;
                pages.put(pageId, parsed);
                continue;
            }

            if (!lang.equals(defaultLanguage)) {
                parsed = tryLoadPage(sourcePack, lang, defaultLanguage, sourceNamespace, folder, pagePath, pageId);
                if (parsed != null) {
                    defaultLanguageHits++;
                    pages.put(pageId, parsed);
                    continue;
                }
            }

            parsed = tryParsePage(
                resourceManager,
                sourcePack,
                lang,
                folder,
                pageId,
                new ResourceLocation(sourceNamespace, folder + "/" + pagePath));
            if (parsed != null) {
                rawSourceHits++;
                pages.put(pageId, parsed);
            } else {
                failedLoads++;
                FMLLog.getLogger()
                    .warn("[GuideNH] [GuideLightweightReloadService] Failed to load guide page {}", pageId);
            }
        }

        long totalNs = System.nanoTime() - startedAt;
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
    private static ParsedGuidePage tryLoadPage(String sourcePack, String requestedLanguage, String sourceLanguage,
        String namespace, String folder, String pagePath, ResourceLocation pageId) {
        return tryParsePageCandidate(
            sourcePack,
            requestedLanguage,
            folder,
            pageId,
            new ResourceLocation(namespace, folder + "/_" + sourceLanguage + "/" + pagePath));
    }

    @Nullable
    private static ParsedGuidePage tryParsePage(IResourceManager resourceManager, String sourcePack, String language,
        String contentRootFolder, ResourceLocation pageId, ResourceLocation sourceId) {
        byte[] bytes = selectPageCandidate(sourceId);
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
        ResourceLocation pageId, ResourceLocation sourceId) {
        byte[] bytes = selectPageCandidate(sourceId);
        if (bytes == null) {
            return null;
        }
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes);
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
        PageCandidate winner = null;
        int order = 0;
        for (IResourcePack resourcePack : resourcePacks) {
            byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
            if (bytes == null) {
                continue;
            }
            PageCandidate candidate = new PageCandidate(bytes, readLoadPriority(sourceId, bytes), order++);
            if (winner == null || candidate.shouldReplace(winner)) {
                winner = candidate;
            }
        }
        return winner != null ? winner.bytes() : null;
    }

    static int readLoadPriority(ResourceLocation sourceId, byte[] bytes) {
        String source = new String(bytes, StandardCharsets.UTF_8);
        var frontmatter = PageCompiler.parseFrontmatterFromSource(sourceId, PageCompiler.normalizeLineEndings(source));
        var navigation = frontmatter.navigationEntry();
        return navigation != null ? navigation.loadPriority() : 0;
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

    @Desugar
    private static class PageCandidate {

        private final byte[] bytes;
        private final int priority;
        private final int order;

        private PageCandidate(byte[] bytes, int priority, int order) {
            this.bytes = bytes;
            this.priority = priority;
            this.order = order;
        }

        private byte[] bytes() {
            return bytes;
        }

        boolean shouldReplace(PageCandidate previous) {
            return priority > previous.priority || priority == previous.priority && order > previous.order;
        }
    }
}
