package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

/**
 * A JSON-declared entity action applied by a Ponder keyframe.
 */
public class PonderKeyframeEntityAction {

    @Nullable
    private String ref;
    @Nullable
    private String id;
    @Nullable
    private Double x;
    @Nullable
    private Double y;
    @Nullable
    private Double z;
    @Nullable
    private Float yaw;
    @Nullable
    private Float pitch;
    @Nullable
    private String nbt;
    @Nullable
    private String path;
    @Nullable
    private String value;
    @Nullable
    private String name;
    @Nullable
    private String uuid;

    @Nullable
    public String getRef() {
        return ref;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public Double getX() {
        return x;
    }

    @Nullable
    public Double getY() {
        return y;
    }

    @Nullable
    public Double getZ() {
        return z;
    }

    @Nullable
    public Float getYaw() {
        return yaw;
    }

    @Nullable
    public Float getPitch() {
        return pitch;
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

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getUuid() {
        return uuid;
    }
}
