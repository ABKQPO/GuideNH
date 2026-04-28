package com.hfstudio.guidenh.guide.internal.recipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeLookup {

    private static final Logger LOG = LoggerFactory.getLogger(RecipeLookup.class);

    private static Field SHAPED_ORE_INPUT;
    private static Field SHAPED_ORE_WIDTH;
    private static Field SHAPED_ORE_HEIGHT;
    private static Field SHAPELESS_ORE_INPUT;

    static {
        try {
            SHAPED_ORE_INPUT = ShapedOreRecipe.class.getDeclaredField("input");
            SHAPED_ORE_INPUT.setAccessible(true);
            SHAPED_ORE_WIDTH = ShapedOreRecipe.class.getDeclaredField("width");
            SHAPED_ORE_WIDTH.setAccessible(true);
            SHAPED_ORE_HEIGHT = ShapedOreRecipe.class.getDeclaredField("height");
            SHAPED_ORE_HEIGHT.setAccessible(true);
            SHAPELESS_ORE_INPUT = ShapelessOreRecipe.class.getDeclaredField("input");
            SHAPELESS_ORE_INPUT.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOG.warn("Failed to reflect ShapedOreRecipe fields; OreDict recipes will be ignored", e);
        }
    }

    private RecipeLookup() {}

    public static class Entry {

        public final ItemStack[] input3x3 = new ItemStack[9];
        public ItemStack result;
        public boolean shapeless;
    }

    public static List<Entry> findByOutput(Item target) {
        if (target == null) return Collections.emptyList();
        List<Entry> out = new ArrayList<>();
        List<IRecipe> list = CraftingManager.getInstance()
            .getRecipeList();
        for (IRecipe r : list) {
            ItemStack res = r.getRecipeOutput();
            if (res == null || res.getItem() != target) continue;
            Entry e = convert(r);
            if (e != null) out.add(e);
        }
        return out;
    }

    @Nullable
    public static Entry convert(IRecipe r) {
        if (r instanceof ShapedRecipes) {
            return fromShaped((ShapedRecipes) r);
        } else if (r instanceof ShapelessRecipes) {
            return fromShapeless((ShapelessRecipes) r);
        } else if (r instanceof ShapedOreRecipe) {
            return fromShapedOre((ShapedOreRecipe) r);
        } else if (r instanceof ShapelessOreRecipe) {
            return fromShapelessOre((ShapelessOreRecipe) r);
        }
        return null;
    }

    private static Entry fromShaped(ShapedRecipes r) {
        Entry e = new Entry();
        e.result = r.getRecipeOutput();
        e.shapeless = false;
        for (int row = 0; row < r.recipeHeight; row++) {
            for (int col = 0; col < r.recipeWidth; col++) {
                int srcIdx = row * r.recipeWidth + col;
                if (srcIdx < r.recipeItems.length) {
                    e.input3x3[row * 3 + col] = copy(r.recipeItems[srcIdx]);
                }
            }
        }
        return e;
    }

    private static Entry fromShapeless(ShapelessRecipes r) {
        Entry e = new Entry();
        e.result = r.getRecipeOutput();
        e.shapeless = true;
        List<ItemStack> items = r.recipeItems;
        int n = Math.min(9, items.size());
        for (int i = 0; i < n; i++) e.input3x3[i] = copy(items.get(i));
        return e;
    }

    @Nullable
    private static Entry fromShapedOre(ShapedOreRecipe r) {
        if (SHAPED_ORE_INPUT == null) return null;
        try {
            Object[] input = (Object[]) SHAPED_ORE_INPUT.get(r);
            int w = SHAPED_ORE_WIDTH.getInt(r);
            int h = SHAPED_ORE_HEIGHT.getInt(r);
            Entry e = new Entry();
            e.result = r.getRecipeOutput();
            e.shapeless = false;
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int srcIdx = row * w + col;
                    if (srcIdx < input.length) {
                        e.input3x3[row * 3 + col] = resolveOre(input[srcIdx]);
                    }
                }
            }
            return e;
        } catch (IllegalAccessException ex) {
            LOG.warn("ShapedOreRecipe reflection failed", ex);
            return null;
        }
    }

    @Nullable
    private static Entry fromShapelessOre(ShapelessOreRecipe r) {
        if (SHAPELESS_ORE_INPUT == null) return null;
        try {
            @SuppressWarnings("unchecked")
            List<Object> input = (List<Object>) SHAPELESS_ORE_INPUT.get(r);
            Entry e = new Entry();
            e.result = r.getRecipeOutput();
            e.shapeless = true;
            int n = Math.min(9, input.size());
            for (int i = 0; i < n; i++) e.input3x3[i] = resolveOre(input.get(i));
            return e;
        } catch (IllegalAccessException ex) {
            LOG.warn("ShapelessOreRecipe reflection failed", ex);
            return null;
        }
    }

    @Nullable
    private static ItemStack resolveOre(Object o) {
        if (o == null) return null;
        if (o instanceof ItemStack) {
            return copy((ItemStack) o);
        }
        if (o instanceof List<?>list) {
            if (!list.isEmpty() && list.get(0) instanceof ItemStack) {
                return copy((ItemStack) list.get(0));
            }
        }
        return null;
    }

    @Nullable
    private static ItemStack copy(@Nullable ItemStack s) {
        if (s == null) return null;
        ItemStack c = s.copy();
        if (c.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            c.setItemDamage(0);
        }
        if (c.stackSize <= 0) c.stackSize = 1;
        return c;
    }

    public static List<ItemStack> asList(Entry e) {
        return Arrays.asList(e.input3x3);
    }
}
