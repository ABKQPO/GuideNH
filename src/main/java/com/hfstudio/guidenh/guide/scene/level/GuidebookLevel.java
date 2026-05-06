package com.hfstudio.guidenh.guide.scene.level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
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

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.distanthorizons.DistantHorizonsCompat;
import com.hfstudio.guidenh.guide.scene.snapshot.GuidebookPreviewAuthorityStore;
import com.hfstudio.guidenh.guide.scene.support.GuideForgeMultipartSupport;
import com.hfstudio.guidenh.guide.scene.support.GuidePreviewStateSupport;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GuidebookLevel implements IBlockAccess, GuidebookChunkSource {

    private final LinkedHashMap<ChunkCoordIntPair, GuidebookChunk> chunks = new LinkedHashMap<>();

    private final HashMap<Long, TileEntity> tileEntities = new HashMap<>();
    private final LinkedHashMap<Integer, Entity> entities = new LinkedHashMap<>();

    private final HashMap<Long, int[]> filledBlocks = new HashMap<>();
    private final HashMap<Long, String> explicitBlockIds = new HashMap<>();

    /** Opaque server-authoritative preview blobs per coordinate ({@link #packPos}); cleared when block becomes air. */
    private final GuidebookPreviewAuthorityStore previewAuthorityStore = new GuidebookPreviewAuthorityStore();

    // Pre-built unmodifiable views returned every call to avoid per-frame
    // Collections.unmodifiableCollection() wrapper allocation (hot on the render loop).
    private final Collection<int[]> filledBlocksView = Collections.unmodifiableCollection(filledBlocks.values());
    private final Collection<TileEntity> tileEntitiesView = Collections.unmodifiableCollection(tileEntities.values());
    private final Collection<Entity> entitiesView = Collections.unmodifiableCollection(entities.values());
    private final Collection<GuidebookChunk> chunksView = Collections.unmodifiableCollection(chunks.values());

    // Reusable bounds scratch buffer returned from getBounds(); callers consume immediately.
    private final int[] boundsScratch = new int[6];

    @Nullable
    private GuidebookFakeWorld fakeWorld;

    private boolean previewStateDirty = true;

    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private boolean boundsDirty = true;

    public GuidebookFakeWorld getOrCreateFakeWorld() {
        if (fakeWorld == null) {
            try (var ignored = DistantHorizonsCompat.suppressClientWorldLoadHooks()) {
                fakeWorld = new GuidebookFakeWorld(this);
            }
        }
        return fakeWorld;
    }

    public void rebindAllTileEntities() {
        GuidebookFakeWorld world = getOrCreateFakeWorld();
        for (TileEntity te : tileEntities.values()) {
            bindTileEntity(te, te.xCoord, te.yCoord, te.zCoord, world);
            te.validate();
        }
        world.syncLoadedTileEntities(tileEntities.values());
        for (Entity entity : entities.values()) {
            bindEntity(entity, world);
        }
        world.syncLoadedEntities(entities.values());
    }

    public void prepareForPreview() {
        if (!previewStateDirty) {
            return;
        }
        previewStateDirty = false;
        rebindAllTileEntities();
        GuidePreviewStateSupport.prepare(this);
        tickPreviewWorld();
        previewStateDirty = false;
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
            explicitBlockIds.remove(key);
            previewAuthorityStore.clearAt(key);
        } else {
            filledBlocks.put(key, new int[] { x, y, z });
            // For ForgeMultipart tiles, extract the primary microblock material to get a meaningful
            // "modid:blockname[:meta]" export ID instead of "ForgeMultipart:multipart".
            String resolvedBlockId;
            if (tileEntity != null && GuideForgeMultipartSupport.isForgeMultipartBlock(block)) {
                String fmpId = GuideForgeMultipartSupport.resolvePrimaryMicroblockId(tileEntity);
                resolvedBlockId = fmpId != null ? fmpId : resolveBlockId(block);
            } else {
                resolvedBlockId = resolveBlockId(block);
            }
            if (resolvedBlockId != null) {
                explicitBlockIds.put(key, resolvedBlockId);
            } else {
                explicitBlockIds.remove(key);
            }
            if (tileEntity != null) {
                bindTileEntity(tileEntity, x, y, z, getOrCreateFakeWorld());
                tileEntity.validate();
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
        previewStateDirty = true;
    }

    public void setBlock(int x, int y, int z, @Nullable Block block, int meta) {
        setBlock(x, y, z, block, meta, null);
    }

    public void setTileEntity(int x, int y, int z, @Nullable TileEntity tileEntity) {
        long key = packPos(x, y, z);
        TileEntity existing = tileEntities.get(key);
        if (existing == tileEntity) {
            if (tileEntity != null) {
                bindTileEntity(tileEntity, x, y, z, getOrCreateFakeWorld());
            }
            return;
        }
        if (tileEntity == null) {
            tileEntities.remove(key);
        } else {
            bindTileEntity(tileEntity, x, y, z, getOrCreateFakeWorld());
            tileEntity.validate();
            tileEntities.put(key, tileEntity);
        }
        previewStateDirty = true;
    }

    public boolean setBlockMetadata(int x, int y, int z, int meta) {
        if (y < 0 || y >= 256) {
            return false;
        }

        GuidebookChunk chunk = getChunk(x >> 4, z >> 4, false);
        if (chunk == null) {
            return false;
        }

        Block block = chunk.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return false;
        }

        chunk.setBlock(x, y, z, block, meta);

        TileEntity tileEntity = tileEntities.get(packPos(x, y, z));
        if (tileEntity != null) {
            bindTileEntity(tileEntity, x, y, z, getOrCreateFakeWorld());
        }

        previewStateDirty = true;
        return true;
    }

    public void setExplicitBlockId(int x, int y, int z, @Nullable String blockId) {
        long key = packPos(x, y, z);
        if (blockId == null || blockId.trim()
            .isEmpty()) {
            explicitBlockIds.remove(key);
        } else {
            explicitBlockIds.put(key, blockId.trim());
        }
    }

    @Nullable
    public String getExplicitBlockId(int x, int y, int z) {
        return explicitBlockIds.get(packPos(x, y, z));
    }

    public GuidebookPreviewAuthorityStore previewAuthorityStore() {
        return previewAuthorityStore;
    }

    public boolean isEmpty() {
        return filledBlocks.isEmpty() && entities.isEmpty();
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

    public Collection<Entity> getEntities() {
        return entitiesView;
    }

    public void addEntity(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }
        if (entity.worldObj == null && fakeWorld != null) {
            bindEntity(entity, fakeWorld);
        }
        entities.put(entity.getEntityId(), entity);
        boundsDirty = true;
        previewStateDirty = true;
    }

    public void removeEntity(int entityId) {
        if (entities.remove(entityId) != null) {
            boundsDirty = true;
            previewStateDirty = true;
        }
    }

    @Nullable
    public Entity getEntity(int entityId) {
        return entities.get(entityId);
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
            for (Entity entity : entities.values()) {
                if (entity == null || entity.boundingBox == null) {
                    continue;
                }
                int ex0 = (int) Math.floor(entity.boundingBox.minX);
                int ey0 = (int) Math.floor(entity.boundingBox.minY);
                int ez0 = (int) Math.floor(entity.boundingBox.minZ);
                int ex1 = Math.max(ex0, (int) Math.ceil(entity.boundingBox.maxX) - 1);
                int ey1 = Math.max(ey0, (int) Math.ceil(entity.boundingBox.maxY) - 1);
                int ez1 = Math.max(ez0, (int) Math.ceil(entity.boundingBox.maxZ) - 1);
                if (ex0 < lx) lx = ex0;
                if (ey0 < ly) ly = ey0;
                if (ez0 < lz) lz = ez0;
                if (ex1 > hx) hx = ex1;
                if (ey1 > hy) hy = ey1;
                if (ez1 > hz) hz = ez1;
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
        double minCenterX = Double.POSITIVE_INFINITY;
        double minCenterY = Double.POSITIVE_INFINITY;
        double minCenterZ = Double.POSITIVE_INFINITY;
        double maxCenterX = Double.NEGATIVE_INFINITY;
        double maxCenterY = Double.NEGATIVE_INFINITY;
        double maxCenterZ = Double.NEGATIVE_INFINITY;
        for (int[] p : filledBlocks.values()) {
            minCenterX = Math.min(minCenterX, p[0]);
            minCenterY = Math.min(minCenterY, p[1]);
            minCenterZ = Math.min(minCenterZ, p[2]);
            maxCenterX = Math.max(maxCenterX, p[0] + 1.0D);
            maxCenterY = Math.max(maxCenterY, p[1] + 1.0D);
            maxCenterZ = Math.max(maxCenterZ, p[2] + 1.0D);
        }
        for (Entity entity : entities.values()) {
            if (entity == null || entity.boundingBox == null) {
                continue;
            }
            minCenterX = Math.min(minCenterX, entity.boundingBox.minX);
            minCenterY = Math.min(minCenterY, entity.boundingBox.minY);
            minCenterZ = Math.min(minCenterZ, entity.boundingBox.minZ);
            maxCenterX = Math.max(maxCenterX, entity.boundingBox.maxX);
            maxCenterY = Math.max(maxCenterY, entity.boundingBox.maxY);
            maxCenterZ = Math.max(maxCenterZ, entity.boundingBox.maxZ);
        }
        if (!Double.isFinite(minCenterX) || !Double.isFinite(maxCenterX)) {
            return new float[] { 0f, 0f, 0f };
        }
        return new float[] { (float) ((minCenterX + maxCenterX) * 0.5D), (float) ((minCenterY + maxCenterY) * 0.5D),
            (float) ((minCenterZ + maxCenterZ) * 0.5D) };
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
        Block block = getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return 0;
        }

        int weakPower = block.isProvidingWeakPower(this, x, y, z, directionIn);
        int strongPower = block.isProvidingStrongPower(this, x, y, z, directionIn);
        return Math.max(weakPower, strongPower);
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

    public static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) | (((long) (z & 0x3FFFFFF)) << 26) | (((long) (y & 0xFF)) << 52);
    }

    private void tickPreviewWorld() {
        GuidebookFakeWorld world = getOrCreateFakeWorld();
        try {
            world.updateEntitiesForPreview();
        } catch (Throwable ignored) {
            tickPreviewTileEntitiesFallback();
        }
    }

    private void tickPreviewTileEntitiesFallback() {
        TileEntity[] snapshot = tileEntities.values()
            .toArray(new TileEntity[0]);
        for (TileEntity tileEntity : snapshot) {
            if (tileEntity == null || !tileEntity.canUpdate()) {
                continue;
            }
            try {
                tileEntity.updateEntity();
            } catch (Throwable ignored) {}
        }
    }

    private void bindEntity(Entity entity, World world) {
        entity.worldObj = world;
        entity.dimension = world.provider.dimensionId;
    }

    private void bindTileEntity(TileEntity tileEntity, int x, int y, int z, World world) {
        try {
            tileEntity.updateContainingBlockInfo();
        } catch (Throwable ignored) {}
        tileEntity.xCoord = x;
        tileEntity.yCoord = y;
        tileEntity.zCoord = z;
        tileEntity.blockType = getBlock(x, y, z);
        tileEntity.blockMetadata = getBlockMetadata(x, y, z);
        tileEntity.setWorldObj(world);
    }

    @Nullable
    public static String resolveBlockId(@Nullable Block block) {
        if (block == null || block == Blocks.air) {
            return null;
        }

        try {
            GameRegistry.UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(block);
            if (uniqueIdentifier != null) {
                return uniqueIdentifier.toString();
            }
        } catch (RuntimeException ignored) {
            // Tests and synthetic preview blocks can reach here without a full loader context.
        }

        Object registryName = Block.blockRegistry.getNameForObject(block);
        if (registryName != null) {
            String normalized = normalizeBlockId(registryName.toString());
            if (normalized != null) {
                return normalized;
            }
        }

        return normalizeBlockId(block.getUnlocalizedName());
    }

    @Nullable
    public static String normalizeBlockId(@Nullable String candidate) {
        if (candidate == null) {
            return null;
        }

        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("tile.") && trimmed.length() > 5) {
            return "minecraft:" + trimmed.substring(5);
        }

        int tileNamespaceIndex = trimmed.indexOf(":tile.");
        if (tileNamespaceIndex >= 0) {
            return trimmed.substring(0, tileNamespaceIndex + 1) + trimmed.substring(tileNamespaceIndex + 6);
        }

        return trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
    }

}
