package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class FlowchartSubgraph {

    private final String id;
    private final String label;
    private final List<String> nodeIds;
    private final List<FlowchartEdge> edges;
    private final List<FlowchartSubgraph> children;
    @Nullable
    private final FlowchartDirection direction;

    public FlowchartSubgraph(String id, @Nullable String label, List<String> nodeIds, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> children) {
        this(id, label, nodeIds, edges, children, null);
    }

    public FlowchartSubgraph(String id, @Nullable String label, List<String> nodeIds, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> children, @Nullable FlowchartDirection direction) {
        this.id = id != null ? id : "";
        this.label = label != null ? label : this.id;
        this.nodeIds = List.copyOf(new ArrayList<>(nodeIds != null ? nodeIds : List.of()));
        this.edges = List.copyOf(new ArrayList<>(edges != null ? edges : List.of()));
        this.children = List.copyOf(new ArrayList<>(children != null ? children : List.of()));
        this.direction = direction;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }

    public List<FlowchartEdge> getEdges() {
        return edges;
    }

    public List<FlowchartSubgraph> getChildren() {
        return children;
    }

    public @Nullable FlowchartDirection getDirection() {
        return direction;
    }
}
