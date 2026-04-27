package com.hfstudio.guidenh.mixins.early.forge;

import net.minecraftforge.client.ForgeHooksClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ForgeHooksClient.class, remap = false)
public interface AccessorForgeHooksClient {

    @Accessor(value = "worldRenderPass", remap = false)
    static void setWorldRenderPass(int worldRenderPass) {
        throw new UnsupportedOperationException();
    }
}
