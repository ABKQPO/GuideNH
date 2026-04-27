package com.hfstudio.guidenh.guide.scene.support;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuideBlockDisplayResolver {

    private static final String BARTWORKS_META_GENERATED_BLOCKS_CLASS =
        "bartworks.system.material.BWMetaGeneratedBlocks";

    private GuideBlockDisplayResolver() {}

    @Nullable
    public static ItemStack resolveDisplayStack(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return null;
        }

        return new ItemStack(item, 1, resolveDisplayMeta(level, block, x, y, z));
    }

    @Nullable
    public static String resolveDisplayName(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        try {
            ItemStack stack = resolveDisplayStack(level, x, y, z);
            if (stack != null) {
                return stack.getDisplayName();
            }
        } catch (Throwable ignored) {}

        try {
            String localizedName = block.getLocalizedName();
            if (localizedName != null && !localizedName.trim()
                .isEmpty()) {
                return localizedName;
            }
        } catch (Throwable ignored) {}

        try {
            return block.getUnlocalizedName();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int resolveDisplayMeta(GuidebookLevel level, Block block, int x, int y, int z) {
        int worldMeta = Math.max(0, level.getBlockMetadata(x, y, z));
        int damageMeta = safeResolveDamageValue(level, block, x, y, z);

        if (isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS) && damageMeta <= 0 && worldMeta > 0) {
            return worldMeta;
        }

        return damageMeta >= 0 ? damageMeta : worldMeta;
    }

    public static boolean isBlockInstanceOf(@Nullable Block block, String className) {
        if (block == null || className == null || className.isEmpty()) {
            return false;
        }

        for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static int safeResolveDamageValue(GuidebookLevel level, Block block, int x, int y, int z) {
        try {
            return Math.max(0, block.getDamageValue(level.getOrCreateFakeWorld(), x, y, z));
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
