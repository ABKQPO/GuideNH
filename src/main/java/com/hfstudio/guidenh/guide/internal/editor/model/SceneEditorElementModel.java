package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

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
    private String textKey;
    private String textMarkdown;
    private String showWhenStructure;
    private String showWhenTier;
    private String showWhenChannels;
    private int maxWidth;
    private int backgroundAlpha;
    private final List<Vector3f> linePoints;
    private final Map<String, String> extraAttributes;

    public SceneEditorElementModel(SceneEditorElementType type) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.primaryX = 0f;
        this.primaryY = 0f;
        this.primaryZ = 0f;
        this.secondaryX = 0f;
        this.secondaryY = 0f;
        this.secondaryZ = 0f;
        this.colorLiteral = type.getDefaultColorLiteral();
        this.thickness = type.getDefaultThickness();
        this.visible = true;
        this.alwaysOnTop = false;
        this.tooltipMarkdown = "";
        this.textKey = "";
        this.textMarkdown = type.getDefaultText();
        this.showWhenStructure = "";
        this.showWhenTier = "";
        this.showWhenChannels = "";
        this.maxWidth = type.getDefaultMaxWidth();
        this.backgroundAlpha = type.getDefaultBackgroundAlpha();
        this.linePoints = new ArrayList<>();
        this.extraAttributes = new LinkedHashMap<>();
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

    public List<Vector3f> getLinePoints() {
        return Collections.unmodifiableList(linePoints);
    }

    public void setLinePoints(List<Vector3f> points) {
        linePoints.clear();
        if (points == null) {
            return;
        }
        for (Vector3f point : points) {
            if (point != null) {
                linePoints.add(new Vector3f(point));
            }
        }
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

    public String getTextKey() {
        return textKey;
    }

    public void setTextKey(String textKey) {
        this.textKey = textKey != null ? textKey : "";
    }

    public String getTextMarkdown() {
        return textMarkdown;
    }

    public void setTextMarkdown(String textMarkdown) {
        this.textMarkdown = textMarkdown;
    }

    public String getShowWhenStructure() {
        return showWhenStructure;
    }

    public void setShowWhenStructure(String showWhenStructure) {
        this.showWhenStructure = showWhenStructure != null ? showWhenStructure : "";
    }

    public String getShowWhenTier() {
        return showWhenTier;
    }

    public void setShowWhenTier(String showWhenTier) {
        this.showWhenTier = showWhenTier != null ? showWhenTier : "";
    }

    public String getShowWhenChannels() {
        return showWhenChannels;
    }

    public void setShowWhenChannels(String showWhenChannels) {
        this.showWhenChannels = showWhenChannels != null ? showWhenChannels : "";
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setBackgroundAlpha(int backgroundAlpha) {
        this.backgroundAlpha = Math.max(0, Math.min(255, backgroundAlpha));
    }

    public Map<String, String> getExtraAttributes() {
        return Collections.unmodifiableMap(extraAttributes);
    }

    public String getExtraAttribute(String name) {
        return extraAttributes.get(name);
    }

    public void setExtraAttribute(String name, String value) {
        if (name == null || name.trim()
            .isEmpty()) {
            return;
        }
        if (value == null || value.trim()
            .isEmpty()) {
            extraAttributes.remove(name);
            return;
        }
        extraAttributes.put(name, value);
    }

    public void setExtraAttributes(Map<String, String> attributes) {
        extraAttributes.clear();
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            setExtraAttribute(entry.getKey(), entry.getValue());
        }
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
        duplicate.setTextKey(this.textKey);
        duplicate.setTextMarkdown(this.textMarkdown);
        duplicate.setShowWhenStructure(this.showWhenStructure);
        duplicate.setShowWhenTier(this.showWhenTier);
        duplicate.setShowWhenChannels(this.showWhenChannels);
        duplicate.setMaxWidth(this.maxWidth);
        duplicate.setBackgroundAlpha(this.backgroundAlpha);
        duplicate.setLinePoints(this.linePoints);
        duplicate.setExtraAttributes(this.extraAttributes);
        return duplicate;
    }
}
