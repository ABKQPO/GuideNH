package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiCategoryReference(String categoryName, @Nullable String sortKey) {}
