package com.hfstudio.guidenh.integration.nei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe.RecipeId;

public class NeiGuideNavigation {

    protected NeiGuideNavigation() {}

    public static boolean handleHoveredStackShortcut(@Nullable GuideScreenNeiBridge.EditorAccess editorAccess,
        @Nullable ItemStack stack) {
        if (!NeiRecipeLookup.isAvailable() || stack == null) {
            return false;
        }
        if (NEIClientConfig.isKeyHashDown("gui.recipe")) {
            return withTemporaryScreenChange(editorAccess, () -> GuiCraftingRecipe.openRecipeGui("item", stack.copy()));
        }
        if (NEIClientConfig.isKeyHashDown("gui.usage")) {
            return withTemporaryScreenChange(editorAccess, () -> GuiUsageRecipe.openRecipeGui("item", stack.copy()));
        }
        return false;
    }

    public static boolean openExactCraftingRecipe(@Nullable GuideScreenNeiBridge.EditorAccess editorAccess,
        Object handler, int recipeIndex, @Nullable ItemStack displayedResult) {
        if (!NeiRecipeLookup.isAvailable() || !(handler instanceof IRecipeHandler recipeHandler)) {
            return false;
        }
        ItemStack recipeAnchor = resolveRecipeAnchorStack(handler, recipeIndex, displayedResult);
        if (recipeAnchor == null) {
            return false;
        }
        RecipeId recipeId = RecipeId.of(recipeHandler, recipeIndex);
        return withTemporaryScreenChange(
            editorAccess,
            () -> GuiCraftingRecipe.createRecipeGui("recipeId", true, recipeAnchor, recipeId) != null);
    }

    private static @Nullable ItemStack resolveRecipeAnchorStack(Object handler, int recipeIndex,
        @Nullable ItemStack displayedResult) {
        if (displayedResult != null) {
            return displayedResult.copy();
        }
        Object result = NeiDirectCalls.resultStack(handler, recipeIndex);
        if (result instanceof PositionedStack positionedStack) {
            ItemStack resolved = copyVisibleStack(positionedStack);
            if (resolved != null) {
                return resolved;
            }
        }
        List<Object> ingredients = NeiDirectCalls.ingredientStacks(handler, recipeIndex);
        for (Object ingredient : ingredients) {
            if (ingredient instanceof PositionedStack positionedStack) {
                ItemStack resolved = copyVisibleStack(positionedStack);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private static @Nullable ItemStack copyVisibleStack(PositionedStack positionedStack) {
        if (positionedStack == null) {
            return null;
        }
        if (positionedStack.item != null) {
            return positionedStack.item.copy();
        }
        if (positionedStack.items == null) {
            return null;
        }
        for (int i = 0, count = positionedStack.items.length; i < count; i++) {
            ItemStack stack = positionedStack.items[i];
            if (stack != null && stack.stackSize > 0) {
                return stack.copy();
            }
        }
        return null;
    }

    private static boolean withTemporaryScreenChange(@Nullable GuideScreenNeiBridge.EditorAccess editorAccess,
        ScreenAction action) {
        if (editorAccess != null) {
            editorAccess.prepareForTemporaryScreenChange();
        }
        boolean handled = false;
        try {
            handled = action.run();
            return handled;
        } finally {
            if (editorAccess != null && Minecraft.getMinecraft().currentScreen == editorAccess.container()) {
                editorAccess.cancelTemporaryScreenChange();
            } else if (!handled && editorAccess != null) {
                editorAccess.cancelTemporaryScreenChange();
            }
        }
    }

    private interface ScreenAction {

        boolean run();
    }
}
