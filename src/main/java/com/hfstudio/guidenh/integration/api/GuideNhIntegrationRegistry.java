package com.hfstudio.guidenh.integration.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuideNhIntegrationRegistry {

    private static final GuideNhIntegrationRegistry GLOBAL = new GuideNhIntegrationRegistry();
    private final List<ItemStackNormalizationProvider> itemStackNormalizationProviders = new ArrayList<>();
    private final List<BlockDisplayProvider> blockDisplayProviders = new ArrayList<>();

    public GuideNhIntegrationRegistry() {}

    public static GuideNhIntegrationRegistry global() {
        return GLOBAL;
    }

    public synchronized void registerItemStackNormalizationProvider(ItemStackNormalizationProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!itemStackNormalizationProviders.contains(provider)) {
            itemStackNormalizationProviders.add(provider);
        }
    }

    public synchronized List<ItemStackNormalizationProvider> itemStackNormalizationProviders() {
        return Collections.unmodifiableList(new ArrayList<>(itemStackNormalizationProviders));
    }

    public synchronized void registerBlockDisplayProvider(BlockDisplayProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!blockDisplayProviders.contains(provider)) {
            blockDisplayProviders.add(provider);
        }
    }

    public synchronized List<BlockDisplayProvider> blockDisplayProviders() {
        return Collections.unmodifiableList(new ArrayList<>(blockDisplayProviders));
    }

    @Nullable
    public ItemStack normalizeItemStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return stack;
        }
        ItemStack current = stack;
        for (ItemStackNormalizationProvider provider : itemStackNormalizationProviders()) {
            ItemStack normalized = provider.normalize(current);
            if (normalized != null && normalized.getItem() != null) {
                current = normalized;
            }
        }
        return current;
    }

    @Nullable
    public ItemStack resolveBlockDisplayStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (level == null || block == null) {
            return null;
        }
        for (BlockDisplayProvider provider : blockDisplayProviders()) {
            ItemStack stack = provider.resolveDisplayStack(level, block, x, y, z, target);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }
}
