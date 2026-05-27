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

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

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
        World fakeWorld = null;

        ItemStack integrationStack = GuideNhIntegrationRegistry.global()
            .resolveBlockDisplayStack(level, block, x, y, z, target);
        if (integrationStack != null) {
            return integrationStack;
        }

        if (target != null) {
            fakeWorld = level.getOrCreateFakeWorld();
        }
        ItemStack pickedStack = safeResolvePickedStack(level, block, x, y, z, target, fakeWorld);
        if (pickedStack != null) {
            return pickedStack;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return null;
        }

        return new ItemStack(item, 1, resolveDisplayMeta(level, block, x, y, z, fakeWorld));
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

        String integrationName = GuideNhIntegrationRegistry.global()
            .resolveBlockDisplayName(level, block, x, y, z, target);
        if (integrationName != null) {
            return integrationName;
        }

        try {
            ItemStack stack = resolveDisplayStack(level, x, y, z, target);
            if (stack != null) {
                return stack.getDisplayName();
            }
        } catch (Throwable ignored) {}

        try {
            String localizedName = block.getLocalizedName();
            if (hasText(localizedName)) {
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
            World fakeWorld = level.getOrCreateFakeWorld();
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                ItemStack stack = new ItemStack(item, 1, resolveDisplayMeta(level, block, x, y, z, fakeWorld));
                String displayName = stack.getDisplayName();
                if (hasText(displayName)) {
                    return displayName;
                }
            }
        } catch (Throwable ignored) {}

        try {
            String localizedName = block.getLocalizedName();
            if (hasText(localizedName)) {
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
        return resolveDisplayMeta(level, block, x, y, z, null);
    }

    public static int resolveDisplayMeta(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable World fakeWorld) {
        int worldMeta = Math.max(0, level.getBlockMetadata(x, y, z));
        int damageMeta = safeResolveDamageValue(level, block, x, y, z, fakeWorld);

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
        return safeResolveDamageValue(level, block, x, y, z, null);
    }

    public static int safeResolveDamageValue(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable World fakeWorld) {
        try {
            return Math
                .max(0, block.getDamageValue(fakeWorld != null ? fakeWorld : level.getOrCreateFakeWorld(), x, y, z));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @Nullable
    public static ItemStack safeResolvePickedStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        return safeResolvePickedStack(level, block, x, y, z, target, null);
    }

    @Nullable
    public static ItemStack safeResolvePickedStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target, @Nullable World fakeWorld) {
        if (target == null) {
            return null;
        }
        EntityPlayer player = null;
        try {
            player = Minecraft.getMinecraft().thePlayer;
        } catch (Throwable ignored) {}
        try {
            return resolvePickedStackForTarget(
                block,
                fakeWorld != null ? fakeWorld : level.getOrCreateFakeWorld(),
                player,
                x,
                y,
                z,
                target);
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

    private static boolean hasText(@Nullable String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > ' ') {
                return true;
            }
        }
        return false;
    }
}
