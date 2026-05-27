package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuideLightweightReloadService;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageIds;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSyntheticPageFactory;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSyntheticPageFactory.SyntheticSourceSnapshot;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

import cpw.mods.fml.common.FMLLog;

public class GuideSitePageCollector {

    @FunctionalInterface
    public interface PageLoader {

        Optional<LoadedPage> load(String language, ResourceLocation pageId);
    }

    private final PageLoader pageLoader;

    public GuideSitePageCollector(PageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    public GuideSitePageCollector(MutableGuide guide, IResourceManager resourceManager) {
        this((language, pagePath) -> tryLoadPage(guide, resourceManager, language, pagePath));
    }

    public List<GuideSitePageVariant> collect(MutableGuide guide) {
        return collect(guide, null);
    }

    public List<GuideSitePageVariant> collect(MutableGuide guide, @Nullable List<String> discoveredLanguages) {
        List<String> languages;
        if (discoveredLanguages != null) {
            languages = new ArrayList<>(discoveredLanguages);
        } else {
            try {
                languages = discoverLanguages();
            } catch (Throwable t) {
                FMLLog.getLogger()
                    .debug(
                        "[GuideNH] [GuideSitePageCollector] Falling back to the guide default language for {}",
                        guide.getId(),
                        t);
                languages = new ArrayList<>();
            }
        }
        if (languages.isEmpty()) {
            languages.add(guide.getDefaultLanguage());
        } else if (!languages.contains(guide.getDefaultLanguage())) {
            languages.add(0, guide.getDefaultLanguage());
        }

        LinkedHashSet<ResourceLocation> pageIdSet;
        try {
            pageIdSet = new LinkedHashSet<>();
            var pathsByNs = DataDrivenGuideLoader.discoverPagePaths(guide.getContentRootFolder());
            for (var entry : pathsByNs.entrySet()) {
                for (String path : entry.getValue()) {
                    pageIdSet.add(new ResourceLocation(entry.getKey(), path));
                }
            }
        } catch (Throwable t) {
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [GuideSitePageCollector] Falling back to already loaded page ids for {}",
                    guide.getId(),
                    t);
            pageIdSet = new LinkedHashSet<>();
        }
        for (ParsedGuidePage page : guide.getPages()) {
            pageIdSet.add(page.getId());
        }
        List<ResourceLocation> pageIds = new ArrayList<>(pageIdSet);
        List<GuideSitePageVariant> variants = new ArrayList<>();
        Map<String, Map<ResourceLocation, Optional<LoadedPage>>> pageCacheByLanguage = new LinkedHashMap<>();
        Map<ResourceLocation, SyntheticSourceSnapshot> syntheticSourceCache = new LinkedHashMap<>();

        for (String language : languages) {
            List<ParsedGuidePage> localizedPages = new ArrayList<>();
            for (ResourceLocation pageId : pageIds) {
                if (MediaWikiPageIds.isSyntheticPage(pageId)) {
                    continue;
                }

                Optional<LoadedPage> loadedPage = loadPageCached(pageCacheByLanguage, language, pageId);
                if (!loadedPage.isPresent()) {
                    continue;
                }

                LoadedPage localized = loadedPage.get();
                ParsedGuidePage page = localized.page();
                localizedPages.add(page);
                variants.add(
                    new GuideSitePageVariant(
                        guide.getId(),
                        page.getId(),
                        language,
                        localized.sourceLanguage(),
                        localized.fallbackUsed(),
                        page));
            }

            var indexedPages = new ArrayList<>(localizedPages);
            indexedPages.removeIf(
                page -> !NavigationTree.areModRequirementsMet(
                    page.getFrontmatter()
                        .navigationEntry()));

            CategoryIndex categoryIndex = new CategoryIndex();
            categoryIndex.rebuild(indexedPages);
            for (ParsedGuidePage syntheticPage : MediaWikiSyntheticPageFactory
                .buildPages(
                    guide,
                    localizedPages,
                    categoryIndex,
                    syntheticSourceCache,
                    (pageId, sourcePack, sourceLanguage, source) -> GuideSitePageCollector
                        .parseSyntheticPage(pageId, sourcePack, sourceLanguage, source))
                .values()) {
                variants.add(
                    new GuideSitePageVariant(
                        guide.getId(),
                        syntheticPage.getId(),
                        language,
                        language,
                        false,
                        syntheticPage));
            }
        }
        return variants;
    }

