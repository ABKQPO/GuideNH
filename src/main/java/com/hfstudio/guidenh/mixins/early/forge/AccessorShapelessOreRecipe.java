package com.hfstudio.guidenh.mixins.early.forge;

import java.util.ArrayList;

import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ShapelessOreRecipe.class, remap = false)
public interface AccessorShapelessOreRecipe {

    @Accessor("input")
    ArrayList<Object> guidenh$getInput();
}
