package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public class SceneEditorElementPropertyController {

    public static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#(?i:[0-9a-f]{6}|[0-9a-f]{8})");

    private final SceneEditorSession session;
    private final SceneEditorMarkdownCodec codec;

    public SceneEditorElementPropertyController(SceneEditorSession session, SceneEditorMarkdownCodec codec) {
        this.session = session;
        this.codec = codec;
    }

    public boolean setColor(UUID elementId, String colorLiteral) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        String normalizedColor = normalizeColorLiteral(colorLiteral);
        if (normalizedColor == null) {
            return false;
        }
        element.setColorLiteral(normalizedColor);
        syncText();
        return true;
    }

    public boolean setAlwaysOnTop(UUID elementId, boolean alwaysOnTop) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        element.setAlwaysOnTop(alwaysOnTop);
        syncText();
        return true;
    }

    public boolean setTooltip(UUID elementId, @Nullable String tooltipMarkdown) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsTooltip()) {
            return false;
        }
        element.setTooltipMarkdown(tooltipMarkdown != null ? tooltipMarkdown : "");
        syncText();
        return true;
    }

    public boolean setThickness(UUID elementId, float thickness) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsThickness() || Float.isNaN(thickness) || Float.isInfinite(thickness)) {
            return false;
        }
        element.setThickness(thickness);
        syncText();
        return true;
    }

    public boolean setPrimaryVector(UUID elementId, float x, float y, float z) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsPrimaryVector() || hasInvalidNumber(x, y, z)) {
            return false;
        }
        element.setPrimaryX(x);
        element.setPrimaryY(y);
        element.setPrimaryZ(z);
        syncText();
        return true;
    }

    public boolean setSecondaryVector(UUID elementId, float x, float y, float z) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || hasInvalidNumber(x, y, z)
            || !element.getType()
                .supportsSecondaryVector()) {
            return false;
        }
        element.setSecondaryX(x);
        element.setSecondaryY(y);
        element.setSecondaryZ(z);
        if (element.getType() == SceneEditorElementType.LINE) {
            syncLineEndpointsIntoStoredPoints(element);
        }
        syncText();
        return true;
    }

    public boolean setLinePoints(UUID elementId, List<Vector3f> points) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || element.getType() != SceneEditorElementType.LINE
            || points == null
            || points.size() < 2) {
            return false;
        }
        for (Vector3f point : points) {
            if (point == null || hasInvalidNumber(point.x, point.y, point.z)) {
                return false;
            }
        }
        element.setLinePoints(points);
        Vector3f first = points.get(0);
        Vector3f last = points.get(points.size() - 1);
        element.setPrimaryX(first.x);
        element.setPrimaryY(first.y);
        element.setPrimaryZ(first.z);
        element.setSecondaryX(last.x);
        element.setSecondaryY(last.y);
        element.setSecondaryZ(last.z);
        syncText();
        return true;
    }

    public boolean setText(UUID elementId, @Nullable String textMarkdown) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsText()) {
            return false;
        }
        element.setTextMarkdown(textMarkdown != null ? textMarkdown : "");
        syncText();
        return true;
    }

    public boolean setMaxWidth(UUID elementId, int maxWidth) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsMaxWidth()) {
            return false;
        }
        element.setMaxWidth(Math.max(0, maxWidth));
        syncText();
        return true;
    }

    public boolean setBackgroundAlpha(UUID elementId, int backgroundAlpha) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !element.getType()
            .supportsBackgroundAlpha()) {
            return false;
        }
        element.setBackgroundAlpha(backgroundAlpha);
        syncText();
        return true;
    }

    @Nullable
    public static String normalizeColorLiteral(@Nullable String colorLiteral) {
        if (colorLiteral == null) {
            return null;
        }
        String normalized = colorLiteral.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("transparent".equalsIgnoreCase(normalized)) {
            return "transparent";
        }
        if (!HEX_COLOR_PATTERN.matcher(normalized)
            .matches()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    public String syncText() {
        SceneEditorSceneModel sceneModel = session.getSceneModel();
        String serialized = codec.serialize(sceneModel);
        session.setRawText(serialized);
        return serialized;
    }

    private boolean hasInvalidNumber(float... values) {
        for (float value : values) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private SceneEditorElementModel requireElement(UUID elementId) {
        return session.getSceneModel()
            .getElement(elementId)
            .orElse(null);
    }

    private void syncLineEndpointsIntoStoredPoints(SceneEditorElementModel element) {
        List<Vector3f> points = new ArrayList<>(element.getLinePoints());
        if (points.size() < 2) {
            points.clear();
            points.add(new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()));
            points.add(new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()));
        } else {
            points.set(0, new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()));
            points.set(
                points.size() - 1,
                new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()));
        }
        element.setLinePoints(points);
    }
}
