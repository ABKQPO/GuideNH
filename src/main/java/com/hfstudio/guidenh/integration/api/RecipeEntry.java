package com.hfstudio.guidenh.integration.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class RecipeEntry {

    private final String handlerName;
    private final String recipeName;
    private final List<RecipeSlot> ingredients;
    private final List<RecipeSlot> supportingSlots;
    private final RecipeSlot result;

    public RecipeEntry(String handlerName, String recipeName, List<RecipeSlot> ingredients,
        List<RecipeSlot> supportingSlots, @Nullable RecipeSlot result) {
        this.handlerName = handlerName;
        this.recipeName = recipeName;
        this.ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        this.supportingSlots = supportingSlots == null ? List.of() : List.copyOf(supportingSlots);
        this.result = result;
    }

    public String handlerName() {
        return handlerName;
    }

    public String recipeName() {
        return recipeName;
    }

    public List<RecipeSlot> ingredients() {
        return ingredients;
    }

    public List<RecipeSlot> supportingSlots() {
        return supportingSlots;
    }

    @Nullable
    public RecipeSlot result() {
        return result;
    }
}
