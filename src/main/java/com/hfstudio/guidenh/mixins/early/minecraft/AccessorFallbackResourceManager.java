package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FallbackResourceManager.class)
public interface AccessorFallbackResourceManager {

    @Accessor("resourcePacks")
    List<IResourcePack> guidenh$getResourcePacks();
}
