package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.nbt.NBTTagCompound;

public class GuideStructureData {

    private final NBTTagCompound root;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public GuideStructureData(NBTTagCompound root, int sizeX, int sizeY, int sizeZ) {
        this.root = root;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public NBTTagCompound getRoot() {
        return root;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }
}
