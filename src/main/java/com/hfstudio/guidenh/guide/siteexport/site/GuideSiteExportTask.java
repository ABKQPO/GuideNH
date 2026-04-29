package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public class GuideSiteExportTask {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSiteExportTask.class);
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();

    private final Path outDir;

    public GuideSiteExportTask(Path outDir) {
        this.outDir = outDir;
    }

    public Result run() throws Exception {
        Files.createDirectories(outDir);

        GuideSiteWriter writer = new GuideSiteWriter();
        writer.cleanupGeneratedOutputs(outDir);
        GuideSiteSearchTextExtractor searchExtractor = new GuideSiteSearchTextExtractor();
        GuideSiteAssetRegistry assets = new GuideSiteAssetRegistry(outDir);
        GuideSiteItemIconExporter itemIconExporter = new GuideSiteItemIconExporter(assets);
        GuideSiteSceneRuntimeExporter sceneExporter = new GuideSiteSceneRuntimeExporter(assets);
        writer.writeBootstrapFiles(outDir);

        int guidesExported = 0;
        int pagesExported = 0;
        int pagesFailed = 0;
        String firstPageUrl = null;
        Map<String, List<Map<String, Object>>> searchEntriesByLanguage = new LinkedHashMap<>();
        IResourceManager resourceManager = null;

        for (MutableGuide guide : GuideRegistry.getAll()) {
            guidesExported++;

            if (resourceManager == null) {
                resourceManager = resolveResourceManager();
                if (resourceManager == null) {
                    throw new IllegalStateException("Minecraft resource manager is not available");
                }
            }

            GuideSitePageCollector collector = new GuideSitePageCollector(guide, resourceManager);
            List<GuideSitePageVariant> variants;
            try {
                variants = collector.collect(guide);
            } catch (Throwable t) {
                LOG.warn("Failed to collect page variants for guide {}", guide.getId(), t);
                pagesFailed++;
                continue;
            }

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
                GuideSitePageAssetExporter assetExporter = createPageAssetExporter(
                    guide,
                    resourceManager,
                    language,
                    assets);
                List<ParsedGuidePage> parsedPages = new ArrayList<>();
                Map<ResourceLocation, ParsedGuidePage> parsedPagesById = new LinkedHashMap<ResourceLocation, ParsedGuidePage>();
                for (GuideSitePageVariant variant : languageVariants) {
                    parsedPages.add(variant.parsedPage());
                    parsedPagesById.put(
                        variant.parsedPage()
                            .getId(),
                        variant.parsedPage());
                }

                NavigationTree navigationTree = NavigationTree.build(parsedPages);
                GuideSiteHtmlCompiler compiler = createHtmlCompiler(
                    assetExporter,
                    new GuideSiteRecipeTagRenderer(itemIconExporter),
                    new GuideSiteMdxTagRenderer(
                        guide,
                        parsedPagesById,
                        navigationTree,
                        assetExporter,
                        itemIconExporter));

                for (GuideSitePageVariant variant : languageVariants) {
                    try {
                        try (GuideSiteHrefResolver.ContextScope ignored = GuideSiteHrefResolver.exportContext(
                            guide.getId()
                                .getResourceDomain(),
                            guide.getId()
                                .getResourcePath(),
                            language)) {
                            GuideSiteTemplateRegistry templates = new GuideSiteTemplateRegistry();
                            GuidePage compiledPage = PageCompiler
                                .compile(guide, guide.getExtensions(), variant.parsedPage());
                            List<GuideSiteExportedScene> exportedScenes = exportScenes(
                                guide,
                                variant.parsedPage(),
                                compiledPage,
                                templates,
                                sceneExporter,
                                assetExporter,
                                itemIconExporter);
                            String body = compiler
                                .compileBody(variant.parsedPage(), templates, createSceneResolver(exportedScenes));
                            String sidebarHtml = writer.renderSidebar(
                                guide,
                                language,
                                navigationTree,
                                variant.pageId(),
                                assetExporter,
                                itemIconExporter,
                                languageLinksByPageId.get(variant.pageId()));
                            String pageFile = toOutputPageFile(variant.parsedPage());
                            String pageUrl = writer.pageUrl(
                                guide.getId()
                                    .getResourceDomain(),
                                guide.getId()
                                    .getResourcePath(),
                                language,
                                pageFile);
                            String pageTitle = searchExtractor.title(guide, variant.parsedPage());

                            writer.writePage(
                                outDir,
                                guide.getId()
                                    .getResourceDomain(),
                                guide.getId()
                                    .getResourcePath(),
                                language,
                                pageFile,
                                sidebarHtml,
                                body,
                                templates.renderAll(),
                                pageTitle);

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
                            searchEntry.put("text", searchExtractor.searchableText(guide, variant.parsedPage()));
                            appendSearchIconData(
                                searchEntry,
                                navigationTree.getNodeById(variant.pageId()),
                                assetExporter,
                                itemIconExporter);
                            searchEntriesByLanguage.computeIfAbsent(language, ignored2 -> new ArrayList<>())
                                .add(searchEntry);

                            if (firstPageUrl == null) {
                                firstPageUrl = pageUrl;
                            }
                        }
                        pagesExported++;
                    } catch (Throwable t) {
                        LOG.warn("Failed to export page {} for language {}", variant.pageId(), language, t);
                        pagesFailed++;
                    }
                }
            }
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : searchEntriesByLanguage.entrySet()) {
            writer.writeSearchIndex(outDir, entry.getKey(), GSON.toJson(entry.getValue()));
        }

        writer.writeLandingPage(outDir, firstPageUrl, "GuideNH Static Export");

        return new Result(guidesExported, pagesExported, pagesFailed, outDir);
    }

    private Map<ResourceLocation, List<GuideSiteLanguageLink>> buildLanguageLinks(GuideSiteWriter writer,
        MutableGuide guide, List<GuideSitePageVariant> variants, List<String> languageOrder) {
        if (variants.isEmpty()) {
            return Collections.emptyMap();
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
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
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

        if (icon.textureId() != null) {
            String iconUrl = GuideSitePageAssetExporter
                .toRootRelativePath(assetExporter.exportResource(icon.textureId()));
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
        return new GuideSitePageAssetExporter(assets, new GuideSitePageAssetExporter.AssetLoader() {

            @Override
            public byte[] load(ResourceLocation assetId) throws Exception {
                return loadGuideAsset(guide, resourceManager, language, assetId);
            }
        });
    }

    private GuideSiteHtmlCompiler createHtmlCompiler(GuideSitePageAssetExporter assetExporter,
        GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer) {
        return new GuideSiteHtmlCompiler(recipeTagRenderer, new GuideSiteHtmlCompiler.ImageResolver() {

            @Override
            public String resolve(String rawUrl, ResourceLocation currentPageId) {
                return assetExporter.resolveImageSrc(rawUrl, currentPageId);
            }
        }, mdxTagRenderer);
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
        return GuideResourceAccess.readBytes(resourceManager, actualResource);
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
        GuidePage compiledPage, GuideSiteTemplateRegistry templates, GuideSiteSceneRuntimeExporter exporter,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        List<GuideSiteExportedScene> scenes = new ArrayList<GuideSiteExportedScene>();
        for (LytGuidebookScene scene : compiledPage.scenes()) {
            try {
                GuideSiteSceneAnnotationSerializer.AnnotationPayload annotationPayload = GuideSiteSceneAnnotationSerializer
                    .serialize(scene, templates, parsedPage.getId(), assetExporter, itemIconResolver);
                String hoverTargetsJson = GuideSiteSceneHoverTargetSerializer
                    .serialize(scene, templates, parsedPage.getId(), assetExporter, itemIconResolver);
                GuideSiteExportedScene exportedScene = exporter.exportScene(scene);
                scenes.add(
                    new GuideSiteExportedScene(
                        exportedScene.placeholderPath(),
                        exportedScene.scenePath(),
                        annotationPayload.inWorldJson(),
                        annotationPayload.overlayJson(),
                        hoverTargetsJson));
            } catch (Throwable t) {
                LOG.warn("Failed to export scene for page {} in guide {}", parsedPage.getId(), guide.getId(), t);
                scenes.add(null);
            }
        }
        return scenes;
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
}
