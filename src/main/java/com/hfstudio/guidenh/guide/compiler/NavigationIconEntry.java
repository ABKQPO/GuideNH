package com.hfstudio.guidenh.guide.compiler;

import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record NavigationIconEntry(ResourceLocation itemId, int meta, @Nullable Map<?, ?> nbt) {}
