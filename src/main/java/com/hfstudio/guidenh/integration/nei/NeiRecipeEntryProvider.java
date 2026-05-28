package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.RecipeEntry;
import com.hfstudio.guidenh.integration.api.RecipeEntryProvider;

public class NeiRecipeEntryProvider implements RecipeEntryProvider {

    @Override
    public List<RecipeEntry> findCraftingRecipeEntries(ItemStack target) {
        return convertEntries(NeiRecipeLookup.findCraftingRecipes(target));
    }

    public static List<RecipeEntry> convertEntries(@Nullable List<NeiRecipeLookup.Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        ArrayList<RecipeEntry> converted = new ArrayList<>(entries.size());
        for (NeiRecipeLookup.Entry entry : entries) {
            if (entry != null) {
                converted.add(convertEntry(entry));
            }
        }
        return converted;
    }

    public static RecipeEntry convertEntry(NeiRecipeLookup.Entry entry) {
        return new RecipeEntry(
            entry.handlerName,
            entry.recipeName,
            NeiRecipeHandlerSlotProvider.convertSlots(entry.ingredients),
            NeiRecipeHandlerSlotProvider.convertSlots(entry.others),
            NeiRecipeHandlerSlotProvider.convertSlot(entry.result));
    }
}
