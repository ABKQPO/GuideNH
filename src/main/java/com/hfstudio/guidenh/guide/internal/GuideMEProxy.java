package com.hfstudio.guidenh.guide.internal;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;

public interface GuideMEProxy {

    static GuideMEProxy instance() {
        return GuideME.PROXY;
    }

    @Nullable
    default String getGuideDisplayName(ResourceLocation guideId) {
        return null;
    }

    boolean openGuide(EntityPlayer player, ResourceLocation guideId, @Nullable PageAnchor anchor);

    Stream<ResourceLocation> getAvailableGuides();

    Stream<ResourceLocation> getAvailablePages(ResourceLocation guideId);

    default boolean reloadResources() {
        return false;
    }
}
