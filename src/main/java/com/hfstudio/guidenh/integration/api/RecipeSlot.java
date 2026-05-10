package com.hfstudio.guidenh.integration.api;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

public class RecipeSlot {

    private final int x;
    private final int y;
    private final List<ItemStack> stacks;

    public RecipeSlot(int x, int y, List<ItemStack> stacks) {
        this.x = x;
        this.y = y;
        this.stacks = stacks == null ? Collections.emptyList() : Collections.unmodifiableList(stacks);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public List<ItemStack> stacks() {
        return stacks;
    }
}
