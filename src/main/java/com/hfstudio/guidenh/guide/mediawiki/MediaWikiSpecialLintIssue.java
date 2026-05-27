package com.hfstudio.guidenh.guide.mediawiki;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialLintIssue(String message, @Nullable Integer lineNumber) {}
