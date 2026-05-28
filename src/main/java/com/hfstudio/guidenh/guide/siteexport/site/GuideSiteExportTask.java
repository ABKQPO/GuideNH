package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageIds;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene.BlockStatsLayoutState;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene.PonderTimelineKeyframe;
import com.hfstudio.guidenh.guide.scene.SceneBlockStatsEntry;
import com.hfstudio.guidenh.guide.scene.SceneSoundCue;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;

import cpw.mods.fml.common.FMLLog;

public class GuideSiteExportTask {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final int MAX_SCENE_STATE_VARIANTS = 256;

    private final Path outDir;
    private final GuideSiteExportOptions options;

    public GuideSiteExportTask(Path outDir) {
        this(outDir, GuideSiteExportOptions.DEFAULT);
    }

    public GuideSiteExportTask(Path outDir, GuideSiteExportOptions options) {
        this.outDir = outDir;
        this.options = options != null ? options : GuideSiteExportOptions.DEFAULT;
    }

    public Result run() throws Exception {
        Files.createDirectories(outDir);

        GuideSiteWriter writer = new GuideSiteWriter();
        writer.cleanupGeneratedOutputs(outDir);
        GuideSiteSearchTextExtractor searchExtractor = new GuideSiteSearchTextExtractor();
        GuideSiteAssetRegistry assets = new GuideSiteAssetRegistry(outDir);
        GuideSiteItemIconExporter itemIconExporter = new GuideSiteItemIconExporter(assets);
        GuideSiteNeiPhase1BackgroundExporter neiPhase1Exporter = new GuideSiteNeiPhase1BackgroundExporter(assets);
        GuideSiteSceneRuntimeExporter sceneExporter = new GuideSiteSceneRuntimeExporter(assets);
        writer.writeBootstrapFiles(outDir);

        int guidesExported = 0;
        int pagesExported = 0;
        int pagesFailed = 0;
        String firstPageUrl = null;
        Map<String, List<Map<String, Object>>> searchEntriesByLanguage = new LinkedHashMap<>();
        Map<ResourceLocation, MutableGuide> guidesById = new LinkedHashMap<>();
        Map<ResourceLocation, List<GuideSitePageVariant>> variantsByGuideId = new LinkedHashMap<>();
        Map<String, List<GuideSitePageVariant>> allVariantsByLanguage = new LinkedHashMap<>();
        IResourceManager resourceManager = null;

        // Capture the user's current Minecraft language so we can restore it after export.
        // We switch the Minecraft locale per-language during export so that item display names,
        // tooltips, and other StatCollector lookups resolve to the correct localization.
        Language originalMcLanguage = null;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getLanguageManager() != null) {
                originalMcLanguage = mc.getLanguageManager()
                    .getCurrentLanguage();
            }
        } catch (Throwable ignored) {}

        try {
            List<String> discoveredLanguages = GuideSitePageCollector.discoverLanguagesOrEmpty();
            for (MutableGuide guide : GuideRegistry.getAll()) {
                if (resourceManager == null) {
                    resourceManager = resolveResourceManager();
                    if (resourceManager == null) {
                        throw new IllegalStateException("Minecraft resource manager is not available");
                    }
                }

                GuideSitePageCollector collector = new GuideSitePageCollector(guide, resourceManager);
                List<GuideSitePageVariant> variants;
                try {
                    variants = collector.collect(guide, discoveredLanguages);
                } catch (Throwable t) {
                    FMLLog.getLogger()
                        .warn(
                            "[GuideNH] [GuideSiteExportTask] Failed to collect page variants for guide {}",
                            guide.getId(),
                            t);
                    recordFailure(outDir, "collect " + guide.getId(), t);
                    pagesFailed++;
                    continue;
                }

                guidesById.put(guide.getId(), guide);
                variantsByGuideId.put(guide.getId(), variants);
                for (GuideSitePageVariant variant : variants) {
                    allVariantsByLanguage.computeIfAbsent(variant.language(), ignored -> new ArrayList<>())
                        .add(variant);
                }
            }

            Map<String, LanguageExportContext> contextsByLanguage = new LinkedHashMap<>();
            for (Map.Entry<String, List<GuideSitePageVariant>> entry : allVariantsByLanguage.entrySet()) {
                String language = entry.getKey();
                List<GuideSitePageVariant> languageVariants = entry.getValue();
                contextsByLanguage.put(
                    language,
                    buildLanguageExportContext(guidesById, languageVariants, resourceManager, language, assets));
            }

            for (Map.Entry<ResourceLocation, MutableGuide> guideEntry : guidesById.entrySet()) {
                MutableGuide guide = guideEntry.getValue();
                List<GuideSitePageVariant> variants = variantsByGuideId.getOrDefault(guideEntry.getKey(), List.of());
                guidesExported++;

                Map<String, List<GuideSitePageVariant>> variantsByLanguage = new LinkedHashMap<>();
                for (GuideSitePageVariant variant : variants) {
                    variantsByLanguage.computeIfAbsent(variant.language(), ignored -> new ArrayList<>())
                        .add(variant);
                }

                List<String> languageOrder = new ArrayList<>(variantsByLanguage.keySet());
                Map<ResourceLocation, List<GuideSiteLanguageLink>> languageLinksByPageId = buildLanguageLinks(
                    writer,
                    guide,
                    variants,
                    languageOrder);

                for (Map.Entry<String, List<GuideSitePageVariant>> languageEntry : variantsByLanguage.entrySet()) {
                    String language = languageEntry.getKey();
                    List<GuideSitePageVariant> languageVariants = languageEntry.getValue();
                    LanguageExportContext context = contextsByLanguage
                        .getOrDefault(language, LanguageExportContext.EMPTY);

                    // Switch the active Minecraft locale so localized item display names and tooltips
                    // resolve to this language while we render this language's pages.
                    switchMinecraftLanguage(language);
                    GuideSitePageAssetExporter assetExporter = context.assetExportersByGuideId()
                        .get(guide.getId());
                    if (assetExporter == null) {
                        assetExporter = createPageAssetExporter(guide, resourceManager, language, assets);
                    }
                    GuideSiteHtmlCompiler compiler = createHtmlCompiler(
                        assets,
                        assetExporter,
                        new GuideSiteRecipeTagRenderer(itemIconExporter, neiPhase1Exporter),
                        new GuideSiteMdxTagRenderer(
                            context.scopedGuidesByGuideId()
                                .getOrDefault(guide.getId(), guide),
                            context.parsedPagesById(),
                            context.navigationTree(),
                            assetExporter,
                            itemIconExporter,
                            context.assetExportersByGuideId(),
                            context.mediaWikiContextsByGuideId()
                                .get(guide.getId())),
                        itemIconExporter);

                    for (GuideSitePageVariant variant : languageVariants) {
                        try {
                            try (GuideSiteHrefResolver.ContextScope ignored = GuideSiteHrefResolver.exportContext(
                                guide.getId()
                                    .getResourceDomain(),
                                guide.getId()
                                    .getResourcePath(),
                                language,
                                context.guideIdsByPageId())) {
                                Guide scopedGuide = context.scopedGuidesByGuideId()
                                    .getOrDefault(guide.getId(), guide);
                                GuideSiteTemplateRegistry templates = new GuideSiteTemplateRegistry();
                                GuidePage compiledPage = PageCompiler
                                    .compile(scopedGuide, scopedGuide.getExtensions(), variant.parsedPage());
                                List<GuideSiteExportedScene> exportedScenes = exportScenes(
                                    guide,
                                    variant.parsedPage(),
                                    compiledPage,
                                    templates,
                                    assets,
                                    sceneExporter,
                                    assetExporter,
                                    itemIconExporter);
                                String body = compiler
                                    .compileBody(variant.parsedPage(), templates, createSceneResolver(exportedScenes));
                                List<GuideSiteLanguageLink> langLinks = languageLinksByPageId.get(variant.pageId());
                                String langSwitcherHtml = writer.renderLanguageSwitcher(language, langLinks);
                                String sidebarHtml = writer.renderSidebar(
                                    guide,
                                    language,
                                    context.navigationTree(),
                                    variant.pageId(),
                                    assetExporter,
                                    itemIconExporter,
                                    langLinks,
                                    context.assetExportersByGuideId());
                                String pageFile = toOutputPageFile(variant.parsedPage());
                                String pageUrl = writer.pageUrl(
                                    guide.getId()
                                        .getResourceDomain(),
                                    guide.getId()
                                        .getResourcePath(),
                                    language,
                                    pageFile);
                                String pageTitle = searchExtractor.title(scopedGuide, variant.parsedPage());

                                writer.writePage(
                                    outDir,
                                    guide.getId()
                                        .getResourceDomain(),
                                    guide.getId()
                                        .getResourcePath(),
                                    language,
                                    pageFile,
                                    langSwitcherHtml,
                                    sidebarHtml,
                                    body,
                                    templates.renderAll(),
                                    pageTitle);

                                if (!MediaWikiPageIds.isSpecialPage(variant.pageId())) {
                                    Map<String, Object> searchEntry = new LinkedHashMap<>();
                                    searchEntry.put("title", pageTitle);
                                    searchEntry.put(
                                        "guideId",
                                        guide.getId()
                                            .toString());
                                    searchEntry.put(
                                        "pageId",
                                        variant.pageId()
                                            .toString());
                                    searchEntry.put("url", pageUrl);
                                    searchEntry
                                        .put("text", searchExtractor.searchableText(scopedGuide, variant.parsedPage()));
                                    searchEntry.put(
                                        "mediaWikiSpecialPage",
                                        MediaWikiPageIds.isCategoryPage(variant.pageId())
                                            || MediaWikiPageIds.isSpecialPage(variant.pageId()));
                                    appendSearchIconData(
                                        searchEntry,
                                        context.navigationTree()
                                            .getNodeById(variant.pageId()),
                                        assetExporter,
                                        itemIconExporter,
                                        context.assetExportersByGuideId());
                                    searchEntriesByLanguage.computeIfAbsent(language, ignored2 -> new ArrayList<>())
                                        .add(searchEntry);
                                }

                                if (firstPageUrl == null) {
                                    firstPageUrl = pageUrl;
                                }
                            }
                            pagesExported++;
                        } catch (Throwable t) {
                            FMLLog.getLogger()
                                .warn(
                                    "[GuideNH] [GuideSiteExportTask] Failed to export page {} for language {}",
                                    variant.pageId(),
                                    language,
                                    t);
                            recordFailure(outDir, "page " + variant.pageId() + " (" + language + ")", t);
                            pagesFailed++;
                        }
                    }
                }
            }
        } finally {
            restoreMinecraftLanguage(originalMcLanguage);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : searchEntriesByLanguage.entrySet()) {
            writer.writeSearchIndex(outDir, entry.getKey(), GSON.toJson(entry.getValue()));
        }

        writer.writeLandingPage(outDir, firstPageUrl, "GuideNH Static Export");

        return new Result(guidesExported, pagesExported, pagesFailed, outDir);
    }

    private static void switchMinecraftLanguage(String requested) {
        if (requested == null || requested.isEmpty()) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }
            LanguageManager manager = mc.getLanguageManager();
            if (manager == null) {
                return;
            }
            String requestedLower = requested.toLowerCase(Locale.ROOT);
            Language target = null;
            for (Language candidate : manager.getLanguages()) {
                String code = candidate.getLanguageCode();
                if (code == null) {
                    continue;
                }
                if (code.toLowerCase(Locale.ROOT)
                    .equals(requestedLower)) {
                    target = candidate;
                    break;
                }
            }
            if (target == null) {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [GuideSiteExportTask] Cannot switch Minecraft locale to {}: no matching Language registered",
                        requested);
                return;
            }
            Language current = manager.getCurrentLanguage();
            if (current != null && current.getLanguageCode() != null
                && requestedLower.equals(
                    current.getLanguageCode()
                        .toLowerCase(Locale.ROOT))) {
                return;
            }
            manager.setCurrentLanguage(target);
            IResourceManager rm = mc.getResourceManager();
            if (rm != null) {
                manager.onResourceManagerReload(rm);
            }
        } catch (Throwable t) {
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideSiteExportTask] Failed to switch Minecraft locale to {}", requested, t);
        }
    }

    private static void restoreMinecraftLanguage(Language original) {
        if (original == null) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }
            LanguageManager manager = mc.getLanguageManager();
            if (manager == null) {
                return;
            }
            manager.setCurrentLanguage(original);
            IResourceManager rm = mc.getResourceManager();
            if (rm != null) {
                manager.onResourceManagerReload(rm);
            }
        } catch (Throwable t) {
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideSiteExportTask] Failed to restore original Minecraft locale", t);
        }
    }

    private static void recordFailure(Path outDir, String label, Throwable t) {
        // Print to stderr so the failure is visible even if SLF4J routing drops it.
        System.err.println("[GuideNH] Static site export failure: " + label);
        t.printStackTrace();
        try {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                pw.println("=== " + label + " ===");
                t.printStackTrace(pw);
                pw.println();
            }
            Files.createDirectories(outDir);
            Files.write(
                outDir.resolve("export-failures.log"),
                sw.toString()
                    .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        } catch (IOException ioException) {
            // best-effort logging only
        }
    }

    private Map<ResourceLocation, ParsedGuidePage> buildParsedPagesById(List<GuideSitePageVariant> variants) {
        Map<ResourceLocation, ParsedGuidePage> parsedPagesById = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.parsedPage() == null || variant.pageId() == null) {
                continue;
            }
            parsedPagesById.putIfAbsent(variant.pageId(), variant.parsedPage());
        }
        return parsedPagesById;
    }

    private LanguageExportContext buildLanguageExportContext(Map<ResourceLocation, MutableGuide> guidesById,
        List<GuideSitePageVariant> languageVariants, IResourceManager resourceManager, String language,
        GuideSiteAssetRegistry assets) {
        Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> scopedPagesByGuideId = buildScopedPagesByGuideId(
            languageVariants);
        Map<ResourceLocation, MediaWikiListContext> mediaWikiContextsByGuideId = buildMediaWikiContextsByGuideId(
            guidesById,
            languageVariants);
        return new LanguageExportContext(
            buildParsedPagesById(languageVariants),
            buildMergedNavigationTree(guidesById, languageVariants),
            indexGuideIdsByPageId(languageVariants),
            mediaWikiContextsByGuideId,
            buildScopedGuidesByGuideId(guidesById, scopedPagesByGuideId, mediaWikiContextsByGuideId),
            buildAssetExportersByGuideId(guidesById, languageVariants, resourceManager, language, assets));
    }

    private Map<ResourceLocation, ResourceLocation> indexGuideIdsByPageId(List<GuideSitePageVariant> variants) {
        Map<ResourceLocation, ResourceLocation> guideIdsByPageId = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.pageId() == null || variant.guideId() == null) {
                continue;
            }
            guideIdsByPageId.putIfAbsent(variant.pageId(), variant.guideId());
        }
        return guideIdsByPageId;
    }

    private NavigationTree buildMergedNavigationTree(Map<ResourceLocation, MutableGuide> availableGuides,
        List<GuideSitePageVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return new NavigationTree();
        }

        Map<ResourceLocation, PageCollection> pageCollectionsByPageId = new LinkedHashMap<>();
        List<ParsedGuidePage> mergedPages = new ArrayList<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.guideId() == null || variant.parsedPage() == null) {
                continue;
            }
            MutableGuide guide = availableGuides.get(variant.guideId());
            if (guide == null) {
                continue;
            }
            pageCollectionsByPageId.putIfAbsent(variant.pageId(), guide);
            mergedPages.add(variant.parsedPage());
        }

        if (pageCollectionsByPageId.isEmpty() || mergedPages.isEmpty()) {
            return new NavigationTree();
        }

        return NavigationTree.buildMergedPages(pageCollectionsByPageId, mergedPages);
    }

    private Map<ResourceLocation, GuideSitePageAssetExporter> buildAssetExportersByGuideId(
        Map<ResourceLocation, MutableGuide> availableGuides, List<GuideSitePageVariant> variants,
        IResourceManager resourceManager, String language, GuideSiteAssetRegistry assets) {
        Map<ResourceLocation, GuideSitePageAssetExporter> exportersByGuideId = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.guideId() == null) {
                continue;
            }
            MutableGuide guide = availableGuides.get(variant.guideId());
            if (guide == null) {
                continue;
            }
            exportersByGuideId.computeIfAbsent(
                guide.getId(),
                ignored -> createPageAssetExporter(guide, resourceManager, language, assets));
        }
        return exportersByGuideId;
    }

    private Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> buildScopedPagesByGuideId(
        List<GuideSitePageVariant> variants) {
        Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> pagesByGuideId = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.guideId() == null
                || variant.pageId() == null
                || variant.parsedPage() == null) {
                continue;
            }
            pagesByGuideId.computeIfAbsent(variant.guideId(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(variant.pageId(), variant.parsedPage());
        }
        return pagesByGuideId;
    }

    private Map<ResourceLocation, MediaWikiListContext> buildMediaWikiContextsByGuideId(
        Map<ResourceLocation, MutableGuide> availableGuides, List<GuideSitePageVariant> variants) {
        Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> pagesByGuideId = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            if (variant == null || variant.guideId() == null
                || variant.parsedPage() == null
                || MediaWikiPageIds.isSyntheticPage(variant.pageId())) {
                continue;
            }
            pagesByGuideId.computeIfAbsent(variant.guideId(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(variant.pageId(), variant.parsedPage());
        }

        Map<ResourceLocation, MediaWikiListContext> contextsByGuideId = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> entry : pagesByGuideId.entrySet()) {
            MutableGuide guide = availableGuides.get(entry.getKey());
            if (guide == null) {
                continue;
            }

            List<ParsedGuidePage> pages = new ArrayList<>(
                entry.getValue()
                    .values());
            Map<ResourceLocation, PageCollection> pageCollectionsByPageId = new LinkedHashMap<>(pages.size());
            for (ParsedGuidePage page : pages) {
                pageCollectionsByPageId.put(page.getId(), guide);
            }

            List<ParsedGuidePage> categoryIndexedPages = new ArrayList<>(pages);
            categoryIndexedPages.removeIf(
                page -> !NavigationTree.areModRequirementsMet(
                    page.getFrontmatter()
                        .navigationEntry()));

            var categoryIndex = new CategoryIndex();
            categoryIndex.rebuild(categoryIndexedPages);
            contextsByGuideId.put(
                entry.getKey(),
                MediaWikiListContext.create(
                    guide,
                    pages,
                    NavigationTree.buildMergedPages(pageCollectionsByPageId, pages),
                    categoryIndex));
        }
        return contextsByGuideId;
    }

    private Map<ResourceLocation, Guide> buildScopedGuidesByGuideId(Map<ResourceLocation, MutableGuide> availableGuides,
        Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> scopedPagesByGuideId,
        Map<ResourceLocation, MediaWikiListContext> mediaWikiContextsByGuideId) {
        Map<ResourceLocation, Guide> scopedGuidesByGuideId = new LinkedHashMap<>(scopedPagesByGuideId.size());
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> entry : scopedPagesByGuideId
            .entrySet()) {
            ResourceLocation guideId = entry.getKey();
            MutableGuide guide = availableGuides.get(guideId);
            if (guide == null) {
                continue;
            }

            Map<Class<?>, PageIndex> indexOverrides = new LinkedHashMap<>();
            MediaWikiListContext mediaWikiContext = mediaWikiContextsByGuideId.get(guideId);
            if (mediaWikiContext != null) {
                indexOverrides.put(CategoryIndex.class, mediaWikiContext.categoryIndex());
            }
            scopedGuidesByGuideId.put(
                guideId,
                new GuideSiteScopedGuide(
                    guide,
                    entry.getValue(),
                    mediaWikiContext != null ? mediaWikiContext.navigationTree() : new NavigationTree(),
                    indexOverrides,
                    mediaWikiContext));
        }
        return scopedGuidesByGuideId;
    }

    private Map<ResourceLocation, List<GuideSiteLanguageLink>> buildLanguageLinks(GuideSiteWriter writer,
        MutableGuide guide, List<GuideSitePageVariant> variants, List<String> languageOrder) {
        if (variants.isEmpty()) {
            return Map.of();
        }

        Map<ResourceLocation, Map<String, GuideSitePageVariant>> variantsByPageId = new LinkedHashMap<>();
        for (GuideSitePageVariant variant : variants) {
            variantsByPageId.computeIfAbsent(variant.pageId(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(variant.language(), variant);
        }

        Map<ResourceLocation, List<GuideSiteLanguageLink>> linksByPageId = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<String, GuideSitePageVariant>> entry : variantsByPageId.entrySet()) {
            List<GuideSiteLanguageLink> links = new ArrayList<>();
            for (String language : languageOrder) {
                GuideSitePageVariant variant = entry.getValue()
                    .get(language);
                if (variant == null) {
                    continue;
                }
                links.add(
                    new GuideSiteLanguageLink(
                        language,
                        writer.pageUrl(
                            guide.getId()
                                .getResourceDomain(),
                            guide.getId()
                                .getResourcePath(),
                            language,
                            toOutputPageFile(variant.parsedPage())),
                        variant.fallbackUsed(),
                        variant.sourceLanguage()));
            }
            linksByPageId.put(entry.getKey(), links);
        }
        return linksByPageId;
    }

    private void appendSearchIconData(Map<String, Object> searchEntry, NavigationNode node,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        if (node == null) {
            return;
        }

        GuidePageIcon icon = node.icon();
        if (icon == null) {
            return;
        }

        if (icon.isItemIcon() && icon.itemStack() != null) {
            String iconUrl = GuideSitePageAssetExporter
                .toRootRelativePath(itemIconResolver.exportIcon(icon.itemStack()));
            if (!iconUrl.isEmpty()) {
                searchEntry.put("iconUrl", iconUrl);
                searchEntry.put("iconKind", "item");
            }
            return;
        }

        ResourceLocation textureId = icon.resolveCurrentTextureId();
        if (textureId == null && icon.resolveCurrentTexture() != null) {
            textureId = icon.resolveCurrentTexture()
                .getSourceId();
        }
        if (textureId != null) {
            GuideSitePageAssetExporter resolvedAssetExporter = assetExporter;
            if (node.guideId() != null && assetExportersByGuideId != null) {
                resolvedAssetExporter = assetExportersByGuideId.getOrDefault(node.guideId(), assetExporter);
            }
            String iconUrl = GuideSitePageAssetExporter
                .toRootRelativePath(resolvedAssetExporter.exportResource(textureId));
            if (!iconUrl.isEmpty()) {
                searchEntry.put("iconUrl", iconUrl);
                searchEntry.put("iconKind", "texture");
            }
        }
    }

    private IResourceManager resolveResourceManager() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null ? minecraft.getResourceManager() : null;
    }

    private GuideSitePageAssetExporter createPageAssetExporter(MutableGuide guide, IResourceManager resourceManager,
        String language, GuideSiteAssetRegistry assets) {
        return new GuideSitePageAssetExporter(
            assets,
            assetId -> loadGuideAsset(guide, resourceManager, language, assetId));
    }

    private GuideSiteHtmlCompiler createHtmlCompiler(GuideSiteAssetRegistry assets,
        GuideSitePageAssetExporter assetExporter, GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer, GuideSiteItemIconResolver itemIconResolver) {
        return new GuideSiteHtmlCompiler(
            recipeTagRenderer,
            assetExporter::resolveImageSrc,
            mdxTagRenderer,
            new GuideSiteLatexExporter(assets),
            assetExporter,
            itemIconResolver);
    }

    private byte[] loadGuideAsset(MutableGuide guide, IResourceManager resourceManager, String language,
        ResourceLocation assetId) throws IOException {
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        String defaultLanguage = LangUtil.normalizeLanguage(guide.getDefaultLanguage());

        byte[] content = loadGuideAssetVariant(
            guide,
            resourceManager,
            LangUtil.getTranslatedAsset(assetId, normalizedLanguage));
        if (content != null) {
            return content;
        }

        if (!normalizedLanguage.equals(defaultLanguage)) {
            content = loadGuideAssetVariant(
                guide,
                resourceManager,
                LangUtil.getTranslatedAsset(assetId, defaultLanguage));
            if (content != null) {
                return content;
            }
        }

        return loadGuideAssetVariant(guide, resourceManager, assetId);
    }

    private byte[] loadGuideAssetVariant(MutableGuide guide, IResourceManager resourceManager, ResourceLocation assetId)
        throws IOException {
        Path developmentPath = guide.getDevelopmentSourcePath(assetId);
        if (developmentPath != null && Files.exists(developmentPath)) {
            return Files.readAllBytes(developmentPath);
        }

        ResourceLocation actualResource = new ResourceLocation(
            assetId.getResourceDomain(),
            guide.getContentRootFolder() + "/" + assetId.getResourcePath());
        byte[] bytes = GuideResourceAccess.readBytes(resourceManager, actualResource);
        if (bytes != null) {
            return bytes;
        }
        // Fallback: try the raw resource path. This covers navigation icon textures that live
        // under assets/<namespace>/textures/... rather than under the guide content root.
        return GuideResourceAccess.readBytes(resourceManager, assetId);
    }

    private static String toOutputPageFile(ParsedGuidePage parsedPage) {
        String path = parsedPage.getId()
            .getResourcePath();
        if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path + ".html";
    }

    private List<GuideSiteExportedScene> exportScenes(MutableGuide guide, ParsedGuidePage parsedPage,
        GuidePage compiledPage, GuideSiteTemplateRegistry templates, GuideSiteAssetRegistry assets,
        GuideSiteSceneRuntimeExporter exporter, GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        GuideSiteCollectedScenes collectedScenes = GuideSiteSceneCollector.collect(compiledPage);
        IdentityHashMap<LytGuidebookScene, GuideSiteExportedScene> exportedScenesByScene = new IdentityHashMap<>();
        ArrayList<LytGuidebookScene> exportOrder = new ArrayList<>(collectedScenes.uniqueScenes());
        Collections.reverse(exportOrder);

        for (LytGuidebookScene scene : exportOrder) {
            try (
                GuideSiteSceneAnnotationSerializer.ExportedSceneLookupScope ignored = GuideSiteSceneAnnotationSerializer
                    .pushExportedSceneLookup(exportedScenesByScene)) {
                GuideSiteExportedScene exportedScene = exportScene(
                    parsedPage,
                    scene,
                    templates,
                    assets,
                    exporter,
                    assetExporter,
                    itemIconResolver);
                if (exportedScene != null) {
                    exportedScenesByScene.put(scene, exportedScene);
                }
            } catch (Throwable t) {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [GuideSiteExportTask] Failed to export scene for page {} in guide {}",
                        parsedPage.getId(),
                        guide.getId(),
                        t);
            }
        }

        List<GuideSiteExportedScene> htmlScenes = new ArrayList<>(
            collectedScenes.htmlSceneSequence()
                .size());
        for (LytGuidebookScene scene : collectedScenes.htmlSceneSequence()) {
            htmlScenes.add(exportedScenesByScene.get(scene));
        }
        return htmlScenes;
    }

    private GuideSiteExportedScene exportScene(ParsedGuidePage parsedPage, LytGuidebookScene scene,
        GuideSiteTemplateRegistry templates, GuideSiteAssetRegistry assets, GuideSiteSceneRuntimeExporter exporter,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) throws Exception {
        GuideSiteExportedScene baseScene = exportSceneState(
            parsedPage,
            scene,
            templates,
            exporter,
            assetExporter,
            itemIconResolver);
        if (baseScene == null) {
            return null;
        }

        String manifestPath = exportSceneStateManifest(
            parsedPage,
            scene,
            templates,
            assets,
            exporter,
            assetExporter,
            itemIconResolver,
            baseScene);
        return new GuideSiteExportedScene(
            baseScene.placeholderPath(),
            baseScene.scenePath(),
            baseScene.logicalWidth(),
            baseScene.logicalHeight(),
            baseScene.inWorldJson(),
            baseScene.overlayJson(),
            baseScene.hoverTargetsJson(),
            baseScene.sceneSoundsJson(),
            manifestPath,
            renderBlockStatsHtml(scene, itemIconResolver),
            blockStatsLayoutClass(scene),
            blockStatsLayoutStyle(scene),
            scene.isGridButtonEnabled(),
            scene.isGridVisible(),
            buildGridAnnotationJson(scene),
            scene.shouldExportBlockStats() && blockStatsButtonEnabled(scene),
            scene.isBlockStatsVisible());
    }

    private GuideSiteExportedScene exportSceneState(ParsedGuidePage parsedPage, LytGuidebookScene scene,
        GuideSiteTemplateRegistry templates, GuideSiteSceneRuntimeExporter exporter,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) throws Exception {
        scene.getLevel()
            .prepareForPreview();
        GuideSiteSceneAnnotationSerializer.AnnotationPayload annotationPayload = GuideSiteSceneAnnotationSerializer
            .serialize(scene, templates, parsedPage.getId(), assetExporter, itemIconResolver);
        String hoverTargetsJson = GuideSiteSceneHoverTargetSerializer
            .serialize(scene, templates, parsedPage.getId(), assetExporter, itemIconResolver);
        String sceneSoundsJson = serializeSceneSounds(scene, parsedPage.getId(), assetExporter);
        GuideSiteExportedScene runtimeExport = exporter.exportScene(scene);
        return new GuideSiteExportedScene(
            runtimeExport.placeholderPath(),
            runtimeExport.scenePath(),
            runtimeExport.logicalWidth(),
            runtimeExport.logicalHeight(),
            annotationPayload.inWorldJson(),
            annotationPayload.overlayJson(),
            hoverTargetsJson,
            sceneSoundsJson,
            null);
    }

    private String serializeSceneSounds(LytGuidebookScene scene, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        if (scene == null || scene.getSoundCues()
            .isEmpty()) {
            return "[]";
        }
        ArrayList<Map<String, Object>> sounds = new ArrayList<>();
        for (SceneSoundCue cue : scene.getSoundCues()) {
            if (cue == null || cue.getSound() == null
                || !scene.isStructureLibConditionSatisfied(cue.getStructureLibCondition())) {
                continue;
            }
            GuideSoundSpec sound = cue.getSound();
            LinkedHashMap<String, Object> data = new LinkedHashMap<>();
            data.put(
                "sound",
                sound.soundId()
                    .toString());
            data.put(
                "src",
                GuideSiteSoundExport
                    .exportSource(sound, name -> "src".equals(name) ? null : null, currentPageId, assetExporter));
            data.put(
                "trigger",
                cue.getTrigger()
                    .name()
                    .toLowerCase(Locale.ROOT));
            data.put("volume", sound.volume());
            data.put("pitch", sound.pitch());
            data.put("cooldown", sound.cooldownMillis());
            data.put("radius", sound.radius());
            data.put("minVolume", sound.minVolume());
            if (sound.hasPosition()) {
                data.put("x", sound.x());
                data.put("y", sound.y());
                data.put("z", sound.z());
            }
            sounds.add(data);
        }
        return GSON.toJson(sounds);
    }

    private String renderBlockStatsHtml(LytGuidebookScene scene, GuideSiteItemIconResolver itemIconResolver) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-scene-block-stats\" data-scene-block-stats>");
        for (SceneBlockStatsEntry entry : scene.getBlockStatsEntriesForExport()) {
            ItemStack displayStack = entry != null ? entry.copyDisplayStack() : null;
            if (displayStack == null) {
                continue;
            }
            GuideSiteExportedItem item = GuideSiteItemSupport.export(displayStack, itemIconResolver);
            html.append("<div class=\"guide-scene-block-stat\" data-block-stat-key=\"")
                .append(escapeAttribute(entry.getKey()))
                .append("\">");
            GuideSiteItemHtml.appendIcon(html, item, "guide-scene-block-stat-icon", 0.75f, true);
            html.append("<span class=\"guide-scene-block-stat-label\">")
                .append(escapeHtml(entry.getLabel()))
                .append("</span><span class=\"guide-scene-block-stat-count\">x")
                .append(entry.getCount())
                .append("</span></div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String blockStatsLayoutClass(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        BlockStatsLayoutState state = scene.getBlockStatsLayoutStateForExport();
        StringBuilder cssClass = new StringBuilder("guide-scene-export-frame--block-stats");
        cssClass.append(" guide-scene-export-frame--block-stats-")
            .append(
                state.dock()
                    .name()
                    .toLowerCase(Locale.ROOT));
        cssClass.append(" guide-scene-export-frame--block-stats-corner-")
            .append(
                state.corner()
                    .name()
                    .toLowerCase(Locale.ROOT)
                    .replace('_', '-'));
        cssClass.append(" guide-scene-export-frame--block-stats-mode-")
            .append(
                state.mode()
                    .name()
                    .toLowerCase(Locale.ROOT));
        if (!state.visible()) {
            cssClass.append(" guide-scene-export-frame--block-stats-hidden");
        }
        cssClass.append(
            state.showNames() ? " guide-scene-export-frame--block-stats-names"
                : " guide-scene-export-frame--block-stats-icons");
        return cssClass.toString();
    }

    private String blockStatsLayoutStyle(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return null;
        }
        BlockStatsLayoutState state = scene.getBlockStatsLayoutStateForExport();
        StringBuilder style = new StringBuilder();
        appendCssPx(style, "--guide-scene-block-stats-max-width", state.maxWidth());
        appendCssPx(style, "--guide-scene-block-stats-max-height", state.maxHeight());
        appendCssPx(style, "--guide-scene-logical-width", state.sceneWidth());
        appendCssPx(style, "--guide-scene-logical-height", state.sceneHeight());
        appendCssPx(style, "--guide-scene-button-reserve", state.buttonColumnReserve());
        return style.toString();
    }

    private void appendCssPx(StringBuilder style, String property, int value) {
        if (style.length() > 0) {
            style.append(';');
        }
        style.append(property)
            .append(':')
            .append(Math.max(0, value))
            .append("px");
    }

    private boolean blockStatsButtonEnabled(LytGuidebookScene scene) {
        if (scene == null || !scene.shouldExportBlockStats()) {
            return false;
        }
        return scene.getSceneButtonColumnReserveForExport() > 0;
    }

    private String buildGridAnnotationJson(LytGuidebookScene scene) {
        if (scene == null || scene.getLevel() == null
            || scene.getLevel()
                .isEmpty()) {
            return null;
        }
        int[] bounds = scene.getLevel()
            .getBounds();
        ArrayList<Map<String, Object>> annotations = new ArrayList<>(bounds[3] - bounds[0] + bounds[5] - bounds[2] + 8);
        float half = 0.02f;
        float y = 0.002f;
        int minX = bounds[0] - 1;
        int minZ = bounds[2] - 1;
        int maxX = bounds[3] + 2;
        int maxZ = bounds[5] + 2;
        for (int x = minX; x <= maxX; x++) {
            annotations.add(buildGridLine(x - half, y, minZ, x + half, y, maxZ));
        }
        for (int z = minZ; z <= maxZ; z++) {
            annotations.add(buildGridLine(minX, y, z - half, maxX, y, z + half));
        }
        return GSON.toJson(annotations);
    }

    private Map<String, Object> buildGridLine(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("type", "box");
        data.put("minCorner", new float[] { Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ) });
        data.put("maxCorner", new float[] { Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ) });
        data.put("color", "rgba(255,255,255,0.33333334)");
        data.put("thickness", 0.00390625f);
        data.put("alwaysOnTop", false);
        return data;
    }

    private String escapeAttribute(String text) {
        return escapeHtml(text);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private String exportSceneStateManifest(ParsedGuidePage parsedPage, LytGuidebookScene scene,
        GuideSiteTemplateRegistry templates, GuideSiteAssetRegistry assets, GuideSiteSceneRuntimeExporter exporter,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        GuideSiteExportedScene baseScene) throws Exception {
        SceneStateManifestPlan plan = buildSceneStateManifestPlan(scene);
        if (plan == null || (plan.states.size() <= 1 && !plan.controls.containsKey("ponder"))) {
            return null;
        }

        SceneVariantState initialState = SceneVariantState.capture(scene, plan.structurePlans);
        LinkedHashMap<String, Object> serializedStates = new LinkedHashMap<>(plan.states.size());

        try {
            for (SceneVariantState state : plan.states) {
                GuideSiteExportedScene exportedVariant;
                if (state.equals(initialState)) {
                    exportedVariant = baseScene;
                } else {
                    if (scene.hasPonderData()) {
                        applySceneVariantState(scene, initialState, plan.structurePlans);
                    }
                    applySceneVariantState(scene, state, plan.structurePlans);
                    exportedVariant = exportSceneState(
                        parsedPage,
                        scene,
                        templates,
                        exporter,
                        assetExporter,
                        itemIconResolver);
                }
                if (exportedVariant == null) {
                    return null;
                }
                serializedStates.put(state.key(), serializeSceneVariant(exportedVariant));
            }
        } finally {
            applySceneVariantState(scene, initialState, plan.structurePlans);
        }

        LinkedHashMap<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("initialState", initialState.toMap());
        manifest.put("controls", plan.controls);
        manifest.put("states", serializedStates);
        return assets.writeShared(
            "scene-manifests",
            ".json",
            GSON.toJson(manifest)
                .getBytes(StandardCharsets.UTF_8));
    }

    private SceneStateManifestPlan buildSceneStateManifestPlan(LytGuidebookScene scene) {
        if (scene == null || (!scene.isInteractive() && !scene.hasPonderData())) {
            return null;
        }

        List<Integer> visibleLayers = buildVisibleLayerStates(scene);
        List<Integer> ponderTicks = buildPonderTickStates(scene);
        List<StructureStatePlan> structurePlans = buildStructureStatePlans(scene);

        long variantCount = (long) visibleLayers.size() * (long) ponderTicks.size();
        for (StructureStatePlan structurePlan : structurePlans) {
            variantCount *= structurePlan.states.size();
            if (variantCount > MAX_SCENE_STATE_VARIANTS) {
                warnSceneStateVariantLimit(variantCount);
                return null;
            }
        }
        if (variantCount > MAX_SCENE_STATE_VARIANTS) {
            warnSceneStateVariantLimit(variantCount);
            return null;
        }
        if (variantCount <= 1 && !scene.hasPonderData()) {
            return null;
        }

        LinkedHashMap<String, Object> controls = new LinkedHashMap<>();
        if (visibleLayers.size() > 1) {
            LinkedHashMap<String, Object> visibleLayerControl = new LinkedHashMap<>();
            visibleLayerControl.put("label", GuidebookText.SceneVisibleLayerLabel.text());
            visibleLayerControl.put("allLabel", GuidebookText.SceneAll.text());
            visibleLayerControl.put("max", visibleLayers.get(visibleLayers.size() - 1));
            controls.put("visibleLayer", visibleLayerControl);
        }
        if (scene.hasPonderData() && !ponderTicks.isEmpty()) {
            LinkedHashMap<String, Object> ponderControl = new LinkedHashMap<>();
            ponderControl.put("label", "Ponder");
            ponderControl.put("timeLabel", "Tick");
            ponderControl.put("previousLabel", GuidebookText.PonderPrevKeyframe.text());
            ponderControl.put("playPauseLabel", GuidebookText.PonderPlayPause.text());
            ponderControl.put("restartLabel", GuidebookText.PonderRestart.text());
            ponderControl.put("totalTime", scene.getPonderTotalTimeForExport());
            ponderControl.put("ticks", new ArrayList<>(ponderTicks));
            ArrayList<Map<String, Object>> keyframes = new ArrayList<>();
            for (PonderTimelineKeyframe keyframe : scene.getPonderTimelineKeyframesForExport()) {
                LinkedHashMap<String, Object> serializedKeyframe = new LinkedHashMap<>();
                serializedKeyframe.put("time", keyframe.getTime());
                serializedKeyframe.put("label", keyframe.getLabel());
                keyframes.add(serializedKeyframe);
            }
            ponderControl.put("keyframes", keyframes);
            controls.put("ponder", ponderControl);
        }
        ArrayList<Map<String, Object>> structureControls = new ArrayList<>();
        for (StructureStatePlan structurePlan : structurePlans) {
            if (structurePlan.hasControls()) {
                structureControls.add(structurePlan.toControlMap());
            }
        }
        if (!structureControls.isEmpty()) {
            controls.put("structures", structureControls);
        }

        ArrayList<SceneVariantState> states = new ArrayList<>((int) variantCount);
        appendSceneVariantStates(states, visibleLayers, ponderTicks, structurePlans, 0, new LinkedHashMap<>());
        return new SceneStateManifestPlan(states, structurePlans, controls);
    }

    private void appendSceneVariantStates(List<SceneVariantState> states, List<Integer> visibleLayers,
        List<Integer> ponderTicks, List<StructureStatePlan> structurePlans, int structureIndex,
        LinkedHashMap<String, StructureVariantState> currentStructures) {
        if (structureIndex >= structurePlans.size()) {
            for (Integer visibleLayer : visibleLayers) {
                for (Integer ponderTick : ponderTicks) {
                    states.add(new SceneVariantState(visibleLayer, ponderTick, currentStructures));
                }
            }
            return;
        }

        StructureStatePlan structurePlan = structurePlans.get(structureIndex);
        for (StructureVariantState structureState : structurePlan.states) {
            currentStructures.put(structurePlan.bindingKey, structureState);
            appendSceneVariantStates(
                states,
                visibleLayers,
                ponderTicks,
                structurePlans,
                structureIndex + 1,
                currentStructures);
            currentStructures.remove(structurePlan.bindingKey);
        }
    }

    private void applySceneVariantState(LytGuidebookScene scene, SceneVariantState state,
        List<StructureStatePlan> structurePlans) {
        if (scene == null || state == null) {
            return;
        }
        boolean structureSelectionChanged = false;
        String structureToNotify = null;
        for (StructureStatePlan structurePlan : structurePlans) {
            StructureVariantState structureState = state.structures.get(structurePlan.bindingKey);
            if (structureState == null) {
                continue;
            }
            boolean changed = scene.applyStructureLibPreviewSelection(
                structurePlan.structureName,
                new StructureLibPreviewSelection(structureState.tier, structureState.channels),
                false);
            if (changed) {
                structureSelectionChanged = true;
                structureToNotify = structurePlan.structureName;
            }
        }
        if (structureSelectionChanged) {
            scene.notifyStructureLibSelectionChanged(structureToNotify);
        }
        if (scene.hasPonderData()) {
            if (state.exportState) {
                scene.seekToTickForExport(state.ponderTick);
            } else {
                scene.restorePonderTickAfterExport(state.ponderTick);
            }
        }
        scene.setVisibleLayer(state.visibleLayer);
    }

    private Map<String, Object> serializeSceneVariant(GuideSiteExportedScene exportedScene) {
        LinkedHashMap<String, Object> serialized = new LinkedHashMap<>();
        serialized
            .put("placeholderSrc", GuideSitePageAssetExporter.toRootRelativePath(exportedScene.placeholderPath()));
        serialized.put("sceneSrc", GuideSitePageAssetExporter.toRootRelativePath(exportedScene.scenePath()));
        serialized
            .put("inWorldAnnotationsJson", exportedScene.inWorldJson() != null ? exportedScene.inWorldJson() : "[]");
        serialized
            .put("overlayAnnotationsJson", exportedScene.overlayJson() != null ? exportedScene.overlayJson() : "[]");
        serialized
            .put("sceneSoundsJson", exportedScene.sceneSoundsJson() != null ? exportedScene.sceneSoundsJson() : "[]");
        serialized.put(
            "hoverTargetsJson",
            exportedScene.hoverTargetsJson() != null ? exportedScene.hoverTargetsJson() : "[]");
        return serialized;
    }

    private List<Integer> buildVisibleLayerStates(LytGuidebookScene scene) {
        if (scene == null || !scene.isVisibleLayerSliderEnabled()
            || scene.getLevel() == null
            || scene.getLevel()
                .isEmpty()) {
            return List.of(scene != null ? scene.getCurrentVisibleLayer() : 0);
        }

        int[] bounds = scene.getLevel()
            .getBounds();
        int layerCount = Math.max(1, bounds[4] - bounds[1] + 1);
        ArrayList<Integer> layers = new ArrayList<>(layerCount + 1);
        layers.add(0);
        for (int layer = 1; layer <= layerCount; layer++) {
            layers.add(layer);
        }
        return layers;
    }

    private List<Integer> buildPonderTickStates(LytGuidebookScene scene) {
        if (scene == null || !scene.hasPonderData()) {
            return List.of(0);
        }
        ArrayList<Integer> states = new ArrayList<>();
        if (options.exportPonderEveryTick()) {
            int totalTime = Math.max(0, scene.getPonderTotalTimeForExport());
            for (int tick = 0; tick <= totalTime; tick++) {
                states.add(tick);
            }
            return states.isEmpty() ? List.of(0) : states;
        }
        addUniquePonderTickState(states, scene.getPonderCurrentTickForExport());
        for (PonderTimelineKeyframe keyframe : scene.getPonderTimelineKeyframesForExport()) {
            addUniquePonderTickState(states, keyframe.getTime());
        }
        addUniquePonderTickState(states, scene.getPonderTotalTimeForExport());
        return states.isEmpty() ? List.of(0) : states;
    }

    private List<StructureStatePlan> buildStructureStatePlans(LytGuidebookScene scene) {
        if (scene == null || scene.getStructureLibBindings()
            .isEmpty()) {
            return List.of();
        }
        ArrayList<StructureStatePlan> plans = new ArrayList<>();
        for (StructureLibSceneBinding binding : scene.getStructureLibBindings()) {
            if (binding == null) {
                continue;
            }
            StructureLibSceneMetadata metadata = binding.getMetadata();
            List<Integer> tiers = buildTierStates(binding, metadata);
            List<StructureLibSceneMetadata.ChannelData> selectableChannels = buildSelectableChannels(metadata);
            ArrayList<String> channelIds = new ArrayList<>(selectableChannels.size());
            ArrayList<List<Integer>> channelValues = new ArrayList<>(selectableChannels.size());
            for (StructureLibSceneMetadata.ChannelData channelData : selectableChannels) {
                channelIds.add(channelData.getChannelId());
                channelValues.add(buildChannelStates(channelData));
            }
            plans.add(
                new StructureStatePlan(
                    binding.getBindingKey(),
                    binding.getName(),
                    buildStructureControlLabel(binding, metadata),
                    tiers,
                    selectableChannels,
                    channelIds,
                    buildStructureVariantStates(tiers, channelIds, channelValues)));
        }
        return plans;
    }

    private String buildStructureControlLabel(StructureLibSceneBinding binding,
        @Nullable StructureLibSceneMetadata metadata) {
        if (binding != null && binding.getName() != null
            && !binding.getName()
                .trim()
                .isEmpty()) {
            return binding.getName();
        }
        if (metadata != null && metadata.getController() != null
            && !metadata.getController()
                .trim()
                .isEmpty()) {
            return metadata.getController();
        }
        return "Structure";
    }

    private List<StructureVariantState> buildStructureVariantStates(List<Integer> tiers, List<String> channelIds,
        List<List<Integer>> channelValues) {
        ArrayList<StructureVariantState> states = new ArrayList<>();
        appendStructureVariantStates(states, tiers, channelIds, channelValues, 0, new ArrayList<>());
        return states.isEmpty()
            ? List.of(new StructureVariantState(StructureLibPreviewSelection.DEFAULT_MASTER_TIER, null))
            : states;
    }

    private void appendStructureVariantStates(List<StructureVariantState> states, List<Integer> tiers,
        List<String> channelIds, List<List<Integer>> channelValues, int channelIndex, List<Integer> currentChannels) {
        if (channelIndex >= channelValues.size()) {
            for (Integer tier : tiers) {
                LinkedHashMap<String, Integer> channelState = new LinkedHashMap<>(channelIds.size());
                for (int i = 0; i < channelIds.size(); i++) {
                    channelState.put(channelIds.get(i), currentChannels.get(i));
                }
                states.add(new StructureVariantState(tier, channelState));
            }
            return;
        }
        for (Integer value : channelValues.get(channelIndex)) {
            currentChannels.add(value);
            appendStructureVariantStates(states, tiers, channelIds, channelValues, channelIndex + 1, currentChannels);
            currentChannels.remove(currentChannels.size() - 1);
        }
    }

    private void addUniquePonderTickState(List<Integer> states, int tick) {
        int normalized = Math.max(0, tick);
        if (!states.contains(normalized)) {
            states.add(normalized);
        }
    }

    private void warnSceneStateVariantLimit(long variantCount) {
        FMLLog.getLogger()
            .warn(
                "[GuideNH] [GuideSiteExportTask] Skipping scene state manifest export because {} variants exceed limit {}.",
                variantCount,
                MAX_SCENE_STATE_VARIANTS);
    }

    private List<Integer> buildTierStates(StructureLibSceneBinding binding, StructureLibSceneMetadata metadata) {
        if (binding == null || metadata == null
            || metadata.getTierData() == null
            || !metadata.getTierData()
                .isSelectable()) {
            return List
                .of(binding != null ? binding.getCurrentTier() : StructureLibPreviewSelection.DEFAULT_MASTER_TIER);
        }
        ArrayList<Integer> tiers = new ArrayList<>();
        for (int tier = metadata.getTierData()
            .getMinValue(); tier <= metadata.getTierData()
                .getMaxValue(); tier++) {
            tiers.add(tier);
        }
        return tiers;
    }

    private List<StructureLibSceneMetadata.ChannelData> buildSelectableChannels(StructureLibSceneMetadata metadata) {
        if (metadata == null) {
            return List.of();
        }
        ArrayList<StructureLibSceneMetadata.ChannelData> channels = new ArrayList<>();
        for (StructureLibSceneMetadata.ChannelData channelData : metadata.getChannelDataList()) {
            if (channelData != null && channelData.isSelectable()) {
                channels.add(channelData);
            }
        }
        return channels;
    }

    private List<Integer> buildChannelStates(StructureLibSceneMetadata.ChannelData channelData) {
        if (channelData == null || !channelData.isSelectable()) {
            return List.of(0);
        }
        ArrayList<Integer> states = new ArrayList<>(channelData.getMaxValue() + 1);
        for (int value = channelData.getMinValue(); value <= channelData.getMaxValue(); value++) {
            states.add(value);
        }
        return states;
    }

    private GuideSiteHtmlCompiler.SceneResolver createSceneResolver(List<GuideSiteExportedScene> exportedScenes) {
        return new GuideSiteHtmlCompiler.SceneResolver() {

            private int index;

            @Override
            public GuideSiteExportedScene nextScene() {
                if (index >= exportedScenes.size()) {
                    return null;
                }
                return exportedScenes.get(index++);
            }
        };
    }

    private static final class SceneStateManifestPlan {

        private final List<SceneVariantState> states;
        private final List<StructureStatePlan> structurePlans;
        private final Map<String, Object> controls;

        private SceneStateManifestPlan(List<SceneVariantState> states, List<StructureStatePlan> structurePlans,
            Map<String, Object> controls) {
            this.states = states;
            this.structurePlans = structurePlans;
            this.controls = controls;
        }
    }

    private static final class SceneVariantState {

        private final int visibleLayer;
        private final int ponderTick;
        private final LinkedHashMap<String, StructureVariantState> structures;
        private final boolean exportState;

        private SceneVariantState(int visibleLayer, int ponderTick,
            LinkedHashMap<String, StructureVariantState> structures) {
            this(visibleLayer, ponderTick, structures, true);
        }

        private SceneVariantState(int visibleLayer, int ponderTick,
            LinkedHashMap<String, StructureVariantState> structures, boolean exportState) {
            this.visibleLayer = Math.max(0, visibleLayer);
            this.ponderTick = Math.max(0, ponderTick);
            this.structures = new LinkedHashMap<>();
            if (structures != null) {
                for (Map.Entry<String, StructureVariantState> entry : structures.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        this.structures.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            this.exportState = exportState;
        }

        private static SceneVariantState capture(LytGuidebookScene scene, List<StructureStatePlan> structurePlans) {
            LinkedHashMap<String, StructureVariantState> structures = new LinkedHashMap<>();
            if (scene != null && structurePlans != null) {
                for (StructureStatePlan structurePlan : structurePlans) {
                    StructureLibSceneBinding binding = scene.resolveStructureLibBinding(structurePlan.structureName);
                    LinkedHashMap<String, Integer> channels = new LinkedHashMap<>();
                    for (String channelId : structurePlan.channelIds) {
                        channels.put(channelId, binding != null ? binding.getChannelValue(channelId) : 0);
                    }
                    structures.put(
                        structurePlan.bindingKey,
                        new StructureVariantState(
                            binding != null ? binding.getCurrentTier()
                                : StructureLibPreviewSelection.DEFAULT_MASTER_TIER,
                            channels));
                }
            }
            return new SceneVariantState(
                scene != null ? scene.getCurrentVisibleLayer() : 0,
                scene != null ? scene.getPonderCurrentTickForExport() : 0,
                structures,
                false);
        }

        private String key() {
            StringBuilder key = new StringBuilder();
            key.append("layer=")
                .append(visibleLayer)
                .append("|ponder=")
                .append(ponderTick);
            for (Map.Entry<String, StructureVariantState> structureEntry : structures.entrySet()) {
                key.append("|structure:")
                    .append(structureEntry.getKey())
                    .append("|tier=")
                    .append(structureEntry.getValue().tier);
                for (Map.Entry<String, Integer> channelEntry : structureEntry.getValue().channels.entrySet()) {
                    key.append("|channel:")
                        .append(channelEntry.getKey())
                        .append("=")
                        .append(channelEntry.getValue());
                }
            }
            return key.toString();
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("visibleLayer", visibleLayer);
            state.put("ponderTick", ponderTick);
            LinkedHashMap<String, Object> serializedStructures = new LinkedHashMap<>(structures.size());
            for (Map.Entry<String, StructureVariantState> entry : structures.entrySet()) {
                serializedStructures.put(
                    entry.getKey(),
                    entry.getValue()
                        .toMap());
            }
            state.put("structures", serializedStructures);
            return state;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SceneVariantState other)) {
                return false;
            }
            return visibleLayer == other.visibleLayer && ponderTick == other.ponderTick
                && structures.equals(other.structures);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * visibleLayer + ponderTick) + structures.hashCode();
        }
    }

    private static final class StructureStatePlan {

        private final String bindingKey;
        @Nullable
        private final String structureName;
        private final String label;
        private final List<Integer> tiers;
        private final List<StructureLibSceneMetadata.ChannelData> selectableChannels;
        private final List<String> channelIds;
        private final List<StructureVariantState> states;

        private StructureStatePlan(String bindingKey, @Nullable String structureName, String label, List<Integer> tiers,
            List<StructureLibSceneMetadata.ChannelData> selectableChannels, List<String> channelIds,
            List<StructureVariantState> states) {
            this.bindingKey = bindingKey;
            this.structureName = structureName;
            this.label = label;
            this.tiers = tiers != null ? new ArrayList<>(tiers) : List.of();
            this.selectableChannels = selectableChannels != null ? new ArrayList<>(selectableChannels) : List.of();
            this.channelIds = channelIds != null ? new ArrayList<>(channelIds) : List.of();
            this.states = states != null ? new ArrayList<>(states)
                : List.of(new StructureVariantState(StructureLibPreviewSelection.DEFAULT_MASTER_TIER, null));
        }

        private boolean hasControls() {
            return tiers.size() > 1 || !selectableChannels.isEmpty();
        }

        private Map<String, Object> toControlMap() {
            LinkedHashMap<String, Object> control = new LinkedHashMap<>();
            control.put("id", bindingKey);
            control.put("label", label);
            if (tiers.size() > 1) {
                LinkedHashMap<String, Object> tierControl = new LinkedHashMap<>();
                tierControl.put("label", GuidebookText.SceneStructureLibTierLabel.text());
                tierControl.put("min", tiers.get(0));
                tierControl.put("max", tiers.get(tiers.size() - 1));
                control.put("tier", tierControl);
            }
            if (!selectableChannels.isEmpty()) {
                ArrayList<Map<String, Object>> channelControls = new ArrayList<>(selectableChannels.size());
                for (StructureLibSceneMetadata.ChannelData channelData : selectableChannels) {
                    LinkedHashMap<String, Object> channelControl = new LinkedHashMap<>();
                    channelControl.put("id", channelData.getChannelId());
                    channelControl.put("label", channelData.getLabel());
                    channelControl.put("min", channelData.getMinValue());
                    channelControl.put("max", channelData.getMaxValue());
                    channelControl.put("unsetLabel", GuidebookText.SceneNotSet.text());
                    channelControls.add(channelControl);
                }
                control.put("channels", channelControls);
            }
            return control;
        }
    }

    private static final class StructureVariantState {

        private final int tier;
        private final LinkedHashMap<String, Integer> channels;

        private StructureVariantState(int tier, @Nullable Map<String, Integer> channels) {
            this.tier = Math.max(1, tier);
            this.channels = channels != null ? new LinkedHashMap<>(channels) : new LinkedHashMap<>();
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("tier", tier);
            state.put("channels", new LinkedHashMap<>(channels));
            return state;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StructureVariantState other)) {
                return false;
            }
            return tier == other.tier && channels.equals(other.channels);
        }

        @Override
        public int hashCode() {
            return 31 * tier + channels.hashCode();
        }
    }

    public static final class Result {

        private final int guidesExported;
        private final int pagesExported;
        private final int pagesFailed;
        private final Path outDir;

        public Result(int guidesExported, int pagesExported, int pagesFailed, Path outDir) {
            this.guidesExported = guidesExported;
            this.pagesExported = pagesExported;
            this.pagesFailed = pagesFailed;
            this.outDir = outDir;
        }

        public int guidesExported() {
            return guidesExported;
        }

        public int pagesExported() {
            return pagesExported;
        }

        public int pagesFailed() {
            return pagesFailed;
        }

        public Path outDir() {
            return outDir;
        }
    }

    @Desugar
    private record LanguageExportContext(Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, Map<ResourceLocation, ResourceLocation> guideIdsByPageId,
        Map<ResourceLocation, MediaWikiListContext> mediaWikiContextsByGuideId,
        Map<ResourceLocation, Guide> scopedGuidesByGuideId,
        Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {

        private static final LanguageExportContext EMPTY = new LanguageExportContext(
            Map.of(),
            new NavigationTree(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of());
    }
}
