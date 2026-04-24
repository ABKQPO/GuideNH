package com.hfstudio.guidenh.guide.scene.level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GuidebookLevel implements IBlockAccess, GuidebookChunkSource {

    private final LinkedHashMap<ChunkCoordIntPair, GuidebookChunk> chunks = new LinkedHashMap<>();

    private final HashMap<Long, TileEntity> tileEntities = new HashMap<>();

    private final HashMap<Long, int[]> filledBlocks = new HashMap<>();

    // Pre-built unmodifiable views returned every call to avoid per-frame
    // Collections.unmodifiableCollection() wrapper allocation (hot on the render loop).
    private final Collection<int[]> filledBlocksView = Collections.unmodifiableCollection(filledBlocks.values());
    private final Collection<TileEntity> tileEntitiesView = Collections.unmodifiableCollection(tileEntities.values());
    private final Collection<GuidebookChunk> chunksView = Collections.unmodifiableCollection(chunks.values());

    // Reusable bounds scratch buffer returned from getBounds(); callers consume immediately.
    private final int[] boundsScratch = new int[6];

    @Nullable
    private GuidebookFakeWorld fakeWorld;

    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private boolean boundsDirty = true;

    public GuidebookFakeWorld getOrCreateFakeWorld() {
        if (fakeWorld == null) {
            fakeWorld = new GuidebookFakeWorld(this);
        }
        return fakeWorld;
    }

    public void rebindAllTileEntities() {
        World world = getOrCreateFakeWorld();
        for (TileEntity te : tileEntities.values()) {
            te.setWorldObj(world);
        }
    }

    public void setBlock(int x, int y, int z, @Nullable Block block, int meta, @Nullable TileEntity tileEntity) {
        if (y < 0 || y >= 256) return;

        boolean isAir = block == null || block == Blocks.air;
        var pair = new ChunkCoordIntPair(x >> 4, z >> 4);
        GuidebookChunk chunk = chunks.get(pair);
        if (chunk == null) {
            if (isAir) return;
            chunk = new GuidebookChunk(x >> 4, z >> 4);
            chunks.put(pair, chunk);
        }

        chunk.setBlock(x, y, z, isAir ? null : block, meta);
        long key = packPos(x, y, z);

        if (isAir) {
            filledBlocks.remove(key);
            tileEntities.remove(key);
        } else {
            filledBlocks.put(key, new int[] { x, y, z });
            if (tileEntity != null) {
                tileEntity.xCoord = x;
                tileEntity.yCoord = y;
                tileEntity.zCoord = z;
                tileEntity.blockType = block;
                tileEntity.blockMetadata = meta;
                tileEntity.setWorldObj(getOrCreateFakeWorld());
                tileEntities.put(key, tileEntity);
            } else {
                tileEntities.remove(key);
            }
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
        boundsDirty = true;
    }

    public void setBlock(int x, int y, int z, @Nullable Block block, int meta) {
        setBlock(x, y, z, block, meta, null);
    }

    public void setTileEntity(int x, int y, int z, @Nullable TileEntity tileEntity) {
        long key = packPos(x, y, z);
        if (tileEntity == null) {
            tileEntities.remove(key);
        } else {
            tileEntity.xCoord = x;
            tileEntity.yCoord = y;
            tileEntity.zCoord = z;
            tileEntity.setWorldObj(getOrCreateFakeWorld());
            tileEntities.put(key, tileEntity);
        }
    }

    public boolean isEmpty() {
        return filledBlocks.isEmpty();
    }

    public Collection<int[]> getFilledBlocks() {
        return filledBlocksView;
    }

    public Collection<GuidebookChunk> getChunks() {
        return chunksView;
    }

    public Collection<TileEntity> getTileEntities() {
        return tileEntitiesView;
    }

    public int[] getBounds() {
        int[] out = boundsScratch;
        if (isEmpty()) {
            out[0] = out[1] = out[2] = out[3] = out[4] = out[5] = 0;
            return out;
        }
        if (boundsDirty) {
            int lx = Integer.MAX_VALUE, ly = Integer.MAX_VALUE, lz = Integer.MAX_VALUE;
            int hx = Integer.MIN_VALUE, hy = Integer.MIN_VALUE, hz = Integer.MIN_VALUE;
            for (int[] p : filledBlocks.values()) {
                if (p[0] < lx) lx = p[0];
                if (p[1] < ly) ly = p[1];
                if (p[2] < lz) lz = p[2];
                if (p[0] > hx) hx = p[0];
                if (p[1] > hy) hy = p[1];
                if (p[2] > hz) hz = p[2];
            }
            minX = lx;
            minY = ly;
            minZ = lz;
            maxX = hx;
            maxY = hy;
            maxZ = hz;
            boundsDirty = false;
        }
        out[0] = minX;
        out[1] = minY;
        out[2] = minZ;
        out[3] = maxX;
        out[4] = maxY;
        out[5] = maxZ;
        return out;
    }

    public float[] getCenter() {
        if (isEmpty()) return new float[] { 0f, 0f, 0f };
        var b = getBounds();
        return new float[] { (b[0] + b[3] + 1) * 0.5f, (b[1] + b[4] + 1) * 0.5f, (b[2] + b[5] + 1) * 0.5f };
    }

    public GuidebookLevel withSampleChest() {
        var te = new TileEntityChest();
        var nbt = new NBTTagCompound();
        var itemsTag = new NBTTagList();
        var slot0 = new NBTTagCompound();
        slot0.setByte("Slot", (byte) 0);
        slot0.setShort("id", (short) Item.getIdFromItem(Items.baked_potato));
        slot0.setByte("Count", (byte) 1);
        slot0.setShort("Damage", (short) 0);
        itemsTag.appendTag(slot0);
        nbt.setTag("Items", itemsTag);
        te.readFromNBT(nbt);
        setBlock(0, 0, 0, Blocks.chest, 0, te);
        return this;
    }

    // GuidebookChunkSource
    @Override
    @Nullable
    public GuidebookChunk getChunk(int chunkX, int chunkZ, boolean create) {
        var pair = new ChunkCoordIntPair(chunkX, chunkZ);
        var chunk = chunks.get(pair);
        if (chunk == null && create) {
            chunk = new GuidebookChunk(chunkX, chunkZ);
            chunks.put(pair, chunk);
        }
        return chunk;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= 256) return Blocks.air;
        var chunk = getChunk(x >> 4, z >> 4, false);
        if (chunk == null) return Blocks.air;
        Block b = chunk.getBlock(x, y, z);
        return b != null ? b : Blocks.air;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return tileEntities.get(packPos(x, y, z));
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (y < 0 || y >= 256) return 0;
        var chunk = getChunk(x >> 4, z >> 4, false);
        return chunk == null ? 0 : chunk.getMeta(x, y, z);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z).getMaterial() == Material.air;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return (15 << 20) | (15 << 4);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getHeight() {
        return 256;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        Block block = getBlock(x, y, z);
        if (block == null || block == Blocks.air) return _default;
        return block.isSideSolid(this, x, y, z, side);
    }

    private static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) | (((long) (z & 0x3FFFFFF)) << 26) | (((long) (y & 0xFF)) << 52);
    }

}
