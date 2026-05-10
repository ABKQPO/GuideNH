package com.hfstudio.guidenh.integration.api;

public interface RecipeAnimationUpdateProvider {

    boolean canUpdateRecipeAnimation(Object handler);

    void updateRecipeAnimation(Object handler);
}
