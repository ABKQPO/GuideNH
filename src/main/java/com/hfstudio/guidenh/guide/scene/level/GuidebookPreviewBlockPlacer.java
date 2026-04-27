package com.hfstudio.guidenh.guide.scene.level;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuidebookPreviewBlockPlacer {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final String GREGTECH_BLOCK_MACHINES_CLASS = "gregtech.common.blocks.BlockMachines";
    private static final String GREGTECH_API_CLASS = "gregtech.api.GregTechAPI";

    private GuidebookPreviewBlockPlacer() {}

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag) {
        place(level, x, y, z, block, meta, tileTag, null);
    }

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId) {
        PlacementData placementData = resolvePlacementData(block, meta, tileTag);

        // Place the block before loading its tile so world-aware tile initialization sees the correct block/meta.
        level.setBlock(x, y, z, block, placementData.blockMeta, null);

        TileEntity tileEntity = null;
        if (tileTag != null || block.hasTileEntity(placementData.blockMeta)) {
            try {
                tileEntity = GuidebookTileEntityLoader
                    .load(level.getOrCreateFakeWorld(), block, placementData.blockMeta, x, y, z, tileTag);
            } catch (Throwable t) {
                LOG.warn("Preview tile entity load failed, falling back to block-only placement", t);
            }
        }
        if (tileEntity != null) {
            level.setTileEntity(x, y, z, tileEntity);
            initializeGregTechMetaTile(tileEntity, placementData.metaTileId, tileTag);
            applyGregTechDefaultFacing(tileEntity, tileTag);
            level.setTileEntity(
                x,
                y,
                z,
                resolveWorldResidentTile(level.getOrCreateFakeWorld(), x, y, z, tileEntity));
        }
        level.setExplicitBlockId(x, y, z, explicitBlockId);
    }

    private static PlacementData resolvePlacementData(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        Integer metaTileId = resolveGregTechMetaTileId(block, requestedMeta, tileTag);
        if (metaTileId == null) {
            return new PlacementData(requestedMeta, null);
        }

        Integer blockMeta = resolveGregTechBaseMeta(metaTileId.intValue());
        if (blockMeta == null) {
            return new PlacementData(requestedMeta, null);
        }

        return new PlacementData(blockMeta.intValue(), metaTileId);
    }

    @Nullable
    private static Integer resolveGregTechMetaTileId(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        if (block == null || !GREGTECH_BLOCK_MACHINES_CLASS.equals(
            block.getClass()
                .getName())) {
            return null;
        }
        if (tileTag != null && tileTag.hasKey("mID")) {
            int tagMetaTileId = tileTag.getInteger("mID");
            return tagMetaTileId > 0 ? Integer.valueOf(tagMetaTileId) : null;
        }
        return requestedMeta > 15 ? Integer.valueOf(requestedMeta) : null;
    }

    @Nullable
    private static Integer resolveGregTechBaseMeta(int metaTileId) {
        if (metaTileId <= 0) {
            return null;
        }
        try {
            Class<?> gregTechApiClass = Class.forName(GREGTECH_API_CLASS);
            Object metaTileEntities = gregTechApiClass.getField("METATILEENTITIES")
                .get(null);
            if (metaTileEntities == null || !metaTileEntities.getClass()
                .isArray()) {
                return null;
            }
            int length = Array.getLength(metaTileEntities);
            if (metaTileId >= length) {
                return null;
            }
            Object metaTileEntity = Array.get(metaTileEntities, metaTileId);
            if (metaTileEntity == null) {
                return null;
            }
            Method getTileEntityBaseType = metaTileEntity.getClass()
                .getMethod("getTileEntityBaseType");
            Object baseMeta = getTileEntityBaseType.invoke(metaTileEntity);
            return baseMeta instanceof Number number ? Integer.valueOf(number.intValue()) : null;
        } catch (Throwable t) {
            LOG.warn("Failed to resolve GregTech base meta for preview block {}", metaTileId, t);
            return null;
        }
    }

    private static void initializeGregTechMetaTile(@Nullable TileEntity tileEntity, @Nullable Integer metaTileId,
        @Nullable NBTTagCompound tileTag) {
        if (tileEntity == null || metaTileId == null || metaTileId.intValue() <= 0) {
            return;
        }
        try {
            Method initializer = tileEntity.getClass()
                .getMethod("setInitialValuesAsNBT", NBTTagCompound.class, short.class);
            NBTTagCompound initTag = tileTag;
            if (initTag != null && (!initTag.hasKey("mID") || initTag.getInteger("mID") != metaTileId.intValue())) {
                initTag = (NBTTagCompound) initTag.copy();
                initTag.setInteger("mID", metaTileId.intValue());
            }
            initializer.invoke(tileEntity, initTag, Short.valueOf((short) metaTileId.intValue()));
        } catch (NoSuchMethodException ignored) {
            // Non-GregTech tiles do not expose this initializer.
        } catch (Throwable t) {
            LOG.warn("Failed to initialize GregTech preview tile {}", metaTileId, t);
        }
    }

    private static void applyGregTechDefaultFacing(@Nullable TileEntity tileEntity, @Nullable NBTTagCompound tileTag) {
        if (tileEntity == null || (tileTag != null && tileTag.hasKey("mFacing"))) {
            return;
        }
        try {
            Method getFrontFacing = tileEntity.getClass()
                .getMethod("getFrontFacing");
            Method isValidFacing = tileEntity.getClass()
                .getMethod("isValidFacing", ForgeDirection.class);
            Method setFrontFacing = tileEntity.getClass()
                .getMethod("setFrontFacing", ForgeDirection.class);

            Object currentFacingValue = getFrontFacing.invoke(tileEntity);
            ForgeDirection currentFacing = currentFacingValue instanceof ForgeDirection direction ? direction
                : ForgeDirection.UNKNOWN;
            if (isFacingValid(isValidFacing, tileEntity, currentFacing)) {
                return;
            }

            ForgeDirection preferredFacing = findPreferredFacing(isValidFacing, tileEntity);
            if (preferredFacing != ForgeDirection.UNKNOWN) {
                setFrontFacing.invoke(tileEntity, preferredFacing);
            }
        } catch (NoSuchMethodException ignored) {
            // Non-GregTech tiles do not expose facing controls.
        } catch (Throwable t) {
            LOG.warn("Failed to assign a default GregTech preview facing", t);
        }
    }

    private static boolean isFacingValid(Method isValidFacing, TileEntity tileEntity, ForgeDirection facing)
        throws ReflectiveOperationException {
        if (facing == null || facing == ForgeDirection.UNKNOWN) {
            return false;
        }
        Object valid = isValidFacing.invoke(tileEntity, facing);
        return valid instanceof Boolean && ((Boolean) valid).booleanValue();
    }

    private static ForgeDirection findPreferredFacing(Method isValidFacing, TileEntity tileEntity)
        throws ReflectiveOperationException {
        ForgeDirection[] preferredOrder = new ForgeDirection[] {
            ForgeDirection.SOUTH,
            ForgeDirection.NORTH,
            ForgeDirection.EAST,
            ForgeDirection.WEST,
            ForgeDirection.UP,
            ForgeDirection.DOWN };
        for (ForgeDirection facing : preferredOrder) {
            if (isFacingValid(isValidFacing, tileEntity, facing)) {
                return facing;
            }
        }
        return ForgeDirection.UNKNOWN;
    }

    private static TileEntity resolveWorldResidentTile(GuidebookFakeWorld world, int x, int y, int z, TileEntity fallback) {
        TileEntity resident = world.getTileEntity(x, y, z);
        return resident != null ? resident : fallback;
    }

    private static final class PlacementData {

        private final int blockMeta;
        @Nullable
        private final Integer metaTileId;

        private PlacementData(int blockMeta, @Nullable Integer metaTileId) {
            this.blockMeta = blockMeta;
            this.metaTileId = metaTileId;
        }
    }
}
