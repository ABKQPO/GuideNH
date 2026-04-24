package com.hfstudio.guidenh.guide.navigation;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record NavigationNode(@Nullable ResourceLocation pageId, String title, @Nullable ItemStack icon,
    List<NavigationNode> children, int position, boolean hasPage) {}
