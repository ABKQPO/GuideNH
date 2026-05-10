package com.hfstudio.guidenh.integration.api;

public interface RecipeHandlerRenderProvider {

    boolean canRenderRecipeHandler(Object handler);

    void renderRecipeHandler(Object handler, int recipeIndex, boolean skipForeground);
}
