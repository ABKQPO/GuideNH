package com.hfstudio.guidenh.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.integration.api.RecipeItemTooltipProvider;

public class NeiRecipeItemTooltipProvider implements RecipeItemTooltipProvider {

    public NeiRecipeItemTooltipProvider() {}

    @Override
    public void appendTooltip(Object handler, ItemStack stack, List<String> tooltip, int recipeIndex) {
        NeiRecipeLookup.appendItemTooltip(handler, stack, tooltip, recipeIndex);
    }
}
