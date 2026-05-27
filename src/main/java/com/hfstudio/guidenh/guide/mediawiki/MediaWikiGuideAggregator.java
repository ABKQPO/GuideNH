package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class MediaWikiGuideAggregator implements Guide {

    private static final Object CACHE_LOCK = new Object();
    @Nullable
    private static volatile CachedAggregation cachedAggregation;

    private final Guide primaryGuide;
    private final Map<ResourceLocation, ParsedGuidePage> parsedPagesById;
    private final Map<ResourceLocation, MutableGuide> ownerGuidesByPageId;
    private final List<MutableGuide> componentGuides;
    private final NavigationTree navigationTree;
    private final CategoryIndex categoryIndex;
    private final Map<Class<?>, PageIndex> indexOverrides;
    private final Map<ParsedGuidePage, GuidePage> compiledPages = Collections
        .synchronizedMap(new IdentityHashMap<ParsedGuidePage, GuidePage>());

    private MediaWikiGuideAggregator(Guide primaryGuide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        Map<ResourceLocation, MutableGuide> ownerGuidesByPageId, List<MutableGuide> componentGuides,
        NavigationTree navigationTree, CategoryIndex categoryIndex) {
        this.primaryGuide = primaryGuide;
        this.parsedPagesById = Collections.unmodifiableMap(new LinkedHashMap<>(parsedPagesById));
        this.ownerGuidesByPageId = Collections.unmodifiableMap(new LinkedHashMap<>(ownerGuidesByPageId));
        this.componentGuides = Collections.unmodifiableList(new ArrayList<>(componentGuides));
        this.navigationTree = navigationTree != null ? navigationTree : new NavigationTree();
        this.categoryIndex = categoryIndex;
        LinkedHashMap<Class<?>, PageIndex> overrides = new LinkedHashMap<>();
        overrides.put(CategoryIndex.class, categoryIndex);
        this.indexOverrides = Collections.unmodifiableMap(overrides);
    }

    public static MediaWikiGuideAggregator create(Guide primaryGuide) {
        CachedAggregation aggregation = cachedAggregation;
        long revision = GuideRegistry.getNavigationRevision();
        if (aggregation == null || aggregation.revision() != revision) {
            synchronized (CACHE_LOCK) {
                aggregation = cachedAggregation;
                if (aggregation == null || aggregation.revision() != revision) {
                    aggregation = rebuildAggregation(revision);
                    cachedAggregation = aggregation;
                }
            }
        }
        return new MediaWikiGuideAggregator(
            primaryGuide,
            aggregation.parsedPagesById(),
            aggregation.ownerGuidesByPageId(),
            aggregation.componentGuides(),
            aggregation.navigationTree(),
            aggregation.categoryIndex());
    }

    private static CachedAggregation rebuildAggregation(long revision) {
        LinkedHashMap<ResourceLocation, ParsedGuidePage> pagesById = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, MutableGuide> ownerGuidesByPageId = new LinkedHashMap<>();
        ArrayList<ParsedGuidePage> indexablePages = new ArrayList<>();
        ArrayList<MutableGuide> componentGuides = new ArrayList<>(GuideRegistry.getAll());
        for (MutableGuide guide : componentGuides) {
            for (ParsedGuidePage page : guide.getPages()) {
                if (page == null || MediaWikiPageIds.isSyntheticPage(page.getId())) {
                    continue;
                }
                pagesById.putIfAbsent(page.getId(), page);
                ownerGuidesByPageId.putIfAbsent(page.getId(), guide);
                if (NavigationTree.areModRequirementsMet(
                    page.getFrontmatter() != null ? page.getFrontmatter()
                        .navigationEntry() : null)) {
                    indexablePages.add(page);
                }
            }
        }
        CategoryIndex categoryIndex = new CategoryIndex();
        categoryIndex.rebuild(indexablePages);
        return new CachedAggregation(
            revision,
            pagesById,
            ownerGuidesByPageId,
            componentGuides,
            GuideRegistry.getMergedNavigationTree(),
            categoryIndex);
    }

    @Override
    public ResourceLocation getId() {
        return primaryGuide.getId();
    }

    @Override
    public String getDefaultNamespace() {
        return primaryGuide.getDefaultNamespace();
    }

    @Override
    public String getContentRootFolder() {
        return primaryGuide.getContentRootFolder();
    }

    @Override
    public ExtensionCollection getExtensions() {
        return primaryGuide.getExtensions();
    }

    @Override
    public <T extends PageIndex> T getIndex(Class<T> indexClass) {
        PageIndex override = indexOverrides.get(indexClass);
        if (override != null) {
            return indexClass.cast(override);
        }
        return primaryGuide.getIndex(indexClass);
    }

    @Override
    public Collection<ParsedGuidePage> getPages() {
        return parsedPagesById.values();
    }

    @Override
    public @Nullable ParsedGuidePage getParsedPage(ResourceLocation id) {
        return parsedPagesById.get(id);
    }

    @Override
    public @Nullable GuidePage getPage(ResourceLocation id) {
        ParsedGuidePage parsedPage = parsedPagesById.get(id);
        if (parsedPage == null) {
            return null;
        }
        synchronized (compiledPages) {
            GuidePage compiledPage = compiledPages.get(parsedPage);
            if (compiledPage == null) {
                compiledPage = PageCompiler.compile(this, getExtensions(), parsedPage);
                compiledPages.put(parsedPage, compiledPage);
            }
            return compiledPage;
        }
    }

    @Override
    public byte @Nullable [] loadAsset(ResourceLocation id) {
        MutableGuide ownerGuide = ownerGuidesByPageId.get(id);
        if (ownerGuide != null) {
            byte[] bytes = ownerGuide.loadAsset(id);
            if (bytes != null) {
                return bytes;
            }
        }
        for (MutableGuide guide : componentGuides) {
            byte[] bytes = guide.loadAsset(id);
            if (bytes != null) {
                return bytes;
            }
        }
        return primaryGuide.loadAsset(id);
    }

    @Override
    public NavigationTree getNavigationTree() {
        return navigationTree;
    }

    @Override
    public boolean pageExists(ResourceLocation pageId) {
        return parsedPagesById.containsKey(pageId);
    }

    @Override
    public boolean isPageFailed(ResourceLocation pageId) {
        MutableGuide ownerGuide = ownerGuidesByPageId.get(pageId);
        if (ownerGuide != null) {
            return ownerGuide.isPageFailed(pageId);
        }
        return false;
    }

    public Collection<MutableGuide> getComponentGuides() {
        return componentGuides;
    }

    public @Nullable MutableGuide findOwnerGuide(ResourceLocation pageId) {
        return ownerGuidesByPageId.get(pageId);
    }

    @Desugar
    private record CachedAggregation(long revision, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        Map<ResourceLocation, MutableGuide> ownerGuidesByPageId, List<MutableGuide> componentGuides,
        NavigationTree navigationTree, CategoryIndex categoryIndex) {}
}
