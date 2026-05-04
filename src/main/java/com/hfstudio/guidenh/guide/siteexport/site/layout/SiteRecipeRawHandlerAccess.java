package com.hfstudio.guidenh.guide.siteexport.site.layout;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;

/**
 * Narrow interface for reading NEI handler slots without depending on
 * {@link com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeTagRenderer.HandlerRuntime}.
 */
public interface SiteRecipeRawHandlerAccess {

    List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex);

    List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex);

    @Nullable
    NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex);
}
