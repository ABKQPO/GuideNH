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
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.compat.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.compat.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
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
    private static final int MAX_SCENE_STATE_VARIANTS = 256;

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
                    recordFailure(outDir, "collect " + guide.getId(), t);
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
                    // Switch the active Minecraft locale so localized item display names and tooltips
                    // resolve to this language while we render this language's pages.
                    switchMinecraftLanguage(language);
                    GuideSitePageAssetExporter assetExporter = createPageAssetExporter(
                        guide,
                        resourceManager,
                        language,
                        assets);
                    List<ParsedGuidePage> parsedPages = new ArrayList<>();
                    Map<ResourceLocation, ParsedGuidePage> parsedPagesById = new LinkedHashMap<>();
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
                                    navigationTree,
                                    variant.pageId(),
                                    assetExporter,
                                    itemIconExporter,
                                    langLinks);
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
                                    langSwitcherHtml,
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
                LOG.warn("Cannot switch Minecraft locale to {}: no matching Language registered", requested);
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
            LOG.warn("Failed to switch Minecraft locale to {}", requested, t);
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
            LOG.warn("Failed to restore original Minecraft locale", t);
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
        return new GuideSitePageAssetExporter(
            assets,
            assetId -> loadGuideAsset(guide, resourceManager, language, assetId));
    }

    private GuideSiteHtmlCompiler createHtmlCompiler(GuideSitePageAssetExporter assetExporter,
        GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer) {
        return new GuideSiteHtmlCompiler(recipeTagRenderer, assetExporter::resolveImageSrc, mdxTagRenderer);
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
                LOG.warn("Failed to export scene for page {} in guide {}", parsedPage.getId(), guide.getId(), t);
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
            baseScene.inWorldJson(),
            baseScene.overlayJson(),
            baseScene.hoverTargetsJson(),
            manifestPath);
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
        GuideSiteExportedScene runtimeExport = exporter.exportScene(scene);
        return new GuideSiteExportedScene(
            runtimeExport.placeholderPath(),
            runtimeExport.scenePath(),
            annotationPayload.inWorldJson(),
            annotationPayload.overlayJson(),
            hoverTargetsJson);
    }

    private String exportSceneStateManifest(ParsedGuidePage parsedPage, LytGuidebookScene scene,
        GuideSiteTemplateRegistry templates, GuideSiteAssetRegistry assets, GuideSiteSceneRuntimeExporter exporter,
        GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        GuideSiteExportedScene baseScene) throws Exception {
        SceneStateManifestPlan plan = buildSceneStateManifestPlan(scene);
        if (plan == null || plan.states.size() <= 1) {
            return null;
        }

        SceneVariantState initialState = SceneVariantState.capture(scene, plan.channelIds);
        LinkedHashMap<String, Object> serializedStates = new LinkedHashMap<>(plan.states.size());

        try {
            for (SceneVariantState state : plan.states) {
                GuideSiteExportedScene exportedVariant;
                if (state.equals(initialState)) {
                    exportedVariant = baseScene;
                } else {
                    applySceneVariantState(scene, state, plan.channelIds);
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
            applySceneVariantState(scene, initialState, plan.channelIds);
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
        if (scene == null || !scene.isInteractive()) {
            return null;
        }

        List<Integer> visibleLayers = buildVisibleLayerStates(scene);
        StructureLibSceneMetadata metadata = scene.getStructureLibSceneMetadata();
        List<Integer> tiers = buildTierStates(scene, metadata);
        List<StructureLibSceneMetadata.ChannelData> selectableChannels = buildSelectableChannels(metadata);
        List<String> channelIds = new ArrayList<>(selectableChannels.size());
        List<List<Integer>> channelValues = new ArrayList<>(selectableChannels.size());
        for (StructureLibSceneMetadata.ChannelData channelData : selectableChannels) {
            channelIds.add(channelData.getChannelId());
            channelValues.add(buildChannelStates(channelData));
        }

        long variantCount = (long) visibleLayers.size() * (long) tiers.size();
        for (List<Integer> values : channelValues) {
            variantCount *= values.size();
            if (variantCount > MAX_SCENE_STATE_VARIANTS) {
                LOG.warn(
                    "Skipping scene state manifest export because {} variants exceed limit {}.",
                    variantCount,
                    MAX_SCENE_STATE_VARIANTS);
                return null;
            }
        }
        if (variantCount <= 1) {
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
        if (tiers.size() > 1 && metadata != null && metadata.getTierData() != null) {
            LinkedHashMap<String, Object> tierControl = new LinkedHashMap<>();
            tierControl.put("label", GuidebookText.SceneStructureLibTierLabel.text());
            tierControl.put(
                "min",
                metadata.getTierData()
                    .getMinValue());
            tierControl.put(
                "max",
                metadata.getTierData()
                    .getMaxValue());
            controls.put("tier", tierControl);
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
            controls.put("channels", channelControls);
        }

        ArrayList<SceneVariantState> states = new ArrayList<>((int) variantCount);
        appendSceneVariantStates(states, visibleLayers, tiers, channelIds, channelValues, 0, new ArrayList<>());
        return new SceneStateManifestPlan(states, channelIds, controls);
    }

    private void appendSceneVariantStates(List<SceneVariantState> states, List<Integer> visibleLayers,
        List<Integer> tiers, List<String> channelIds, List<List<Integer>> channelValues, int channelIndex,
        List<Integer> currentChannels) {
        if (channelIndex >= channelValues.size()) {
            for (Integer visibleLayer : visibleLayers) {
                for (Integer tier : tiers) {
                    LinkedHashMap<String, Integer> channelState = new LinkedHashMap<>(channelIds.size());
                    for (int i = 0; i < channelIds.size(); i++) {
                        channelState.put(channelIds.get(i), currentChannels.get(i));
                    }
                    states.add(new SceneVariantState(visibleLayer, tier, channelState));
                }
            }
            return;
        }

        for (Integer value : channelValues.get(channelIndex)) {
            currentChannels.add(value);
            appendSceneVariantStates(
                states,
                visibleLayers,
                tiers,
                channelIds,
                channelValues,
                channelIndex + 1,
                currentChannels);
            currentChannels.remove(currentChannels.size() - 1);
        }
    }

    private void applySceneVariantState(LytGuidebookScene scene, SceneVariantState state, List<String> channelIds) {
        if (scene == null || state == null) {
            return;
        }
        scene.setStructureLibCurrentTier(state.tier);
        for (String channelId : channelIds) {
            int value = state.channels.containsKey(channelId) ? state.channels.get(channelId) : 0;
            scene.setStructureLibChannelValue(channelId, value);
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
            return Collections.singletonList(scene != null ? scene.getCurrentVisibleLayer() : 0);
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

    private List<Integer> buildTierStates(LytGuidebookScene scene, StructureLibSceneMetadata metadata) {
        if (scene == null || metadata == null
            || metadata.getTierData() == null
            || !metadata.getTierData()
                .isSelectable()) {
            return Collections.singletonList(scene != null ? scene.getStructureLibCurrentTier() : 1);
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
            return Collections.emptyList();
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
            return Collections.singletonList(0);
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
        private final List<String> channelIds;
        private final Map<String, Object> controls;

        private SceneStateManifestPlan(List<SceneVariantState> states, List<String> channelIds,
            Map<String, Object> controls) {
            this.states = states;
            this.channelIds = channelIds;
            this.controls = controls;
        }
    }

    private static final class SceneVariantState {

        private final int visibleLayer;
        private final int tier;
        private final LinkedHashMap<String, Integer> channels;

        private SceneVariantState(int visibleLayer, int tier, LinkedHashMap<String, Integer> channels) {
            this.visibleLayer = Math.max(0, visibleLayer);
            this.tier = Math.max(1, tier);
            this.channels = channels != null ? new LinkedHashMap<>(channels) : new LinkedHashMap<>();
        }

        private static SceneVariantState capture(LytGuidebookScene scene, List<String> channelIds) {
            LinkedHashMap<String, Integer> channels = new LinkedHashMap<>();
            if (scene != null && channelIds != null) {
                for (String channelId : channelIds) {
                    channels.put(channelId, scene.getStructureLibChannelValue(channelId));
                }
            }
            return new SceneVariantState(
                scene != null ? scene.getCurrentVisibleLayer() : 0,
                scene != null ? scene.getStructureLibCurrentTier() : StructureLibPreviewSelection.DEFAULT_MASTER_TIER,
                channels);
        }

        private String key() {
            StringBuilder key = new StringBuilder();
            key.append("layer=")
                .append(visibleLayer)
                .append("|tier=")
                .append(tier);
            for (Map.Entry<String, Integer> channelEntry : channels.entrySet()) {
                key.append("|channel:")
                    .append(channelEntry.getKey())
                    .append("=")
                    .append(channelEntry.getValue());
            }
            return key.toString();
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("visibleLayer", visibleLayer);
            state.put("tier", tier);
            state.put("channels", new LinkedHashMap<>(channels));
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
            return visibleLayer == other.visibleLayer && tier == other.tier && channels.equals(other.channels);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * visibleLayer + tier) + channels.hashCode();
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
}
