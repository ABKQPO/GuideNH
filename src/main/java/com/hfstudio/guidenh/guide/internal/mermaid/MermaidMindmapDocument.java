package com.hfstudio.guidenh.guide.internal.mermaid;

public class MermaidMindmapDocument {

    private final MermaidMindmapLayoutMode layoutMode;
    private final MermaidMindmapNode root;

    public MermaidMindmapDocument(MermaidMindmapLayoutMode layoutMode, MermaidMindmapNode root) {
        this.layoutMode = layoutMode != null ? layoutMode : MermaidMindmapLayoutMode.MINDMAP;
        this.root = root;
    }

    public MermaidMindmapLayoutMode getLayoutMode() {
        return layoutMode;
    }

    public MermaidMindmapNode getRoot() {
        return root;
    }
}
