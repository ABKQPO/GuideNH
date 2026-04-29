package com.hfstudio.guidenh.guide.document.interaction;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.siteexport.ResourceExporter;

public class ItemTooltip implements GuideTooltip {

    private final ItemStack stack;

    public ItemTooltip(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        exporter.referenceItemStack(stack);
    }
}
