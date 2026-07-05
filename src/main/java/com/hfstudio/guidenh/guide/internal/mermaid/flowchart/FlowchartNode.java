package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;

public class FlowchartNode {

    private final String id;
    private final String label;
    private final MermaidNodeShape shape;
    private final List<String> classes;
    @Nullable
    private final String styleOverride;
    @Nullable
    private final String icon;
    private final boolean markdownLabel;
    @Nullable
    private final Map<String, String> extendedProperties;

    public FlowchartNode(String id, String label, MermaidNodeShape shape, List<String> classes,
        @Nullable String styleOverride) {
        this(id, label, shape, classes, styleOverride, null, false, null);
    }

    public FlowchartNode(String id, String label, MermaidNodeShape shape, List<String> classes,
        @Nullable String styleOverride, @Nullable String icon, boolean markdownLabel,
        @Nullable Map<String, String> extendedProperties) {
        this.id = id != null ? id : "";
        this.label = label != null ? label : "";
        this.shape = shape != null ? shape : MermaidNodeShape.DEFAULT;
        this.classes = List.copyOf(new ArrayList<>(classes != null ? classes : List.of()));
        this.styleOverride = styleOverride;
        this.icon = icon;
        this.markdownLabel = markdownLabel;
        this.extendedProperties = extendedProperties != null ? Map.copyOf(new LinkedHashMap<>(extendedProperties))
            : null;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public MermaidNodeShape getShape() {
        return shape;
    }

    public List<String> getClasses() {
        return classes;
    }

    public @Nullable String getStyleOverride() {
        return styleOverride;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public boolean isMarkdownLabel() {
        return markdownLabel;
    }

    public @Nullable Map<String, String> getExtendedProperties() {
        return extendedProperties;
    }
}
