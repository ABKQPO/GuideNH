package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface RawRecipeHandlerProvider {

    List<Object> queryRawCraftingHandlers(ItemStack target);

    List<Object> queryRawUsageHandlers(ItemStack target);
}
