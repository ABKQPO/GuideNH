package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.util.ElkGraphUtil;

public class ElkLayoutStrategy implements FlowchartLayoutStrategy {

    private static final int NODE_WIDTH = 80;
    private static final int NODE_HEIGHT = 30;

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

    // ---- Compound graph layout with SEPARATE_CHILDREN + boundary ports ----
    //
    // Subgraphs with different directions become ElkNode compounds with
    // SEPARATE_CHILDREN (so each gets its own layout run with its own DIRECTION).
    // Cross-hierarchy edges are split at the compound boundary: internal segments
    // (source→compound port) and external segments (compound port→target in parent).
    // ElkEdge references are stored so we can extract and stitch the sections
    // after layout without fragile key matching.

    @Override
    public FlowchartLayoutResult layout(FlowchartDocument document,
        Map<String, FlowchartLayoutResult.NodeMinSize> nodeMinSizes) {
        Map<String, FlowchartNode> nodes = document.getNodes();
        if (nodes.isEmpty()) {
            return new FlowchartLayoutResult(Map.of(), List.of(), 0, 0);
        }

        Map<String, FlowchartSubgraph> nodeToSubgraph = buildNodeToSubgraphMap(document.getSubgraphs());
        Map<String, FlowchartSubgraph> subgraphById = buildSubgraphByIdMap(document.getSubgraphs());
        Map<String, String> parentSgMap = buildParentSubgraphMap(document.getSubgraphs());
        var cfg = document.getConfig();

        Set<String> compoundSgIds = buildCompoundSgIds(document, parentSgMap, subgraphById);

        // ---- Build ELK compound graph ----
        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, toElkDirection(document.getDirection()));
        root.setProperty(CoreOptions.SPACING_NODE_NODE, (double) cfg.nodeSpacing());
        root.setProperty(CoreOptions.SPACING_EDGE_NODE, (double) cfg.nodeSpacing());
        root.setProperty(CoreOptions.PADDING, new ElkPadding(cfg.canvasPadding()));

        // Create compound nodes
        Map<String, ElkNode> compoundMap = new LinkedHashMap<>();
        for (FlowchartSubgraph sg : document.getSubgraphs()) {
            createCompounds(sg, compoundSgIds, parentSgMap, root, compoundMap);
        }

        // Create leaf nodes inside their parents
        Map<String, ElkNode> elkNodeMap = new LinkedHashMap<>();
        for (FlowchartNode node : nodes.values()) {
            ElkNode parent = nodeParent(
                node.getId(),
                nodeToSubgraph,
                compoundSgIds,
                parentSgMap,
                root,
                compoundMap,
                subgraphById);
            ElkNode elkNode = ElkGraphUtil.createNode(parent);
            elkNode.setIdentifier(node.getId());
            FlowchartLayoutResult.NodeMinSize minSize = nodeMinSizes != null ? nodeMinSizes.get(node.getId()) : null;
            elkNode.setWidth(minSize != null ? Math.max(NODE_WIDTH, minSize.width()) : NODE_WIDTH);
            elkNode.setHeight(minSize != null ? Math.max(NODE_HEIGHT, minSize.height()) : NODE_HEIGHT);
            elkNodeMap.put(node.getId(), elkNode);
        }

        // Create edges — split cross-hierarchy at compound boundary with ports
        List<SplitInfo> splitInfos = new ArrayList<>();

