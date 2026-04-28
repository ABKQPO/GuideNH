package com.hfstudio.guidenh.guide.internal.scene;

import javax.annotation.Nullable;

import org.joml.Vector3f;

public final class GuidebookPreviewPlayerPose {

    public static final Vector3f DEFAULT_CAPE_ROTATION_DEGREES = new Vector3f(6.0f, 0.0f, 0.0f);
    public static final GuidebookPreviewPlayerPose DEFAULT = new GuidebookPreviewPlayerPose(null, null, null, null, null,
        null);

    @Nullable
    private final Vector3f headRotationDegrees;
    @Nullable
    private final Vector3f leftArmRotationDegrees;
    @Nullable
    private final Vector3f rightArmRotationDegrees;
    @Nullable
    private final Vector3f leftLegRotationDegrees;
    @Nullable
    private final Vector3f rightLegRotationDegrees;
    @Nullable
    private final Vector3f capeRotationDegrees;

    public GuidebookPreviewPlayerPose(@Nullable Vector3f headRotationDegrees, @Nullable Vector3f leftArmRotationDegrees,
        @Nullable Vector3f rightArmRotationDegrees, @Nullable Vector3f leftLegRotationDegrees,
        @Nullable Vector3f rightLegRotationDegrees, @Nullable Vector3f capeRotationDegrees) {
        this.headRotationDegrees = copy(headRotationDegrees);
        this.leftArmRotationDegrees = copy(leftArmRotationDegrees);
        this.rightArmRotationDegrees = copy(rightArmRotationDegrees);
        this.leftLegRotationDegrees = copy(leftLegRotationDegrees);
        this.rightLegRotationDegrees = copy(rightLegRotationDegrees);
        this.capeRotationDegrees = copy(capeRotationDegrees);
    }

    @Nullable
    public Vector3f getHeadRotationDegrees() {
        return copy(headRotationDegrees);
    }

    @Nullable
    public Vector3f getLeftArmRotationDegrees() {
        return copy(leftArmRotationDegrees);
    }

    @Nullable
    public Vector3f getRightArmRotationDegrees() {
        return copy(rightArmRotationDegrees);
    }

    @Nullable
    public Vector3f getLeftLegRotationDegrees() {
        return copy(leftLegRotationDegrees);
    }

    @Nullable
    public Vector3f getRightLegRotationDegrees() {
        return copy(rightLegRotationDegrees);
    }

    public Vector3f resolveCapeRotationDegrees() {
        return capeRotationDegrees != null ? new Vector3f(capeRotationDegrees) : new Vector3f(DEFAULT_CAPE_ROTATION_DEGREES);
    }

    @Nullable
    private static Vector3f copy(@Nullable Vector3f vector) {
        return vector == null ? null : new Vector3f(vector);
    }
}
