package com.hfstudio.guidenh.guide;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.block.LytDocument;

@Desugar
public record GuidePage(String sourcePack, ResourceLocation id, LytDocument document) {}
