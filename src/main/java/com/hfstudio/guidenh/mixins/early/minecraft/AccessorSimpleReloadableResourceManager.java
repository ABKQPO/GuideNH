package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.Map;

import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleReloadableResourceManager.class)
public interface AccessorSimpleReloadableResourceManager {

    @Accessor("domainResourceManagers")
    Map<String, FallbackResourceManager> guidenh$getDomainResourceManagers();
}
