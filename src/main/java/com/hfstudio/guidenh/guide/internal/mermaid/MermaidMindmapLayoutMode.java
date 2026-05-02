package com.hfstudio.guidenh.guide.internal.mermaid;

public enum MermaidMindmapLayoutMode {

    MINDMAP,
    TIDY_TREE;

    public static MermaidMindmapLayoutMode fromConfigValue(String value) {
        if (value == null) {
            return MINDMAP;
        }
        return switch (value.trim()
            .toLowerCase()) {
            case "tidy-tree" -> TIDY_TREE;
            default -> MINDMAP;
        };
    }
}
