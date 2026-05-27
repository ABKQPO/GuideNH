package com.hfstudio.guidenh.guide.mediawiki;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;

@Desugar
public record MediaWikiListEntry(ResourceLocation pageId, String title, @Nullable GuidePageIcon icon, String sortKey) {}
