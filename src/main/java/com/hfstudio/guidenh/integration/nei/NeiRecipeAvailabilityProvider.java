package com.hfstudio.guidenh.integration.nei;

import com.hfstudio.guidenh.integration.api.RecipeAvailabilityProvider;

public class NeiRecipeAvailabilityProvider implements RecipeAvailabilityProvider {

    @Override
    public boolean isRecipeIntegrationAvailable() {
        return NeiRecipeLookup.isAvailable();
    }
}
