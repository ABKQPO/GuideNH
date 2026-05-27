package com.hfstudio.guidenh.guide.mediawiki;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialPageResult(MediaWikiSpecialDefinition definition, MediaWikiSpecialPageKind kind,
    List<MediaWikiSpecialListEntry> flatEntries, List<MediaWikiSpecialGroupedEntry> groupedEntries, boolean hasMore,
    boolean searchEnabled) {}
