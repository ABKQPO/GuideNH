package com.hfstudio.guidenh.compat.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import codechicken.nei.PositionedStack;
import codechicken.nei.drawable.DrawableResource;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.HandlerInfo;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NeiDirectCalls {

    public static List<Object> getCraftingHandlers(ItemStack target) {
        ArrayList<ICraftingHandler> original = GuiCraftingRecipe.craftinghandlers;
        GuiCraftingRecipe.craftinghandlers = new ArrayList<>(original);
        try {
            List<ICraftingHandler> result = GuiCraftingRecipe.getCraftingHandlers("item", target);
            List<Object> out = new ArrayList<>(result.size());
            for (ICraftingHandler h : result) {
                if (h != null) out.add(h);
            }
            return out;
        } finally {
            GuiCraftingRecipe.craftinghandlers = original;
        }
    }

    public static List<Object> getUsageHandlers(ItemStack target) {
        ArrayList<IUsageHandler> original = GuiUsageRecipe.usagehandlers;
        GuiUsageRecipe.usagehandlers = new ArrayList<>(original);
        try {
            List<IUsageHandler> result = GuiUsageRecipe.getUsageHandlers("item", target);
            List<Object> out = new ArrayList<>(result.size());
            for (IUsageHandler h : result) {
                if (h != null) out.add(h);
            }
            return out;
        } finally {
            GuiUsageRecipe.usagehandlers = original;
        }
    }

    private static IRecipeHandler h(Object handler) {
        return (IRecipeHandler) handler;
    }

    public static int numRecipes(Object handler) {
        return h(handler).numRecipes();
    }

    public static String recipeName(Object handler) {
        return h(handler).getRecipeName();
    }

    /** Returns {@code null} when the handler's default {@code getHandlerId()} returns null. */
    public static @Nullable String handlerId(Object handler) {
        return h(handler).getHandlerId();
    }

    public static @Nullable String overlayId(Object handler) {
        return h(handler).getOverlayIdentifier();
    }

    public static void onUpdate(Object handler) {
        h(handler).onUpdate();
    }

    public static void drawBackground(Object handler, int idx) {
        h(handler).drawBackground(idx);
    }

    public static void drawForeground(Object handler, int idx) {
        h(handler).drawForeground(idx);
    }

    /** Calls {@code drawExtras} only when {@code handler} is a {@link TemplateRecipeHandler}. */
    public static void drawExtras(Object handler, int idx) {
        if (handler instanceof TemplateRecipeHandler tmpl) {
            tmpl.drawExtras(idx);
        }
    }

    public static int recipeHeight(Object handler, int idx) {
        return h(handler).getRecipeHeight(idx);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Object> ingredientStacks(Object handler, int idx) {
        // IRecipeHandler.getIngredientStacks returns List<PositionedStack>;
        // raw-cast to List<Object> is safe since we only ever pass elements back to readSlot.
        return (List) h(handler).getIngredientStacks(idx);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Object> otherStacks(Object handler, int idx) {
        return (List) h(handler).getOtherStacks(idx);
    }

    /**
     * Calls {@code getOtherStacks} and returns {@code true} if any exception is thrown. Used to
     * detect broken handlers without going through GTNH-NEI's logged safe-wrapper.
     */
    public static boolean otherStacksThrows(Object handler, int idx) {
        try {
            h(handler).getOtherStacks(idx);
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    public static @Nullable Object resultStack(Object handler, int idx) {
        return h(handler).getResultStack(idx);
    }

    public static void handleItemTooltip(Object handler, ItemStack stack, List<String> tooltip, int idx) {
        // Passing null for GuiRecipe is intentional — we are not inside a live recipe GUI.
        List<String> result = h(handler).handleItemTooltip(null, stack, tooltip, idx);
        if (result != null && result != tooltip) {
            tooltip.clear();
            tooltip.addAll(result);
        }
    }

    public static int relX(Object ps) {
        return ((PositionedStack) ps).relx;
    }

    public static int relY(Object ps) {
        return ((PositionedStack) ps).rely;
    }

    public static @Nullable ItemStack[] items(Object ps) {
        return ((PositionedStack) ps).items;
    }

    public static @Nullable ItemStack item(Object ps) {
        return ((PositionedStack) ps).item;
    }

    // -----------------------------------------------------------------------
    // HandlerInfo via GuiRecipeTab.getHandlerInfo (public static method)
    // -----------------------------------------------------------------------

    @Nullable
    private static HandlerInfo info(Object handler) {
        return GuiRecipeTab.getHandlerInfo(h(handler));
    }

    public static @Nullable ItemStack handlerIconStack(Object handler) {
        HandlerInfo info = info(handler);
        return info != null ? info.getItemStack() : null;
    }

    public static int handlerWidth(Object handler) {
        HandlerInfo info = info(handler);
        return info != null ? info.getWidth() : 166;
    }

    public static int handlerHeight(Object handler) {
        HandlerInfo info = info(handler);
        return info != null ? info.getHeight() : 65;
    }

    public static int handlerYShift(Object handler) {
        HandlerInfo info = info(handler);
        return info != null ? info.getYShift() : 0;
    }

    /**
     * Returns the raw {@link DrawableResource} for this handler as an opaque {@code Object}, or
     * {@code null} when no image is registered or this NEI build does not expose
     * {@code HandlerInfo.getImage()}.
     */
    public static @Nullable Object handlerImage(Object handler) {
        HandlerInfo info = info(handler);
        if (info == null) return null;
        try {
            return info.getImage();
        } catch (AbstractMethodError ignored) {
            // getImage() absent in this NEI build
            return null;
        }
    }

    public static int drawableWidth(Object drawable) {
        return ((DrawableResource) drawable).getWidth();
    }

    public static int drawableHeight(Object drawable) {
        return ((DrawableResource) drawable).getHeight();
    }

    public static void drawDrawable(Object drawable, int x, int y) {
        ((DrawableResource) drawable).draw(x, y);
    }
}
