package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.shapes.FlowchartShapes;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartEdge;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.EdgePath;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodeMinSize;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodePosition;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartSubgraph;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytMermaidFlowchartCanvas extends LytMermaidCanvas<LytMermaidFlowchartCanvas> {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 120;
    private static final int MAX_HEIGHT = 320;
    private static final int CONNECTOR_THICKNESS = 2;
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int ICON_GAP_Y = 4;
    private static final int BADGE_PADDING_X = 4;
    private static final int BADGE_PADDING_Y = 2;
    private static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    private static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);
    private static final ConstantColor NODE_TEXT = new ConstantColor(0xFFD7DEE7);
    private static final ConstantColor ROOT_TEXT_COLOR = new ConstantColor(0xFFF1F6FB);
    private static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(0xFFB8C2CF);
    private static final ConstantColor EDGE_COLOR = new ConstantColor(0xFF5D6C7C);
    private static final ConstantColor[] SUBGRAPH_BG = { new ConstantColor(0x301E2A45), new ConstantColor(0x302A1E45),
        new ConstantColor(0x301E2A2A), new ConstantColor(0x302A2A1E), };
    private static final ConstantColor[] SUBGRAPH_BORDER = { new ConstantColor(0x99434C57),
        new ConstantColor(0x994C5743), new ConstantColor(0x99575743), new ConstantColor(0x9943574C), };
    private static final int SUBGRAPH_PADDING = 8;

    private static final ResolvedTextStyle NODE_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        NODE_TEXT,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);
    private static final ResolvedTextStyle ROOT_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        ROOT_TEXT_COLOR,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);
    private static final ResolvedTextStyle ICON_TEXT_STYLE = new ResolvedTextStyle(
        0.85f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        ICON_TEXT_COLOR,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);

    private final FlowchartDocument document;
    private final Map<String, LytBlock> nodeContentBlocks;
    private Map<String, NodeContentLayout> nodeContentLayouts = Map.of();
    private FlowchartLayoutResult layout;
    private int preferredWidth;
    private int preferredHeight;
    private int lastPickDocX;
    private int lastPickDocY;
    private boolean lastPickValid;
    @Nullable
    private LytParagraph lastFlowHoverParagraph;
    @Nullable
    private LytFlowContent lastFlowHoverContent;

    public LytMermaidFlowchartCanvas(FlowchartDocument document, Map<String, LytBlock> nodeContentBlocks) {
        this.document = document;
        this.nodeContentBlocks = nodeContentBlocks == null ? Collections.emptyMap()
            : new LinkedHashMap<>(nodeContentBlocks);
        for (LytBlock block : this.nodeContentBlocks.values()) {
            block.parent = this;
        }
    }

    @Override
    public int canvasPadding() {
        return CANVAS_PADDING;
    }

    @Override
    public int contentWidth() {
        return layout != null ? layout.getWidth() : 0;
    }

    @Override
    public int contentHeight() {
        return layout != null ? layout.getHeight() : 0;
    }

    @Override
    public int contentOriginX() {
        return 0;
    }

    @Override
    public int contentOriginY() {
        return 0;
    }

    @Override
    protected boolean diagramReady() {
        return layout != null;
    }

    @Override
    protected void renderPanel(RenderContext context) {
        context.fillRect(bounds, context.resolveColor(PANEL_BACKGROUND));
        context.drawBorder(bounds, context.resolveColor(PANEL_BORDER), 1);
    }

    @Override
    protected void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom) {
        renderSubgraphs(context, baseX, baseY, activeZoom);
        renderEdges(context, baseX, baseY, activeZoom);
        renderNodes(context, baseX, baseY, activeZoom);
    }

    @Override
    protected void onPreRender() {
        refreshFlowHover();
    }

    public void setPreferredSize(int width, int height) {
        preferredWidth = Math.max(0, width);
        preferredHeight = Math.max(0, height);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int previousContentOffsetX = getRawOffsetX();
        int previousContentOffsetY = getRawOffsetY();
        int previousViewportWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int previousViewportHeight = Math.max(1, bounds.height() - CANVAS_PADDING * 2);
        int previousContentWidth = layout != null ? layout.getWidth() : 0;
        int previousContentHeight = layout != null ? layout.getHeight() : 0;

        int safeWidth = preferredWidth > 0 ? Math.clamp(preferredWidth, 1, availableWidth)
            : Math.max(1, availableWidth);

        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(document.getLayoutMode());
        var minSizes = computeNodeMinSizes(context);
        layout = strategy.layout(document, minSizes);

        Map<String, NodeContentLayout> layouts = new LinkedHashMap<>();
        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            LytBlock block = nodeContentBlocks.get(nodeId);
            if (block != null) {
                LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
                int contentWidth = Math.clamp(layout.getWidth() / 3, 96, 240);
                block.layout(localContext, 0, 0, contentWidth);
                LytRect visualBounds = resolveBlockVisualBounds(block);
                layouts.put(nodeId, new NodeContentLayout(block, visualBounds));
            }
        }
        nodeContentLayouts = layouts;

        int desiredHeight = (layout != null ? layout.getHeight() : 0) + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.clamp(desiredHeight, MIN_HEIGHT, MAX_HEIGHT);
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);
        int innerViewportHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);

        restoreViewportAfterLayout(
            previousContentOffsetX,
            previousContentOffsetY,
            previousViewportWidth,
            previousViewportHeight,
            previousContentWidth,
            previousContentHeight,
            viewportWidth,
            innerViewportHeight);

        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    private Map<String, NodeMinSize> computeNodeMinSizes(LayoutContext context) {
        int innerWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int maxTextWidth = Math.clamp(innerWidth / 3, 72, 180);
        String rootNodeId = document.getNodeOrder()
            .isEmpty() ? null
                : document.getNodeOrder()
                    .get(0);

        Map<String, NodeMinSize> result = new LinkedHashMap<>();
        for (var entry : document.getNodes()
            .entrySet()) {
            String nodeId = entry.getKey();
            FlowchartNode node = entry.getValue();
            boolean isRoot = nodeId.equals(rootNodeId);

            LytBlock block = nodeContentBlocks.get(nodeId);
            int textWidth;
            int textHeight;

            if (block != null) {
                LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
                int contentWidth = Math.clamp(maxTextWidth + 60, 96, 240);
                block.layout(localContext, 0, 0, contentWidth);
                LytRect vb = resolveBlockVisualBounds(block);
                textWidth = vb.width();
                textHeight = vb.height();
            } else {
                ResolvedTextStyle style = isRoot ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
                String label = node.getLabel();
                List<String> lines = MermaidNodeRenderer
                    .wrapText(context, style, label != null ? label : "", maxTextWidth);
                if (lines.isEmpty()) lines = List.of(" ");
                textWidth = 0;
                for (String line : lines) {
                    textWidth = Math.max(textWidth, MermaidNodeRenderer.measureText(context, style, line));
                }
                int lineHeight = context.getLineHeight(style);
                textHeight = Math.max(1, lines.size()) * lineHeight;
            }

            int width = textWidth + NODE_PADDING_X * 2;
            int height = textHeight + NODE_PADDING_Y * 2;

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = MermaidNodeRenderer.measureText(context, ICON_TEXT_STYLE, badgeText)
                        + BADGE_PADDING_X * 2;
                    int badgeHeight = context.getLineHeight(ICON_TEXT_STYLE) + BADGE_PADDING_Y * 2;
                    width = Math.max(width, badgeWidth + NODE_PADDING_X * 2);
                    height += badgeHeight + ICON_GAP_Y;
                }
            }

            switch (node.getShape()) {
                case ROUNDED -> width += 8;
                case SUBPROCESS -> width += 16;
                case CIRCLE, DOUBLE_CIRCLE -> {
                    width += 12;
                    height += 8;
                    width = Math.max(width, height + 14);
                }
                case HEXAGON -> width += 14;
                case CLOUD -> width += 16;
                case BANG -> width += 10;
                default -> {}
            }

            if (isRoot) {
                width += 10;
                height += 4;
            }

            result.put(nodeId, new NodeMinSize(width, height));
        }
        return result;
    }

    private void restoreViewportAfterLayout(int previousOffsetX, int previousOffsetY, int previousViewportWidth,
        int previousViewportHeight, int previousContentWidth, int previousContentHeight, int viewportWidth,
        int viewportHeight) {
        if (previousContentWidth <= 0 || previousContentHeight <= 0) {
            centerDiagram(viewportWidth, viewportHeight, layout.getWidth(), layout.getHeight());
            return;
        }
        float curZoom = getRawZoom();
        float anchorX = (previousViewportWidth * 0.5f - previousOffsetX) / Math.max(curZoom, 0.0001f);
        float anchorY = (previousViewportHeight * 0.5f - previousOffsetY) / Math.max(curZoom, 0.0001f);
        setContentOffset(
            Math.round(viewportWidth * 0.5f - anchorX * curZoom),
            Math.round(viewportHeight * 0.5f - anchorY * curZoom));
        clampOffsets();
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public List<? extends LytNode> getChildren() {
        return new ArrayList<>(nodeContentBlocks.values());
    }

    private void renderEdges(RenderContext context, int baseX, int baseY, float activeZoom) {
        int defaultColor = context.resolveColor(EDGE_COLOR);
        for (EdgePath edgePath : layout.getEdgePaths()) {
            FlowchartEdge flowEdge = lookupEdge(edgePath.getFromId(), edgePath.getToId());
            MermaidEdgeStyle style = flowEdge != null ? flowEdge.getStyle() : MermaidEdgeStyle.SOLID;
            boolean arrowFwd = flowEdge == null || flowEdge.isArrowFwd();
            boolean arrowRev = flowEdge != null && flowEdge.isArrowRev();
            MermaidArrowHead fwdHead = flowEdge != null ? flowEdge.getForwardHead() : MermaidArrowHead.TRIANGLE;
            MermaidArrowHead revHead = flowEdge != null ? flowEdge.getReverseHead() : MermaidArrowHead.NONE;
            String label = flowEdge != null ? flowEdge.getLabel() : null;

            if (style == MermaidEdgeStyle.INVISIBLE) continue;

            int edgeColor = defaultColor;
            int edgeThickness = style == MermaidEdgeStyle.THICK ? CONNECTOR_THICKNESS * 2 : CONNECTOR_THICKNESS;
            if (flowEdge != null) {
                String edgeStyles = flowEdge.getStyleOverride();
                if (edgeStyles != null) {
                    String stroke = getStyleProperty(edgeStyles, "stroke");
                    if (stroke != null) {
                        int parsed = parseHexColor(stroke);
                        if (parsed != 0) edgeColor = parsed;
                    }
                    String width = getStyleProperty(edgeStyles, "stroke-width");
                    if (width != null) {
                        try {
                            edgeThickness = Math.max(1, Integer.parseInt(width.replace("px", "").trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            List<FlowchartLayoutResult.Point> points = edgePath.getPoints();
            if (points.size() < 2) continue;

            for (int i = 1; i < points.size(); i++) {
                FlowchartLayoutResult.Point from = points.get(i - 1);
                FlowchartLayoutResult.Point to = points.get(i);
                float x1 = scaled(baseX, from.getX(), activeZoom);
                float y1 = scaled(baseY, from.getY(), activeZoom);
                float x2 = scaled(baseX, to.getX(), activeZoom);
                float y2 = scaled(baseY, to.getY(), activeZoom);

                if (style == MermaidEdgeStyle.DASHED || style == MermaidEdgeStyle.DOTTED) {
                    drawDashedLine(context, x1, y1, x2, y2, edgeThickness, edgeColor, style == MermaidEdgeStyle.DOTTED);
                } else {
                    context.drawLine(x1, y1, x2, y2, edgeThickness, edgeColor);
                }
            }

            if (arrowFwd || arrowRev) {
                FlowchartLayoutResult.Point last = points.getLast();
                FlowchartLayoutResult.Point prev = points.size() >= 2 ? points.get(points.size() - 2) : last;
                float tipX = scaled(baseX, last.getX(), activeZoom);
                float tipY = scaled(baseY, last.getY(), activeZoom);
                float dirX = tipX - scaled(baseX, prev.getX(), activeZoom);
                float dirY = tipY - scaled(baseY, prev.getY(), activeZoom);
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len > 0.5f) {
                    dirX /= len;
                    dirY /= len;
                    if (arrowFwd) {
                        drawArrowHeadVariant(context, tipX, tipY, dirX, dirY, activeZoom, edgeColor, fwdHead);
                    }
                }

                if (arrowRev) {
                    FlowchartLayoutResult.Point first = points.get(0);
                    FlowchartLayoutResult.Point second = points.size() >= 2 ? points.get(1) : first;
                    float tailX = scaled(baseX, first.getX(), activeZoom);
                    float tailY = scaled(baseY, first.getY(), activeZoom);
                    float revDirX = tailX - scaled(baseX, second.getX(), activeZoom);
                    float revDirY = tailY - scaled(baseY, second.getY(), activeZoom);
                    float revLen = (float) Math.sqrt(revDirX * revDirX + revDirY * revDirY);
                    if (revLen > 0.5f) {
                        revDirX /= revLen;
                        revDirY /= revLen;
                        drawArrowHeadVariant(
                            context,
                            tailX,
                            tailY,
                            revDirX,
                            revDirY,
                            activeZoom,
                            edgeColor,
                            revHead);
                    }
                }
            }

            if (label != null && !label.isEmpty()) {
                drawEdgeLabel(context, points, baseX, baseY, activeZoom, label);
            }
        }
    }

    private @Nullable FlowchartEdge lookupEdge(String fromId, String toId) {
        for (FlowchartEdge e : document.getEdges()) {
            if (e.getFrom()
                .equals(fromId)
                && e.getTo()
                    .equals(toId))
                return e;
        }
        return null;
    }

    private void drawDashedLine(RenderContext context, float x1, float y1, float x2, float y2, int thickness, int color,
        boolean dotted) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        float nx = dx / len;
        float ny = dy / len;
        float dashLen = dotted ? Math.max(1f, thickness * 0.5f) : Math.max(2f, thickness * 4f);
        float gapLen = dotted ? Math.max(2f, thickness * 1.5f) : Math.max(1f, thickness);
        float drawn = 0f;
        boolean draw = true;
        while (drawn < len) {
            float segEnd = Math.min(drawn + dashLen, len);
            float sx = x1 + nx * drawn;
            float sy = y1 + ny * drawn;
            float ex = x1 + nx * segEnd;
            float ey = y1 + ny * segEnd;
            if (draw) {
                context.drawLine(sx, sy, ex, ey, thickness, color);
            }
            drawn = segEnd + gapLen;
            draw = !draw;
        }
    }

    private void drawArrowHeadVariant(RenderContext context, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color, MermaidArrowHead headType) {
        switch (headType) {
            case CIRCLE -> drawCircleHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
            case CROSS -> drawCrossHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
            default -> drawTriangleHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
        }
    }

    private void drawTriangleHead(RenderContext context, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color) {
        float size = Math.max(4f, 8f * activeZoom);
        float perpX = -dirY;
        float baseX = tipX - dirX * size;
        float baseY = tipY - dirY * size;
        float leftX = baseX + perpX * size * 0.4f;
        float leftY = baseY + dirX * size * 0.4f;
        float rightX = baseX - perpX * size * 0.4f;
        float rightY = baseY - dirX * size * 0.4f;
        context.fillTriangle(tipX, tipY, leftX, leftY, rightX, rightY, color);
    }

    private void drawCircleHead(RenderContext context, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float radius = Math.max(3f, 5f * activeZoom);
        float cx = tipX - dirX * radius;
        float cy = tipY - dirY * radius;
        context.fillCircle(cx, cy, radius, color);
    }

    private void drawCrossHead(RenderContext context, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float size = Math.max(3f, 5f * activeZoom);
        float perpX = -dirY;
        float cx = tipX - dirX * size * 0.5f;
        float cy = tipY - dirY * size * 0.5f;
        float thickness = Math.max(1f, 1.5f * activeZoom);
        context.drawLine(
            cx + perpX * size * 0.7f,
            cy + dirX * size * 0.7f,
            cx - perpX * size * 0.7f,
            cy - dirX * size * 0.7f,
            thickness,
            color);
        context.drawLine(
            cx + perpX * size * 0.7f,
            cy - dirX * size * 0.7f,
            cx - perpX * size * 0.7f,
            cy + dirX * size * 0.7f,
            thickness,
            color);
    }

    private void drawEdgeLabel(RenderContext context, List<FlowchartLayoutResult.Point> points, int baseX, int baseY,
        float activeZoom, String label) {
        float totalLen = 0f;
        float[] segLens = new float[points.size() - 1];
        for (int i = 1; i < points.size(); i++) {
            float dx = points.get(i)
                .getX()
                - points.get(i - 1)
                    .getX();
            float dy = points.get(i)
                .getY()
                - points.get(i - 1)
                    .getY();
            segLens[i - 1] = (float) Math.sqrt(dx * dx + dy * dy);
            totalLen += segLens[i - 1];
        }
        if (totalLen < 1f) return;
        float halfLen = totalLen * 0.5f;
        float accumulated = 0f;
        float mx = points.getFirst()
            .getX();
        float my = points.getFirst()
            .getY();
        for (int i = 0; i < segLens.length; i++) {
            if (accumulated + segLens[i] >= halfLen) {
                float frac = (halfLen - accumulated) / Math.max(segLens[i], 0.0001f);
                mx = points.get(i)
                    .getX()
                    + (points.get(i + 1)
                        .getX()
                        - points.get(i)
                            .getX())
                        * frac;
                my = points.get(i)
                    .getY()
                    + (points.get(i + 1)
                        .getY()
                        - points.get(i)
                            .getY())
                        * frac;
                break;
            }
            accumulated += segLens[i];
        }
        int screenX = Math.round(scaled(baseX, Math.round(mx), activeZoom));
        int screenY = Math.round(scaled(baseY, Math.round(my), activeZoom));
        ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
        int textWidth = context.getStringWidth(label, labelStyle);
        int textHeight = context.getLineHeight(labelStyle);
        int pad = Math.max(1, Math.round(2 * activeZoom));
        int bgColor = context.resolveColor(new ConstantColor(0xCC0C1117));
        LytRect bg = new LytRect(
            screenX - textWidth / 2 - pad,
            screenY - textHeight / 2 - pad,
            textWidth + pad * 2,
            textHeight + pad * 2);
        context.fillRect(bg, bgColor);
        context.drawText(label, screenX - textWidth / 2, screenY - textHeight / 2, labelStyle);
    }

    private void renderNodes(RenderContext context, int baseX, int baseY, float activeZoom) {
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
        String rootNodeId = document.getNodeOrder()
            .isEmpty() ? null
                : document.getNodeOrder()
                    .get(0);

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            NodePosition pos = entry.getValue();
            FlowchartNode node = document.getNodes()
                .get(nodeId);
            if (node == null) continue;

            boolean isRoot = nodeId.equals(rootNodeId);
            int nodePaddingX = isRoot ? Math.max(1, Math.round(NODE_PADDING_X * 1.5f * activeZoom)) : paddingX;
            int nodePaddingY = isRoot ? Math.max(1, Math.round(NODE_PADDING_Y * 1.5f * activeZoom)) : paddingY;
            ResolvedTextStyle style = getOrScaleStyle(isRoot ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE, activeZoom);

            int sx = scaled(baseX, pos.getX(), activeZoom);
            int sy = scaled(baseY, pos.getY(), activeZoom);
            int sw = Math.max(1, Math.round(pos.getWidth() * activeZoom));
            int sh = Math.max(1, Math.round(pos.getHeight() * activeZoom));
            LytRect rect = new LytRect(sx, sy, sw, sh);

            var colors = MermaidNodeRenderer.resolveNodeColors(node.getClasses(), node.getShape(), isRoot);
            String nodeStyles = node.getStyleOverride();
            if (nodeStyles != null) {
                String fill = getStyleProperty(nodeStyles, "fill");
                String stroke = getStyleProperty(nodeStyles, "stroke");
                if (fill != null) {
                    int fillColor = parseHexColor(fill);
                    if (fillColor != 0) colors = new MermaidNodeRenderer.NodeColors(fillColor, colors.border(), colors.accent());
                }
                if (stroke != null) {
                    int strokeColor = parseHexColor(stroke);
                    if (strokeColor != 0) colors = new MermaidNodeRenderer.NodeColors(colors.background(), strokeColor, colors.accent());
                }
            }
            FlowchartShapes.render(context, rect, node.getShape(), colors.background(), colors.border());
            if (colors.accent() != MermaidNodeRenderer.DEFAULT_ACCENT) {
                MermaidNodeRenderer.renderAccentBar(context, rect, colors.accent());
            }

            int textY = rect.y() + nodePaddingY;

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = Math.max(
                        1,
                        context.getStringWidth(badgeText, badgeStyle)
                            + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)) * 2);
                    int badgeHeight = Math.max(
                        1,
                        context.getLineHeight(badgeStyle) + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)) * 2);
                    int badgeX = rect.x() + nodePaddingX;
                    LytRect badge = new LytRect(badgeX, textY, badgeWidth, badgeHeight);
                    context.fillRect(badge, MermaidNodeRenderer.BADGE_BACKGROUND);
                    context.drawBorder(badge, MermaidNodeRenderer.BADGE_BORDER, 1);
                    context.drawText(
                        badgeText,
                        badge.x() + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)),
                        badge.y() + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)),
                        badgeStyle);
                    textY += badgeHeight + Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
                }
            }

            NodeContentLayout contentLayout = nodeContentLayouts.get(nodeId);
            if (contentLayout != null) {
                renderNodeContent(context, contentLayout, rect, nodePaddingX, textY, activeZoom);
            } else {
                String label = node.getLabel();
                if (label == null || label.isEmpty()) continue;

                int maxTextWidth = Math.max(1, rect.width() - nodePaddingX * 2);
                List<String> lines = MermaidNodeRenderer.wrapText(context, style, label, maxTextWidth);
                int lineHeight = context.getLineHeight(style);
                for (int i = 0; i < lines.size(); i++) {
                    int lineWidth = context.getStringWidth(lines.get(i), style);
                    int textX = rect.x() + Math.max(nodePaddingX, (rect.width() - lineWidth) / 2);
                    context.drawText(lines.get(i), textX, textY + i * lineHeight, style);
                }
            }
        }
    }

    private void renderNodeContent(RenderContext context, NodeContentLayout contentLayout, LytRect rect, int paddingX,
        int contentY, float activeZoom) {
        LytRect innerViewport = getInnerViewport();
        LytRect contentViewport = resolveNodeContentRect(contentLayout, rect, paddingX, contentY, activeZoom);
        LytRect clip = intersect(innerViewport, contentViewport);
        if (clip == null) return;

        context.pushLocalScissor(clip);
        try {
            int originX = contentViewport.x() - Math.round(
                contentLayout.visualBounds()
                    .x() * activeZoom);
            int originY = contentViewport.y() - Math.round(
                contentLayout.visualBounds()
                    .y() * activeZoom);
            NodeContentRenderContext nodeContext = new NodeContentRenderContext(
                context,
                clip,
                originX,
                originY,
                activeZoom);
            renderNodeContentBlock(contentLayout.block(), nodeContext);
        } finally {
            context.popScissor();
        }
    }

    private void renderNodeContentBlock(LytBlock block, NodeContentRenderContext nodeContext) {
        if (block instanceof LytNode container && !container.getChildren()
            .isEmpty()) {
            for (var child : new ArrayList<>(container.getChildren())) {
                if (child instanceof LytBlock childBlock) {
                    renderNodeContentBlock(childBlock, nodeContext);
                }
            }
            renderContainerDecoration(container, nodeContext);
        } else if (usesRawGl(block)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(nodeContext.getDocumentOriginX(), nodeContext.getDocumentOriginY(), 0f);
            GL11.glScalef(nodeContext.getScale(), nodeContext.getScale(), 1f);
            try {
                block.render(nodeContext);
            } finally {
                GL11.glPopMatrix();
            }
        } else {
            block.render(nodeContext);
        }
    }

    private void refreshFlowHover() {
        if (!lastPickValid || layout == null) return;
        NodeHit hit = pickNodeHit(lastPickDocX, lastPickDocY);
        LytFlowContent hoveredFlow = null;
        LytParagraph hoveredParagraph = null;
        if (hit != null) {
            for (var content : hit.flowPath()
                .targets()) {
                if (content instanceof InteractiveElement) {
                    hoveredFlow = content;
                    break;
                }
            }
            if (hoveredFlow != null) {
                for (LytNode node = hit.node(); node != null; node = node.getParent()) {
                    if (node instanceof LytParagraph p) {
                        hoveredParagraph = p;
                        break;
                    }
                }
            }
        }
        if (hoveredParagraph != lastFlowHoverParagraph || hoveredFlow != lastFlowHoverContent) {
            if (lastFlowHoverParagraph != null) lastFlowHoverParagraph.onMouseLeave();
            if (hoveredParagraph != null) hoveredParagraph.onMouseEnter(hoveredFlow);
            lastFlowHoverParagraph = hoveredParagraph;
            lastFlowHoverContent = hoveredFlow;
        }
    }

    @Nullable
    private NodeHit pickNodeHit(int documentX, int documentY) {
        if (layout == null) return null;
        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            NodeContentLayout contentLayout = nodeContentLayouts.get(nodeId);
            if (contentLayout == null) continue;

            NodePosition pos = entry.getValue();
            int sx = scaled(baseX, pos.getX(), activeZoom);
            int sy = scaled(baseY, pos.getY(), activeZoom);
            int sw = Math.max(1, Math.round(pos.getWidth() * activeZoom));
            int sh = Math.max(1, Math.round(pos.getHeight() * activeZoom));
            LytRect nodeRect = new LytRect(sx, sy, sw, sh);

            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int contentY = nodeRect.y() + Math.max(1, Math.round(NODE_PADDING_Y * activeZoom))
                + resolveNodeBadgeHeight(entry.getKey(), activeZoom);
            LytRect contentScreenRect = resolveNodeContentRect(contentLayout, nodeRect, paddingX, contentY, activeZoom);

            if (!contentScreenRect.contains(documentX, documentY)) continue;

            int localX = unscaleCoordinate(documentX - contentScreenRect.x(), activeZoom);
            int localY = unscaleCoordinate(documentY - contentScreenRect.y(), activeZoom);
            DocumentInteractionSnapshot hit = LytDocument.pick(contentLayout.block(), localX, localY);
            if (hit != null) {
                return new NodeHit(hit.node(), hit.flowPath(), localX, localY);
            }
        }
        return null;
    }

    private int resolveNodeBadgeHeight(String nodeId, float activeZoom) {
        FlowchartNode node = document.getNodes()
            .get(nodeId);
        if (node == null || node.getIcon() == null) return 0;
        String badgeText = MermaidNodeRenderer.simplifyIcon(node.getIcon());
        if (badgeText == null) return 0;
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        return contextLineHeight(badgeStyle) + badgePaddingY * 2 + iconGapY;
    }

    private int unscaleCoordinate(int coordinate, float activeZoom) {
        return Math.max(0, Math.round(coordinate / Math.max(activeZoom, 0.0001f)));
    }

    private int contextLineHeight(ResolvedTextStyle style) {
        return Math.max(1, Math.round((9 + 1) * style.fontScale()));
    }

    private LytRect resolveNodeContentRect(NodeContentLayout contentLayout, LytRect nodeRect, int paddingX,
        int contentY, float activeZoom) {
        return new LytRect(
            nodeRect.x() + paddingX,
            contentY,
            Math.max(
                1,
                Math.round(
                    contentLayout.visualBounds()
                        .width() * activeZoom)),
            Math.max(
                1,
                Math.round(
                    contentLayout.visualBounds()
                        .height() * activeZoom)));
    }

    @Override
    public LytNode pickNode(int x, int y) {
        if (!getBounds().contains(x, y)) return null;
        lastPickDocX = x;
        lastPickDocY = y;
        lastPickValid = true;
        NodeHit hit = pickNodeHit(x, y);
        return hit != null ? hit.node() : this;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (layout == null || !getInnerViewport().contains(x, y)) return false;
        NodeHit hit = pickNodeHit(x, y);
        if (hit == null) return false;
        boolean handled = false;
        for (var content : hit.flowPath()
            .targets()) {
            if (content instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
                if (handled) return true;
            }
        }
        for (LytNode current = hit.node(); current != null && current != this
            && !handled; current = current.getParent()) {
            if (current instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
            }
        }
        return handled;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (layout == null || !getInnerViewport().contains((int) x, (int) y)) return Optional.empty();
        NodeHit hit = pickNodeHit((int) x, (int) y);
        if (hit == null) return Optional.empty();
        for (var content : hit.flowPath()
            .targets()) {
            if (content instanceof InteractiveElement interactiveElement) {
                Optional<GuideTooltip> tooltip = interactiveElement.getTooltip(hit.localX(), hit.localY());
                if (tooltip.isPresent()) return tooltip;
            }
        }
        for (LytNode current = hit.node(); current != null && current != this; current = current.getParent()) {
            if (current instanceof InteractiveElement interactiveElement) {
                Optional<GuideTooltip> tooltip = interactiveElement.getTooltip(hit.localX(), hit.localY());
                if (tooltip.isPresent()) return tooltip;
            }
        }
        return Optional.empty();
    }

    // ---- Subgraph rendering ----

    private void renderSubgraphs(RenderContext context, int baseX, int baseY, float activeZoom) {
        if (layout == null) return;
        for (var subgraph : document.getSubgraphs()) {
            renderSubgraphRecursive(context, subgraph, layout.getNodePositions(), baseX, baseY, activeZoom, 0);
        }
    }

    private void renderSubgraphRecursive(RenderContext context, FlowchartSubgraph subgraph,
        Map<String, NodePosition> positions, int baseX, int baseY, float activeZoom, int depth) {
        LytRect bounds = computeSubgraphBounds(subgraph, positions);
        if (bounds == null) return;

        int pad = Math.round(SUBGRAPH_PADDING * activeZoom);
        int sx = scaled(baseX, bounds.x() - pad, activeZoom);
        int sy = scaled(baseY, bounds.y() - pad, activeZoom);
        int sw = Math.max(1, Math.round((bounds.width() + pad * 2) * activeZoom));
        int sh = Math.max(1, Math.round((bounds.height() + pad * 2) * activeZoom));
        LytRect sgRect = new LytRect(sx, sy, sw, sh);

        int bg = context.resolveColor(SUBGRAPH_BG[depth % SUBGRAPH_BG.length]);
        int border = context.resolveColor(SUBGRAPH_BORDER[depth % SUBGRAPH_BORDER.length]);
        context.fillRect(sgRect, bg);
        context.drawBorder(sgRect, border, Math.max(1, Math.round(1.5f * activeZoom)));

        String label = subgraph.getLabel();
        if (label != null && !label.isEmpty()) {
            int labelPadX = Math.max(2, Math.round(4 * activeZoom));
            int labelPadY = Math.max(1, Math.round(2 * activeZoom));
            ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
            LytRect labelRect = new LytRect(
                sgRect.x() + labelPadX,
                sgRect.y() + labelPadY,
                context.getStringWidth(label, labelStyle),
                context.getLineHeight(labelStyle));
            context.drawText(label, labelRect.x(), labelRect.y(), labelStyle);
        }

        for (var child : subgraph.getChildren()) {
            renderSubgraphRecursive(context, child, positions, baseX, baseY, activeZoom, depth + 1);
        }
    }

    @Nullable
    private static LytRect computeSubgraphBounds(FlowchartSubgraph subgraph, Map<String, NodePosition> positions) {
        LytRect result = null;
        for (String nodeId : subgraph.getNodeIds()) {
            NodePosition pos = positions.get(nodeId);
            if (pos != null) {
                LytRect nodeRect = new LytRect(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
                result = result != null ? LytRect.union(result, nodeRect) : nodeRect;
            }
        }
        for (var child : subgraph.getChildren()) {
            LytRect childBounds = computeSubgraphBounds(child, positions);
            if (childBounds != null) {
                result = result != null ? LytRect.union(result, childBounds) : childBounds;
            }
        }
        return result;
    }

    private static boolean usesRawGl(LytBlock block) {
        return block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock
            || block instanceof LytItemImage
            || block instanceof LytNeiRecipeBox;
    }

    private static void renderContainerDecoration(LytNode container, RenderContext context) {
        if (!(container instanceof LytBox box)) return;
        LytRect b = container.getBounds();
        if (box.getBackgroundColor() != null) {
            context.fillRect(b, box.getBackgroundColor());
        }
        int topW = box.getBorderTop()
            .width();
        int bottomW = box.getBorderBottom()
            .width();
        if (topW > 0) {
            context.fillRect(
                b.x(),
                b.y(),
                b.width(),
                topW,
                context.resolveColor(
                    box.getBorderTop()
                        .color()));
        }
        if (bottomW > 0) {
            context.fillRect(
                b.x(),
                b.bottom() - bottomW,
                b.width(),
                bottomW,
                context.resolveColor(
                    box.getBorderBottom()
                        .color()));
        }
    }

    private static LytRect resolveBlockVisualBounds(LytBlock block) {
        LytRect[] result = { LytRect.empty() };
        block.visit(new LytVisitor() {

            @Override
            public LytVisitor.Result beforeNode(LytNode node) {
                if (node instanceof LytBlock childBlock) {
                    result[0] = LytRect.union(result[0], resolveSelfVisualBounds(childBlock));
                }
                return LytVisitor.Result.CONTINUE;
            }
        });
        return result[0];
    }

    private static LytRect resolveSelfVisualBounds(LytBlock block) {
        LytRect bounds = block.getBounds();
        if (bounds == null) return LytRect.empty();
        if (block instanceof LytLatexBlock latexBlock) return latexBlock.getVisualBounds();
        if (block instanceof LytLatexDisplayBlock latexDisplayBlock) return latexDisplayBlock.getVisualBounds();
        return bounds;
    }

    @Nullable
    private static LytRect intersect(LytRect a, LytRect b) {
        int left = Math.max(a.x(), b.x());
        int top = Math.max(a.y(), b.y());
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (right <= left || bottom <= top) return null;
        return new LytRect(left, top, right - left, bottom - top);
    }


    @Nullable
    private static String getStyleProperty(@Nullable String styleOverride, String property) {
        if (styleOverride == null) return null;
        String last = null;
        for (String part : styleOverride.split(",")) {
            int colon = part.indexOf(':');
            if (colon > 0 && part.substring(0, colon).trim().equalsIgnoreCase(property)) {
                last = part.substring(colon + 1).trim();
            }
        }
        return last;
    }

    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            if (h.length() == 6) {
                return 0xFF000000 | Integer.parseInt(h, 16);
            } else if (h.length() == 8) {
                return (int) Long.parseLong(h, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    // ---- Inner classes ----

    public record NodeContentLayout(LytBlock block, LytRect visualBounds) {

        public NodeContentLayout(LytBlock block, LytRect visualBounds) {
            this.block = block;
            this.visualBounds = visualBounds != null && !visualBounds.isEmpty() ? visualBounds : LytRect.empty();
        }
    }

    public record NodeHit(LytNode node, FlowInteractionPath flowPath, int localX, int localY) {

        public NodeHit(LytNode node, @Nullable FlowInteractionPath flowPath, int localX, int localY) {
            this.node = node;
            this.flowPath = flowPath != null ? flowPath : FlowInteractionPath.empty();
            this.localX = localX;
            this.localY = localY;
        }
    }

    public static class NodeContentRenderContext implements RenderContext {

        private final RenderContext delegate;
        private final LytRect viewport;
        private final int originX;
        private final int originY;
        private final float scale;
        private final Map<ResolvedTextStyle, ResolvedTextStyle> scaledStyleCache = new IdentityHashMap<>();

        public NodeContentRenderContext(RenderContext delegate, LytRect viewport, int originX, int originY,
            float scale) {
            this.delegate = delegate;
            this.viewport = new LytRect(
                0,
                0,
                Math.max(1, Math.round(viewport.width() / scale)),
                Math.max(1, Math.round(viewport.height() / scale)));
            this.originX = originX;
            this.originY = originY;
            this.scale = Math.max(0.0001f, scale);
        }

        public float getScale() {
            return scale;
        }

        @Override
        public LightDarkMode lightDarkMode() {
            return delegate.lightDarkMode();
        }

        @Override
        public LytRect viewport() {
            return viewport;
        }

        @Override
        public int getDocumentOriginX() {
            return originX;
        }

        @Override
        public int getDocumentOriginY() {
            return originY;
        }

        @Override
        public LytRect toScreenRect(LytRect rect) {
            LytRect s = scaleRect(rect);
            return new LytRect(
                s.x() + delegate.getDocumentOriginX(),
                s.y() + delegate.getDocumentOriginY() - delegate.getScrollOffsetY(),
                s.width(),
                s.height());
        }

        @Override
        public int resolveColor(ColorValue ref) {
            return delegate.resolveColor(ref);
        }

        @Override
        public void fillRect(LytRect rect, int argbColor) {
            delegate.fillRect(scaleRect(rect), argbColor);
        }

        @Override
        public void fillRect(int x, int y, int width, int height, int argbColor) {
            delegate.fillRect(scaleX(x), scaleY(y), scaleLength(width), scaleLength(height), argbColor);
        }

        @Override
        public void drawBorder(LytRect rect, int argbColor, int thickness) {
            delegate.drawBorder(scaleRect(rect), argbColor, Math.max(1, scaleLength(thickness)));
        }

        @Override
        public void drawBorder(int x, int y, int width, int height, int argbColor, int thickness) {
            delegate.drawBorder(
                scaleX(x),
                scaleY(y),
                scaleLength(width),
                scaleLength(height),
                argbColor,
                Math.max(1, scaleLength(thickness)));
        }

        @Override
        public void drawText(String text, int x, int y, ResolvedTextStyle style) {
            delegate.drawText(text, scaleX(x), scaleY(y), scaleStyle(style));
        }

        @Override
        public int getStringWidth(String text, ResolvedTextStyle style) {
            return scaleLength(delegate.getStringWidth(text, style));
        }

        @Override
        public int getLineHeight(ResolvedTextStyle style) {
            return scaleLength(delegate.getLineHeight(style));
        }

        @Override
        public void renderItem(ItemStack stack, int x, int y) {
            renderScaledItem(stack, x, y, true);
        }

        @Override
        public void renderItemIcon(ItemStack stack, int x, int y) {
            renderScaledItem(stack, x, y, false);
        }

        private void renderScaledItem(ItemStack stack, int x, int y, boolean overlay) {
            int screenX = scaleX(x);
            int screenY = scaleY(y);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(screenX, screenY, 0f);
                GL11.glScalef(scale, scale, 1f);
                if (overlay) delegate.renderItem(stack, 0, 0);
                else delegate.renderItemIcon(stack, 0, 0);
            } finally {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void blitGuiSprite(LytRect rect, GuiSprite sprite) {
            if (sprite == null) return;
            int sx = scaleX(rect.x());
            int sy = scaleY(rect.y());
            GL11.glPushMatrix();
            GL11.glTranslatef(sx, sy, 0f);
            GL11.glScalef(scale, scale, 1f);
            try {
                delegate.blitTexture(
                    sprite.getTexture(),
                    0,
                    0,
                    sprite.getU(),
                    sprite.getV(),
                    sprite.getWidth(),
                    sprite.getHeight());
            } finally {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void fillIcon(LytRect rect, GuiSprite sprite, ColorValue color) {
            delegate.fillIcon(scaleRect(rect), sprite, color);
        }

        @Override
        public void blitTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
            delegate.blitTexture(texture, scaleX(x), scaleY(y), u, v, scaleLength(width), scaleLength(height));
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, float thickness, int argbColor) {
            delegate.drawLine(
                scaleFloatX(x1),
                scaleFloatY(y1),
                scaleFloatX(x2),
                scaleFloatY(y2),
                Math.max(1f, thickness * scale),
                argbColor);
        }

        @Override
        public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
            delegate.fillTriangle(
                scaleFloatX(x1),
                scaleFloatY(y1),
                scaleFloatX(x2),
                scaleFloatY(y2),
                scaleFloatX(x3),
                scaleFloatY(y3),
                argbColor);
        }

        @Override
        public void fillPolygon(float[] xs, float[] ys, int argbColor) {
            float[] scaledXs = new float[xs.length];
            float[] scaledYs = new float[ys.length];
            for (int i = 0; i < xs.length; i++) {
                scaledXs[i] = scaleFloatX(xs[i]);
                scaledYs[i] = scaleFloatY(ys[i]);
            }
            delegate.fillPolygon(scaledXs, scaledYs, argbColor);
        }

        @Override
        public void fillCircle(float cx, float cy, float radius, int argbColor) {
            delegate.fillCircle(scaleFloatX(cx), scaleFloatY(cy), radius * scale, argbColor);
        }

        @Override
        public void drawCircleOutline(float cx, float cy, float radius, float thickness, int argbColor) {
            delegate.drawCircleOutline(
                scaleFloatX(cx),
                scaleFloatY(cy),
                radius * scale,
                Math.max(1f, thickness * scale),
                argbColor);
        }

        @Override
        public void pushScissor(LytRect rect) {
            delegate.pushScissor(scaleRect(rect));
        }

        @Override
        public void pushLocalScissor(LytRect rect) {
            delegate.pushScissor(scaleRect(rect));
        }

        @Override
        public LytRect currentScissor() {
            return delegate.currentScissor();
        }

        @Override
        public void popScissor() {
            delegate.popScissor();
        }

        @Override
        public void restoreExternalRenderState() {
            delegate.restoreExternalRenderState();
        }

        @Override
        public void beginLocalView() {
            GL11.glPushMatrix();
            GL11.glTranslatef(originX, originY, 0f);
            GL11.glScalef(scale, scale, 1f);
        }

        @Override
        public void endLocalView() {
            GL11.glPopMatrix();
        }

        private ResolvedTextStyle scaleStyle(ResolvedTextStyle style) {
            return scaledStyleCache.computeIfAbsent(
                style,
                key -> new ResolvedTextStyle(
                    key.fontScale() * scale,
                    key.bold(),
                    key.italic(),
                    key.underlined(),
                    key.wavyUnderline(),
                    key.dottedUnderline(),
                    key.strikethrough(),
                    key.obfuscated(),
                    key.font(),
                    key.color(),
                    key.whiteSpace(),
                    key.alignment(),
                    key.dropShadow(),
                    key.backgroundColor(),
                    key.inlineCode()));
        }

        private LytRect scaleRect(LytRect rect) {
            return new LytRect(
                scaleX(rect.x()),
                scaleY(rect.y()),
                scaleLength(rect.width()),
                scaleLength(rect.height()));
        }

        private int scaleX(int x) {
            return originX + Math.round(x * scale);
        }

        private int scaleY(int y) {
            return originY + Math.round(y * scale);
        }

        private int scaleLength(int value) {
            return Math.max(1, Math.round(value * scale));
        }

        private float scaleFloatX(float x) {
            return originX + x * scale;
        }

        private float scaleFloatY(float y) {
            return originY + y * scale;
        }
    }
}
