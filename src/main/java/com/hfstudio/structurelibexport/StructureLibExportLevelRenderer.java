package com.hfstudio.structurelibexport;

import java.util.List;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class StructureLibExportLevelRenderer extends GuidebookLevelRenderer {

    public void renderExportTile(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        int panelX, int panelY, int panelWidth, int panelHeight, int tileWidth, int tileHeight) {
        renderExportTile(
            level,
            camera,
            layers,
            List.of(),
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            tileWidth,
            tileHeight);
    }

    public void renderExportTile(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        List<InWorldAnnotation> annotations, int panelX, int panelY, int panelWidth, int panelHeight, int tileWidth,
        int tileHeight) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            0,
            0,
            tileWidth,
            tileHeight,
            0f,
            annotations != null ? annotations : List.of(),
            LightDarkMode.LIGHT_MODE,
            layers,
            List.of());
    }
}
