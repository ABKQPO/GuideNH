package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

public final class ExportBlockContext {

    private final ExportSession session;
    private final int worldX;
    private final int worldY;
    private final int worldZ;
    private final Block block;
    private final int meta;
    @Nullable
    private final TileEntity tileEntity;
    private final NBTTagCompound structureBlockTag;

    public ExportBlockContext(ExportSession session, int worldX, int worldY, int worldZ, Block block, int meta,
        @Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag) {
        this.session = session;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.block = block;
        this.meta = meta;
        this.tileEntity = tileEntity;
        this.structureBlockTag = structureBlockTag;
    }

    public ExportSession session() {
        return session;
    }

    public int worldX() {
        return worldX;
    }

    public int worldY() {
        return worldY;
    }

    public int worldZ() {
        return worldZ;
    }

    public Block block() {
        return block;
    }

    public int meta() {
        return meta;
    }

    @Nullable
    public TileEntity tileEntity() {
        return tileEntity;
    }

    public NBTTagCompound structureBlockTag() {
        return structureBlockTag;
    }
}
