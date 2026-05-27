package com.hfstudio.guidenh.guide.internal;

import net.minecraft.util.ResourceLocation;

public class GuideWarmupWorkItem {

    public enum Kind {
        HIGH_PRIORITY_PAGE,
        NORMAL_PAGE,
        DEV_VALIDATION
    }

    private final ResourceLocation guideId;
    private final ResourceLocation pageId;
    private final Kind kind;
    private int stepIndex;
    private long nextEligibleTick;

    public GuideWarmupWorkItem(ResourceLocation guideId, ResourceLocation pageId, Kind kind) {
        this.guideId = guideId;
        this.pageId = pageId;
        this.kind = kind;
    }

    public ResourceLocation guideId() {
        return guideId;
    }

    public ResourceLocation pageId() {
        return pageId;
    }

    public Kind kind() {
        return kind;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public void advanceStep() {
        stepIndex++;
    }

    public long nextEligibleTick() {
        return nextEligibleTick;
    }

    public void setNextEligibleTick(long nextEligibleTick) {
        this.nextEligibleTick = nextEligibleTick;
    }
}
