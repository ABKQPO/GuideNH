package com.hfstudio.guidenh.compat.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.compat.Mods;

public class NeiRecipeLookup {

    private static final Logger LOG = LoggerFactory.getLogger(NeiRecipeLookup.class);

    public static final boolean AVAILABLE;

    static {
        boolean ok = false;
        if (!Mods.NotEnoughItems.isModLoaded()) {
            LOG.info("NEI mod not loaded; GuideNH recipe rendering falls back to vanilla.");
        } else {
            try {
                Class.forName(
                    "com.hfstudio.guidenh.compat.nei.NeiDirectCalls",
                    true,
                    NeiRecipeLookup.class.getClassLoader());
                ok = true;
            } catch (Throwable t) {
                LOG.warn("NEI API incompatible; recipe rendering falls back to vanilla. Reason: {}", t.toString());
            }
        }
        AVAILABLE = ok;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static class Slot {

        public final int relx;
        public final int rely;
        public final List<ItemStack> stacks;

        public Slot(int relx, int rely, List<ItemStack> stacks) {
            this.relx = relx;
            this.rely = rely;
            this.stacks = stacks;
        }
    }

    public static class Entry {

        public final String handlerName;
        public final String recipeName;
        public final List<Slot> ingredients;
        public final List<Slot> others;
        public final @Nullable Slot result;

        public Entry(String handlerName, String recipeName, List<Slot> ingredients, List<Slot> others,
            @Nullable Slot result) {
            this.handlerName = handlerName;
            this.recipeName = recipeName;
            this.ingredients = ingredients;
            this.others = others;
            this.result = result;
        }
    }

    public static List<Entry> findCraftingRecipes(ItemStack target) {
        if (!AVAILABLE || target == null) return Collections.emptyList();
        try {
            return processHandlers(NeiDirectCalls.getCraftingHandlers(target));
        } catch (Throwable t) {
            LOG.warn("NEI crafting query failed", t);
            return Collections.emptyList();
        }
    }

    public static List<Entry> findUsages(ItemStack target) {
        if (!AVAILABLE || target == null) return Collections.emptyList();
        try {
            return processHandlers(NeiDirectCalls.getUsageHandlers(target));
        } catch (Throwable t) {
            LOG.warn("NEI usage query failed", t);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the raw {@code IRecipeHandler} instances matching {@code target}. Caller must check
     * {@link #lookupNumRecipes(Object)} before iterating recipe indices.
     */
    public static List<Object> queryRawCraftingHandlers(ItemStack target) {
        if (!AVAILABLE || target == null) return Collections.emptyList();
        try {
            return NeiDirectCalls.getCraftingHandlers(target);
        } catch (Throwable t) {
            LOG.warn("queryRawCraftingHandlers failed", t);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the raw {@code IUsageHandler} instances matching {@code target}. These cover handlers
     * that consume {@code target} as an input (anvil / fuel / brewing ingredient).
     */
    public static List<Object> queryRawUsageHandlers(ItemStack target) {
        if (!AVAILABLE || target == null) return Collections.emptyList();
        try {
            return NeiDirectCalls.getUsageHandlers(target);
        } catch (Throwable t) {
            LOG.warn("queryRawUsageHandlers failed", t);
            return Collections.emptyList();
        }
    }

    public static int lookupNumRecipes(Object handler) {
        if (!AVAILABLE || handler == null) return 0;
        try {
            return NeiDirectCalls.numRecipes(handler);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String lookupHandlerName(Object handler) {
        if (!AVAILABLE || handler == null) return "";
        try {
            return NeiDirectCalls.recipeName(handler);
        } catch (Throwable t) {
            return "";
        }
    }

    public static @Nullable String lookupOverlayIdentifier(Object handler) {
        if (!AVAILABLE || handler == null) return null;
        try {
            return NeiDirectCalls.overlayId(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void callOnUpdate(Object handler) {
        if (!AVAILABLE || handler == null) return;
        try {
            NeiDirectCalls.onUpdate(handler);
        } catch (Throwable ignored) {}
    }

    public static void callDrawBackground(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return;
        try {
            NeiDirectCalls.drawBackground(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawForeground(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return;
        try {
            NeiDirectCalls.drawForeground(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawExtras(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return;
        try {
            NeiDirectCalls.drawExtras(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    /**
     * Append handler-specific tooltip lines for a hovered stack.
     */
    public static void appendItemTooltip(Object handler, ItemStack stack, List<String> tooltip, int recipeIndex) {
        if (!AVAILABLE || handler == null || stack == null || tooltip == null) return;
        try {
            NeiDirectCalls.handleItemTooltip(handler, stack, tooltip, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static int lookupRecipeHeight(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return 0;
        try {
            return NeiDirectCalls.recipeHeight(handler, recipeIndex);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static List<Slot> readIngredientSlots(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return Collections.emptyList();
        try {
            return readSlotList(NeiDirectCalls.ingredientStacks(handler, recipeIndex));
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static List<Slot> readOtherSlots(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return Collections.emptyList();
        try {
            return readSlotList(NeiDirectCalls.otherStacks(handler, recipeIndex));
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns {@code true} if invoking {@code getOtherStacks} on this handler throws an exception.
     * Used to skip {@code drawForeground}/{@code drawExtras} for broken handlers, keeping the log
     * clean.
     */
    public static boolean otherStacksThrows(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return false;
        try {
            return NeiDirectCalls.otherStacksThrows(handler, recipeIndex);
        } catch (Throwable t) {
            return false;
        }
    }

    public static @Nullable Slot readResultSlot(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null) return null;
        try {
            return readSlot(NeiDirectCalls.resultStack(handler, recipeIndex));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Returns the {@code HandlerInfo} display stack for a handler's recipe tab icon. */
    public static @Nullable ItemStack lookupHandlerIcon(Object handler) {
        if (!AVAILABLE || handler == null) return null;
        try {
            return NeiDirectCalls.handlerIconStack(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the raw {@code DrawableResource} for this handler's tab image as an opaque
     * {@code Object}, or {@code null} when absent. Pass the result to {@link #drawableWidth},
     * {@link #drawableHeight}, and {@link #drawHandlerImage}.
     */
    public static @Nullable Object lookupHandlerImage(Object handler) {
        if (!AVAILABLE || handler == null) return null;
        try {
            return NeiDirectCalls.handlerImage(handler);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int lookupHandlerWidth(Object handler) {
        if (!AVAILABLE || handler == null) return 166;
        try {
            return NeiDirectCalls.handlerWidth(handler);
        } catch (Throwable t) {
            return 166;
        }
    }

    public static int lookupHandlerHeight(Object handler) {
        if (!AVAILABLE || handler == null) return 65;
        try {
            return NeiDirectCalls.handlerHeight(handler);
        } catch (Throwable t) {
            return 65;
        }
    }

    public static int lookupHandlerYShift(Object handler) {
        if (!AVAILABLE || handler == null) return 0;
        try {
            return NeiDirectCalls.handlerYShift(handler);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Native pixel width of a {@code DrawableResource} (includes padding). */
    public static int drawableWidth(Object drawable) {
        if (drawable == null || !AVAILABLE) return 0;
        try {
            return NeiDirectCalls.drawableWidth(drawable);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Native pixel height of a {@code DrawableResource} (includes padding). */
    public static int drawableHeight(Object drawable) {
        if (drawable == null || !AVAILABLE) return 0;
        try {
            return NeiDirectCalls.drawableHeight(drawable);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Invoke {@code DrawableResource.draw(x, y)} at native pixel size. Callers that need scaling
     * should wrap the call in {@code glPushMatrix / glScalef / glPopMatrix}.
     */
    public static void drawHandlerImage(Object drawable, int x, int y) {
        if (drawable == null || !AVAILABLE) return;
        try {
            NeiDirectCalls.drawDrawable(drawable, x, y);
        } catch (Throwable ignored) {}
    }

    public static @Nullable Entry[] readHandler(Object handler) {
        try {
            int n = NeiDirectCalls.numRecipes(handler);
            if (n <= 0) return new Entry[0];
            String recipeName = NeiDirectCalls.recipeName(handler);
            String handlerName = handler.getClass()
                .getSimpleName();
            Entry[] out = new Entry[n];
            for (int i = 0; i < n; i++) {
                List<Slot> ing = readSlotList(NeiDirectCalls.ingredientStacks(handler, i));
                List<Slot> oth = readSlotList(NeiDirectCalls.otherStacks(handler, i));
                Slot res = readSlot(NeiDirectCalls.resultStack(handler, i));
                out[i] = new Entry(handlerName, recipeName, ing, oth, res);
            }
            return out;
        } catch (Throwable t) {
            LOG.debug("NEI handler {} read failed", handler.getClass(), t);
            return null;
        }
    }

    public static List<Slot> readSlotList(Object obj) {
        if (!(obj instanceof List)) return Collections.emptyList();
        List<Slot> out = new ArrayList<>();
        for (Object ps : (List<?>) obj) {
            Slot s = readSlot(ps);
            if (s != null) out.add(s);
        }
        return out;
    }

    public static @Nullable Slot readSlot(@Nullable Object ps) {
        if (ps == null) return null;
        try {
            int relx = NeiDirectCalls.relX(ps);
            int rely = NeiDirectCalls.relY(ps);
            List<ItemStack> stacks = new ArrayList<>();
            ItemStack[] itemsArr = NeiDirectCalls.items(ps);
            if (itemsArr != null) {
                for (ItemStack s : itemsArr) {
                    if (s != null) stacks.add(s);
                }
            }
            if (stacks.isEmpty()) {
                ItemStack single = NeiDirectCalls.item(ps);
                if (single != null) stacks.add(single);
            }
            if (stacks.isEmpty()) return null;
            return new Slot(relx, rely, stacks);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String safeString(@Nullable Object o) {
        return o == null ? "" : o.toString();
    }

    private static List<Entry> processHandlers(List<Object> handlers) {
        List<Entry> out = new ArrayList<>();
        for (Object handler : handlers) {
            if (handler == null) continue;
            Entry[] entries = readHandler(handler);
            if (entries != null) out.addAll(Arrays.asList(entries));
        }
        return out;
    }
}
