package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;

public class GuideSitePageCollector {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSitePageCollector.class);

    @FunctionalInterface
    public interface PageLoader {

        Optional<ParsedGuidePage> load(String language, String pagePath);
    }

    private final PageLoader pageLoader;

    public GuideSitePageCollector(PageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    public GuideSitePageCollector(MutableGuide guide, IResourceManager resourceManager) {
        this((language, pagePath) -> tryLoadPage(guide, resourceManager, language, pagePath));
    }

    public List<GuideSitePageVariant> collect(MutableGuide guide) {
        List<String> languages;
        try {
            languages = discoverLanguages(guide.getId());
        } catch (Throwable t) {
            LOG.debug("Falling back to the guide default language for {}", guide.getId(), t);
            languages = new ArrayList<>();
        }
        if (languages.isEmpty()) {
            languages.add(guide.getDefaultLanguage());
        } else if (!languages.contains(guide.getDefaultLanguage())) {
            languages.add(0, guide.getDefaultLanguage());
        }

        LinkedHashSet<String> pagePathSet;
        try {
            pagePathSet = new LinkedHashSet<>(
                DataDrivenGuideLoader.discoverPagePaths(guide.getId(), guide.getContentRootFolder()));
        } catch (Throwable t) {
            LOG.debug("Falling back to already loaded page ids for {}", guide.getId(), t);
            pagePathSet = new LinkedHashSet<>();
        }
        for (ParsedGuidePage page : guide.getPages()) {
            pagePathSet.add(
                page.getId()
                    .getResourcePath());
        }
        List<String> pagePaths = new ArrayList<>(pagePathSet);
        return collect(guide.getId(), guide.getDefaultLanguage(), languages, pagePaths);
    }

    public List<GuideSitePageVariant> collect(ResourceLocation guideId, String defaultLanguage, List<String> languages,
        List<String> pagePaths) {
        List<GuideSitePageVariant> variants = new ArrayList<>();

        for (String language : languages) {
            for (String pagePath : pagePaths) {
                Optional<ParsedGuidePage> localized = pageLoader.load(language, pagePath);
                if (localized.isPresent()) {
                    ParsedGuidePage page = localized.get();
                    variants.add(new GuideSitePageVariant(guideId, page.getId(), language, language, false, page));
                    continue;
                }

                Optional<ParsedGuidePage> fallback = pageLoader.load(defaultLanguage, pagePath);
                fallback.ifPresent(page -> variants
                        .add(new GuideSitePageVariant(guideId, page.getId(), language, defaultLanguage, true, page)));
            }
        }

        return variants;
    }

    private static List<String> discoverLanguages(ResourceLocation guideId) {
        Map<ResourceLocation, LinkedHashSet<String>> discovered = new LinkedHashMap<>();
        for (var resourcePack : DataDrivenGuideLoader.getActiveResourcePacks()) {
            DataDrivenGuideLoader.scanResourcePack(resourcePack, discovered);
        }
        return new ArrayList<>(discovered.getOrDefault(guideId, new LinkedHashSet<>()));
    }

    private static Optional<ParsedGuidePage> tryLoadPage(MutableGuide guide, IResourceManager resourceManager,
        String language, String pagePath) {
        ResourceLocation pageId = new ResourceLocation(
            guide.getId()
                .getResourceDomain(),
            pagePath);
        ResourceLocation localizedSource = new ResourceLocation(
            guide.getId()
                .getResourceDomain(),
            guide.getContentRootFolder() + "/_" + language + "/" + pagePath);

        try (var stream = GuideResourceAccess.openStream(resourceManager, localizedSource)) {
            if (stream == null) {
                return Optional.empty();
            }
            return Optional.of(
                PageCompiler.parse(
                    "resources:" + guide.getId()
                        .getResourceDomain(),
                    language,
                    pageId,
                    stream));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
