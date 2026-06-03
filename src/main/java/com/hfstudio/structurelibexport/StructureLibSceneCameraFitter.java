package com.hfstudio.structurelibexport;

import org.joml.Vector3f;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class StructureLibSceneCameraFitter {

    public static final int PADDING_PIXELS = 16;

    public FittedCamera fit(GuidebookLevel level, StructureLibExportTaskSpec task) {
        return fit(level, task.getPixelsPerBlock(), task.getScale(), task.getView());
    }

    public FittedCamera fit(GuidebookLevel level, int pixelsPerBlock, float scale, StructureLibExportView view) {
        int[] bounds = level.getBounds();
        int blockPixels = Math.max(1, Math.round(pixelsPerBlock * scale));
        CameraSettings camera = new CameraSettings();
        StructureLibExportView effectiveView = view != null ? view : StructureLibExportView.defaultView();
        effectiveView.apply(camera);
        float centerX = (bounds[0] + bounds[3] + 1f) * 0.5f;
        float centerY = (bounds[1] + bounds[4] + 1f) * 0.5f;
        float centerZ = (bounds[2] + bounds[5] + 1f) * 0.5f;
        camera.setRotationCenter(centerX, centerY, centerZ);
        camera.setZoom(blockPixels / 10f);
        camera.setViewportSize(1024, 1024);

        ProjectionBounds projectionBounds = projectBounds(camera, bounds);
        int width = Math.max(16, (int) Math.ceil(projectionBounds.width()) + PADDING_PIXELS * 2);
        int height = Math.max(16, (int) Math.ceil(projectionBounds.height()) + PADDING_PIXELS * 2);
        camera.setViewportSize(width, height);
        projectionBounds = projectBounds(camera, bounds);
        camera.setOffsetX(-(projectionBounds.minX + projectionBounds.maxX) * 0.5f);
        camera.setOffsetY((projectionBounds.minY + projectionBounds.maxY) * 0.5f);
        return new FittedCamera(camera, width, height);
    }

    private ProjectionBounds projectBounds(CameraSettings camera, int[] bounds) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float lx = bounds[0];
        float ly = bounds[1];
        float lz = bounds[2];
        float hx = bounds[3] + 1f;
        float hy = bounds[4] + 1f;
        float hz = bounds[5] + 1f;
        for (int index = 0; index < 8; index++) {
            float x = (index & 1) == 0 ? lx : hx;
            float y = (index & 2) == 0 ? ly : hy;
            float z = (index & 4) == 0 ? lz : hz;
            Vector3f projected = camera.worldToScreen(x, y, z);
            minX = Math.min(minX, projected.x);
            maxX = Math.max(maxX, projected.x);
            minY = Math.min(minY, projected.y);
            maxY = Math.max(maxY, projected.y);
        }
        return new ProjectionBounds(minX, maxX, minY, maxY);
    }

    @Desugar
    public record FittedCamera(CameraSettings camera, int width, int height) {}

    @Desugar
    public record ProjectionBounds(float minX, float maxX, float minY, float maxY) {

        public float width() {
            return maxX - minX;
        }

        public float height() {
            return maxY - minY;
        }
    }
}
