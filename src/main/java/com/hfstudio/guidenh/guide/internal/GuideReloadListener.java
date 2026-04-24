package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class GuideReloadListener implements IResourceManagerReloadListener {

    public static final ResourceLocation ID = GuideME.makeId("guides");
    private static final Logger LOG = LoggerFactory.getLogger(GuideReloadListener.class);
    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        LOG.info("Reloading guides...");
        // Drop cached NEI reflection data so freshly (re)registered handlers are picked up.
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        var guidePages = new IdentityHashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();

        String language = LangUtil.getCurrentLanguage();

        for (var guide : GuideRegistry.getStaticGuides()) {
            if (!guidePages.containsKey(guide.getId())) {
                var pages = loadPages(
                    resourceManager,
                    guide.getContentRootFolder(),
                    guide.getDefaultLanguage(),
                    language);
                guidePages.put(guide.getId(), pages);
            }
        }

        for (var entry : guidePages.entrySet()) {
            GuideRegistry.updatePages(entry.getKey(), entry.getValue());
        }

        try {
            GuideME.getSearch()
                .indexAll();
        } catch (Throwable t) {
            LOG.warn("Failed to reindex search after reload", t);
        }

        LOG.info("Guide reload complete, loaded {} guides", guidePages.size());
    }

    /**
     * 
     * <pre>
     * {
     *   "namespace": "guidenh",
     *   "pages": ["index.md", "subdir/page2.md", ...]
     * }
     * </pre>
     */
    private Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, String folder,
        String defaultLanguage, @Nullable String currentLanguage) {
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();

        String namespace = "guidenh";
        String[] folderParts = folder.split("/");
        if (folderParts.length >= 2) {
            namespace = folderParts[1];
        }

        var manifestId = new ResourceLocation(namespace, folder + "/_manifest.json");
        JsonObject manifest;
        try {
            IResource res = resourceManager.getResource(manifestId);
            try (var reader = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
                manifest = GSON.fromJson(reader, JsonObject.class);
            }
        } catch (IOException ex) {
            LOG.warn("No _manifest.json found for guide folder '{}' (looked at {}), skipping", folder, manifestId);
            return pages;
        }

        if (manifest == null || !manifest.has("pages")) {
            LOG.warn("Guide manifest at {} has no 'pages' array", manifestId);
            return pages;
        }

        var pagesArray = manifest.getAsJsonArray("pages");
        String sourcePack = "resources:" + namespace;
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;

        for (var element : pagesArray) {
            String pagePath = element.getAsString();
            ResourceLocation pageId = new ResourceLocation(namespace, pagePath);

            ParsedGuidePage parsed = tryLoadPage(
                resourceManager,
                sourcePack,
                lang,
                namespace,
                folder,
                pagePath,
                pageId);
            if (parsed == null && !lang.equals(defaultLanguage)) {
                parsed = tryLoadPage(resourceManager, sourcePack, defaultLanguage, namespace, folder, pagePath, pageId);
            }
            if (parsed != null) {
                pages.put(pageId, parsed);
            } else {
                LOG.warn("Failed to load guide page {}", pageId);
            }
        }

        LOG.info("Loaded {} pages for folder {}", pages.size(), folder);
        return pages;
    }

    @Nullable
    private ParsedGuidePage tryLoadPage(IResourceManager resourceManager, String sourcePack, String language,
        String namespace, String folder, String pagePath, ResourceLocation pageId) {
        var withLang = new ResourceLocation(namespace, folder + "/" + language + "/" + pagePath);
        var noLang = new ResourceLocation(namespace, folder + "/" + pagePath);
        for (var candidate : new ResourceLocation[] { withLang, noLang }) {
            try {
                IResource res = resourceManager.getResource(candidate);
                try (var in = res.getInputStream()) {
                    return PageCompiler.parse(sourcePack, language, pageId, in);
                }
            } catch (IOException ignored) {} catch (Exception ex) {
                LOG.error("Error parsing page {} from {}", pageId, candidate, ex);
                return null;
            }
        }
        return null;
    }
}
