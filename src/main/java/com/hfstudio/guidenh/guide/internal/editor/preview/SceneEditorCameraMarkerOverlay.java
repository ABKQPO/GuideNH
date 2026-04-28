package com.hfstudio.guidenh.guide.internal.editor.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

public class SceneEditorCameraMarkerOverlay {

    private static final int MARKER_SIZE = 16;
    private static final int MARKER_HALF_SIZE = MARKER_SIZE / 2;
    private static final int MARKER_SHADOW_COLOR = 0x70000000;
    private static final int MARKER_TINT = 0xF8FFFFFF;
    private final Vector3f projectedScratch = new Vector3f();

    public LytRect getMarkerBounds(SceneEditorSceneModel model, CameraSettings camera, LytRect viewport) {
        Vector3f projected = camera
            .worldToScreen(model.getCenterX(), model.getCenterY(), model.getCenterZ(), projectedScratch);
        int centerX = viewport.x() + viewport.width() / 2 + Math.round(projected.x);
        int centerY = viewport.y() + viewport.height() / 2 + Math.round(projected.y);
        return new LytRect(centerX - MARKER_HALF_SIZE, centerY - MARKER_HALF_SIZE, MARKER_SIZE, MARKER_SIZE);
    }

    public void render(SceneEditorSceneModel model, CameraSettings camera, LytRect viewport) {
        LytRect bounds = getMarkerBounds(model, camera, viewport);
        Gui.drawRect(bounds.x() + 2, bounds.y() + 2, bounds.right() - 2, bounds.bottom() - 2, MARKER_SHADOW_COLOR);
        GuideIconButton.drawIcon(
            Minecraft.getMinecraft(),
            GuideIconButton.Role.SHOW_ANNOTATIONS,
            bounds.x(),
            bounds.y(),
            bounds.width(),
            bounds.height(),
            MARKER_TINT);
    }
}
