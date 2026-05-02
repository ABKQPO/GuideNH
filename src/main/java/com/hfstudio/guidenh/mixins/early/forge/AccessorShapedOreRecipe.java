package com.hfstudio.guidenh.mixins.early.forge;

import net.minecraftforge.oredict.ShapedOreRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ShapedOreRecipe.class, remap = false)
public interface AccessorShapedOreRecipe {

    @Accessor("input")
    Object[] guidenh$getInput();

    @Accessor("width")
    int guidenh$getWidth();

    @Accessor("height")
    int guidenh$getHeight();
}
