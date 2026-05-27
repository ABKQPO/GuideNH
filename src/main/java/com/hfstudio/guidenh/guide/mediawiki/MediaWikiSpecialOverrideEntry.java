package com.hfstudio.guidenh.guide.mediawiki;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialOverrideEntry(int priority, String sourcePack, String language, String sourceId) {}
