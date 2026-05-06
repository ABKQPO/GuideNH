package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * One placed structure voxel after {@link com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer}; carries
 * the full {@code blocks[]} compound for sidecars.
 */
public final class ImportBlockContext {

    private final GuidebookLevel level;
    private final int x;
    private final int y;
    private final int z;
    @Nullable
    private final NBTTagCompound structureBlockCompound;

    public ImportBlockContext(GuidebookLevel level, int x, int y, int z,
        @Nullable NBTTagCompound structureBlockCompound) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.structureBlockCompound = structureBlockCompound;
    }

    public GuidebookLevel level() {
        return level;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Nullable
    public NBTTagCompound structureBlockCompound() {
        return structureBlockCompound;
    }
}
