package com.hfstudio.guidenh.guide.compiler;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/**
 * A single icon entry for navigation cycling. {@link #itemId} is the raw registry key string
 * (mod ID casing preserved) used for item lookup.
 */
@Desugar
public record NavigationIconEntry(String itemId, int meta, @Nullable Map<?, ?> nbt) {}
