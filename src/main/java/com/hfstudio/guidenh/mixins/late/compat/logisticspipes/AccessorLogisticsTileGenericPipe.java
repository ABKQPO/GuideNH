package com.hfstudio.guidenh.mixins.late.compat.logisticspipes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.renderer.state.PipeRenderState;

@Mixin(value = LogisticsTileGenericPipe.class, remap = false)
public interface AccessorLogisticsTileGenericPipe {

    @Accessor("pipe")
    CoreUnroutedPipe getPipe();

    @Accessor("renderState")
    PipeRenderState getRenderState();
}
