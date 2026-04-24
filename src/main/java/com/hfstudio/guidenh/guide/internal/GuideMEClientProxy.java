package com.hfstudio.guidenh.guide.internal;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;

class GuideMEClientProxy extends GuideMEServerProxy {

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation guideId, @Nullable PageAnchor anchor) {
        GuideScreen.open(guideId, anchor);
        return true;
    }

    @Override
    public boolean reloadResources() {
        var mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        mc.refreshResources();
        return true;
    }
}
