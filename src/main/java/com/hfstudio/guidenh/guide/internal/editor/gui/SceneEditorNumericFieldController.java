package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public class SceneEditorNumericFieldController {

    private final boolean integerMode;
    private final float minValue;
    private final float maxValue;
    private final Consumer<Float> valueApplier;
    @Nullable
    private final Consumer<Float> nullableValueApplier;
    private final boolean allowEmptyDraft;

    private float value;
    private String draftText;
    private boolean validationError;

    private SceneEditorNumericFieldController(boolean integerMode, float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier, @Nullable Consumer<Float> nullableValueApplier, boolean allowEmptyDraft) {
        this.integerMode = integerMode;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.valueApplier = valueApplier;
        this.nullableValueApplier = nullableValueApplier;
        this.allowEmptyDraft = allowEmptyDraft;
        this.value = Float.isNaN(initialValue) ? clamp(minValue) : clamp(initialValue);
        this.draftText = Float.isNaN(initialValue) && allowEmptyDraft ? "" : formatValue(this.value);
        this.validationError = false;
    }

    public static SceneEditorNumericFieldController integer(float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier) {
        return new SceneEditorNumericFieldController(true, initialValue, minValue, maxValue, valueApplier, null, false);
    }

    public static SceneEditorNumericFieldController decimal(float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier) {
        return new SceneEditorNumericFieldController(
            false,
            initialValue,
            minValue,
            maxValue,
            valueApplier,
            null,
            false);
    }

    public static SceneEditorNumericFieldController optionalDecimal(float initialValue, float minValue, float maxValue,
        Consumer<Float> valueApplier, Consumer<Float> nullableValueApplier) {
        return new SceneEditorNumericFieldController(
            false,
            initialValue,
            minValue,
            maxValue,
            valueApplier,
            nullableValueApplier,
            true);
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
            if (!allowEmptyDraft || nullableValueApplier == null) {
                draftText = formatValue(value);
                validationError = false;
                valueApplier.accept(value);
                return true;
            }
            validationError = false;
            draftText = "";
            nullableValueApplier.accept(null);
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
        if (Float.isNaN(nextValue) && allowEmptyDraft) {
            this.value = Float.NaN;
            this.draftText = "";
        } else {
            this.value = clamp(nextValue);
            this.draftText = formatValue(this.value);
        }
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
        return Math.min(nextValue, maxValue);
    }

    private String formatValue(float nextValue) {
        if (integerMode || Math.abs(nextValue - Math.round(nextValue)) < 0.0001f) {
            return Integer.toString(Math.round(nextValue));
        }
        return Float.toString(nextValue);
    }
}
