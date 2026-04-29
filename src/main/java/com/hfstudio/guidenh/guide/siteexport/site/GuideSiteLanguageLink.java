package com.hfstudio.guidenh.guide.siteexport.site;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record GuideSiteLanguageLink(String language, String url, boolean fallbackUsed, String sourceLanguage) {}
