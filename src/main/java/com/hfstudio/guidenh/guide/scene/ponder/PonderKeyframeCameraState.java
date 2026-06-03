package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Camera state snapshot for a single Ponder keyframe.
 * Any field left null inherits from the preceding keyframe (backward-resolved).
 */
@Getter
@RequiredArgsConstructor
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

    public boolean isEmpty() {
        return zoom == null && rotX == null && rotY == null && rotZ == null && offX == null && offY == null;
    }
}
