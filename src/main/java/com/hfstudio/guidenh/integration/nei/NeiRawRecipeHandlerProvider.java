package com.hfstudio.guidenh.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.integration.api.RawRecipeHandlerProvider;

public class NeiRawRecipeHandlerProvider implements RawRecipeHandlerProvider {

    @Override
    public List<Object> queryRawCraftingHandlers(ItemStack target) {
        return NeiRecipeLookup.queryRawCraftingHandlers(target);
    }

    @Override
    public List<Object> queryRawUsageHandlers(ItemStack target) {
        return NeiRecipeLookup.queryRawUsageHandlers(target);
    }
}
