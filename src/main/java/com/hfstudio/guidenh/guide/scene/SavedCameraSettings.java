package com.hfstudio.guidenh.guide.scene;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record SavedCameraSettings(float rotationX, float rotationY, float rotationZ, float offsetX, float offsetY,
    float zoom) {

    public static SavedCameraSettings identity() {
        return new SavedCameraSettings(0f, 0f, 0f, 0f, 0f, 1f);
    }
}
