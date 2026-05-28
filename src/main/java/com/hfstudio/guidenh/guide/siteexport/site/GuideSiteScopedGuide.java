package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContextProvider;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class GuideSiteScopedGuide implements Guide, MediaWikiListContextProvider {

    private final Guide delegate;
    private final Map<ResourceLocation, ParsedGuidePage> parsedPagesById;
    private final NavigationTree navigationTree;
    private final Map<Class<?>, PageIndex> indexOverrides;
    @Nullable
    private final MediaWikiListContext mediaWikiListContext;
    private final Map<ParsedGuidePage, GuidePage> compiledPages = Collections.synchronizedMap(new IdentityHashMap<>());

    public GuideSiteScopedGuide(Guide delegate, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, Map<Class<?>, PageIndex> indexOverrides,
        @Nullable MediaWikiListContext mediaWikiListContext) {
        this.delegate = delegate;
        this.parsedPagesById = Map.copyOf(new LinkedHashMap<>(parsedPagesById));
        this.navigationTree = navigationTree != null ? navigationTree : new NavigationTree();
        this.indexOverrides = indexOverrides != null ? Map.copyOf(new LinkedHashMap<>(indexOverrides)) : Map.of();
        this.mediaWikiListContext = mediaWikiListContext;
    }

    @Override
    public ResourceLocation getId() {
        return delegate.getId();
    }

    @Override
    public String getDefaultNamespace() {
        return delegate.getDefaultNamespace();
    }

    @Override
    public String getContentRootFolder() {
        return delegate.getContentRootFolder();
    }

    @Override
    public ExtensionCollection getExtensions() {
        return delegate.getExtensions();
    }

    @Override
    public <T extends PageIndex> T getIndex(Class<T> indexClass) {
        PageIndex override = indexOverrides.get(indexClass);
        if (override != null) {
            return indexClass.cast(override);
        }
        return delegate.getIndex(indexClass);
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
        return delegate.loadAsset(id);
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
        return delegate.isPageFailed(pageId);
    }

    @Override
    public @Nullable MediaWikiListContext getMediaWikiListContext() {
        return mediaWikiListContext;
    }
}
