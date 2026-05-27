package com.hfstudio.guidenh.guide.mediawiki;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;

@Desugar
public record MediaWikiSpecialListEntry(String title, String subtitle, String searchBlob,
    @Nullable ResourceLocation pageId, @Nullable Integer lineNumber, @Nullable GuidePageIcon icon,
    @Nullable String externalUrl) {

    public MediaWikiSpecialListEntry(String title, String subtitle, String searchBlob,
        @Nullable ResourceLocation pageId, @Nullable Integer lineNumber) {
        this(title, subtitle, searchBlob, pageId, lineNumber, null, null);
    }

    public MediaWikiSpecialListEntry(String title, String subtitle, String searchBlob,
        @Nullable ResourceLocation pageId, @Nullable Integer lineNumber, @Nullable GuidePageIcon icon) {
        this(title, subtitle, searchBlob, pageId, lineNumber, icon, null);
    }
}
