package com.hfstudio.guidenh.guide.compiler;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;

/**
 * Inserts a page into the navigation tree. Null parent means top-level category.
 */
@Desugar
public record FrontmatterNavigation(String title, @Nullable ResourceLocation parent, int position,
    @Nullable ResourceLocation iconItemId, @Nullable Map<?, ?> iconComponents,
    @Nullable ResourceLocation iconTextureId) {}
