package com.hfstudio.guidenh.integration.nei;

import com.hfstudio.guidenh.integration.api.RecipeHandlerRenderProvider;

public class NeiRecipeHandlerRenderProvider implements RecipeHandlerRenderProvider {

    @Override
    public boolean canRenderRecipeHandler(Object handler) {
        return handler != null && NeiRecipeLookup.isAvailable();
    }

    @Override
    public void renderRecipeHandler(Object handler, int recipeIndex, boolean skipForeground) {
        NeiRecipeLookup.callDrawBackground(handler, recipeIndex);
        if (!skipForeground) {
            NeiRecipeLookup.callDrawForeground(handler, recipeIndex);
            NeiRecipeLookup.callDrawExtras(handler, recipeIndex);
        }
    }
}
