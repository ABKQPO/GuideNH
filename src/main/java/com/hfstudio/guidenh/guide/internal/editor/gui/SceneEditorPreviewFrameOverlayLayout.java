package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.LytRect;

public final class SceneEditorPreviewFrameOverlayLayout {

    private static final int LABEL_PADDING = 4;
    private static final int LABEL_ABOVE_OFFSET = 10;

    private SceneEditorPreviewFrameOverlayLayout() {}

    public static Layout resolve(LytRect previewViewport, int previewWidth, int previewHeight) {
        if (previewViewport == null || previewViewport.isEmpty()) {
            return new Layout(LytRect.empty(), 0, 0);
        }

        int frameWidth = Math.min(Math.max(0, previewWidth), previewViewport.width());
        int frameHeight = Math.min(Math.max(0, previewHeight), previewViewport.height());
        if (frameWidth <= 0 || frameHeight <= 0) {
            return new Layout(LytRect.empty(), 0, 0);
        }

        LytRect frameRect = new LytRect(0, 0, frameWidth, frameHeight).centerIn(previewViewport);
        int labelX = frameRect.x() + LABEL_PADDING;
        int labelY = Math.max(previewViewport.y() + LABEL_PADDING, frameRect.y() - LABEL_ABOVE_OFFSET);
        return new Layout(frameRect, labelX, labelY);
    }

    @Desugar
    public record Layout(LytRect frameRect, int labelX, int labelY) {}
}
