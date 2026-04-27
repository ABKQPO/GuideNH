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

import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideGregTechTileSupport;

public class GuidebookPreviewBlockPlacer {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final String GREGTECH_BLOCK_MACHINES_CLASS = "gregtech.common.blocks.BlockMachines";
    private static final String BARTWORKS_META_GENERATED_BLOCKS_CLASS = "bartworks.system.material.BWMetaGeneratedBlocks";
    private static final String BARTWORKS_META_GENERATED_TILE_CLASS = "bartworks.system.material.TileEntityMetaGeneratedBlock";
    private static final String GREGTECH_API_CLASS = "gregtech.api.GregTechAPI";

    private GuidebookPreviewBlockPlacer() {}

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag) {
        place(level, x, y, z, block, meta, tileTag, null);
    }

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId) {
        PlacementData placementData = resolvePlacementData(block, meta, tileTag);
        logPlacementRequest(x, y, z, block, meta, tileTag, explicitBlockId, placementData);

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
            logLoadedTile("loaded", x, y, z, tileEntity, placementData.metaTileId, tileTag);
            initializeGregTechMetaTile(tileEntity, placementData.metaTileId, tileTag);
            logLoadedTile("gregtech-init", x, y, z, tileEntity, placementData.metaTileId, tileTag);
            applyGregTechDefaultFacing(tileEntity, tileTag);
            applyBartWorksGeneratedBlockMeta(tileEntity, block, placementData.blockMeta);
            logLoadedTile("post-facing-meta", x, y, z, tileEntity, placementData.metaTileId, tileTag);
            TileEntity residentTile = resolveWorldResidentTile(level.getOrCreateFakeWorld(), x, y, z, tileEntity);
            level.setTileEntity(x, y, z, residentTile);
            logLoadedTile("resident", x, y, z, residentTile, placementData.metaTileId, tileTag);
        } else if (shouldLogPlacement(block, placementData)) {
            GuideGregTechTileSupport.logInfoOnce(
                "preview-place-missing-tile:" + x
                    + ":"
                    + y
                    + ":"
                    + z
                    + ":"
                    + GuideGregTechTileSupport.describeBlock(block),
                "Preview tile load produced no TileEntity at {} for block={} explicitId={} blockMeta={} metaTileId={} tileTag=[{}]",
                describePosition(x, y, z),
                GuideGregTechTileSupport.describeBlock(block),
                explicitBlockId,
                Integer.valueOf(placementData.blockMeta),
                placementData.metaTileId,
                GuideGregTechTileSupport.describeTileTag(tileTag));
        }
        invokeOnBlockAdded(block, level.getOrCreateFakeWorld(), x, y, z);
        level.setExplicitBlockId(x, y, z, explicitBlockId);
    }

    private static PlacementData resolvePlacementData(Block block, int requestedMeta,
        @Nullable NBTTagCompound tileTag) {
        Integer metaTileId = resolveGregTechMetaTileId(block, requestedMeta, tileTag);
        if (metaTileId == null) {
            Integer bartWorksMeta = resolveBartWorksBlockMeta(block, requestedMeta, tileTag);
            return new PlacementData(bartWorksMeta != null ? bartWorksMeta.intValue() : requestedMeta, null);
        }

        Integer blockMeta = resolveGregTechBaseMeta(metaTileId.intValue());
        if (blockMeta == null) {
            return new PlacementData(requestedMeta, null);
        }

        return new PlacementData(blockMeta.intValue(), metaTileId);
    }

    @Nullable
    private static Integer resolveGregTechMetaTileId(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        if (!GuideBlockDisplayResolver.isBlockInstanceOf(block, GREGTECH_BLOCK_MACHINES_CLASS)) {
            return null;
        }
        if (tileTag != null && tileTag.hasKey("mID")) {
            int tagMetaTileId = tileTag.getInteger("mID");
            return tagMetaTileId > 0 ? Integer.valueOf(tagMetaTileId) : null;
        }
        return requestedMeta > 15 ? Integer.valueOf(requestedMeta) : null;
    }

    @Nullable
    private static Integer resolveBartWorksBlockMeta(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        if (!GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS)) {
            return null;
        }
        if (tileTag != null && tileTag.hasKey("m")) {
            int metaFromTag = tileTag.getShort("m");
            if (metaFromTag <= 0) {
                metaFromTag = tileTag.getInteger("m");
            }
            if (metaFromTag > 0) {
                return Integer.valueOf(metaFromTag);
            }
        }
        return Integer.valueOf(Math.max(0, requestedMeta));
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
        ForgeDirection[] preferredOrder = new ForgeDirection[] { ForgeDirection.SOUTH, ForgeDirection.NORTH,
            ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.UP, ForgeDirection.DOWN };
        for (ForgeDirection facing : preferredOrder) {
            if (isFacingValid(isValidFacing, tileEntity, facing)) {
                return facing;
            }
        }
        return ForgeDirection.UNKNOWN;
    }

    private static void applyBartWorksGeneratedBlockMeta(@Nullable TileEntity tileEntity, Block block, int blockMeta) {
        if (tileEntity == null || blockMeta <= 0
            || !GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS)
            || !isInstanceOf(tileEntity, BARTWORKS_META_GENERATED_TILE_CLASS)) {
            return;
        }
        try {
            tileEntity.getClass()
                .getField("mMetaData")
                .setShort(tileEntity, (short) blockMeta);
        } catch (Throwable t) {
            LOG.warn("Failed to apply BartWorks preview meta {}", blockMeta, t);
        }
    }

    private static void invokeOnBlockAdded(Block block, GuidebookFakeWorld world, int x, int y, int z) {
        if (block == null || world == null) {
            return;
        }
        try {
            block.onBlockAdded(world, x, y, z);
        } catch (Throwable t) {
            LOG.warn("Preview block onBlockAdded hook failed for {}", block, t);
        }
    }

    private static boolean isInstanceOf(@Nullable Object instance, String className) {
        if (instance == null || className == null || className.isEmpty()) {
            return false;
        }
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static TileEntity resolveWorldResidentTile(GuidebookFakeWorld world, int x, int y, int z,
        TileEntity fallback) {
        TileEntity resident = world.getTileEntity(x, y, z);
        return resident != null ? resident : fallback;
    }

    private static void logPlacementRequest(int x, int y, int z, Block block, int requestedMeta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId, PlacementData placementData) {
        if (!shouldLogPlacement(block, placementData)) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "preview-place-request:" + x + ":" + y + ":" + z + ":" + GuideGregTechTileSupport.describeBlock(block),
            "Preview place request {}: block={} explicitId={} requestedMeta={} resolvedBlockMeta={} resolvedMetaTileId={} tileTag=[{}]",
            describePosition(x, y, z),
            GuideGregTechTileSupport.describeBlock(block),
            explicitBlockId,
            Integer.valueOf(requestedMeta),
            Integer.valueOf(placementData.blockMeta),
            placementData.metaTileId,
            GuideGregTechTileSupport.describeTileTag(tileTag));
    }

    private static void logLoadedTile(String stage, int x, int y, int z, @Nullable TileEntity tileEntity,
        @Nullable Integer metaTileId, @Nullable NBTTagCompound tileTag) {
        if (!GuideGregTechTileSupport.isGregTechTileEntity(tileEntity)
            && !isInstanceOf(tileEntity, BARTWORKS_META_GENERATED_TILE_CLASS)) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "preview-place-" + stage
                + ":"
                + x
                + ":"
                + y
                + ":"
                + z
                + ":"
                + (tileEntity != null ? tileEntity.getClass()
                    .getName() : "null"),
            "Preview tile {} {}: tile={} metaTileId={} tileTag=[{}]",
            stage,
            describePosition(x, y, z),
            GuideGregTechTileSupport.describeTile(tileEntity),
            metaTileId,
            GuideGregTechTileSupport.describeTileTag(tileTag));
    }

    private static boolean shouldLogPlacement(Block block, PlacementData placementData) {
        return placementData.metaTileId != null
            || GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS);
    }

    private static String describePosition(int x, int y, int z) {
        return "(" + x + "," + y + "," + z + ")";
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
