package com.hfstudio.guidenh.integration.nei;

import com.hfstudio.guidenh.integration.api.RecipeAnimationUpdateProvider;

public class NeiRecipeAnimationUpdateProvider implements RecipeAnimationUpdateProvider {

    public NeiRecipeAnimationUpdateProvider() {}

    @Override
    public boolean canUpdateRecipeAnimation(Object handler) {
        return handler != null && NeiRecipeLookup.isAvailable();
    }

    @Override
    public void updateRecipeAnimation(Object handler) {
        NeiRecipeLookup.callOnUpdate(handler);
    }
}
