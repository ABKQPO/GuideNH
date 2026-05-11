package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface StructureLibPreviewItemProvider {

    void appendPreviewItems(List<ItemStack> stacks);

    default void configureTrigger(ItemStack triggerStack, StructureLibPreviewSelection selection) {}
}
