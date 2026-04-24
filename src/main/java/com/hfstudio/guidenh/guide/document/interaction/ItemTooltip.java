package com.hfstudio.guidenh.guide.document.interaction;

import net.minecraft.item.ItemStack;

public class ItemTooltip implements GuideTooltip {

    private final ItemStack stack;

    public ItemTooltip(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }
}
