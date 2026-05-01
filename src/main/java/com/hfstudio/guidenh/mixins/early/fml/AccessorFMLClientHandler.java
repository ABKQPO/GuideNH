package com.hfstudio.guidenh.mixins.early.fml;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.client.FMLClientHandler;

/**
 * Exposes the private {@code resourcePackList} field of {@link FMLClientHandler} so the
 * data-driven guide loader can iterate base resource packs without resorting to per-call
 * reflection (which previously incurred {@code getDeclaredField}+{@code setAccessible} on
 * every guide reload).
 */
@Mixin(value = FMLClientHandler.class, remap = false)
public interface AccessorFMLClientHandler {

    @Accessor("resourcePackList")
    List<IResourcePack> guidenh$getResourcePackList();
}
