package com.hfstudio.guidenh.guide.mediawiki;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialGroupedEntry(String title, String summary, String searchBlob,
    List<MediaWikiSpecialListEntry> children) {}