    private static ParsedGuidePage parseSyntheticPage(ResourceLocation pageId, String sourcePack, String language,
        String source) {
        return com.hfstudio.guidenh.guide.compiler.PageCompiler.parse(sourcePack, language, pageId, source);
    }

    public static List<String> discoverLanguagesOrEmpty() {
        try {
            return discoverLanguages();
        } catch (Throwable t) {
            FMLLog.getLogger()
                .debug("[GuideNH] [GuideSitePageCollector] Falling back to no discovered site export languages", t);
            return new ArrayList<>();
        }
    }

    public List<GuideSitePageVariant> collect(ResourceLocation guideId, String defaultLanguage, List<String> languages,
        List<ResourceLocation> pageIds) {
        List<GuideSitePageVariant> variants = new ArrayList<>();
        Map<String, Map<ResourceLocation, Optional<LoadedPage>>> pageCacheByLanguage = new LinkedHashMap<>();

        for (String language : languages) {
            for (ResourceLocation pageId : pageIds) {
                Optional<LoadedPage> loadedPage = loadPageCached(pageCacheByLanguage, language, pageId);
                if (!loadedPage.isPresent()) {
                    continue;
                }
                LoadedPage localized = loadedPage.get();
                ParsedGuidePage page = localized.page();
                boolean fallback = localized.fallbackUsed();
                variants.add(
                    new GuideSitePageVariant(
                        guideId,
                        page.getId(),
                        language,
                        localized.sourceLanguage(),
                        fallback,
                        page));
            }
        }

        return variants;
    }

    private static List<String> discoverLanguages() {
        Map<ResourceLocation, LinkedHashSet<String>> discovered = new LinkedHashMap<>();
        for (var resourcePack : DataDrivenGuideLoader.getActiveResourcePacks()) {
            DataDrivenGuideLoader.scanResourcePack(resourcePack, discovered);
        }
        var merged = new LinkedHashSet<String>();
        for (var langs : discovered.values()) {
            merged.addAll(langs);
        }
        return new ArrayList<>(merged);
    }

    private Optional<LoadedPage> loadPageCached(
        Map<String, Map<ResourceLocation, Optional<LoadedPage>>> pageCacheByLanguage, String language,
        ResourceLocation pageId) {
        return pageCacheByLanguage.computeIfAbsent(language, ignored -> new LinkedHashMap<>())
            .computeIfAbsent(pageId, ignored -> pageLoader.load(language, pageId));
    }

    private static Optional<LoadedPage> tryLoadPage(MutableGuide guide, IResourceManager resourceManager,
        String language, ResourceLocation pageId) {
        String sourceLanguage = language;
        ParsedGuidePage localized = GuideLightweightReloadService
            .loadPageForLanguage(guide.getId(), guide.getContentRootFolder(), language, sourceLanguage, pageId);
        if (localized != null) {
            return Optional.of(new LoadedPage(sourceLanguage, false, localized));
        }

        String defaultLanguage = guide.getDefaultLanguage();
        if (!defaultLanguage.equals(language)) {
            ParsedGuidePage fallback = GuideLightweightReloadService
                .loadPageForLanguage(guide.getId(), guide.getContentRootFolder(), language, defaultLanguage, pageId);
            if (fallback != null) {
                return Optional.of(new LoadedPage(defaultLanguage, true, fallback));
            }
        }

        String namespace = pageId.getResourceDomain();
        ParsedGuidePage neutral = GuideLightweightReloadService.tryLoadNeutralPageForExport(
            resourceManager,
            "resources:" + namespace,
            language,
            guide.getContentRootFolder(),
            pageId,
            new ResourceLocation(namespace, guide.getContentRootFolder() + "/" + pageId.getResourcePath()));
        return neutral != null ? Optional.of(new LoadedPage(null, true, neutral)) : Optional.empty();
    }

    @Desugar
    public record LoadedPage(@Nullable String sourceLanguage, boolean fallbackUsed, ParsedGuidePage page) {}
}
