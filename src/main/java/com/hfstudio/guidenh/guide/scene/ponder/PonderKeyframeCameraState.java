package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

/**
 * Camera state snapshot for a single Ponder keyframe.
 * Any field left null inherits from the preceding keyframe (backward-resolved).
 */
public class PonderKeyframeCameraState {

    @Nullable
    private final Float zoom;
    @Nullable
    private final Float rotX;
    @Nullable
    private final Float rotY;
    @Nullable
    private final Float rotZ;
    @Nullable
    private final Float offX;
    @Nullable
    private final Float offY;

    public PonderKeyframeCameraState(@Nullable Float zoom, @Nullable Float rotX, @Nullable Float rotY,
        @Nullable Float rotZ, @Nullable Float offX, @Nullable Float offY) {
        this.zoom = zoom;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.offX = offX;
        this.offY = offY;
    }

    @Nullable
    public Float getZoom() {
        return zoom;
    }

    @Nullable
    public Float getRotX() {
        return rotX;
    }

    @Nullable
    public Float getRotY() {
        return rotY;
    }

    @Nullable
    public Float getRotZ() {
        return rotZ;
    }

    @Nullable
    public Float getOffX() {
        return offX;
    }

    @Nullable
    public Float getOffY() {
        return offY;
    }

    public boolean isEmpty() {
        return zoom == null && rotX == null && rotY == null && rotZ == null && offX == null && offY == null;
    }
}
