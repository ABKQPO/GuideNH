package com.hfstudio.guidenh.mixins.late.compat.buildcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import buildcraft.transport.Pipe;
import buildcraft.transport.TileGenericPipe;

@Mixin(value = TileGenericPipe.class, remap = false)
public interface AccessorTileGenericPipe {

    @Accessor("pipe")
    Pipe<?> getPipe();

    @Invoker("computeConnections")
    void invokeComputeConnections();

    @Invoker("refreshRenderState")
    void invokeRefreshRenderState();

    @Invoker("getItemMetadata")
    int invokeGetItemMetadata();
}
