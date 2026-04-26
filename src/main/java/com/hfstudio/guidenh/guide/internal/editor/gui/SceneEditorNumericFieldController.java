package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.function.Consumer;

public final class SceneEditorNumericFieldController {

    private final boolean integerMode;
    private final float minValue;
    private final float maxValue;
    private final Consumer<Float> valueApplier;

    private float value;
    private String draftText;
    private boolean validationError;

    private SceneEditorNumericFieldController(boolean integerMode, float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier) {
        this.integerMode = integerMode;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.valueApplier = valueApplier;
        this.value = clamp(initialValue);
        this.draftText = formatValue(this.value);
        this.validationError = false;
    }

    public static SceneEditorNumericFieldController integer(float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier) {
        return new SceneEditorNumericFieldController(true, initialValue, minValue, maxValue, valueApplier);
    }

    public static SceneEditorNumericFieldController decimal(float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier) {
        return new SceneEditorNumericFieldController(false, initialValue, minValue, maxValue, valueApplier);
    }

    public float getValue() {
        return value;
    }

    public String getDraftText() {
        return draftText;
    }

    public void setDraftText(String draftText) {
        this.draftText = draftText != null ? draftText : "";
        this.validationError = false;
    }

    public boolean hasValidationError() {
        return validationError;
    }

    public boolean commitDraftText() {
        if (draftText.trim()
            .isEmpty()) {
            draftText = formatValue(value);
            validationError = false;
            valueApplier.accept(value);
            return true;
        }

        float parsedValue;
        try {
            parsedValue = integerMode ? Integer.parseInt(draftText.trim()) : Float.parseFloat(draftText.trim());
        } catch (NumberFormatException e) {
            validationError = true;
            return false;
        }

        applyValue(parsedValue);
        return true;
    }

    public void applySliderValue(float nextValue) {
        applyValue(nextValue);
    }

    public void nudgeByWheel(int wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }
        applyValue(value + Integer.signum(wheelDelta));
    }

    public float getSliderFraction() {
        float range = maxValue - minValue;
        if (range <= 0.0001f) {
            return 0f;
        }
        return (value - minValue) / range;
    }

    public void syncFromModel(float nextValue) {
        this.value = clamp(nextValue);
        this.draftText = formatValue(this.value);
        this.validationError = false;
    }

    public void restoreDraftState(String draftText, boolean validationError) {
        this.draftText = draftText != null ? draftText : "";
        this.validationError = validationError;
    }

    private void applyValue(float nextValue) {
        value = clamp(nextValue);
        if (integerMode) {
            value = Math.round(value);
        }
        draftText = formatValue(value);
        validationError = false;
        valueApplier.accept(value);
    }

    private float clamp(float nextValue) {
        if (nextValue < minValue) {
            return minValue;
        }
        if (nextValue > maxValue) {
            return maxValue;
        }
        return nextValue;
    }

    private String formatValue(float nextValue) {
        if (integerMode || Math.abs(nextValue - Math.round(nextValue)) < 0.0001f) {
            return Integer.toString(Math.round(nextValue));
        }
        return Float.toString(nextValue);
    }
}
