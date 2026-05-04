package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.buildcraft.BuildCraftHelpers;
import com.hfstudio.guidenh.compat.logisticspipes.LogisticsPipesHelpers;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuideBlockDisplayResolver {

    public static final String BARTWORKS_META_GENERATED_BLOCKS_CLASS = "bartworks.system.material.BWMetaGeneratedBlocks";

    private GuideBlockDisplayResolver() {}

    @Nullable
    public static ItemStack resolveDisplayStack(GuidebookLevel level, int x, int y, int z) {
        return resolveDisplayStack(level, x, y, z, null);
    }

    @Nullable
    public static ItemStack resolveDisplayStack(GuidebookLevel level, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        if (Mods.LogisticsPipes.isModLoaded()) {
            ItemStack lpStack = LogisticsPipesHelpers.resolveDisplayStack(level, block, x, y, z);
            if (lpStack != null) {
                return lpStack;
            }
        }

        if (Mods.BuildCraftTransport.isModLoaded()) {
            ItemStack bcStack = BuildCraftHelpers.resolveDisplayStack(level, block, x, y, z);
            if (bcStack != null) {
                return bcStack;
            }
        }

        ItemStack carpentersStack = GuideCarpentersBlockSupport.resolveDisplayStack(level, block, x, y, z, target);
        if (carpentersStack != null) {
            return carpentersStack;
        }

        ItemStack pickedStack = safeResolvePickedStack(level, block, x, y, z, target);
        if (pickedStack != null) {
            return pickedStack;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return null;
        }

        return new ItemStack(item, 1, resolveDisplayMeta(level, block, x, y, z));
    }

    @Nullable
    public static String resolveDisplayName(GuidebookLevel level, int x, int y, int z) {
        return resolveDisplayName(level, x, y, z, null);
    }

    @Nullable
    public static String resolveDisplayName(GuidebookLevel level, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        if (GuideCarpentersBlockSupport.isCarpentersBlock(block)) {
            String carpentersName = resolveIntrinsicBlockDisplayName(level, block, x, y, z);
            if (carpentersName != null) {
                return carpentersName;
            }
        }

        try {
            ItemStack stack = resolveDisplayStack(level, x, y, z, target);
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

    @Nullable
    public static String resolveIntrinsicBlockDisplayName(GuidebookLevel level, Block block, int x, int y, int z) {
        try {
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                ItemStack stack = new ItemStack(item, 1, resolveDisplayMeta(level, block, x, y, z));
                String displayName = stack.getDisplayName();
                if (displayName != null && !displayName.trim()
                    .isEmpty()) {
                    return displayName;
                }
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

    public static int safeResolveDamageValue(GuidebookLevel level, Block block, int x, int y, int z) {
        try {
            return Math.max(0, block.getDamageValue(level.getOrCreateFakeWorld(), x, y, z));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @Nullable
    public static ItemStack safeResolvePickedStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (target == null) {
            return null;
        }
        var world = level.getOrCreateFakeWorld();
        EntityPlayer player = null;
        try {
            player = Minecraft.getMinecraft().thePlayer;
        } catch (Throwable ignored) {}
        try {
            return resolvePickedStackForTarget(block, world, player, x, y, z, target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    static ItemStack resolvePickedStackForTarget(Block block, @Nullable World world, @Nullable EntityPlayer player,
        int x, int y, int z, @Nullable MovingObjectPosition target) {
        if (block == null || target == null) {
            return null;
        }
        try {
            ItemStack pickedStack = block.getPickBlock(target, world, x, y, z, player);
            if (pickedStack != null) {
                return pickedStack.copy();
            }
        } catch (Throwable ignored) {}
        try {
            ItemStack pickedStack = block.getPickBlock(target, world, x, y, z);
            return pickedStack != null ? pickedStack.copy() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
