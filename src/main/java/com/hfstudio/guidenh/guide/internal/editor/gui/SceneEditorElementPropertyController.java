package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

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
        if (element == null) {
            return false;
        }
        element.setTooltipMarkdown(tooltipMarkdown != null ? tooltipMarkdown : "");
        syncText();
        return true;
    }

    public boolean setThickness(UUID elementId, float thickness) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || !supportsThickness(element.getType())
            || Float.isNaN(thickness)
            || Float.isInfinite(thickness)) {
            return false;
        }
        element.setThickness(thickness);
        syncText();
        return true;
    }

    public boolean setPrimaryVector(UUID elementId, float x, float y, float z) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null || hasInvalidNumber(x, y, z)) {
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
        if (element == null || hasInvalidNumber(x, y, z) || !supportsSecondaryVector(element.getType())) {
            return false;
        }
        element.setSecondaryX(x);
        element.setSecondaryY(y);
        element.setSecondaryZ(z);
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

    private boolean supportsSecondaryVector(SceneEditorElementType type) {
        return type == SceneEditorElementType.BOX || type == SceneEditorElementType.LINE;
    }

    private boolean supportsThickness(SceneEditorElementType type) {
        return type != SceneEditorElementType.DIAMOND;
    }

    @Nullable
    private SceneEditorElementModel requireElement(UUID elementId) {
        return session.getSceneModel()
            .getElement(elementId)
            .orElse(null);
    }
}
