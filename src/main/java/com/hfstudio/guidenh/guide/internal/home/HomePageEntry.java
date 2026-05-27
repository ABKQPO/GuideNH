package com.hfstudio.guidenh.guide.internal.home;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;

public class HomePageEntry {

    private final ResourceLocation guideId;
    private final ResourceLocation pageId;
    private final PageAnchor anchor;
    private final String title;
    private final String summary;
    @Nullable
    private final GuidePageIcon icon;
    private final int recommend;

    public HomePageEntry(ResourceLocation guideId, ResourceLocation pageId, PageAnchor anchor, String title,
        String summary, @Nullable GuidePageIcon icon, int recommend) {
        this.guideId = guideId;
        this.pageId = pageId;
        this.anchor = anchor;
        this.title = title;
        this.summary = summary;
        this.icon = icon;
        this.recommend = recommend;
    }

    public ResourceLocation guideId() {
        return guideId;
    }

    public ResourceLocation pageId() {
        return pageId;
    }

    public PageAnchor anchor() {
        return anchor;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    @Nullable
    public GuidePageIcon icon() {
        return icon;
    }

    public int recommend() {
        return recommend;
    }
}
