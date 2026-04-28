package com.hfstudio.guidenh.guide.internal.recipe;

public class NeiRecipeLayoutMetrics {

    private NeiRecipeLayoutMetrics() {}

    static int resolveBodyHeight(int handlerHeight, int recipeHeight, int defaultBodyHeight) {
        int resolvedHandlerHeight = handlerHeight > 0 ? handlerHeight : defaultBodyHeight;
        int resolvedRecipeHeight = Math.max(recipeHeight, 0);
        return Math.max(1, Math.max(resolvedHandlerHeight, resolvedRecipeHeight));
    }
}
