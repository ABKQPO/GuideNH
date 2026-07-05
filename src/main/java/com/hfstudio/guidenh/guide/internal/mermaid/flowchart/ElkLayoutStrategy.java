package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

public class ElkLayoutStrategy implements FlowchartLayoutStrategy {

    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int PADDING = 20;

    private static volatile boolean warmedUp;

    public static void warmup() {
        if (warmedUp) return;
        warmedUp = true;
        try {
            ElkNode root = ElkGraphUtil.createGraph();
            root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
            root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
            ElkNode a = ElkGraphUtil.createNode(root);
            a.setWidth(80);
            a.setHeight(30);
            ElkNode b = ElkGraphUtil.createNode(root);
            b.setWidth(80);
            b.setHeight(30);
            ElkGraphUtil.createSimpleEdge(a, b);
            RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
            engine.layout(root, new NullElkProgressMonitor());
        } catch (Exception ignored) {}
    }

    @Override
    public String getName() {
        return "elk";
    }

    @Override
    public FlowchartLayoutResult layout(FlowchartDocument document,
        Map<String, FlowchartLayoutResult.NodeMinSize> nodeMinSizes) {
        Map<String, FlowchartNode> nodes = document.getNodes();
        if (nodes.isEmpty()) {
            return new FlowchartLayoutResult(Map.of(), List.of(), 0, 0);
        }

        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.SPACING_NODE_NODE, 20.0);
        root.setProperty(CoreOptions.SPACING_EDGE_NODE, 20.0);
        root.setProperty(CoreOptions.PADDING, new org.eclipse.elk.core.math.ElkPadding(20));

        switch (document.getDirection()) {
            case LR -> root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
            case RL -> root.setProperty(CoreOptions.DIRECTION, Direction.LEFT);
            case BT -> root.setProperty(CoreOptions.DIRECTION, Direction.UP);
            default -> root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
        }

        Map<String, ElkNode> elkNodeMap = new LinkedHashMap<>();
        for (FlowchartNode node : nodes.values()) {
            ElkNode elkNode = ElkGraphUtil.createNode(root);
            FlowchartLayoutResult.NodeMinSize minSize = nodeMinSizes != null ? nodeMinSizes.get(node.getId()) : null;
            elkNode.setWidth(minSize != null ? Math.max(NODE_WIDTH, minSize.width()) : NODE_WIDTH);
            elkNode.setHeight(minSize != null ? Math.max(NODE_HEIGHT, minSize.height()) : NODE_HEIGHT);
            elkNodeMap.put(node.getId(), elkNode);
        }

        for (FlowchartEdge edge : document.getEdges()) {
            ElkNode source = elkNodeMap.get(edge.getFrom());
            ElkNode target = elkNodeMap.get(edge.getTo());
            if (source == null || target == null) continue;
            ElkGraphUtil.createSimpleEdge(source, target);
        }

        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        Map<String, FlowchartLayoutResult.NodePosition> positions = new LinkedHashMap<>();
        int maxX = 0;
        int maxY = 0;
        for (FlowchartNode node : nodes.values()) {
            ElkNode elkNode = elkNodeMap.get(node.getId());
            if (elkNode == null) continue;
            int x = PADDING + (int) Math.round(elkNode.getX());
            int y = PADDING + (int) Math.round(elkNode.getY());
            int w = Math.max(1, (int) Math.round(elkNode.getWidth()));
            int h = Math.max(1, (int) Math.round(elkNode.getHeight()));
            positions.put(node.getId(), new FlowchartLayoutResult.NodePosition(x, y, w, h));
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
        }

        List<FlowchartLayoutResult.EdgePath> edgePaths = new ArrayList<>();
        for (FlowchartEdge edge : document.getEdges()) {
            ElkNode source = elkNodeMap.get(edge.getFrom());
            ElkNode target = elkNodeMap.get(edge.getTo());
            if (source == null || target == null) continue;

            ElkEdge elkEdge = findElkEdge(source, target);
            if (elkEdge == null) continue;

            List<FlowchartLayoutResult.Point> points = new ArrayList<>();
            for (ElkEdgeSection section : elkEdge.getSections()) {
                points.add(
                    new FlowchartLayoutResult.Point(
                        PADDING + (int) Math.round(section.getStartX()),
                        PADDING + (int) Math.round(section.getStartY())));
                for (var bp : section.getBendPoints()) {
                    points.add(
                        new FlowchartLayoutResult.Point(
                            PADDING + (int) Math.round(bp.getX()),
                            PADDING + (int) Math.round(bp.getY())));
                }
                points.add(
                    new FlowchartLayoutResult.Point(
                        PADDING + (int) Math.round(section.getEndX()),
                        PADDING + (int) Math.round(section.getEndY())));
            }
            if (!points.isEmpty()) {
                edgePaths.add(new FlowchartLayoutResult.EdgePath(edge.getFrom(), edge.getTo(), points));
            }
        }

        return new FlowchartLayoutResult(positions, edgePaths, maxX + PADDING, maxY + PADDING);
    }

    private static ElkEdge findElkEdge(ElkNode source, ElkNode target) {
        for (ElkEdge edge : source.getOutgoingEdges()) {
            if (!edge.getTargets()
                .isEmpty()
                && ElkGraphUtil.connectableShapeToNode(
                    edge.getTargets()
                        .get(0))
                    == target) {
                return edge;
            }
        }
        return null;
    }
}
