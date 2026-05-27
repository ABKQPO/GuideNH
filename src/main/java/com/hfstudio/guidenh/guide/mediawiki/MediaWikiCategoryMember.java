package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;

@Desugar
public record MediaWikiCategoryMember(String categoryName, @Nullable String sortKey, PageAnchor pageAnchor) {}
