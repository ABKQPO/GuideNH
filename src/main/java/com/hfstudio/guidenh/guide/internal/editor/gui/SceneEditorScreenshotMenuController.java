package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorScreenshotFormat;
import com.hfstudio.guidenh.guide.internal.ui.GuideSliderRenderer;

public class SceneEditorScreenshotMenuController {

    public static final int MENU_WIDTH = 156;
    public static final int MENU_PADDING = 6;
    public static final int FORMAT_ROW_HEIGHT = 18;
    public static final int SECTION_GAP = 6;
    public static final int SCALE_LABEL_HEIGHT = 10;
    public static final int SCALE_INPUT_WIDTH = 30;
    public static final int SCALE_INPUT_HEIGHT = 16;

    private final Consumer<SceneEditorScreenshotFormat> formatApplier;
    private final IntConsumer scaleApplier;
    private final SceneEditorNumericFieldController scaleController;
    private SceneEditorScreenshotFormat format;
    private boolean open;

    public SceneEditorScreenshotMenuController(SceneEditorScreenshotFormat initialFormat, int initialScale,
        Consumer<SceneEditorScreenshotFormat> formatApplier, IntConsumer scaleApplier) {
        this.format = initialFormat != null ? initialFormat : SceneEditorScreenshotFormat.PNG;
        this.formatApplier = formatApplier;
        this.scaleApplier = scaleApplier;
        this.scaleController = SceneEditorNumericFieldController
            .integer(initialScale, 1f, 8f, value -> this.scaleApplier.accept(Math.round(value)));
    }

    public boolean isOpen() {
        return open;
    }

    public void toggleOpen() {
        open = !open;
    }

    public void close() {
        open = false;
    }

    public void handleOutsideClick(boolean insideButton, boolean insideMenu) {
        if (!insideButton && !insideMenu) {
            close();
        }
    }

    public SceneEditorScreenshotFormat getFormat() {
        return format;
    }

    public int getScale() {
        return Math.round(scaleController.getValue());
    }

    public String getScaleDraftText() {
        return scaleController.getDraftText();
    }

    public void setScaleDraftText(String draft) {
        scaleController.setDraftText(draft);
    }

    public boolean hasScaleValidationError() {
        return scaleController.hasValidationError();
    }

    public int menuHeight() {
        return MENU_PADDING + SceneEditorScreenshotFormat.values().length * FORMAT_ROW_HEIGHT
            + SECTION_GAP
            + SCALE_LABEL_HEIGHT
            + SCALE_INPUT_HEIGHT
            + 14
            + MENU_PADDING;
    }

    @Nullable
    public SceneEditorScreenshotFormat formatAt(LytRect menuBounds, int mouseX, int mouseY) {
        if (menuBounds == null || mouseX < menuBounds.x() || mouseX >= menuBounds.right()) {
            return null;
        }
        int localY = mouseY - (menuBounds.y() + MENU_PADDING);
        if (localY < 0) {
            return null;
        }
        int index = localY / FORMAT_ROW_HEIGHT;
        SceneEditorScreenshotFormat[] values = SceneEditorScreenshotFormat.values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }

    public void selectFormat(SceneEditorScreenshotFormat nextFormat) {
        if (nextFormat == null) {
            return;
        }
        format = nextFormat;
        formatApplier.accept(nextFormat);
    }

    public boolean commitScaleDraft(String draft) {
        scaleController.setDraftText(draft);
        return scaleController.commitDraftText();
    }

    public void nudgeScale(int wheelDelta) {
        scaleController.nudgeByWheel(wheelDelta);
    }

    public void applyScaleFraction(float fraction) {
        scaleController.applySliderValue(1f + 7f * fraction);
    }

    public LytRect scaleInputBounds(LytRect menuBounds) {
        int sectionTop = menuBounds.y() + MENU_PADDING
            + SceneEditorScreenshotFormat.values().length * FORMAT_ROW_HEIGHT
            + SECTION_GAP
            + SCALE_LABEL_HEIGHT;
        return new LytRect(menuBounds.x() + MENU_PADDING, sectionTop, SCALE_INPUT_WIDTH, SCALE_INPUT_HEIGHT);
    }

    public LytRect scaleSliderBounds(LytRect menuBounds) {
        LytRect inputBounds = scaleInputBounds(menuBounds);
        int sliderX = inputBounds.right() + 8;
        int sliderWidth = Math.max(24, menuBounds.right() - MENU_PADDING - sliderX);
        int sliderY = inputBounds.y() + (SCALE_INPUT_HEIGHT - GuideSliderRenderer.TRACK_HEIGHT) / 2;
        return new LytRect(sliderX, sliderY, sliderWidth, GuideSliderRenderer.TRACK_HEIGHT);
    }

    public float scaleFraction() {
        return scaleController.getSliderFraction();
    }
}
