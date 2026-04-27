package com.hfstudio.guidenh.guide.scene.level;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuidebookTileEntityLoader {

    private GuidebookTileEntityLoader() {}

    @Nullable
    public static TileEntity load(World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        TileEntity tileEntity = tryCreateFromBlock(block, world, meta);
        if (tileEntity != null) {
            bindTile(tileEntity, world, block, meta, x, y, z);
            if (tag != null) {
                applyTag(tileEntity, tag, x, y, z);
                tileEntity = resolveWorldReplacement(world, x, y, z, tileEntity);
                bindTile(tileEntity, world, block, meta, x, y, z);
            }
            return tileEntity;
        }

        tileEntity = tag != null ? tryCreateAndLoad(tag, x, y, z) : null;
        if (tileEntity != null) {
            bindTile(tileEntity, world, block, meta, x, y, z);
            if (tag != null) {
                applyTag(tileEntity, tag, x, y, z);
                tileEntity = resolveWorldReplacement(world, x, y, z, tileEntity);
                bindTile(tileEntity, world, block, meta, x, y, z);
            }
        }

        return tileEntity;
    }

    @Nullable
    private static TileEntity tryCreateAndLoad(NBTTagCompound tag, int x, int y, int z) {
        try {
            return TileEntity.createAndLoadEntity(withWorldPosition(tag, x, y, z));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static TileEntity tryCreateFromBlock(Block block, World world, int meta) {
        try {
            if (block.hasTileEntity(meta)) {
                return block.createTileEntity(world, meta);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void applyTag(TileEntity tileEntity, NBTTagCompound tag, int x, int y, int z) {
        try {
            tileEntity.readFromNBT(withWorldPosition(tag, x, y, z));
        } catch (Exception ignored) {}
    }

    private static void bindTile(TileEntity tileEntity, World world, Block block, int meta, int x, int y, int z) {
        tileEntity.xCoord = x;
        tileEntity.yCoord = y;
        tileEntity.zCoord = z;
        tileEntity.blockType = block;
        tileEntity.blockMetadata = meta;
        tileEntity.setWorldObj(world);
    }

    private static TileEntity resolveWorldReplacement(World world, int x, int y, int z, TileEntity current) {
        try {
            TileEntity replacement = world.getTileEntity(x, y, z);
            if (replacement != null && replacement != current) {
                return replacement;
            }
        } catch (Throwable ignored) {}
        return current;
    }

    private static NBTTagCompound withWorldPosition(NBTTagCompound original, int x, int y, int z) {
        NBTTagCompound copy = (NBTTagCompound) original.copy();
        copy.setInteger("x", x);
        copy.setInteger("y", y);
        copy.setInteger("z", z);
        return copy;
    }
}
