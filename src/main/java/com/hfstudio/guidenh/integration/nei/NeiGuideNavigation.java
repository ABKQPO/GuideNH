package com.hfstudio.guidenh.integration.nei;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String ITEM_LOOKUP_ID = "item";
    private static final Constructor<GuiCraftingRecipe> CRAFTING_GUI_CONSTRUCTOR = resolveGuiConstructor(
        GuiCraftingRecipe.class);
    private static final Constructor<GuiUsageRecipe> USAGE_GUI_CONSTRUCTOR = resolveGuiConstructor(
        GuiUsageRecipe.class);
    private static final Map<Class<?>, @Nullable String> HANDLER_NAME_CACHE = new HashMap<>();

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
        List<ItemStack> recipeSignature = buildGregTechRecipeSignature(recipeHandler, recipeIndex);
        if (recipeSignature == null) {
            return null;
        }
        return new ExactGregTechRecipeTarget(recipeAnchor, RecipeId.of(recipeAnchor, handlerName, recipeSignature));
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

    private static @Nullable List<ItemStack> buildGregTechRecipeSignature(IRecipeHandler recipeHandler,
        int recipeIndex) {
        List<PositionedStack> ingredients = recipeHandler.getIngredientStacks(recipeIndex);
        List<PositionedStack> outputs = recipeHandler.getOtherStacks(recipeIndex);
        if (ingredients.isEmpty() || outputs.isEmpty()) {
            return null;
        }
        List<ItemStack> signature = new ArrayList<>(ingredients.size() + outputs.size());
        appendVisibleStacks(signature, ingredients);
        int ingredientCount = signature.size();
        appendVisibleStacks(signature, outputs);
        if (ingredientCount == 0 || signature.size() == ingredientCount) {
            return null;
        }
        return signature;
    }

    private static void appendVisibleStacks(List<ItemStack> target, List<PositionedStack> positionedStacks) {
        for (PositionedStack positionedStack : positionedStacks) {
            ItemStack resolved = copyVisibleStack(positionedStack);
            if (resolved != null) {
                target.add(resolved);
            }
        }
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
        String lookupId = resolvePreferredHandlerLookupId(recipeHandler);
        GuiRecipe<?> craftingHandlerGui = createCraftingHandlerScopedRecipeGui(
            recipeHandler,
            lookupId,
            recipeAnchor,
            recipeId);
        if (craftingHandlerGui != null) {
            return true;
        }
        GuiRecipe<?> usageHandlerGui = createUsageHandlerScopedRecipeGui(
            recipeHandler,
            lookupId,
            recipeAnchor,
            recipeId);
        if (usageHandlerGui != null) {
            return true;
        }
        GuiRecipe<?> itemGui = GuiCraftingRecipe.createRecipeGui(ITEM_LOOKUP_ID, true, recipeAnchor);
        if (itemGui == null) {
            return false;
        }
        itemGui.openTargetRecipe(recipeId);
        return true;
    }

    private static @Nullable GuiRecipe<?> createCraftingHandlerScopedRecipeGui(IRecipeHandler recipeHandler,
        @Nullable String lookupId, ItemStack recipeAnchor, RecipeId recipeId) {
        if (!(recipeHandler instanceof ICraftingHandler craftingHandler)) {
            return null;
        }
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
        @Nullable String lookupId, ItemStack recipeAnchor, RecipeId recipeId) {
        if (!(recipeHandler instanceof IUsageHandler usageHandler)) {
            return null;
        }
        if (lookupId == null) {
            return null;
        }
        IUsageHandler scopedHandler = usageHandler.getUsageAndCatalystHandler(lookupId, recipeAnchor);
        if (scopedHandler == null || scopedHandler.numRecipes() <= 0) {
            scopedHandler = usageHandler.getUsageHandler(ITEM_LOOKUP_ID, recipeAnchor);
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
        return newGuiRecipe(CRAFTING_GUI_CONSTRUCTOR, handlers);
    }

    private static @Nullable GuiRecipe<?> newGuiUsageRecipe(ArrayList<IUsageHandler> handlers) {
        return newGuiRecipe(USAGE_GUI_CONSTRUCTOR, handlers);
    }

    private static @Nullable GuiRecipe<?> newGuiRecipe(@Nullable Constructor<? extends GuiRecipe> constructor,
        ArrayList<?> handlers) {
        if (constructor == null) {
            return null;
        }
        try {
            return constructor.newInstance(handlers);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static <T extends GuiRecipe> @Nullable Constructor<T> resolveGuiConstructor(Class<T> guiClass) {
        try {
            Constructor<T> constructor = guiClass.getDeclaredConstructor(ArrayList.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable String resolvePreferredHandlerLookupId(IRecipeHandler recipeHandler) {
        String overlayId = recipeHandler.getOverlayIdentifier();
        if (overlayId != null && !overlayId.isEmpty()) {
            return overlayId;
        }
        String handlerId = recipeHandler.getHandlerId();
        if (recipeHandler instanceof TemplateRecipeHandler && handlerId != null && !handlerId.isEmpty()) {
            return handlerId;
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

    private static @Nullable String resolveHandlerName(IRecipeHandler recipeHandler) {
        Class<?> handlerClass = recipeHandler.getClass();
        synchronized (HANDLER_NAME_CACHE) {
            if (HANDLER_NAME_CACHE.containsKey(handlerClass)) {
                return HANDLER_NAME_CACHE.get(handlerClass);
            }
        }
        String handlerName;
        try {
            handlerName = GuiRecipeTab.getHandlerInfo(recipeHandler)
                .getHandlerName();
        } catch (Throwable ignored) {
            handlerName = null;
        }
        synchronized (HANDLER_NAME_CACHE) {
            HANDLER_NAME_CACHE.put(handlerClass, handlerName);
        }
        return handlerName;
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
