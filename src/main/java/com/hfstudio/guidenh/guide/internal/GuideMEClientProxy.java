package com.hfstudio.guidenh.guide.internal;

import org.jetbrains.annotations.Nullable;

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
        return GuideMEClientReloadDispatcher.dispatch(mc.func_152345_ab(), mc::func_152344_a, mc::refreshResources);
    }
}
