package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NeiGuideNavigation {

    private static final String GREGTECH_DEFAULT_NEI_HANDLER = "gregtech.nei.GTNEIDefaultHandler";

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
        if (isGregTechDefaultHandler(recipeHandler)) {
            return openExactGregTechRecipe(editorAccess, recipeHandler, recipeIndex, displayedResult);
        }
        return openExactLegacyRecipe(editorAccess, recipeHandler, recipeIndex, displayedResult);
    }

    private static boolean openExactLegacyRecipe(@Nullable GuideScreenNeiBridge.EditorAccess editorAccess,
        IRecipeHandler recipeHandler, int recipeIndex, @Nullable ItemStack displayedResult) {
        ItemStack recipeAnchor = resolveLegacyRecipeAnchorStack(recipeHandler, recipeIndex, displayedResult);
        if (recipeAnchor == null) {
            return false;
        }
        RecipeId recipeId = resolveLegacyRecipeId(recipeHandler, recipeIndex, recipeAnchor);
        if (recipeId == null) {
            return false;
        }
        return withTemporaryScreenChange(
            editorAccess,
            () -> openRecipeGuiWithFallback(recipeHandler, recipeAnchor, recipeId));
    }

    private static boolean openExactGregTechRecipe(@Nullable GuideScreenNeiBridge.EditorAccess editorAccess,
        IRecipeHandler recipeHandler, int recipeIndex, @Nullable ItemStack displayedResult) {
        ExactGregTechRecipeTarget recipeTarget = resolveGregTechRecipeTarget(
            recipeHandler,
            recipeIndex,
            displayedResult);
        if (recipeTarget == null) {
            return false;
        }
        return withTemporaryScreenChange(editorAccess, () -> openGregTechRecipeGui(recipeHandler, recipeTarget));
    }

    private static boolean openGregTechRecipeGui(IRecipeHandler recipeHandler, ExactGregTechRecipeTarget recipeTarget) {
        return openRecipeGuiWithFallback(recipeHandler, recipeTarget.anchorStack(), recipeTarget.recipeId());
    }

    private static @Nullable ExactGregTechRecipeTarget resolveGregTechRecipeTarget(IRecipeHandler recipeHandler,
        int recipeIndex, @Nullable ItemStack displayedResult) {
        ItemStack recipeAnchor = resolveGregTechRecipeAnchorStack(recipeHandler, recipeIndex, displayedResult);
        if (recipeAnchor == null) {
            return null;
        }
        String handlerName = resolveHandlerName(recipeHandler);
        if (handlerName == null) {
            return null;
        }
        List<ItemStack> ingredients = copyVisibleStacks(recipeHandler.getIngredientStacks(recipeIndex));
        List<ItemStack> outputs = copyVisibleStacks(recipeHandler.getOtherStacks(recipeIndex));
        if (ingredients.isEmpty() || outputs.isEmpty()) {
            return null;
        }
        return new ExactGregTechRecipeTarget(
            recipeAnchor,
            RecipeId.of(recipeAnchor, handlerName, mergeRecipeSignature(ingredients, outputs)));
    }

    private static List<ItemStack> copyVisibleStacks(List<PositionedStack> positionedStacks) {
        List<ItemStack> stacks = new ArrayList<>(positionedStacks.size());
        for (PositionedStack positionedStack : positionedStacks) {
            ItemStack resolved = copyVisibleStack(positionedStack);
            if (resolved != null) {
                stacks.add(resolved);
            }
        }
        return stacks;
    }

    private static List<ItemStack> mergeRecipeSignature(List<ItemStack> ingredients, List<ItemStack> outputs) {
        List<ItemStack> signature = new ArrayList<>(ingredients.size() + outputs.size());
        signature.addAll(copyStacks(ingredients));
        signature.addAll(copyStacks(outputs));
        return signature;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> source) {
        List<ItemStack> copied = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            if (stack != null) {
                copied.add(stack.copy());
            }
        }
        return copied;
    }

    private static @Nullable RecipeId resolveLegacyRecipeId(IRecipeHandler recipeHandler, int recipeIndex,
        ItemStack recipeAnchor) {
        RecipeId directId = RecipeId.of(recipeHandler, recipeIndex);
        if (directId != null) {
            return directId;
        }
        return RecipeId.of(recipeAnchor, recipeHandler.getHandlerId(), recipeHandler.getIngredientStacks(recipeIndex));
    }

    private static boolean openRecipeGuiWithFallback(IRecipeHandler recipeHandler, ItemStack recipeAnchor,
        RecipeId recipeId) {
        GuiRecipe<?> exactGui = GuiCraftingRecipe.createRecipeGui("recipeId", true, recipeAnchor, recipeId);
        if (exactGui != null) {
            return true;
        }
        GuiRecipe<?> craftingHandlerGui = createCraftingHandlerScopedRecipeGui(recipeHandler, recipeAnchor, recipeId);
        if (craftingHandlerGui != null) {
            return true;
        }
        GuiRecipe<?> usageHandlerGui = createUsageHandlerScopedRecipeGui(recipeHandler, recipeAnchor, recipeId);
        if (usageHandlerGui != null) {
            return true;
        }
        GuiRecipe<?> itemGui = GuiCraftingRecipe.createRecipeGui("item", true, recipeAnchor);
        if (itemGui == null) {
            return false;
        }
        itemGui.openTargetRecipe(recipeId);
        return true;
    }

    private static @Nullable GuiRecipe<?> createCraftingHandlerScopedRecipeGui(IRecipeHandler recipeHandler,
        ItemStack recipeAnchor, RecipeId recipeId) {
        if (!(recipeHandler instanceof ICraftingHandler craftingHandler)) {
            return null;
        }
        String lookupId = resolvePreferredHandlerLookupId(recipeHandler);
        if (lookupId == null) {
            return null;
        }
        ICraftingHandler scopedHandler = craftingHandler.getRecipeHandler(lookupId, recipeAnchor);
        if (scopedHandler == null || scopedHandler.numRecipes() <= 0) {
            return null;
        }
        String targetHandlerName = recipeId.getHandleName();
        if (!Objects.equals(targetHandlerName, resolveHandlerName(scopedHandler))) {
            return null;
        }
        ArrayList<ICraftingHandler> handlers = new ArrayList<>(1);
        handlers.add(scopedHandler);
        GuiRecipe<?> gui = newGuiCraftingRecipe(handlers);
        if (gui == null) {
            return null;
        }
        Minecraft.getMinecraft()
            .displayGuiScreen(gui);
        gui.openTargetRecipe(recipeId);
        return gui;
    }

    private static @Nullable GuiRecipe<?> createUsageHandlerScopedRecipeGui(IRecipeHandler recipeHandler,
        ItemStack recipeAnchor, RecipeId recipeId) {
        if (!(recipeHandler instanceof IUsageHandler usageHandler)) {
            return null;
        }
        String lookupId = resolvePreferredHandlerLookupId(recipeHandler);
        if (lookupId == null) {
            return null;
        }
        IUsageHandler scopedHandler = usageHandler.getUsageAndCatalystHandler(lookupId, recipeAnchor);
        if (scopedHandler == null || scopedHandler.numRecipes() <= 0) {
            scopedHandler = usageHandler.getUsageHandler("item", recipeAnchor);
        }
        if (scopedHandler == null || scopedHandler.numRecipes() <= 0) {
            return null;
        }
        if (!Objects.equals(recipeId.getHandleName(), resolveHandlerName(scopedHandler))) {
            return null;
        }
        ArrayList<IUsageHandler> handlers = new ArrayList<>(1);
        handlers.add(scopedHandler);
        GuiRecipe<?> gui = newGuiUsageRecipe(handlers);
        if (gui == null) {
            return null;
        }
        Minecraft.getMinecraft()
            .displayGuiScreen(gui);
        gui.openTargetRecipe(recipeId);
        return gui;
    }

    private static @Nullable GuiRecipe<?> newGuiCraftingRecipe(ArrayList<ICraftingHandler> handlers) {
        try {
            var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
            constructor.setAccessible(true);
            return constructor.newInstance(handlers);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable GuiRecipe<?> newGuiUsageRecipe(ArrayList<IUsageHandler> handlers) {
        try {
            var constructor = GuiUsageRecipe.class.getDeclaredConstructor(ArrayList.class);
            constructor.setAccessible(true);
            return constructor.newInstance(handlers);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable String resolvePreferredHandlerLookupId(IRecipeHandler recipeHandler) {
        String overlayId = recipeHandler.getOverlayIdentifier();
        if (overlayId != null && !overlayId.isEmpty()) {
            return overlayId;
        }
        if (recipeHandler instanceof TemplateRecipeHandler) {
            String handlerId = recipeHandler.getHandlerId();
            if (handlerId != null && !handlerId.isEmpty()) {
                return handlerId;
            }
        }
        return null;
    }

    private static @Nullable ItemStack resolveLegacyRecipeAnchorStack(Object handler, int recipeIndex,
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

    private static @Nullable ItemStack resolveGregTechRecipeAnchorStack(Object handler, int recipeIndex,
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
        List<Object> otherStacks = NeiDirectCalls.otherStacks(handler, recipeIndex);
        for (Object otherStack : otherStacks) {
            if (otherStack instanceof PositionedStack positionedStack) {
                ItemStack resolved = copyVisibleStack(positionedStack);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private static boolean matchesHandlerName(IRecipeHandler recipeHandler, String handlerName) {
        try {
            return handlerName.equals(resolveHandlerName(recipeHandler));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static @Nullable String resolveHandlerName(IRecipeHandler recipeHandler) {
        try {
            return GuiRecipeTab.getHandlerInfo(recipeHandler)
                .getHandlerName();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable ItemStack copyVisibleStack(PositionedStack positionedStack) {
        if (positionedStack.item != null) {
            return positionedStack.item.copy();
        }
        if (positionedStack.items == null) {
            return null;
        }
        for (ItemStack stack : positionedStack.items) {
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

    private static boolean isGregTechDefaultHandler(Object handler) {
        return handler != null && GREGTECH_DEFAULT_NEI_HANDLER.equals(
            handler.getClass()
                .getName());
    }

    private interface ScreenAction {

        boolean run();
    }

    private record ExactGregTechRecipeTarget(ItemStack anchorStack, RecipeId recipeId) {}
}
