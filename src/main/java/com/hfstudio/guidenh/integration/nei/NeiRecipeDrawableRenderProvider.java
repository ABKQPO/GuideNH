package com.hfstudio.guidenh.integration.nei;

import com.hfstudio.guidenh.integration.api.RecipeDrawableRenderProvider;

public class NeiRecipeDrawableRenderProvider implements RecipeDrawableRenderProvider {

    @Override
    public boolean drawDrawable(Object drawable, int x, int y) {
        if (drawable == null || !NeiRecipeLookup.isAvailable()) {
            return false;
        }
        NeiRecipeLookup.drawHandlerImage(drawable, x, y);
        return true;
    }
}
