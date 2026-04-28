package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.UUID;

public class SceneEditorElementModel {

    private final UUID id;
    private final SceneEditorElementType type;
    private float primaryX;
    private float primaryY;
    private float primaryZ;
    private float secondaryX;
    private float secondaryY;
    private float secondaryZ;
    private String colorLiteral;
    private float thickness;
    private boolean visible;
    private boolean alwaysOnTop;
    private String tooltipMarkdown;

    public SceneEditorElementModel(SceneEditorElementType type) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.primaryX = 0f;
        this.primaryY = 0f;
        this.primaryZ = 0f;
        this.secondaryX = 0f;
        this.secondaryY = 0f;
        this.secondaryZ = 0f;
        this.colorLiteral = type == SceneEditorElementType.DIAMOND ? "#FF00E000" : "#FFFFFFFF";
        this.thickness = 1f;
        this.visible = true;
        this.alwaysOnTop = false;
        this.tooltipMarkdown = "";
    }

    public UUID getId() {
        return id;
    }

    public SceneEditorElementType getType() {
        return type;
    }

    public float getPrimaryX() {
        return primaryX;
    }

    public void setPrimaryX(float primaryX) {
        this.primaryX = primaryX;
    }

    public float getPrimaryY() {
        return primaryY;
    }

    public void setPrimaryY(float primaryY) {
        this.primaryY = primaryY;
    }

    public float getPrimaryZ() {
        return primaryZ;
    }

    public void setPrimaryZ(float primaryZ) {
        this.primaryZ = primaryZ;
    }

    public float getSecondaryX() {
        return secondaryX;
    }

    public void setSecondaryX(float secondaryX) {
        this.secondaryX = secondaryX;
    }

    public float getSecondaryY() {
        return secondaryY;
    }

    public void setSecondaryY(float secondaryY) {
        this.secondaryY = secondaryY;
    }

    public float getSecondaryZ() {
        return secondaryZ;
    }

    public void setSecondaryZ(float secondaryZ) {
        this.secondaryZ = secondaryZ;
    }

    public String getColorLiteral() {
        return colorLiteral;
    }

    public void setColorLiteral(String colorLiteral) {
        this.colorLiteral = colorLiteral;
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    public String getTooltipMarkdown() {
        return tooltipMarkdown;
    }

    public void setTooltipMarkdown(String tooltipMarkdown) {
        this.tooltipMarkdown = tooltipMarkdown;
    }

    public SceneEditorElementModel duplicate() {
        SceneEditorElementModel duplicate = new SceneEditorElementModel(this.type);
        duplicate.setPrimaryX(this.primaryX);
        duplicate.setPrimaryY(this.primaryY);
        duplicate.setPrimaryZ(this.primaryZ);
        duplicate.setSecondaryX(this.secondaryX);
        duplicate.setSecondaryY(this.secondaryY);
        duplicate.setSecondaryZ(this.secondaryZ);
        duplicate.setColorLiteral(this.colorLiteral);
        duplicate.setThickness(this.thickness);
        duplicate.setVisible(this.visible);
        duplicate.setAlwaysOnTop(this.alwaysOnTop);
        duplicate.setTooltipMarkdown(this.tooltipMarkdown);
        return duplicate;
    }
}
