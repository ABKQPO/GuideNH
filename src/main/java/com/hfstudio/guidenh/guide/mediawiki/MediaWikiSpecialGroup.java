package com.hfstudio.guidenh.guide.mediawiki;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialGroup(String name, String titleKey, int order) {}
