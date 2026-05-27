package com.hfstudio.guidenh.guide.mediawiki;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

@Desugar
public record MediaWikiSpecialDataIndex(Map<ResourceLocation, ParsedGuidePage> normalPagesById,
    Map<ResourceLocation, List<ResourceLocation>> translationsBySourcePage,
    Map<ResourceLocation, Set<String>> pagePropertiesById, Map<ResourceLocation, List<String>> externalLinksByPage,
    Map<ResourceLocation, Long> pageSizesById, Map<ResourceLocation, Long> assetSizesById,
    Map<String, List<ResourceLocation>> fileUsageByPath,
    Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> lintIssuesByPage,
    Map<String, List<ResourceLocation>> ambiguousItemBindings,
    Map<ResourceLocation, List<MediaWikiSpecialOverrideEntry>> overridesByPage, Set<String> unusedFiles) {

    private static final MediaWikiSpecialDataIndex EMPTY = new MediaWikiSpecialDataIndex(
        Collections.<ResourceLocation, ParsedGuidePage>emptyMap(),
        Collections.<ResourceLocation, List<ResourceLocation>>emptyMap(),
        Collections.<ResourceLocation, Set<String>>emptyMap(),
        Collections.<ResourceLocation, List<String>>emptyMap(),
        Collections.<ResourceLocation, Long>emptyMap(),
        Collections.<ResourceLocation, Long>emptyMap(),
        Collections.<String, List<ResourceLocation>>emptyMap(),
        Collections.<ResourceLocation, List<MediaWikiSpecialLintIssue>>emptyMap(),
        Collections.<String, List<ResourceLocation>>emptyMap(),
        Collections.<ResourceLocation, List<MediaWikiSpecialOverrideEntry>>emptyMap(),
        Collections.<String>emptySet());

    public static MediaWikiSpecialDataIndex empty() {
        return EMPTY;
    }
}
