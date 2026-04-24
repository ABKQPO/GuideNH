package com.hfstudio.guidenh.guide;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.internal.GuideMEProxy;

public final class GuidesCommon {

    private GuidesCommon() {}

    public static void openGuide(EntityPlayer player, ResourceLocation guideId) {
        GuideMEProxy.instance()
            .openGuide(player, guideId, null);
    }

    public static void openGuide(EntityPlayer player, ResourceLocation guideId, PageAnchor anchor) {
        GuideMEProxy.instance()
            .openGuide(player, guideId, Objects.requireNonNull(anchor, "anchor"));
    }
}
