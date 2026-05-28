package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

public class GuideBlockStatsStackResolver {

    protected GuideBlockStatsStackResolver() {}

    public static List<ItemStack> resolveStacks(GuidebookLevel level, int x, int y, int z) {
        List<ResolvedStack> entries = resolveEntries(level, x, y, z);
        if (entries.isEmpty()) {
            return List.of();
        }
        ArrayList<ItemStack> stacks = new ArrayList<>(entries.size());
        for (ResolvedStack entry : entries) {
            if (entry.stack() != null) {
                stacks.add(entry.stack());
            }
        }
        return stacks;
    }

    public static List<ResolvedStack> resolveEntries(GuidebookLevel level, int x, int y, int z) {
        return resolveEntriesInto(level, x, y, z, new ArrayList<>(4));
    }

    public static List<ResolvedStack> resolveEntriesInto(GuidebookLevel level, int x, int y, int z,
        List<ResolvedStack> entries) {
        entries.clear();
        if (level == null) {
            return entries;
        }
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return entries;
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        AxisAlignedBB fallbackBounds = GuideBlockBoundsResolver.resolveSelectedBounds(level, x, y, z);
        if (fallbackBounds == null) {
            fallbackBounds = GuideBlockBoundsResolver.resolveWorldBounds(level, x, y, z);
        }
        appendMultipartEntries(level, block, tileEntity, x, y, z, fallbackBounds, entries);
        if (entries.isEmpty()) {
            appendFallbackEntry(level, x, y, z, fallbackBounds, entries);
        }
        normalizeEntries(entries);
        return entries;
    }

    private static void appendMultipartEntries(GuidebookLevel level, Block block, TileEntity tileEntity, int x, int y,
        int z, AxisAlignedBB fallbackBounds, List<ResolvedStack> entries) {
        entries.addAll(
            GuideNhIntegrationRegistry.global()
                .resolveBlockStatsEntries(level, block, tileEntity, x, y, z, fallbackBounds));
    }

    private static void appendFallbackEntry(GuidebookLevel level, int x, int y, int z, AxisAlignedBB fallbackBounds,
        List<ResolvedStack> entries) {
        try {
            ItemStack stack = GuideBlockDisplayResolver.resolveDisplayStack(level, x, y, z);
            if (stack != null) {
                entries.add(new ResolvedStack(stack, fallbackBounds));
            }
        } catch (Throwable ignored) {}
    }

    private static void normalizeEntries(List<ResolvedStack> entries) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            ResolvedStack entry = entries.get(i);
            ItemStack stack = entry != null ? entry.stack() : null;
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
                entries.remove(i);
            }
        }
    }

    public static class ResolvedStack {

        private final ItemStack stack;
        private final AxisAlignedBB bounds;

        public ResolvedStack(ItemStack stack, AxisAlignedBB bounds) {
            this.stack = stack;
            this.bounds = bounds;
        }

        public ItemStack stack() {
            return stack;
        }

        public AxisAlignedBB bounds() {
            return bounds;
        }
    }
}
