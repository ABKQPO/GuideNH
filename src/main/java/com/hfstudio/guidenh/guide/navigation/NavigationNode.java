package com.hfstudio.guidenh.guide.navigation;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;

@Desugar
public record NavigationNode(@Nullable ResourceLocation pageId, String title, @Nullable GuidePageIcon icon,
    List<NavigationNode> children, int position, boolean hasPage) {}
