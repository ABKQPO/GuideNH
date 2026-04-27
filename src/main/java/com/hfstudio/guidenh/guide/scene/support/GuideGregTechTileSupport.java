package com.hfstudio.guidenh.guide.scene.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GuideGregTechTileSupport {

    private static final String GREGTECH_TILE_CLASS = "gregtech.api.interfaces.tileentity.IGregTechTileEntity";
    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final Set<String> LOGGED_KEYS = Collections.synchronizedSet(new HashSet<>());

    private static volatile Class<?> gregTechTileClass;
    private static volatile boolean gregTechTileClassResolved;

    private GuideGregTechTileSupport() {}

    public static boolean isGregTechTileEntity(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return false;
        }
        Class<?> type = resolveGregTechTileClass();
        return type != null && type.isInstance(tileEntity);
    }

    public static int resolveMetaTileId(@Nullable TileEntity tileEntity, int fallback) {
        if (!isGregTechTileEntity(tileEntity)) {
            return fallback;
        }
        try {
            Object value = tileEntity.getClass()
                .getMethod("getMetaTileID")
                .invoke(tileEntity);
            if (value instanceof Number number && number.intValue() > 0) {
                return number.intValue();
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    public static boolean hasValidMetaTileBinding(@Nullable TileEntity tileEntity) {
        if (!isGregTechTileEntity(tileEntity)) {
            return false;
        }
        try {
            Object metaTileEntity = tileEntity.getClass()
                .getMethod("getMetaTileEntity")
                .invoke(tileEntity);
            if (metaTileEntity == null) {
                return false;
            }
            Object baseMetaTileEntity = metaTileEntity.getClass()
                .getMethod("getBaseMetaTileEntity")
                .invoke(metaTileEntity);
            return baseMetaTileEntity == tileEntity;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean repairMetaTileBinding(@Nullable TileEntity tileEntity) {
        if (!isGregTechTileEntity(tileEntity)) {
            return false;
        }
        try {
            Object metaTileEntity = tileEntity.getClass()
                .getMethod("getMetaTileEntity")
                .invoke(tileEntity);
            if (metaTileEntity != null) {
                if (tryRebindExistingMetaTile(tileEntity, metaTileEntity)) {
                    logOnce(
                        "repair-rebind:" + describeTile(tileEntity),
                        "Rebound existing GregTech MetaTileEntity: {}",
                        describeTile(tileEntity));
                    return true;
                }
            }

            int metaTileId = resolveMetaTileId(tileEntity, 0);
            if (metaTileId <= 0) {
                logOnce(
                    "repair-missing-id:" + describeTile(tileEntity),
                    "Cannot repair GregTech tile because no MetaTile id was available: {}",
                    describeTile(tileEntity));
                return false;
            }

            NBTTagCompound snapshot = captureTileNbt(tileEntity, metaTileId);
            tileEntity.getClass()
                .getMethod("setInitialValuesAsNBT", NBTTagCompound.class, short.class)
                .invoke(tileEntity, snapshot, (short) 0);
            boolean repaired = hasValidMetaTileBinding(tileEntity);
            logOnce(
                (repaired ? "repair-success:" : "repair-failed:") + describeTile(tileEntity),
                repaired ? "Recreated GregTech MetaTileEntity binding successfully: {}"
                    : "GregTech MetaTileEntity binding was still invalid after recreation: {}",
                describeTile(tileEntity));
            return repaired;
        } catch (Throwable ignored) {
            logOnce(
                "repair-exception:" + describeTile(tileEntity),
                "Exception while repairing GregTech MetaTileEntity binding: {}",
                describeTile(tileEntity));
            return false;
        }
    }

    public static void logInfoOnce(String key, String message, Object... args) {
        if (key == null || key.isEmpty() || message == null || message.isEmpty()) {
            return;
        }
        if (LOGGED_KEYS.add(key)) {
            LOG.info(message, args);
        }
    }

    public static String describeBlock(@Nullable Block block) {
        if (block == null) {
            return "null-block";
        }
        try {
            Object registryName = Block.blockRegistry.getNameForObject(block);
            if (registryName != null) {
                return registryName.toString();
            }
        } catch (Throwable ignored) {}
        try {
            return block.getUnlocalizedName();
        } catch (Throwable ignored) {
            return block.getClass()
                .getName();
        }
    }

    public static String describeTileTag(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null) {
            return "null-tag";
        }
        StringBuilder builder = new StringBuilder();
        appendTagValue(builder, tileTag, "id");
        appendTagValue(builder, tileTag, "mID");
        appendTagValue(builder, tileTag, "mFacing");
        appendTagValue(builder, tileTag, "m");
        if (builder.length() == 0) {
            builder.append("empty-tag");
        }
        return builder.toString();
    }

    private static boolean tryRebindExistingMetaTile(TileEntity tileEntity, Object metaTileEntity) {
        try {
            Method setter = findBaseSetter(metaTileEntity.getClass());
            if (setter == null) {
                return false;
            }
            setter.invoke(metaTileEntity, tileEntity);
            return hasValidMetaTileBinding(tileEntity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Method findBaseSetter(Class<?> type) {
        for (Method method : type.getMethods()) {
            if ("setBaseMetaTileEntity".equals(method.getName()) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    private static NBTTagCompound captureTileNbt(TileEntity tileEntity, int metaTileId) {
        try {
            NBTTagCompound snapshot = new NBTTagCompound();
            tileEntity.writeToNBT(snapshot);
            if (!snapshot.hasKey("mID")) {
                snapshot.setInteger("mID", metaTileId);
            }
            return snapshot;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Class<?> resolveGregTechTileClass() {
        if (!gregTechTileClassResolved) {
            gregTechTileClassResolved = true;
            try {
                gregTechTileClass = Class.forName(GREGTECH_TILE_CLASS);
            } catch (Throwable ignored) {
                gregTechTileClass = null;
            }
        }
        return gregTechTileClass;
    }

    public static String describeTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return "null-tile";
        }
        return tileEntity.getClass()
            .getName() + "@("
            + tileEntity.xCoord
            + ","
            + tileEntity.yCoord
            + ","
            + tileEntity.zCoord
            + ")"
            + " metaId="
            + resolveMetaTileId(tileEntity, -1)
            + " valid="
            + hasValidMetaTileBinding(tileEntity);
    }

    private static void logOnce(String key, String message, Object arg) {
        logInfoOnce(key, message, arg);
    }

    private static void appendTagValue(StringBuilder builder, NBTTagCompound tileTag, String key) {
        if (!tileTag.hasKey(key)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(key)
            .append('=');
        try {
            builder.append(tileTag.getTag(key));
        } catch (Throwable ignored) {
            builder.append("<unavailable>");
        }
    }
}
