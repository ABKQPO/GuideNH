package com.hfstudio.guidenh.integration.api;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public interface ItemStackNormalizationProvider {

    @Nullable
    ItemStack normalize(ItemStack stack);
}
