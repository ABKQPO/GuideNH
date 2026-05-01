package com.hfstudio.guidenh.mixins.late.compat.ae2;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.api.networking.IGridNode;
import appeng.me.helpers.AENetworkProxy;

/**
 * Exposes the private {@code node} and {@code data} fields of AE2's
 * {@link AENetworkProxy}. Required by {@link com.hfstudio.guidenh.compat.ae2.Ae2Helpers}
 * to synthesize a grid node for guide preview rendering without per-call reflection.
 *
 * <p>
 * Only applied when AE2 is loaded; gated through the {@code Mods.AE2} requirement
 * declared in {@link com.hfstudio.guidenh.mixins.Mixins}.
 * </p>
 */
@Mixin(value = AENetworkProxy.class, remap = false)
public interface AccessorAENetworkProxy {

    @Accessor("node")
    IGridNode guidenh$getNode();

    @Accessor("node")
    void guidenh$setNode(IGridNode node);

    @Accessor("data")
    NBTTagCompound guidenh$getData();
}
