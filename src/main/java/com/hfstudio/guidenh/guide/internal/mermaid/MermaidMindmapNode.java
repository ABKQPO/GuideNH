package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class MermaidMindmapNode {

    private final String id;
    private final String text;
    private final MermaidMindmapNodeShape shape;
    private final List<String> classes;
    @Nullable
    private final String icon;
    @Nullable
    private final Integer x;
    @Nullable
    private final Integer y;
    private final List<MermaidMindmapNode> children = new ArrayList<>();

    public MermaidMindmapNode(String id, String text, MermaidMindmapNodeShape shape, List<String> classes,
        @Nullable String icon, @Nullable Integer x, @Nullable Integer y) {
        this.id = id != null ? id : "";
        this.text = text != null ? text : "";
        this.shape = shape != null ? shape : MermaidMindmapNodeShape.DEFAULT;
        this.classes = Collections
            .unmodifiableList(new ArrayList<>(classes != null ? classes : Collections.emptyList()));
        this.icon = icon != null && !icon.trim()
            .isEmpty() ? icon.trim() : null;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public MermaidMindmapNodeShape getShape() {
        return shape;
    }

    public List<String> getClasses() {
        return classes;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public @Nullable Integer getX() {
        return x;
    }

    public @Nullable Integer getY() {
        return y;
    }

    public List<MermaidMindmapNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(MermaidMindmapNode child) {
        if (child != null) {
            children.add(child);
        }
    }
}
