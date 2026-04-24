package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lightweight client-only world wrapper backed by a {@link GuidebookLevel}.
 */
@SideOnly(Side.CLIENT)
public class GuidebookFakeWorld extends World {

    private final GuidebookLevel level;

    public GuidebookFakeWorld(GuidebookLevel level) {
        super(
            new SaveHandlerMP(),
            "guidenh_preview",
            WorldProvider.getProviderForDimension(0),
            new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT),
            new Profiler());
        this.level = level;
        this.isRemote = true;
    }

    public GuidebookLevel getGuidebookLevel() {
        return level;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return null;
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
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        if (level == null) return _default;
        return level.isSideSolid(x, y, z, side, _default);
    }
}
