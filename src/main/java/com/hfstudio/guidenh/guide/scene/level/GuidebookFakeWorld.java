package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
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

    public GuidebookLevel getGuidebookLevel() {
        return level;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
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
    public void markBlockForUpdate(int x, int y, int z) {}

    @Override
    public void markTileEntityChunkModified(int x, int y, int z, TileEntity tileEntity) {}

    @Override
    public void notifyBlockChange(int x, int y, int z, Block block) {}

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {}

    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tileEntityIn) {
        level.setTileEntity(x, y, z, tileEntityIn);
        if (tileEntityIn != null) {
            tileEntityIn.validate();
        }
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        TileEntity existing = level.getTileEntity(x, y, z);
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
        level.setBlock(x, y, z, blockIn, metadataIn, null);
        return true;
    }
}
