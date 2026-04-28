package com.hfstudio.guidenh.guide.internal.recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reflection bridge to NEI (codechicken.nei.*). All methods are null-safe; when NEI is absent,
 * {@link #isAvailable()} returns false and every query returns an empty result.
 *
 * <p>
 * Bound entry points (see explorer report):
 * <ul>
 * <li>{@code GuiCraftingRecipe.getCraftingHandlers(String, Object...)}</li>
 * <li>{@code IRecipeHandler} drawBackground / drawForeground / numRecipes /
 * get{Ingredient,Result,Other}Stacks / getHandlerId / getRecipeName / getOverlayIdentifier /
 * handleItemTooltip / onUpdate</li>
 * <li>{@code TemplateRecipeHandler} drawExtras / getGuiTexture</li>
 * <li>{@code PositionedStack.relx/rely/items/item}</li>
 * <li>{@code GuiRecipeTab.handlerMap} + {@code HandlerInfo.getItemStack/getWidth/getHeight/getYShift}</li>
 * </ul>
 */
public class NeiRecipeLookup {

    private static final Logger LOG = LoggerFactory.getLogger(NeiRecipeLookup.class);

    private static final boolean AVAILABLE;
    private static final @Nullable Method GET_CRAFTING_HANDLERS;
    private static final @Nullable Method GET_USAGE_HANDLERS;
    private static final @Nullable Method H_NUM_RECIPES;
    private static final @Nullable Method H_GET_INGREDIENTS;
    private static final @Nullable Method H_GET_RESULT;
    private static final @Nullable Method H_GET_OTHERS;
    private static final @Nullable Method H_GET_HANDLER_ID;
    private static final @Nullable Method H_GET_RECIPE_NAME;
    private static final @Nullable Method H_DRAW_BACKGROUND;
    private static final @Nullable Method H_DRAW_FOREGROUND;
    private static final @Nullable Method H_ON_UPDATE;
    private static final @Nullable Method H_GET_RECIPE_HEIGHT;
    private static final @Nullable Method H_GET_OVERLAY_IDENTIFIER;
    private static final @Nullable Method H_HANDLE_ITEM_TOOLTIP;
    private static final @Nullable Method TMPL_DRAW_EXTRAS;
    private static final @Nullable Method TMPL_FIND_FUELS_ONCE;
    private static final @Nullable Method TMPL_GET_GUI_TEXTURE;
    private static final @Nullable Field PS_RELX;
    private static final @Nullable Field PS_RELY;
    private static final @Nullable Field PS_ITEMS;
    private static final @Nullable Field PS_ITEM;
    private static final @Nullable Field TAB_HANDLER_MAP;
    private static final @Nullable Method INFO_GET_ITEMSTACK;
    private static final @Nullable Method INFO_GET_IMAGE;
    private static final @Nullable Method INFO_GET_WIDTH;
    private static final @Nullable Method INFO_GET_HEIGHT;
    private static final @Nullable Method INFO_GET_Y_SHIFT;
    private static final @Nullable Method DRAWABLE_GET_WIDTH;
    private static final @Nullable Method DRAWABLE_GET_HEIGHT;
    private static final @Nullable Method DRAWABLE_DRAW;
    private static final @Nullable Class<?> CLASS_GUI_RECIPE;
    private static final @Nullable Class<?> CLASS_TEMPLATE_HANDLER;

    static {
        Method gch = null, guh = null, nr = null, gi = null, gr = null, go = null, ghd = null, grn = null;
        Method drawBg = null, drawFg = null, onUp = null, getRecipeHeight = null, getOverlay = null,
            handleItemTt = null;
        Method drawExtras = null, findFuelsOnce = null, getGuiTexture = null;
        Method infoGetStack = null, infoGetImage = null, infoGetW = null, infoGetH = null, infoGetYShift = null;
        Method drawableGetW = null, drawableGetH = null, drawableDraw = null;
        Field prx = null, pry = null, pits = null, pit = null, handlerMap = null;
        Class<?> guiRecipe = null, templateHandler = null;
        boolean ok = false;
        // Short-circuit when NEI is not present at all; avoids a meaningless ClassNotFoundException in logs.
        boolean modPresent;
        try {
            modPresent = cpw.mods.fml.common.Loader.isModLoaded("NotEnoughItems");
        } catch (Throwable t) {
            modPresent = true; // Loader itself missing is unreachable in Forge, but be safe.
        }
        if (!modPresent) {
            LOG.info("NEI mod not loaded; GuideNH recipe rendering falls back to vanilla.");
        } else try {
            Class<?> guiCrafting = Class.forName("codechicken.nei.recipe.GuiCraftingRecipe");
            Class<?> guiUsage = Class.forName("codechicken.nei.recipe.GuiUsageRecipe");
            Class<?> recipeHandler = Class.forName("codechicken.nei.recipe.IRecipeHandler");
            Class<?> positioned = Class.forName("codechicken.nei.PositionedStack");
            templateHandler = Class.forName("codechicken.nei.recipe.TemplateRecipeHandler");
            guiRecipe = Class.forName("codechicken.nei.recipe.GuiRecipe");
            Class<?> guiRecipeTab = Class.forName("codechicken.nei.recipe.GuiRecipeTab");
            Class<?> handlerInfo = Class.forName("codechicken.nei.recipe.HandlerInfo");

            gch = guiCrafting.getMethod("getCraftingHandlers", String.class, Object[].class);
            guh = guiUsage.getMethod("getUsageHandlers", String.class, Object[].class);
            nr = recipeHandler.getMethod("numRecipes");
            gi = recipeHandler.getMethod("getIngredientStacks", int.class);
            gr = recipeHandler.getMethod("getResultStack", int.class);
            go = recipeHandler.getMethod("getOtherStacks", int.class);
            ghd = recipeHandler.getMethod("getHandlerId");
            grn = recipeHandler.getMethod("getRecipeName");
            drawBg = recipeHandler.getMethod("drawBackground", int.class);
            drawFg = recipeHandler.getMethod("drawForeground", int.class);
            onUp = recipeHandler.getMethod("onUpdate");
            getRecipeHeight = recipeHandler.getMethod("getRecipeHeight", int.class);
            getOverlay = recipeHandler.getMethod("getOverlayIdentifier");
            handleItemTt = recipeHandler
                .getMethod("handleItemTooltip", guiRecipe, ItemStack.class, List.class, int.class);

            drawExtras = templateHandler.getMethod("drawExtras", int.class);
            findFuelsOnce = templateHandler.getMethod("findFuelsOnce");
            getGuiTexture = templateHandler.getMethod("getGuiTexture");

            prx = positioned.getField("relx");
            pry = positioned.getField("rely");
            pits = positioned.getField("items");
            pit = positioned.getField("item");

            handlerMap = guiRecipeTab.getField("handlerMap");
            infoGetStack = handlerInfo.getMethod("getItemStack");
            infoGetW = handlerInfo.getMethod("getWidth");
            infoGetH = handlerInfo.getMethod("getHeight");
            infoGetYShift = handlerInfo.getMethod("getYShift");
            // HandlerInfo.getImage / DrawableResource are optional — only present on newer NEI builds.
            // Failing to bind them must not abort the whole reflection block, so try them in a
            // nested try/catch that leaves everything else usable.
            try {
                infoGetImage = handlerInfo.getMethod("getImage");
                Class<?> drawable = Class.forName("codechicken.nei.drawable.DrawableResource");
                drawableGetW = drawable.getMethod("getWidth");
                drawableGetH = drawable.getMethod("getHeight");
                drawableDraw = drawable.getMethod("draw", int.class, int.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                LOG.debug("NEI DrawableResource binding unavailable: {}", e.toString());
                infoGetImage = null;
                drawableGetW = drawableGetH = drawableDraw = null;
            }
            ok = true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            LOG.info("NEI not present or incompatible; falling back to vanilla RecipeLookup. Reason: {}", e.toString());
        } catch (Throwable t) {
            LOG.warn("Failed to bind NEI reflection", t);
        }
        AVAILABLE = ok;
        GET_CRAFTING_HANDLERS = gch;
        GET_USAGE_HANDLERS = guh;
        H_NUM_RECIPES = nr;
        H_GET_INGREDIENTS = gi;
        H_GET_RESULT = gr;
        H_GET_OTHERS = go;
        H_GET_HANDLER_ID = ghd;
        H_GET_RECIPE_NAME = grn;
        H_DRAW_BACKGROUND = drawBg;
        H_DRAW_FOREGROUND = drawFg;
        H_ON_UPDATE = onUp;
        H_GET_RECIPE_HEIGHT = getRecipeHeight;
        H_GET_OVERLAY_IDENTIFIER = getOverlay;
        H_HANDLE_ITEM_TOOLTIP = handleItemTt;
        TMPL_DRAW_EXTRAS = drawExtras;
        TMPL_FIND_FUELS_ONCE = findFuelsOnce;
        TMPL_GET_GUI_TEXTURE = getGuiTexture;
        PS_RELX = prx;
        PS_RELY = pry;
        PS_ITEMS = pits;
        PS_ITEM = pit;
        TAB_HANDLER_MAP = handlerMap;
        INFO_GET_ITEMSTACK = infoGetStack;
        INFO_GET_IMAGE = infoGetImage;
        INFO_GET_WIDTH = infoGetW;
        INFO_GET_HEIGHT = infoGetH;
        INFO_GET_Y_SHIFT = infoGetYShift;
        DRAWABLE_GET_WIDTH = drawableGetW;
        DRAWABLE_GET_HEIGHT = drawableGetH;
        DRAWABLE_DRAW = drawableDraw;
        CLASS_GUI_RECIPE = guiRecipe;
        CLASS_TEMPLATE_HANDLER = templateHandler;
    }

    private NeiRecipeLookup() {}

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
        return queryHandlers(GET_CRAFTING_HANDLERS, "item", new Object[] { target });
    }

    public static List<Entry> findUsages(ItemStack target) {
        if (!AVAILABLE || target == null) return Collections.emptyList();
        return queryHandlers(GET_USAGE_HANDLERS, "item", new Object[] { target });
    }

    /**
     * Query NEI for all crafting handlers matching {@code target} and return the raw
     * {@code IRecipeHandler} instances (ready for drawBackground/drawForeground). Caller must check
     * {@link #lookupNumRecipes(Object)} before iterating recipe indices.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> queryRawCraftingHandlers(ItemStack target) {
        if (!AVAILABLE || target == null || GET_CRAFTING_HANDLERS == null) return Collections.emptyList();
        try {
            Object handlers = GET_CRAFTING_HANDLERS.invoke(null, "item", new Object[] { target });
            if (handlers instanceof List) {
                List<Object> src = (List<Object>) handlers;
                List<Object> out = new ArrayList<>(src.size());
                for (Object h : src) if (h != null) out.add(h);
                return out;
            }
        } catch (Throwable t) {
            LOG.debug("queryRawCraftingHandlers failed", t);
        }
        return Collections.emptyList();
    }

    /**
     * Query NEI for all usage handlers matching {@code target}. These cover handlers that treat the
     * target stack as an <em>input</em> rather than a product: anvil (repair), fuel, brewing
     * ingredient, etc.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> queryRawUsageHandlers(ItemStack target) {
        if (!AVAILABLE || target == null || GET_USAGE_HANDLERS == null) return Collections.emptyList();
        try {
            Object handlers = GET_USAGE_HANDLERS.invoke(null, "item", new Object[] { target });
            if (handlers instanceof List) {
                List<Object> src = (List<Object>) handlers;
                List<Object> out = new ArrayList<>(src.size());
                for (Object h : src) if (h != null) out.add(h);
                return out;
            }
        } catch (Throwable t) {
            LOG.debug("queryRawUsageHandlers failed", t);
        }
        return Collections.emptyList();
    }

    public static int lookupNumRecipes(Object handler) {
        if (!AVAILABLE || handler == null || H_NUM_RECIPES == null) return 0;
        try {
            Object v = H_NUM_RECIPES.invoke(handler);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String lookupHandlerName(Object handler) {
        if (!AVAILABLE || handler == null || H_GET_RECIPE_NAME == null) return "";
        try {
            return safeString(H_GET_RECIPE_NAME.invoke(handler));
        } catch (Throwable t) {
            return "";
        }
    }

    private static @Nullable String lookupHandlerId(Object handler) {
        if (!AVAILABLE || handler == null || H_GET_HANDLER_ID == null) return null;
        try {
            Object v = H_GET_HANDLER_ID.invoke(handler);
            return v == null ? null : v.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static @Nullable String lookupOverlayIdentifier(Object handler) {
        if (!AVAILABLE || handler == null || H_GET_OVERLAY_IDENTIFIER == null) return null;
        try {
            Object v = H_GET_OVERLAY_IDENTIFIER.invoke(handler);
            return v == null ? null : v.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Reflective access to {@code GuiRecipeTab.getHandlerInfo(handler).getItemStack()}. */
    public static @Nullable ItemStack lookupHandlerIcon(Object handler) {
        if (!AVAILABLE || handler == null || TAB_HANDLER_MAP == null || INFO_GET_ITEMSTACK == null) return null;
        try {
            Object info = lookupHandlerInfo(handler);
            if (info == null) return null;
            Object stack = INFO_GET_ITEMSTACK.invoke(info);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Reflective access to {@code HandlerInfo.getImage()}. Returns the raw
     * {@code codechicken.nei.drawable.DrawableResource} so callers can hand it back into
     * {@link #drawHandlerImage(Object, int, int)} without leaking NEI types into the rest of the
     * code base. Returns {@code null} when the handler has no image or when NEI is too old to
     * expose {@code getImage}.
     */
    public static @Nullable Object lookupHandlerImage(Object handler) {
        if (!AVAILABLE || handler == null || TAB_HANDLER_MAP == null || INFO_GET_IMAGE == null) return null;
        try {
            Object info = lookupHandlerInfo(handler);
            if (info == null) return null;
            return INFO_GET_IMAGE.invoke(info);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Native pixel width of a {@code DrawableResource} (includes padding). */
    public static int drawableWidth(Object drawable) {
        if (drawable == null || DRAWABLE_GET_WIDTH == null) return 0;
        try {
            Object v = DRAWABLE_GET_WIDTH.invoke(drawable);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Native pixel height of a {@code DrawableResource} (includes padding). */
    public static int drawableHeight(Object drawable) {
        if (drawable == null || DRAWABLE_GET_HEIGHT == null) return 0;
        try {
            Object v = DRAWABLE_GET_HEIGHT.invoke(drawable);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Invoke {@code DrawableResource.draw(xOffset, yOffset)} at native pixel size. Callers that
     * need scaling should wrap the call in {@code glPushMatrix / glScalef / glPopMatrix} themselves
     * \u2014 the draw itself binds its own texture and enables {@code GL_TEXTURE_2D}.
     */
    public static void drawHandlerImage(Object drawable, int x, int y) {
        if (drawable == null || DRAWABLE_DRAW == null) return;
        try {
            DRAWABLE_DRAW.invoke(drawable, x, y);
        } catch (Throwable ignored) {}
    }

    public static int lookupHandlerWidth(Object handler) {
        return lookupHandlerDimension(handler, INFO_GET_WIDTH, 166);
    }

    public static int lookupHandlerHeight(Object handler) {
        return lookupHandlerDimension(handler, INFO_GET_HEIGHT, 65);
    }

    public static int lookupHandlerYShift(Object handler) {
        return lookupHandlerDimension(handler, INFO_GET_Y_SHIFT, 0);
    }

    public static int lookupRecipeHeight(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_GET_RECIPE_HEIGHT == null) return 0;
        try {
            Object v = H_GET_RECIPE_HEIGHT.invoke(handler, recipeIndex);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int lookupHandlerDimension(Object handler, @Nullable Method getter, int fallback) {
        if (!AVAILABLE || handler == null || getter == null || TAB_HANDLER_MAP == null) return fallback;
        try {
            Object info = lookupHandlerInfo(handler);
            if (info == null) return fallback;
            Object v = getter.invoke(info);
            return v instanceof Integer ? (Integer) v : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object lookupHandlerInfo(Object handler) throws IllegalAccessException {
        if (TAB_HANDLER_MAP == null) return null;
        Object rawMap = TAB_HANDLER_MAP.get(null);
        if (!(rawMap instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) rawMap;
        return resolveHandlerInfo(
            map,
            lookupHandlerId(handler),
            lookupOverlayIdentifier(handler),
            handler.getClass()
                .getName(),
            handler.getClass()
                .getSimpleName());
    }

    private static @Nullable Object resolveHandlerInfo(Map<String, Object> handlerMap, @Nullable String handlerId,
        @Nullable String overlayId, String className, String simpleName) {
        Object info = handlerId == null ? null : handlerMap.get(handlerId);
        if (info != null) return info;
        info = overlayId == null ? null : handlerMap.get(overlayId);
        if (info != null) return info;
        // Fallback for older/local tests that still key HandlerInfo by handler class name.
        info = handlerMap.get(className);
        if (info != null) return info;
        return handlerMap.get(simpleName);
    }

    public static void callOnUpdate(Object handler) {
        if (!AVAILABLE || handler == null || H_ON_UPDATE == null) return;
        try {
            H_ON_UPDATE.invoke(handler);
        } catch (Throwable ignored) {}
    }

    public static void callDrawBackground(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_DRAW_BACKGROUND == null) return;
        try {
            H_DRAW_BACKGROUND.invoke(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawForeground(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_DRAW_FOREGROUND == null) return;
        try {
            H_DRAW_FOREGROUND.invoke(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    public static void callDrawExtras(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || TMPL_DRAW_EXTRAS == null || CLASS_TEMPLATE_HANDLER == null) return;
        if (!CLASS_TEMPLATE_HANDLER.isInstance(handler)) return;
        try {
            TMPL_DRAW_EXTRAS.invoke(handler, recipeIndex);
        } catch (Throwable ignored) {}
    }

    /**
     * Append handler-specific tooltip lines for a hovered stack. NEI's built-in handlers do not
     * read GuiRecipe fields here, but third-party handlers might — hence the Throwable catch.
     */
    @SuppressWarnings("unchecked")
    public static void appendItemTooltip(Object handler, ItemStack stack, List<String> tooltip, int recipeIndex) {
        if (!AVAILABLE || handler == null || stack == null || tooltip == null || H_HANDLE_ITEM_TOOLTIP == null) return;
        try {
            Object result = H_HANDLE_ITEM_TOOLTIP.invoke(handler, null, stack, tooltip, recipeIndex);
            if (result instanceof List && result != tooltip) {
                tooltip.clear();
                tooltip.addAll((List<String>) result);
            }
        } catch (Throwable ignored) {}
    }

    public static List<Slot> readIngredientSlots(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_GET_INGREDIENTS == null) return Collections.emptyList();
        try {
            return readSlotList(H_GET_INGREDIENTS.invoke(handler, recipeIndex));
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static List<Slot> readOtherSlots(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_GET_OTHERS == null) return Collections.emptyList();
        if (shouldSkipOtherSlotLookup(handler)) return Collections.emptyList();
        try {
            return readSlotList(H_GET_OTHERS.invoke(handler, recipeIndex));
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static @Nullable Slot readResultSlot(Object handler, int recipeIndex) {
        if (!AVAILABLE || handler == null || H_GET_RESULT == null) return null;
        try {
            return readSlot(H_GET_RESULT.invoke(handler, recipeIndex));
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Entry> queryHandlers(Method lookupMethod, String kind, Object[] args) {
        if (lookupMethod == null) return Collections.emptyList();
        try {
            Object handlers = lookupMethod.invoke(null, kind, args);
            if (!(handlers instanceof List)) return Collections.emptyList();
            List<Entry> out = new ArrayList<>();
            for (Object handler : (List<Object>) handlers) {
                if (handler == null) continue;
                Entry[] entries = readHandler(handler);
                if (entries != null) out.addAll(Arrays.asList(entries));
            }
            return out;
        } catch (Throwable t) {
            LOG.warn("NEI query failed", t);
            return Collections.emptyList();
        }
    }

    private static @Nullable Entry[] readHandler(Object handler) {
        try {
            int n = (int) H_NUM_RECIPES.invoke(handler);
            if (n <= 0) return new Entry[0];
            String recipeName = safeString(H_GET_RECIPE_NAME.invoke(handler));
            String handlerName = handler.getClass()
                .getSimpleName();
                Entry[] out = new Entry[n];
            for (int i = 0; i < n; i++) {
                List<Slot> ing = readIngredientSlots(handler, i);
                List<Slot> oth = readOtherSlots(handler, i);
                Slot res = readResultSlot(handler, i);
                out[i] = new Entry(handlerName, recipeName, ing, oth, res);
            }
            return out;
        } catch (Throwable t) {
            LOG.debug("NEI handler {} read failed", handler.getClass(), t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Slot> readSlotList(Object obj) {
        if (!(obj instanceof List)) return Collections.emptyList();
        List<Slot> out = new ArrayList<>();
        for (Object ps : (List<Object>) obj) {
            Slot s = readSlot(ps);
            if (s != null) out.add(s);
        }
        return out;
    }

    private static @Nullable Slot readSlot(@Nullable Object ps) {
        if (ps == null) return null;
        try {
            int relx = PS_RELX.getInt(ps);
            int rely = PS_RELY.getInt(ps);
            List<ItemStack> stacks = new ArrayList<>();
            Object itemsArr = PS_ITEMS.get(ps);
            if (itemsArr instanceof ItemStack[]arr) {
                for (ItemStack s : arr) {
                    if (s != null && s.stackSize > 0) stacks.add(s);
                }
            }
            if (stacks.isEmpty()) {
                Object single = PS_ITEM.get(ps);
                if (single instanceof ItemStack s && s.stackSize > 0) stacks.add(s);
            }
            if (stacks.isEmpty()) return null;
            return new Slot(relx, rely, stacks);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean shouldSkipOtherSlotLookup(Object handler) {
        if (!isFurnaceRecipeHandler(handler)) return false;
        ensureFurnaceFuelCacheInitialized();
        return isStaticCollectionFieldEmpty(handler.getClass(), "afuels");
    }

    private static boolean isFurnaceRecipeHandler(Object handler) {
        if (handler == null) return false;
        for (Class<?> type = handler.getClass(); type != null; type = type.getSuperclass()) {
            if ("FurnaceRecipeHandler".equals(type.getSimpleName())) return true;
        }
        return false;
    }

    private static void ensureFurnaceFuelCacheInitialized() {
        if (TMPL_FIND_FUELS_ONCE == null) return;
        try {
            TMPL_FIND_FUELS_ONCE.invoke(null);
        } catch (Throwable ignored) {}
    }

    private static boolean isStaticCollectionFieldEmpty(Class<?> type, String fieldName) {
        Field field = findFieldInHierarchy(type, fieldName);
        if (field == null || !Modifier.isStatic(field.getModifiers())) return false;
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            if (value == null) return true;
            return value instanceof Collection<?> collection && collection.isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    private static @Nullable Field findFieldInHierarchy(Class<?> type, String fieldName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static String safeString(@Nullable Object o) {
        return o == null ? "" : o.toString();
    }
}
