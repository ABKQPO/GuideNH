package com.hfstudio.guidenh.guide;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

@Desugar
public record GuidePageChange(@Nullable String language, ResourceLocation pageId, @Nullable ParsedGuidePage oldPage,
    @Nullable ParsedGuidePage newPage) {

    @Deprecated
    public GuidePageChange(ResourceLocation pageId, @Nullable ParsedGuidePage oldPage,
        @Nullable ParsedGuidePage newPage) {
        this(null, pageId, oldPage, newPage);
    }
}
