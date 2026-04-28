package com.hfstudio.guidenh.guide;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public interface PageCollection {

    <T extends PageIndex> T getIndex(Class<T> indexClass);

    Collection<ParsedGuidePage> getPages();

    @Nullable
    ParsedGuidePage getParsedPage(ResourceLocation id);

    @Nullable
    GuidePage getPage(ResourceLocation id);

    @Nullable
    byte[] loadAsset(ResourceLocation id);

    NavigationTree getNavigationTree();

    boolean pageExists(ResourceLocation pageId);
}
