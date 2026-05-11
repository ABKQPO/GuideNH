package com.hfstudio.guidenh.guide.internal;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

import cpw.mods.fml.common.FMLLog;

public class GuideReloadListener implements IResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        FMLLog.getLogger()
            .info("[GuideNH] [GuideReloadListener] Reloading guides...");
        // Drop cached NEI reflection data so freshly (re)registered handlers are picked up.
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        GuidePageTexture.clear();
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
            FMLLog.getLogger()
                .warn("[GuideNH] [GuideReloadListener] Failed to reindex search after reload", t);
        }

        FMLLog.getLogger()
            .info("[GuideNH] [GuideReloadListener] Guide reload complete, loaded {} guides", guidePages.size());
    }

    /**
     * Scans the guide folder tree and loads all markdown files under {@code assets/<namespace>/<folder>/_<lang>/...}.
     */
    private Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager, ResourceLocation guideId,
        String folder, String defaultLanguage, @Nullable String currentLanguage) {
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();
        var pagePathsByNamespace = DataDrivenGuideLoader.discoverPagePaths(folder);
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;

        for (var nsEntry : pagePathsByNamespace.entrySet()) {
            String sourceNamespace = nsEntry.getKey();
            String sourcePack = "resources:" + sourceNamespace;

            for (var pagePath : nsEntry.getValue()) {
                ResourceLocation pageId = new ResourceLocation(sourceNamespace, pagePath);

                ParsedGuidePage parsed = tryLoadPage(
                    resourceManager,
                    sourcePack,
                    lang,
                    sourceNamespace,
                    folder,
                    pagePath,
                    pageId);
                if (parsed == null && !lang.equals(defaultLanguage)) {
                    parsed = tryLoadPage(
                        resourceManager,
                        sourcePack,
                        defaultLanguage,
                        sourceNamespace,
                        folder,
                        pagePath,
                        pageId);
                }
                if (parsed == null) {
                    parsed = tryParsePage(
                        resourceManager,
                        sourcePack,
                        defaultLanguage,
                        pageId,
                        new ResourceLocation(sourceNamespace, folder + "/" + pagePath));
                }
                if (parsed != null) {
                    pages.put(pageId, parsed);
                } else {
                    FMLLog.getLogger()
                        .warn("[GuideNH] [GuideReloadListener] Failed to load guide page {}", pageId);
                }
            }
        }

        FMLLog.getLogger()
            .info("[GuideNH] [GuideReloadListener] Loaded {} pages for folder {}", pages.size(), folder);
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
            FMLLog.getLogger()
                .error("[GuideNH] [GuideReloadListener] Error parsing page {} from {}", pageId, sourceId, ex);
            return null;
        }
    }
}
