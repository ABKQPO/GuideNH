package com.hfstudio.guidenh.guide.internal.recipe;

final class NeiRecipeLayoutMetrics {

    private NeiRecipeLayoutMetrics() {}

    static int resolveBodyHeight(int handlerHeight, int recipeHeight, int defaultBodyHeight) {
        int resolvedHandlerHeight = handlerHeight > 0 ? handlerHeight : defaultBodyHeight;
        int resolvedRecipeHeight = recipeHeight > 0 ? recipeHeight : 0;
        return Math.max(1, Math.max(resolvedHandlerHeight, resolvedRecipeHeight));
    }
}
