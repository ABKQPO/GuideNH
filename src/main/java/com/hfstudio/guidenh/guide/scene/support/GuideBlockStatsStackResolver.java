package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.Ae2Helpers;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksHelpers;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartHelpers;

public final class GuideBlockStatsStackResolver {

    private GuideBlockStatsStackResolver() {}

    public static List<ItemStack> resolveStacks(GuidebookLevel level, int x, int y, int z) {
        List<ResolvedStack> entries = resolveEntries(level, x, y, z);
        if (entries.isEmpty()) {
            return Collections.emptyList();
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
        if (level == null) {
            return Collections.emptyList();
        }
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return Collections.emptyList();
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        ArrayList<ResolvedStack> entries = new ArrayList<>(4);
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
        if (Mods.AE2.isModLoaded()) {
            Ae2Helpers.appendCableBusStatEntries(tileEntity, entries, x, y, z);
        }
        if (entries.isEmpty() && Mods.ForgeMultipart.isModLoaded()
            && (ForgeMultipartHelpers.isForgeMultipartBlock(block)
                || ForgeMultipartHelpers.isMultipartTileEntity(tileEntity))) {
            ArrayList<ItemStack> stacks = new ArrayList<>(4);
            ForgeMultipartHelpers.appendMultipartStatStacks(tileEntity, stacks);
            appendWithFallbackBounds(stacks, fallbackBounds, entries);
        }
        if (entries.isEmpty() && CarpentersBlocksHelpers.isCarpentersBlock(block)) {
            ArrayList<ItemStack> stacks = new ArrayList<>(4);
            CarpentersBlocksHelpers.appendComponentStatStacks(tileEntity, stacks);
            appendWithFallbackBounds(stacks, fallbackBounds, entries);
        }
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

    private static void appendWithFallbackBounds(List<ItemStack> stacks, AxisAlignedBB fallbackBounds,
        List<ResolvedStack> entries) {
        for (ItemStack stack : stacks) {
            entries.add(new ResolvedStack(stack, fallbackBounds));
        }
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

    public static final class ResolvedStack {

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
