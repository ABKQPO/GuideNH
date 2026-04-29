package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class SceneEditorSceneNodeModel {

    private final SceneEditorSceneNodeType type;
    private final Map<String, String> attributes;
    private final List<SceneEditorElementModel> templateElements;
    @Nullable
    private SceneEditorElementModel annotationElement;

    public SceneEditorSceneNodeModel(SceneEditorSceneNodeType type) {
        this.type = type;
        this.attributes = new LinkedHashMap<>();
        this.templateElements = new ArrayList<>();
        this.annotationElement = null;
    }

    public SceneEditorSceneNodeType getType() {
        return type;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    @Nullable
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<SceneEditorElementModel> getTemplateElements() {
        return templateElements;
    }

    public void addTemplateElement(SceneEditorElementModel element) {
        templateElements.add(element);
    }

    @Nullable
    public SceneEditorElementModel getAnnotationElement() {
        return annotationElement;
    }

    public void setAnnotationElement(@Nullable SceneEditorElementModel annotationElement) {
        this.annotationElement = annotationElement;
    }

    public SceneEditorSceneNodeModel duplicate() {
        SceneEditorSceneNodeModel duplicate = new SceneEditorSceneNodeModel(type);
        duplicate.attributes.putAll(this.attributes);
        for (SceneEditorElementModel element : templateElements) {
            duplicate.templateElements.add(element.duplicate());
        }
        if (annotationElement != null) {
            duplicate.annotationElement = annotationElement.duplicate();
        }
        return duplicate;
    }
}
