package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public interface RecipeEntryProvider {

    @Nullable
    List<RecipeEntry> findCraftingRecipeEntries(ItemStack target);
}
