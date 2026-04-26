package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

public final class SceneEditorSceneModel {

    @Nullable
    private String structureSource;
    @Nullable
    private String perspectivePreset;
    private int previewWidth;
    private int previewHeight;
    private float rotationX;
    private float rotationY;
    private float rotationZ;
    private float offsetX;
    private float offsetY;
    private float zoom;
    private boolean interactive;
    private float centerX;
    private float centerY;
    private float centerZ;
    private final List<SceneEditorElementModel> elements;

    private SceneEditorSceneModel(@Nullable String structureSource) {
        this.structureSource = structureSource;
        this.perspectivePreset = null;
        this.previewWidth = 256;
        this.previewHeight = 192;
        this.rotationX = 35f;
        this.rotationY = 45f;
        this.rotationZ = 0f;
        this.offsetX = 0f;
        this.offsetY = 0f;
        this.zoom = 1f;
        this.interactive = true;
        this.centerX = 0f;
        this.centerY = 0f;
        this.centerZ = 0f;
        this.elements = new ArrayList<>();
    }

    public static SceneEditorSceneModel blank() {
        return new SceneEditorSceneModel(null);
    }

    public static SceneEditorSceneModel withStructureSource(String structureSource) {
        return new SceneEditorSceneModel(structureSource);
    }

    public SceneEditorSceneModel copy() {
        SceneEditorSceneModel copy = new SceneEditorSceneModel(this.structureSource);
        copy.setPerspectivePreset(this.perspectivePreset);
        copy.setPreviewWidth(this.previewWidth);
        copy.setPreviewHeight(this.previewHeight);
        copy.setRotationX(this.rotationX);
        copy.setRotationY(this.rotationY);
        copy.setRotationZ(this.rotationZ);
        copy.setOffsetX(this.offsetX);
        copy.setOffsetY(this.offsetY);
        copy.setZoom(this.zoom);
        copy.setInteractive(this.interactive);
        copy.setCenterX(this.centerX);
        copy.setCenterY(this.centerY);
        copy.setCenterZ(this.centerZ);
        for (SceneEditorElementModel element : this.elements) {
            copy.addElement(element.duplicate());
        }
        return copy;
    }

    @Nullable
    public String getStructureSource() {
        return structureSource;
    }

    public void setStructureSource(@Nullable String structureSource) {
        this.structureSource = structureSource;
    }

    @Nullable
    public String getPerspectivePreset() {
        return perspectivePreset;
    }

    public void setPerspectivePreset(@Nullable String perspectivePreset) {
        this.perspectivePreset = perspectivePreset;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
    }

    public float getRotationZ() {
        return rotationZ;
    }

    public void setRotationZ(float rotationZ) {
        this.rotationZ = rotationZ;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public float getCenterX() {
        return centerX;
    }

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public float getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(float centerZ) {
        this.centerZ = centerZ;
    }

    public List<SceneEditorElementModel> getElements() {
        return elements;
    }

    public void addElement(SceneEditorElementModel element) {
        elements.add(element);
    }

    public Optional<SceneEditorElementModel> getElement(UUID elementId) {
        for (SceneEditorElementModel element : elements) {
            if (element.getId()
                .equals(elementId)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    public boolean removeElement(UUID elementId) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i)
                .getId()
                .equals(elementId)) {
                elements.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean moveElement(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= elements.size()
            || toIndex < 0
            || toIndex >= elements.size()
            || fromIndex == toIndex) {
            return false;
        }
        SceneEditorElementModel element = elements.remove(fromIndex);
        elements.add(toIndex, element);
        return true;
    }
}
