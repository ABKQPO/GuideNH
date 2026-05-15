package com.hfstudio.guidenh.integration.nei;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.RecipeHandlerMetadataProvider;

public class NeiRecipeHandlerMetadataProvider implements RecipeHandlerMetadataProvider {

    @Override
    public String lookupHandlerName(Object handler) {
        return NeiRecipeLookup.lookupHandlerName(handler);
    }

    @Override
    public @Nullable ItemStack lookupHandlerIcon(Object handler) {
        return NeiRecipeLookup.lookupHandlerIcon(handler);
    }

    @Override
    public @Nullable Object lookupHandlerImage(Object handler) {
        return NeiRecipeLookup.lookupHandlerImage(handler);
    }

    @Override
    public Integer lookupHandlerWidth(Object handler) {
        return NeiRecipeLookup.lookupHandlerWidth(handler);
    }

    @Override
    public Integer lookupHandlerHeight(Object handler) {
        return NeiRecipeLookup.lookupHandlerHeight(handler);
    }

    @Override
    public Integer lookupRecipeHeight(Object handler, int recipeIndex) {
        return NeiRecipeLookup.lookupRecipeHeight(handler, recipeIndex);
    }

    @Override
    public Integer lookupHandlerYShift(Object handler) {
        return NeiRecipeLookup.lookupHandlerYShift(handler);
    }

    @Override
    public String lookupHandlerOverlayIdentifier(Object handler) {
        return NeiRecipeLookup.lookupOverlayIdentifier(handler);
    }

    @Override
    public String lookupHandlerId(Object handler) {
        return NeiRecipeLookup.lookupHandlerId(handler);
    }

    @Override
    public Integer lookupRecipeCount(Object handler) {
        return NeiRecipeLookup.lookupNumRecipes(handler);
    }

    @Override
    public Integer lookupDrawableWidth(Object drawable) {
        return NeiRecipeLookup.drawableWidth(drawable);
    }

    @Override
    public Integer lookupDrawableHeight(Object drawable) {
        return NeiRecipeLookup.drawableHeight(drawable);
    }

    @Override
    public Boolean otherStacksThrows(Object handler, int recipeIndex) {
        return NeiRecipeLookup.otherStacksThrows(handler, recipeIndex);
    }
}
