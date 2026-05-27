package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialDefinition(String name, String titleKey, String groupName, MediaWikiSpecialPageKind kind,
    boolean searchableFromGuideSearch, boolean showsAllByDefault, int defaultVisibleCount,
    @Nullable String externalUrl) {}
