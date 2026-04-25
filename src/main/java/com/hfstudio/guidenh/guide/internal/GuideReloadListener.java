package com.hfstudio.guidenh.guide.internal;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class GuideReloadListener implements IResourceManagerReloadListener {

    public static final ResourceLocation ID = GuideME.makeId("guides");
    private static final Logger LOG = LoggerFactory.getLogger(GuideReloadListener.class);

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        LOG.info("Reloading guides...");
        // Drop cached NEI reflection data so freshly (re)registered handlers are picked up.
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        GuideRegistry.setDataDriven(DataDrivenGuideLoader.load());
        var guidePages = new HashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();

        String language = LangUtil.getCurrentLanguage();

        for (var guide : GuideRegistry.getAll()) {
            var pages = loadPages(
                resourceManager,
                guide.getId(),
                guide.getContentRootFolder(),
                guide.getDefaultLanguage(),
                language);
            guidePages.put(guide.getId(), pages);
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
     * Scans the guide folder tree and loads all markdown files under {@code assets/<namespace>/<folder>/_<lang>/...}.
     */
    private Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage) {
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();
        var pagePaths = DataDrivenGuideLoader.discoverPagePaths(guideId, folder);
        String namespace = guideId.getResourceDomain();
        String sourcePack = "resources:" + namespace;
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;

        for (var pagePath : pagePaths) {
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
            if (parsed == null) {
                parsed = tryParsePage(
                    resourceManager,
                    sourcePack,
                    defaultLanguage,
                    pageId,
                    new ResourceLocation(namespace, folder + "/" + pagePath));
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
        return tryParsePage(
            resourceManager,
            sourcePack,
            language,
            pageId,
            new ResourceLocation(namespace, folder + "/_" + language + "/" + pagePath));
    }

    private @Nullable ParsedGuidePage tryParsePage(IResourceManager resourceManager, String sourcePack, String language,
        ResourceLocation pageId, ResourceLocation sourceId) {
        try (var stream = GuideResourceAccess.openStream(resourceManager, sourceId)) {
            if (stream == null) {
                return null;
            }
            return PageCompiler.parse(sourcePack, language, pageId, stream);
        } catch (Exception ex) {
            LOG.error("Error parsing page {} from {}", pageId, sourceId, ex);
            return null;
        }
    }
}
