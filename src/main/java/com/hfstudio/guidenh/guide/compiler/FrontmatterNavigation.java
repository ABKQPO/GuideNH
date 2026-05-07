package com.hfstudio.guidenh.guide.compiler;

import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/**
 * Inserts a page into the navigation tree. Null parent means top-level category.
 */
@Desugar
public record FrontmatterNavigation(String title, @Nullable ResourceLocation parent, int position,
    @Nullable String iconItemId, int iconItemMeta, @Nullable Map<?, ?> iconComponents,
    @Nullable ResourceLocation iconTextureId, @Nullable List<NavigationIconEntry> iconEntries,
    @Nullable List<ResourceLocation> iconTextureEntries) {}
