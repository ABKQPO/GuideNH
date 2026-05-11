package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

/**
 * A tile-entity NBT operation applied by a Ponder keyframe.
 */
public class PonderKeyframeTileNbtOperation {

    private int x;
    private int y;
    private int z;
    @Nullable
    private String nbt;
    @Nullable
    private String path;
    @Nullable
    private String value;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Nullable
    public String getNbt() {
        return nbt;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    @Nullable
    public String getValue() {
        return value;
    }
}
