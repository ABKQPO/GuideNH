package com.hfstudio.guidenh.guide.scene.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuideCarpentersBlockSupport {

    public static final String CARPENTERS_BLOCK_PACKAGE = "com.carpentersblocks.block.";
    public static final String CARPENTERS_TILE_PACKAGE = "com.carpentersblocks.tileentity.";
    public static final String CARPENTERS_BLOCK_PROPERTIES_CLASS = "com.carpentersblocks.util.BlockProperties";
    public static final String FEATURE_SENSITIVE_STACK_METHOD = "getFeatureSensitiveSideItemStack";
    public static final int BASE_COVER_SIDE = 6;

    private GuideCarpentersBlockSupport() {}

    public static boolean isCarpentersBlock(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name != null && name.startsWith(CARPENTERS_BLOCK_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemStack resolveDisplayStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (!isCarpentersBlock(block)) {
            return null;
        }

        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (!isCarpentersTile(tileEntity)) {
            return null;
        }

        ItemStack featureSensitiveStack = resolveFeatureSensitiveStack(tileEntity, target);
        if (featureSensitiveStack != null) {
            return featureSensitiveStack;
        }

        int preferredSide = resolvePreferredSide(target);
        ItemStack preferredStack = resolveCoverStack(tileEntity, preferredSide);
        if (preferredStack != null) {
            return preferredStack;
        }

        return preferredSide != BASE_COVER_SIDE ? resolveCoverStack(tileEntity, BASE_COVER_SIDE) : null;
    }

    public static int resolvePreferredSide(@Nullable MovingObjectPosition target) {
        return target != null && target.sideHit >= 0 && target.sideHit < BASE_COVER_SIDE ? target.sideHit
            : BASE_COVER_SIDE;
    }

    public static boolean isCarpentersTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return false;
        }
        for (Class<?> type = tileEntity.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name != null && name.startsWith(CARPENTERS_TILE_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemStack resolveFeatureSensitiveStack(TileEntity tileEntity, @Nullable MovingObjectPosition target) {
        Method method = resolveFeatureSensitiveStackMethod(tileEntity.getClass());
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null, tileEntity, resolveForgeDirection(target));
            return value instanceof ItemStack itemStack ? itemStack.copy() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Method resolveFeatureSensitiveStackMethod(Class<?> tileEntityClass) {
        try {
            for (Method method : Class.forName(CARPENTERS_BLOCK_PROPERTIES_CLASS)
                .getMethods()) {
                if (!FEATURE_SENSITIVE_STACK_METHOD.equals(method.getName())
                    || !Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 2) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes[0].isAssignableFrom(tileEntityClass)
                    && parameterTypes[1].isAssignableFrom(ForgeDirection.class)) {
                    return method;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static ForgeDirection resolveForgeDirection(@Nullable MovingObjectPosition target) {
        return target != null && target.sideHit >= 0 && target.sideHit < BASE_COVER_SIDE
            ? ForgeDirection.getOrientation(target.sideHit)
            : ForgeDirection.UNKNOWN;
    }

    @Nullable
    public static ItemStack resolveCoverStack(TileEntity tileEntity, int sideIndex) {
        byte[] coverAttributes = resolveCoverAttributes(tileEntity);
        if (coverAttributes == null || sideIndex < 0 || sideIndex >= coverAttributes.length) {
            return null;
        }

        byte coverAttribute = coverAttributes[sideIndex];
        if (!hasAttribute(tileEntity, coverAttribute)) {
            return null;
        }

        try {
            Object value = tileEntity.getClass()
                .getMethod("getAttribute", byte.class)
                .invoke(tileEntity, coverAttribute);
            return value instanceof ItemStack itemStack ? itemStack.copy() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static byte[] resolveCoverAttributes(TileEntity tileEntity) {
        try {
            Object value = tileEntity.getClass()
                .getField("ATTR_COVER")
                .get(null);
            return value instanceof byte[]coverAttributes ? coverAttributes : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hasAttribute(TileEntity tileEntity, byte attribute) {
        try {
            Object value = tileEntity.getClass()
                .getMethod("hasAttribute", byte.class)
                .invoke(tileEntity, attribute);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