        for (FlowchartEdge edge : document.getEdges()) {
            ElkNode source = elkNodeMap.get(edge.getFrom());
            ElkNode target = elkNodeMap.get(edge.getTo());
            if (source == null || target == null) continue;

            String srcCont = containerId(edge.getFrom(), nodeToSubgraph, compoundSgIds, parentSgMap, subgraphById);
            String tgtCont = containerId(edge.getTo(), nodeToSubgraph, compoundSgIds, parentSgMap, subgraphById);

            if (isSameContainer(srcCont, tgtCont)) {
                ElkGraphUtil.createSimpleEdge(source, target);
            } else {
                ElkPort srcPort = null;
                ElkPort tgtPort = null;
                ElkEdge srcInternalEdge = null;
                ElkEdge tgtInternalEdge = null;

                if (srcCont != null) {
                    ElkNode compound = compoundMap.get(srcCont);
                    srcPort = ElkGraphUtil.createPort(compound);
                    srcInternalEdge = ElkGraphUtil.createSimpleEdge(source, srcPort);
                }
                if (tgtCont != null) {
                    ElkNode compound = compoundMap.get(tgtCont);
                    tgtPort = ElkGraphUtil.createPort(compound);
                    tgtInternalEdge = ElkGraphUtil.createSimpleEdge(tgtPort, target);
                }

                ElkNode extSrc = srcPort != null ? srcPort.getParent() : source;
                ElkNode extTgt = tgtPort != null ? tgtPort.getParent() : target;

                ElkNode extContainer = findLowestCommonAncestor(extSrc, extTgt);
                ElkEdge externalEdge = ElkGraphUtil.createEdge(extContainer);
                if (srcPort != null) {
                    externalEdge.getSources()
                        .add(srcPort);
                } else {
                    ElkPort p = ElkGraphUtil.createPort(source);
                    externalEdge.getSources()
                        .add(p);
                }
                if (tgtPort != null) {
                    externalEdge.getTargets()
                        .add(tgtPort);
                } else {
                    ElkPort p = ElkGraphUtil.createPort(target);
                    externalEdge.getTargets()
                        .add(p);
                }

                splitInfos.add(
                    new SplitInfo(
                        edge.getFrom(),
                        edge.getTo(),
                        srcInternalEdge,
                        externalEdge,
                        tgtInternalEdge,
                        srcPort,
                        tgtPort));
            }
        }

        // ---- Run layout ----
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        // ---- Extract positions ----
        Map<String, FlowchartLayoutResult.NodePosition> positions = new LinkedHashMap<>();
        collectPositions(root, 0, 0, cfg.canvasPadding(), positions, elkNodeMap);

        // ---- Extract and stitch edge paths ----
        List<FlowchartLayoutResult.EdgePath> edgePaths = new ArrayList<>();

        // Stitch split edges from stored ElkEdge references
        for (SplitInfo si : splitInfos) {
            List<FlowchartLayoutResult.Point> merged = new ArrayList<>();

            if (si.srcInternalEdge != null) {
                List<FlowchartLayoutResult.Point> pts = edgePoints(si.srcInternalEdge, root, cfg.canvasPadding());
                if (pts.size() >= 2) {
                    merged.addAll(pts.subList(0, pts.size() - 1));
                }
            }

            if (si.externalEdge != null) {
                List<FlowchartLayoutResult.Point> pts = edgePoints(si.externalEdge, root, cfg.canvasPadding());
                if (!pts.isEmpty()) {
                    if (merged.isEmpty()) {
                        merged.addAll(pts);
                    } else {
                        int skip = pointsMatch(merged.get(merged.size() - 1), pts.get(0)) ? 1 : 0;
                        merged.addAll(pts.subList(skip, pts.size()));
                    }
                }
            }

            if (si.tgtInternalEdge != null) {
                List<FlowchartLayoutResult.Point> pts = edgePoints(si.tgtInternalEdge, root, cfg.canvasPadding());
                if (pts.size() >= 2) {
                    int skip = !merged.isEmpty() && pointsMatch(merged.get(merged.size() - 1), pts.get(0)) ? 1 : 0;
                    merged.addAll(pts.subList(skip, pts.size()));
                }
            }

            if (!merged.isEmpty()) {
                edgePaths.add(
                    new FlowchartLayoutResult.EdgePath(
                        extractNodeId(si.srcInternalEdge != null ? si.srcInternalEdge : si.externalEdge, true),
                        extractNodeId(si.tgtInternalEdge != null ? si.tgtInternalEdge : si.externalEdge, false),
                        merged));
            }
        }

        // Collect unsplit edges from the whole tree (skip hierarchical edges)
        collectRemainingEdges(root, 0, 0, cfg.canvasPadding(), edgePaths, splitInfos);

