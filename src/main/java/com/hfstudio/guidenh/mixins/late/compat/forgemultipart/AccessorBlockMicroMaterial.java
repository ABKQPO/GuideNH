package com.hfstudio.guidenh.mixins.late.compat.forgemultipart;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.microblock.BlockMicroMaterial;

@Mixin(value = BlockMicroMaterial.class, remap = false)
public interface AccessorBlockMicroMaterial {

    @Accessor("block")
    Block getBlock();

    @Accessor("meta")
    int getMeta();
}
