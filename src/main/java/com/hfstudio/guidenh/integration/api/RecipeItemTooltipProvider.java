package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface RecipeItemTooltipProvider {

    void appendTooltip(Object handler, ItemStack stack, List<String> tooltip, int recipeIndex);
}