        int maxX = 0;
        int maxY = 0;
        for (FlowchartLayoutResult.NodePosition pos : positions.values()) {
            maxX = Math.max(maxX, pos.getX() + pos.getWidth());
            maxY = Math.max(maxY, pos.getY() + pos.getHeight());
        }
        return new FlowchartLayoutResult(positions, edgePaths, maxX + cfg.canvasPadding(), maxY + cfg.canvasPadding());
    }

    // ---- Edge point extraction ----

    private static List<FlowchartLayoutResult.Point> edgePoints(ElkEdge edge, ElkNode root, int padding) {
        ElkNode container = edge.getContainingNode();
        int[] off = nodeOffset(container, root);
        return edgePoints(edge, off[0], off[1], padding);
    }

    private static List<FlowchartLayoutResult.Point> edgePoints(ElkEdge edge, int offsetX, int offsetY, int padding) {
        List<FlowchartLayoutResult.Point> pts = new ArrayList<>();
        for (ElkEdgeSection section : edge.getSections()) {
            pts.add(
                new FlowchartLayoutResult.Point(
                    padding + offsetX + (int) Math.round(section.getStartX()),
                    padding + offsetY + (int) Math.round(section.getStartY())));
            for (var bp : section.getBendPoints()) {
                pts.add(
                    new FlowchartLayoutResult.Point(
                        padding + offsetX + (int) Math.round(bp.getX()),
                        padding + offsetY + (int) Math.round(bp.getY())));
            }
            pts.add(
                new FlowchartLayoutResult.Point(
                    padding + offsetX + (int) Math.round(section.getEndX()),
                    padding + offsetY + (int) Math.round(section.getEndY())));
        }
        return pts;
    }

    private static int[] nodeOffset(ElkNode node, ElkNode root) {
        int ox = 0, oy = 0;
        ElkNode cur = node;
        while (cur != null && cur != root) {
            ox += (int) Math.round(cur.getX());
            oy += (int) Math.round(cur.getY());
            cur = cur.getParent();
        }
        return new int[] { ox, oy };
    }

    private static boolean pointsMatch(FlowchartLayoutResult.Point a, FlowchartLayoutResult.Point b) {
        return a.getX() == b.getX() && a.getY() == b.getY();
    }

    private static String extractNodeId(ElkEdge edge, boolean source) {
        var shapes = source ? edge.getSources() : edge.getTargets();
        if (shapes.isEmpty()) return "";
        ElkNode n = ElkGraphUtil.connectableShapeToNode(shapes.get(0));
        return n != null && n.getIdentifier() != null ? n.getIdentifier() : "";
    }

    private static void collectRemainingEdges(ElkNode node, int offsetX, int offsetY, int padding,
        List<FlowchartLayoutResult.EdgePath> edgePaths, List<SplitInfo> splitInfos) {
        int absX = offsetX + (int) Math.round(node.getX());
        int absY = offsetY + (int) Math.round(node.getY());

        for (ElkEdge edge : node.getContainedEdges()) {
            if (edge.isHierarchical()) continue;
            if (edge.getSources()
                .isEmpty()
                || edge.getTargets()
                    .isEmpty())
                continue;

            // Skip edges we already handled via split stitching
            boolean isSplit = false;
            for (SplitInfo si : splitInfos) {
                if (edge == si.srcInternalEdge || edge == si.externalEdge || edge == si.tgtInternalEdge) {
                    isSplit = true;
                    break;
                }
            }
            if (isSplit) continue;

            String srcId = ElkGraphUtil.connectableShapeToNode(
                edge.getSources()
                    .get(0))
                .getIdentifier();
            String tgtId = ElkGraphUtil.connectableShapeToNode(
                edge.getTargets()
                    .get(0))
                .getIdentifier();
            if (srcId == null || tgtId == null) continue;
            if (srcId.startsWith("__sg_") || tgtId.startsWith("__sg_")) continue;

            List<FlowchartLayoutResult.Point> pts = edgePoints(edge, absX, absY, padding);
            if (!pts.isEmpty()) {
                edgePaths.add(new FlowchartLayoutResult.EdgePath(srcId, tgtId, pts));
            }
        }

        for (ElkNode child : node.getChildren()) {
            collectRemainingEdges(child, absX, absY, padding, edgePaths, splitInfos);
        }
    }

    // ---- Graph construction helpers ----

    private static ElkNode findLowestCommonAncestor(ElkNode a, ElkNode b) {
        Set<ElkNode> ancestors = new LinkedHashSet<>();
        ElkNode cur = a;
        while (cur != null) {
            ancestors.add(cur);
            cur = cur.getParent();
        }
        cur = b;
        while (cur != null) {
            if (ancestors.contains(cur)) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private static Map<String, String> buildParentSubgraphMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, String> result = new LinkedHashMap<>();
        buildParentSubgraphMapRec(subgraphs, null, result);
        return result;
    }

    private static void buildParentSubgraphMapRec(List<FlowchartSubgraph> subgraphs, String parentId,
        Map<String, String> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            result.put(sg.getId(), parentId);
            buildParentSubgraphMapRec(sg.getChildren(), sg.getId(), result);
        }
    }

    private static Map<String, FlowchartSubgraph> buildSubgraphByIdMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, FlowchartSubgraph> result = new LinkedHashMap<>();
        buildSubgraphByIdMapRec(subgraphs, result);
        return result;
    }

    private static void buildSubgraphByIdMapRec(List<FlowchartSubgraph> subgraphs,
        Map<String, FlowchartSubgraph> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            result.put(sg.getId(), sg);
            buildSubgraphByIdMapRec(sg.getChildren(), result);
        }
    }

    private static Set<String> buildCompoundSgIds(FlowchartDocument document, Map<String, String> parentSgMap,
        Map<String, FlowchartSubgraph> subgraphById) {
        Set<String> result = new LinkedHashSet<>();
        for (FlowchartSubgraph sg : document.getSubgraphs()) {
            collectCompoundSgIds(sg, parentSgMap, subgraphById, result, document.getDirection());
        }
        return result;
    }

    private static void collectCompoundSgIds(FlowchartSubgraph sg, Map<String, String> parentSgMap,
        Map<String, FlowchartSubgraph> subgraphById, Set<String> result, FlowchartDirection documentDirection) {
        FlowchartDirection parentDir = effectiveDirection(sg.getId(), parentSgMap, subgraphById, documentDirection);

        if (sg.getDirection() != null && sg.getDirection() != parentDir) {
            result.add(sg.getId());
        }

        FlowchartDirection sgDir = sg.getDirection() != null ? sg.getDirection() : parentDir;
        for (FlowchartSubgraph child : sg.getChildren()) {
            collectCompoundSgIds(child, parentSgMap, subgraphById, result, sgDir);
        }
    }

    private static FlowchartDirection effectiveDirection(String sgId, Map<String, String> parentSgMap,
        Map<String, FlowchartSubgraph> subgraphById, FlowchartDirection documentDirection) {
        String parentId = parentSgMap.get(sgId);
        while (parentId != null) {
            FlowchartSubgraph parent = subgraphById.get(parentId);
            if (parent != null && parent.getDirection() != null) {
                return parent.getDirection();
            }
            parentId = parentSgMap.get(parentId);
        }
        return documentDirection;
    }

    private static void createCompounds(FlowchartSubgraph sg, Set<String> compoundSgIds,
        Map<String, String> parentSgMap, ElkNode root, Map<String, ElkNode> compoundMap) {
        if (!compoundSgIds.contains(sg.getId())) {
            for (FlowchartSubgraph child : sg.getChildren()) {
                createCompounds(child, compoundSgIds, parentSgMap, root, compoundMap);
            }
            return;
        }

        ElkNode parent = root;
        String parentId = parentSgMap.get(sg.getId());
        while (parentId != null) {
            if (compoundMap.containsKey(parentId)) {
                parent = compoundMap.get(parentId);
                break;
            }
            parentId = parentSgMap.get(parentId);
        }

        ElkNode compound = ElkGraphUtil.createNode(parent);
        compound.setIdentifier("__sg_" + sg.getId());
        compound.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.SEPARATE_CHILDREN);
        compound.setProperty(CoreOptions.DIRECTION, toElkDirection(sg.getDirection()));
        compoundMap.put(sg.getId(), compound);

        for (FlowchartSubgraph child : sg.getChildren()) {
            createCompounds(child, compoundSgIds, parentSgMap, compound, compoundMap);
        }
    }

    private static ElkNode nodeParent(String nodeId, Map<String, FlowchartSubgraph> nodeToSubgraph,
        Set<String> compoundSgIds, Map<String, String> parentSgMap, ElkNode root, Map<String, ElkNode> compoundMap,
        Map<String, FlowchartSubgraph> subgraphById) {
        FlowchartSubgraph sg = nodeToSubgraph.get(nodeId);
        while (sg != null) {
            if (compoundSgIds.contains(sg.getId())) {
                ElkNode compound = compoundMap.get(sg.getId());
                if (compound != null) return compound;
            }
            String parentId = parentSgMap.get(sg.getId());
            sg = parentId != null ? subgraphById.get(parentId) : null;
        }
        return root;
    }

    private static String containerId(String nodeId, Map<String, FlowchartSubgraph> nodeToSubgraph,
        Set<String> compoundSgIds, Map<String, String> parentSgMap, Map<String, FlowchartSubgraph> subgraphById) {
        FlowchartSubgraph sg = nodeToSubgraph.get(nodeId);
        while (sg != null) {
            if (compoundSgIds.contains(sg.getId())) return sg.getId();
            String parentId = parentSgMap.get(sg.getId());
            sg = parentId != null ? subgraphById.get(parentId) : null;
        }
        return null;
    }

    private static boolean isSameContainer(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ---- Position extraction ----

    private static void collectPositions(ElkNode node, int offsetX, int offsetY, int padding,
        Map<String, FlowchartLayoutResult.NodePosition> positions, Map<String, ElkNode> elkNodeMap) {
        boolean isCompound = node.getIdentifier() != null && node.getIdentifier()
            .startsWith("__sg_");
        int absX = offsetX + (int) Math.round(node.getX());
        int absY = offsetY + (int) Math.round(node.getY());

        if (!isCompound && node.getIdentifier() != null && elkNodeMap.containsKey(node.getIdentifier())) {
            positions.put(
                node.getIdentifier(),
                new FlowchartLayoutResult.NodePosition(
                    padding + absX,
                    padding + absY,
                    Math.max(1, (int) Math.round(node.getWidth())),
                    Math.max(1, (int) Math.round(node.getHeight()))));
        }

        for (ElkNode child : node.getChildren()) {
            collectPositions(child, absX, absY, padding, positions, elkNodeMap);
        }
    }

    // ---- Misc helpers ----

    private static Map<String, FlowchartSubgraph> buildNodeToSubgraphMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, FlowchartSubgraph> result = new LinkedHashMap<>();
        buildNodeToSubgraphMapRec(subgraphs, result);
        return result;
    }

    private static void buildNodeToSubgraphMapRec(List<FlowchartSubgraph> subgraphs,
        Map<String, FlowchartSubgraph> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            buildNodeToSubgraphMapRec(sg.getChildren(), result);
            for (String nodeId : sg.getNodeIds()) {
                result.putIfAbsent(nodeId, sg);
            }
        }
    }

    private static Direction toElkDirection(FlowchartDirection dir) {
        if (dir == null) return Direction.DOWN;
        return switch (dir) {
            case LR -> Direction.RIGHT;
            case RL -> Direction.LEFT;
            case BT -> Direction.UP;
            default -> Direction.DOWN;
        };
    }

    // ---- Data records ----

    private record SplitInfo(String sourceId, String targetId, ElkEdge srcInternalEdge, ElkEdge externalEdge,
        ElkEdge tgtInternalEdge, ElkPort srcPort, ElkPort tgtPort) {}
}
