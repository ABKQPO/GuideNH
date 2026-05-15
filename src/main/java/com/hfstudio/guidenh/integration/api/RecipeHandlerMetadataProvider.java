package com.hfstudio.guidenh.integration.api;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public interface RecipeHandlerMetadataProvider {

    @Nullable
    String lookupHandlerName(Object handler);

    @Nullable
    ItemStack lookupHandlerIcon(Object handler);

    @Nullable
    Object lookupHandlerImage(Object handler);

    @Nullable
    Integer lookupHandlerWidth(Object handler);

    @Nullable
    Integer lookupHandlerHeight(Object handler);

    @Nullable
    Integer lookupRecipeHeight(Object handler, int recipeIndex);

    @Nullable
    Integer lookupHandlerYShift(Object handler);

    @Nullable
    String lookupHandlerOverlayIdentifier(Object handler);

    @Nullable
    String lookupHandlerId(Object handler);

    @Nullable
    Integer lookupRecipeCount(Object handler);

    @Nullable
    Integer lookupDrawableWidth(Object drawable);

    @Nullable
    Integer lookupDrawableHeight(Object drawable);

    @Nullable
    Boolean otherStacksThrows(Object handler, int recipeIndex);
}
