package com.hfstudio.guidenh.integration.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public interface RecipeHandlerSlotProvider {

    @Nullable
    List<RecipeSlot> readIngredientSlots(Object handler, int recipeIndex);

    @Nullable
    List<RecipeSlot> readOtherSlots(Object handler, int recipeIndex);

    @Nullable
    RecipeSlot readResultSlot(Object handler, int recipeIndex);
}
