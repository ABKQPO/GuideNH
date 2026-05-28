package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.RecipeHandlerSlotProvider;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

public class NeiRecipeHandlerSlotProvider implements RecipeHandlerSlotProvider {

    @Override
    public List<RecipeSlot> readIngredientSlots(Object handler, int recipeIndex) {
        return convertSlots(NeiRecipeLookup.readIngredientSlots(handler, recipeIndex));
    }

    @Override
    public List<RecipeSlot> readOtherSlots(Object handler, int recipeIndex) {
        return convertSlots(NeiRecipeLookup.readOtherSlots(handler, recipeIndex));
    }

    @Override
    public @Nullable RecipeSlot readResultSlot(Object handler, int recipeIndex) {
        return convertSlot(NeiRecipeLookup.readResultSlot(handler, recipeIndex));
    }

    public static List<RecipeSlot> convertSlots(List<NeiRecipeLookup.Slot> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        ArrayList<RecipeSlot> converted = new ArrayList<>(slots.size());
        for (NeiRecipeLookup.Slot slot : slots) {
            RecipeSlot convertedSlot = convertSlot(slot);
            if (convertedSlot != null) {
                converted.add(convertedSlot);
            }
        }
        return converted;
    }

    @Nullable
    public static RecipeSlot convertSlot(@Nullable NeiRecipeLookup.Slot slot) {
        if (slot == null) {
            return null;
        }
        return new RecipeSlot(slot.relx, slot.rely, slot.stacks);
    }
}
