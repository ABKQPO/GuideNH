package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class FlowchartDocument {

    @Getter
    private final FlowchartDirection direction;
    @Getter
    private final Map<String, FlowchartNode> nodes;
    @Getter
    private final List<FlowchartEdge> edges;
    @Getter
    private final List<FlowchartSubgraph> subgraphs;
    @Getter
    private final List<String> nodeOrder;
    @Getter
    private final FlowchartLayoutMode layoutMode;
    @Getter
    private final FlowchartConfig config;

    public record FlowchartConfig(int nodeSpacing, int rankSpacing, int canvasPadding) {

        public static final FlowchartConfig DEFAULT = new FlowchartConfig(20, 20, 20);
    }

    public FlowchartDocument(FlowchartDirection direction, Map<String, FlowchartNode> nodes, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> subgraphs) {
        this(direction, nodes, edges, subgraphs, null, null);
    }

    public FlowchartDocument(FlowchartDirection direction, Map<String, FlowchartNode> nodes, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> subgraphs, @Nullable FlowchartLayoutMode layoutMode, @Nullable FlowchartConfig config) {
        this.direction = direction != null ? direction : FlowchartDirection.TB;
        Map<String, FlowchartNode> src = nodes != null ? nodes : Map.of();
        this.nodes = Map.copyOf(new LinkedHashMap<>(src));
        this.edges = List.copyOf(new ArrayList<>(edges != null ? edges : List.of()));
        this.subgraphs = List.copyOf(new ArrayList<>(subgraphs != null ? subgraphs : List.of()));
        this.layoutMode = layoutMode != null ? layoutMode : FlowchartLayoutMode.BUILTIN;
        this.config = config != null ? config : FlowchartConfig.DEFAULT;
        List<String> order = new ArrayList<>(src.keySet());
        this.nodeOrder = Collections.unmodifiableList(order);
    }
}
