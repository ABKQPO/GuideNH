package com.hfstudio.guidenh.mixins.early.minecraft;

import java.io.File;

import net.minecraft.client.resources.AbstractResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected {@code resourcePackFile} (SRG: {@code field_110597_b}) of
 * {@link AbstractResourcePack} so the data-driven guide loader can resolve the backing
 * file of a resource pack without per-call reflection.
 */
@Mixin(AbstractResourcePack.class)
public interface AccessorAbstractResourcePack {

    @Accessor("resourcePackFile")
    File guidenh$getResourcePackFile();
}
