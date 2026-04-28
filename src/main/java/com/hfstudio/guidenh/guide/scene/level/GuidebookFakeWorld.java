package com.hfstudio.guidenh.guide.scene.level;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lightweight client-only world wrapper backed by a {@link GuidebookLevel}.
 */
@SideOnly(Side.CLIENT)
public class GuidebookFakeWorld extends WorldClient {

    private static final long FROZEN_WORLD_TIME = 0L;
    private static volatile boolean gregTechDummyWorldRegistrationAttempted;
    private static final String BARTWORKS_META_GENERATED_TILE_CLASS = "bartworks.system.material.TileEntityMetaGeneratedBlock";
    private static volatile Field bartWorksMetaField;
    private static volatile boolean bartWorksMetaFieldResolved;

    private final GuidebookLevel level;

    public GuidebookFakeWorld(GuidebookLevel level) {
        super(
            resolveNetHandler(),
            new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT),
            resolveDimensionId(),
            resolveDifficulty(),
            new Profiler());
        this.level = level;
        this.isRemote = true;
        registerOptionalDummyWorldIntegrations();
    }

    private static NetHandlerPlayClient resolveNetHandler() {
        var netHandler = Minecraft.getMinecraft()
            .getNetHandler();
        if (netHandler == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }
        return netHandler;
    }

    private static int resolveDimensionId() {
        var currentWorld = Minecraft.getMinecraft().theWorld;
        return currentWorld != null ? currentWorld.provider.dimensionId : 0;
    }

    private static EnumDifficulty resolveDifficulty() {
        var currentWorld = Minecraft.getMinecraft().theWorld;
        return currentWorld != null ? currentWorld.difficultySetting : EnumDifficulty.NORMAL;
    }

    private static void registerOptionalDummyWorldIntegrations() {
        if (gregTechDummyWorldRegistrationAttempted) {
            return;
        }
        gregTechDummyWorldRegistrationAttempted = true;
        try {
            Class<?> gregTechApiClass = Class.forName("gregtech.api.GregTechAPI");
            gregTechApiClass.getMethod("addDummyWorld", Class.class)
                .invoke(null, GuidebookFakeWorld.class);
        } catch (Throwable ignored) {}
    }

    public GuidebookLevel getGuidebookLevel() {
        return level;
    }

    @Override
    public Entity getEntityByID(int id) {
        if (level == null) {
            return null;
        }
        return level.getEntity(id);
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (level == null) return Blocks.air;
        return level.getBlock(x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (level == null) return 0;
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        Integer bartWorksMeta = resolveBartWorksMetadata(tileEntity);
        if (bartWorksMeta != null) {
            return bartWorksMeta.intValue();
        }
        return level.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        if (level == null) return null;
        return level.getTileEntity(x, y, z);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == Blocks.air;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        return 15;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return (15 << 20) | (15 << 4);
    }

    @Override
    public int getBlockLightValue_do(int x, int y, int z, boolean p_72849_4_) {
        return 15;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) {
        if (level == null) {
            return 0;
        }
        return level.isBlockProvidingPowerTo(x, y, z, directionIn);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public void tick() {}

    @Override
    public boolean blockExists(int x, int y, int z) {
        return y >= 0 && y < 256;
    }

    @Override
    public boolean checkChunksExist(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true;
    }

    @Override
    public boolean doChunksNearChunkExist(int x, int y, int z, int radius) {
        return true;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public long getTotalWorldTime() {
        return FROZEN_WORLD_TIME;
    }

    @Override
    public long getWorldTime() {
        return FROZEN_WORLD_TIME;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        if (level == null) return _default;
        return level.isSideSolid(x, y, z, side, _default);
    }

    @Override
    public boolean func_147451_t(int x, int y, int z) {
        return false;
    }

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        TileEntity tileEntity = getTileEntity(x, y, z);
        if (tileEntity == null) {
            return;
        }
        applyDescriptionPacket(tileEntity);
    }

    @Override
    public void markTileEntityChunkModified(int x, int y, int z, TileEntity tileEntity) {}

    @Override
    public void notifyBlockChange(int x, int y, int z, Block block) {}

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {}

    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tileEntityIn) {
        unregisterTrackedTileEntity(x, y, z, null);
        level.setTileEntity(x, y, z, tileEntityIn);
        if (tileEntityIn != null) {
            tileEntityIn.validate();
            addTileEntity(tileEntityIn);
        }
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        TileEntity existing = level.getTileEntity(x, y, z);
        unregisterTrackedTileEntity(x, y, z, existing);
        level.setTileEntity(x, y, z, null);
        if (existing != null) {
            existing.invalidate();
        }
    }

    @Override
    public boolean func_147480_a(int x, int y, int z, boolean dropBlock) {
        Block existing = level.getBlock(x, y, z);
        if (existing == Blocks.air) {
            return false;
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        level.setBlock(x, y, z, Blocks.air, 0, null);
        if (tileEntity != null) {
            tileEntity.invalidate();
        }
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block blockType) {
        return setBlock(x, y, z, blockType, 0, 3);
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block blockIn, int metadataIn, int flags) {
        TileEntity tileEntity = null;
        if (blockIn != null && blockIn != Blocks.air && blockIn.hasTileEntity(metadataIn)) {
            tileEntity = GuidebookTileEntityLoader.load(this, blockIn, metadataIn, x, y, z, null);
        }
        level.setBlock(x, y, z, blockIn, metadataIn, tileEntity);
        if (blockIn != null && blockIn != Blocks.air) {
            try {
                blockIn.onBlockAdded(this, x, y, z);
            } catch (Throwable ignored) {}
        }
        return true;
    }

    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, int flags) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return false;
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (tileEntity == null && block.hasTileEntity(metadata)) {
            tileEntity = GuidebookTileEntityLoader.load(this, block, metadata, x, y, z, null);
        }
        level.setBlock(x, y, z, block, metadata, tileEntity);
        return true;
    }

    @Override
    public void updateEntities() {}

    public void updateEntitiesForPreview() {
        super.updateEntities();
    }

    public void syncLoadedTileEntities(Collection<TileEntity> tileEntities) {
        loadedTileEntityList.clear();
        if (tileEntities == null || tileEntities.isEmpty()) {
            return;
        }
        for (TileEntity tileEntity : tileEntities) {
            if (tileEntity == null || tileEntity.isInvalid()) {
                continue;
            }
            addTileEntity(tileEntity);
        }
    }

    public void syncLoadedEntities(Collection<Entity> entities) {
        loadedEntityList.clear();
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (Entity entity : entities) {
            if (entity == null || entity.isDead) {
                continue;
            }
            entity.worldObj = this;
            entity.dimension = provider.dimensionId;
            loadedEntityList.add(entity);
        }
    }

    private void unregisterTrackedTileEntity(int x, int y, int z, TileEntity exactTileEntity) {
        Iterator<TileEntity> iterator = loadedTileEntityList.iterator();
        while (iterator.hasNext()) {
            TileEntity tracked = iterator.next();
            if (tracked == null) {
                iterator.remove();
                continue;
            }
            if (exactTileEntity != null ? tracked == exactTileEntity
                : tracked.xCoord == x && tracked.yCoord == y && tracked.zCoord == z) {
                iterator.remove();
            }
        }
    }

    @Nullable
    private static Integer resolveBartWorksMetadata(@Nullable TileEntity tileEntity) {
        if (!isInstanceOf(tileEntity, BARTWORKS_META_GENERATED_TILE_CLASS)) {
            return null;
        }
        Field metaField = resolveBartWorksMetaField(tileEntity);
        if (metaField == null) {
            return null;
        }
        try {
            return Integer.valueOf(Math.max(0, metaField.getShort(tileEntity)));
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Field resolveBartWorksMetaField(@Nullable TileEntity tileEntity) {
        if (bartWorksMetaFieldResolved) {
            return bartWorksMetaField;
        }
        bartWorksMetaFieldResolved = true;
        if (tileEntity == null) {
            return null;
        }
        try {
            bartWorksMetaField = tileEntity.getClass()
                .getField("mMetaData");
        } catch (Throwable ignored) {
            bartWorksMetaField = null;
        }
        return bartWorksMetaField;
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

    private static void applyDescriptionPacket(TileEntity tileEntity) {
        try {
            Packet packet = tileEntity.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tileEntity.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }
}
