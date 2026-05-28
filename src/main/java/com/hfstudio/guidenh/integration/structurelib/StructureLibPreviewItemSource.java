package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.structure.IItemSource;

public class StructureLibPreviewItemSource implements IItemSource {

    private final List<ItemStack> preferredStacks;

    public StructureLibPreviewItemSource(List<ItemStack> preferredStacks) {
        this.preferredStacks = immutableCopies(preferredStacks);
    }

    public static StructureLibPreviewItemSource create() {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (StructureLibPreviewItemProvider provider : StructureLibControllerIntegrationRegistry.global()
            .previewItemProviders()) {
            provider.appendPreviewItems(stacks);
        }
        return new StructureLibPreviewItemSource(stacks);
    }

    @Nonnull
    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
        LinkedHashMap<ItemStack, Integer> result = new LinkedHashMap<>();
        if (predicate == null || count <= 0) {
            return result;
        }
        ItemStack stack = findMatchingStack(predicate);
        if (stack != null) {
            result.put(stack, count);
        }
        return result;
    }

    @Override
    public boolean takeOne(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null || stack.stackSize != 1) {
            throw new IllegalArgumentException();
        }
        return true;
    }

    @Override
    public boolean takeAll(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException();
        }
        return true;
    }

    @Override
    public ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
        if (predicate == null) {
            throw new IllegalArgumentException();
        }
        return findMatchingStack(predicate);
    }

    @Nullable
    private ItemStack findMatchingStack(Predicate<ItemStack> predicate) {
        for (ItemStack preferredStack : preferredStacks) {
            ItemStack copy = copyWithSingleSize(preferredStack);
            if (matches(predicate, copy)) {
                return copy;
            }
        }
        for (Object object : Item.itemRegistry) {
            if (!(object instanceof Item item)) {
                continue;
            }
            ItemStack stack = findMatchingItemStack(item, predicate);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    @Nullable
    private ItemStack findMatchingItemStack(Item item, Predicate<ItemStack> predicate) {
        if (item instanceof ItemBlock itemBlock) {
            for (int meta = 0; meta <= 15; meta++) {
                ItemStack stack = new ItemStack(item, 1, meta);
                if (matches(predicate, stack)) {
                    return stack;
                }
            }
        }
        ItemStack stack = new ItemStack(item, 1, 0);
        return matches(predicate, stack) ? stack : null;
    }

    private boolean matches(Predicate<ItemStack> predicate, ItemStack stack) {
        try {
            return stack != null && stack.getItem() != null && predicate.test(stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<ItemStack> immutableCopies(List<ItemStack> source) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if (source != null) {
            for (ItemStack stack : source) {
                if (stack != null && stack.getItem() != null) {
                    stacks.add(copyWithSingleSize(stack));
                }
            }
        }
        return stacks;
    }

    private static ItemStack copyWithSingleSize(ItemStack stack) {
        ItemStack copied = stack.copy();
        copied.stackSize = 1;
        return copied;
    }
}
