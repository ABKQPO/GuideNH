package com.hfstudio.guidenh.guide.siteexport;

import java.nio.file.Path;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public interface ResourceExporter {

    default Path getOutDir() {
        return null;
    }

    default void referenceTexture(ResourceLocation textureId) {}

    default void referenceItem(Item item) {}

    default void referenceItemStack(ItemStack stack) {
        if (stack != null && stack.getItem() != null) referenceItem(stack.getItem());
    }
}
