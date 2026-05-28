package com.hfstudio.structurelibexport;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;

public class StructureLibExportOverlayRenderer {

    public void render(CameraSettings camera, List<OverlayAnnotation> overlays, int panelX, int panelY, int panelWidth,
        int panelHeight, int tileWidth, int tileHeight) {
        List<OverlayAnnotation> effectiveOverlays = overlays != null ? overlays : List.of();
        if (effectiveOverlays.isEmpty()) {
            return;
        }
        VanillaRenderContext context = new VanillaRenderContext(
            LightDarkMode.LIGHT_MODE,
            new LytRect(0, 0, tileWidth, tileHeight),
            tileHeight);
        context.setDocumentOrigin(0, 0);
        context.setScrollOffsetY(0);
        context.restoreExternalRenderState();
        LytRect viewport = new LytRect(panelX, panelY, panelWidth, panelHeight);
        LytRect scissor = new LytRect(0, 0, tileWidth, tileHeight);
        context.pushScissor(scissor);
        try {
            for (OverlayAnnotation overlay : effectiveOverlays) {
                overlay.render(camera, context, viewport);
            }
        } finally {
            context.popScissor();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }
}
