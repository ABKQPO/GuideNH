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
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapLayoutMode;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapNode;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytMermaidMindmapCanvas extends LytMermaidCanvas<LytMermaidMindmapCanvas> implements DebugComponent {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 170;
    private static final int MAX_HEIGHT = 320;
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int NODE_GAP_X = 32;
    private static final int NODE_GAP_Y = 14;
    private static final int ICON_GAP_Y = 4;
    private static final int CONNECTOR_THICKNESS = 1;
    private static final float ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;
    static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);
    static final ConstantColor ROOT_TEXT_COLOR = new ConstantColor(0xFFF1F6FB);
    static final ConstantColor NODE_TEXT_COLOR = new ConstantColor(0xFFD7DEE7);
    static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(0xFFB8C2CF);

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
        NODE_TEXT_COLOR,
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

    private final MindmapDocument mindmap;
    private final Map<String, LytBlock> nodeContentBlocks;

    private DiagramLayout layout;
    private int preferredWidth;
    private int preferredHeight;
    private int lastPickDocX;
    private int lastPickDocY;
    private boolean lastPickValid;
    @Nullable
    private LytParagraph lastFlowHoverParagraph;
    @Nullable
    private LytFlowContent lastFlowHoverContent;

    public LytMermaidMindmapCanvas(MindmapDocument mindmap, Map<String, LytBlock> nodeContentBlocks) {
        this.mindmap = mindmap;
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
        return layout != null ? layout.diagramWidth() : 0;
    }

    @Override
    public int contentHeight() {
        return layout != null ? layout.diagramHeight() : 0;
    }

    @Override
    public int contentOriginX() {
        return layout != null ? layout.contentBounds()
            .x() : 0;
    }

    @Override
    public int contentOriginY() {
        return layout != null ? layout.contentBounds()
            .y() : 0;
    }

    @Override
    public boolean diagramReady() {
        return layout != null;
    }

    public MindmapDocument getMindmap() {
        return mindmap;
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
        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(preferredWidth, availableWidth))
            : Math.max(1, availableWidth);
        layout = buildLayout(context, safeWidth);
        int desiredHeight = layout.diagramHeight() + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, desiredHeight));
        if (preferredHeight > 0 && safeWidth < resolvePreferredViewportWidth()) {
            viewportHeight = Math.max(viewportHeight, Math.min(MAX_HEIGHT, desiredHeight));
        }
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);
        int innerViewportHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);
        restoreViewportAfterLayout(
            layout,
            previousContentOffsetX,
            previousContentOffsetY,
            previousViewportWidth,
            previousViewportHeight,
            viewportWidth,
            innerViewportHeight);
        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    protected void onPreRender() {
        refreshFlowHover();
    }

    @Override
    protected void renderPanel(RenderContext context) {
        context.fillRect(bounds, PANEL_BACKGROUND);
        context.drawBorder(bounds, context.resolveColor(PANEL_BORDER), 1);
    }

    @Override
    protected void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom) {
        renderConnectors(context, layout.root(), baseX, baseY);
        renderNodes(context, layout.root(), baseX, baseY);
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
    public List<? extends LytNode> getChildren() {
        return new ArrayList<>(nodeContentBlocks.values());
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

    private DiagramLayout buildLayout(LayoutContext context, int availableWidth) {
        int innerWidth = Math.max(72, availableWidth - CANVAS_PADDING * 2);
        int maxNodeTextWidth = Math.clamp(innerWidth / 3, 72, 180);
        NodeLayout root = prepareLayout(context, mindmap.getRoot(), 0, maxNodeTextWidth);

        if (mindmap.getLayoutMode() == MindmapLayoutMode.TIDY_TREE) {
            measureTopDown(root);
            layoutTopDown(root, 0, 0);
            return buildDiagramLayout(root);
        }

        List<NodeLayout> leftChildren = new ArrayList<>();
        List<NodeLayout> rightChildren = new ArrayList<>();
        for (int i = 0; i < root.children.size(); i++) {
            if ((i & 1) == 0) {
                rightChildren.add(root.children.get(i));
            } else {
                leftChildren.add(root.children.get(i));
            }
        }

        int leftWidth = 0;
        int leftHeight = 0;
        for (NodeLayout child : leftChildren) {
            measureSideTree(child);
            leftWidth = Math.max(leftWidth, child.subtreeWidth);
            leftHeight += child.subtreeHeight;
        }
        if (leftChildren.size() > 1) {
            leftHeight += NODE_GAP_Y * (leftChildren.size() - 1);
        }

        int rightWidth = 0;
        int rightHeight = 0;
        for (NodeLayout child : rightChildren) {
            measureSideTree(child);
            rightWidth = Math.max(rightWidth, child.subtreeWidth);
            rightHeight += child.subtreeHeight;
        }
        if (rightChildren.size() > 1) {
            rightHeight += NODE_GAP_Y * (rightChildren.size() - 1);
        }

        int leftGap = leftWidth > 0 ? NODE_GAP_X : 0;
        int rightGap = rightWidth > 0 ? NODE_GAP_X : 0;
        int diagramWidth = leftWidth + leftGap + root.width + rightGap + rightWidth;
        int diagramHeight = Math.max(root.height, Math.max(leftHeight, rightHeight));
        int rootX = leftWidth + leftGap;
        int rootCenterY = diagramHeight / 2;
        root.x = rootX;
        root.y = rootCenterY - root.height / 2;

        int rightAnchorX = root.x + root.width + NODE_GAP_X;
        int rightCursorY = rootCenterY - rightHeight / 2;
        for (NodeLayout child : rightChildren) {
            int childCenterY = rightCursorY + child.subtreeHeight / 2;
            layoutSideTree(child, rightAnchorX, childCenterY, true);
            rightCursorY += child.subtreeHeight + NODE_GAP_Y;
        }

        int leftAnchorX = root.x - NODE_GAP_X;
        int leftCursorY = rootCenterY - leftHeight / 2;
        for (NodeLayout child : leftChildren) {
            int childCenterY = leftCursorY + child.subtreeHeight / 2;
            layoutSideTree(child, leftAnchorX, childCenterY, false);
            leftCursorY += child.subtreeHeight + NODE_GAP_Y;
        }

        return buildDiagramLayout(root);
    }

    private DiagramLayout buildDiagramLayout(NodeLayout root) {
        LytRect contentBounds = collectRenderedBounds(root);
        return new DiagramLayout(
            root,
            Math.max(1, contentBounds.width()),
            Math.max(1, contentBounds.height()),
            contentBounds,
            collectContentNodes(root, new ArrayList<>()));
    }

    private LytRect collectRenderedBounds(NodeLayout node) {
        LytRect bounds = resolveNodeVisualRect(node);
        for (NodeLayout child : node.children) {
            bounds = LytRect.union(bounds, collectRenderedBounds(child));
        }
        return bounds;
    }

    private List<NodeLayout> collectContentNodes(NodeLayout node, List<NodeLayout> result) {
        if (node.contentLayout != null) {
            result.add(node);
        }
        for (NodeLayout child : node.children) {
            collectContentNodes(child, result);
        }
        return result;
    }

    private NodeLayout prepareLayout(LayoutContext context, MindmapNode node, int depth, int maxNodeTextWidth) {
        String badgeText = simplifyIcon(node.getIcon());
        String primaryText = node.getText();
        boolean showBadge = badgeText != null && !badgeText.isEmpty()
            && primaryText != null
            && !primaryText.trim()
                .isEmpty()
            && !badgeText.equalsIgnoreCase(primaryText.trim());
        if ((primaryText == null || primaryText.trim()
            .isEmpty()) && badgeText != null) {
            primaryText = badgeText;
            showBadge = false;
            badgeText = null;
        }

        NodeContentLayout contentLayout = prepareNodeContentLayout(context, node, maxNodeTextWidth);
        List<String> lines = new ArrayList<>();
        int textWidth = 0;
        int textHeight = 0;
        if (contentLayout == null) {
            ResolvedTextStyle style = depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
            lines = wrapText(context, style, primaryText, maxNodeTextWidth);
            if (lines.isEmpty()) {
                lines.add(" ");
            }

            for (String line : lines) {
                textWidth = Math.max(textWidth, measureText(context, style, line));
            }
            int lineHeight = context.getLineHeight(style);
            textHeight = Math.max(1, lines.size()) * lineHeight;
        } else {
            textWidth = contentLayout.visualBounds()
                .width();
            textHeight = contentLayout.visualBounds()
                .height();
        }

        int badgeWidth = 0;
        int badgeHeight = 0;
        if (showBadge && badgeText != null) {
            badgeWidth = measureText(context, ICON_TEXT_STYLE, badgeText) + 8;
            badgeHeight = context.getLineHeight(ICON_TEXT_STYLE) + 4;
            textWidth = Math.max(textWidth, badgeWidth);
        }

        int width = textWidth + NODE_PADDING_X * 2;
        int height = textHeight + NODE_PADDING_Y * 2;
        if (badgeHeight > 0) {
            height += badgeHeight + ICON_GAP_Y;
        }
        switch (node.getShape()) {
            case ROUNDED -> width += 8;
            case CIRCLE -> {
                width += 12;
                height += 8;
                width = Math.max(width, height + 14);
            }
            case HEXAGON -> width += 14;
            case CLOUD -> width += 16;
            case BANG -> width += 10;
            default -> {}
        }
        if (depth == 0) {
            width += 10;
            height += 4;
        }

        NodeLayout layout = new NodeLayout(node, depth, lines, badgeText, showBadge, contentLayout, width, height);
        for (MindmapNode child : node.getChildren()) {
            layout.children.add(prepareLayout(context, child, depth + 1, maxNodeTextWidth));
        }
        return layout;
    }

    private @Nullable NodeContentLayout prepareNodeContentLayout(LayoutContext context, MindmapNode node,
        int maxNodeTextWidth) {
        LytBlock block = nodeContentBlocks.get(node.getId());
        if (block == null) {
            return null;
        }
        LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
        int contentWidth = Math.clamp(maxNodeTextWidth + 60, 96, 240);
        block.layout(localContext, 0, 0, contentWidth);
        LytRect visualBounds = resolveBlockVisualBounds(block);
        return new NodeContentLayout(block, visualBounds);
    }

    private void measureSideTree(NodeLayout node) {
        if (node.children.isEmpty()) {
            node.subtreeWidth = node.width;
            node.subtreeHeight = node.height;
            return;
        }

        int childrenHeight = 0;
        int childrenWidth = 0;
        for (NodeLayout child : node.children) {
            measureSideTree(child);
            childrenHeight += child.subtreeHeight;
            childrenWidth = Math.max(childrenWidth, child.subtreeWidth);
        }
        childrenHeight += NODE_GAP_Y * (node.children.size() - 1);
        node.subtreeWidth = node.width + NODE_GAP_X + childrenWidth;
        node.subtreeHeight = Math.max(node.height, childrenHeight);
    }

    private void layoutSideTree(NodeLayout node, int anchorX, int centerY, boolean rightSide) {
        if (node.node.getX() != null) {
            node.x = node.node.getX();
        } else {
            node.x = rightSide ? anchorX : anchorX - node.width;
        }
        if (node.node.getY() != null) {
            node.y = node.node.getY();
        } else {
            node.y = centerY - node.height / 2;
        }
        if (node.children.isEmpty()) {
            return;
        }

        int childrenHeight = 0;
        for (NodeLayout child : node.children) {
            childrenHeight += child.subtreeHeight;
        }
        childrenHeight += NODE_GAP_Y * (node.children.size() - 1);

        int cursorY = centerY - childrenHeight / 2;
        for (NodeLayout child : node.children) {
            int childCenterY = cursorY + child.subtreeHeight / 2;
            int childAnchorX = rightSide ? node.x + node.width + NODE_GAP_X : node.x - NODE_GAP_X;
            layoutSideTree(child, childAnchorX, childCenterY, rightSide);
            cursorY += child.subtreeHeight + NODE_GAP_Y;
        }
    }

    private void measureTopDown(NodeLayout node) {
        if (node.children.isEmpty()) {
            node.subtreeWidth = node.width;
            node.subtreeHeight = node.height;
            return;
        }

        int childrenWidth = 0;
        int childrenHeight = 0;
        for (NodeLayout child : node.children) {
            measureTopDown(child);
            childrenWidth += child.subtreeWidth;
            childrenHeight = Math.max(childrenHeight, child.subtreeHeight);
        }
        childrenWidth += NODE_GAP_X * (node.children.size() - 1);
        node.subtreeWidth = Math.max(node.width, childrenWidth);
        node.subtreeHeight = node.height + NODE_GAP_Y + childrenHeight;
    }

    private void layoutTopDown(NodeLayout node, int x, int y) {
        node.x = node.node.getX() != null ? node.node.getX() : x + (node.subtreeWidth - node.width) / 2;
        node.y = node.node.getY() != null ? node.node.getY() : y;
        if (node.children.isEmpty()) {
            return;
        }

        int childrenWidth = 0;
        for (NodeLayout child : node.children) {
            childrenWidth += child.subtreeWidth;
        }
        childrenWidth += NODE_GAP_X * (node.children.size() - 1);

        int cursorX = x + (node.subtreeWidth - childrenWidth) / 2;
        int childY = y + node.height + NODE_GAP_Y;
        for (NodeLayout child : node.children) {
            layoutTopDown(child, cursorX, childY);
            cursorX += child.subtreeWidth + NODE_GAP_X;
        }
    }

    private void renderConnectors(RenderContext context, NodeLayout node, int baseX, int baseY) {
        float activeZoom = getActiveZoom();
        for (NodeLayout child : node.children) {
            if (mindmap.getLayoutMode() == MindmapLayoutMode.TIDY_TREE) {
                drawVerticalConnector(
                    context,
                    scaled(baseX, node.centerX(), activeZoom),
                    scaled(baseY, node.bottom(), activeZoom),
                    scaled(baseX, child.centerX(), activeZoom),
                    scaled(baseY, child.y, activeZoom),
                    0xFF5D6C7C);
            } else {
                boolean rightSide = child.centerX() >= node.centerX();
                int parentEdgeX = scaled(baseX, rightSide ? node.right() : node.x, activeZoom);
                int childEdgeX = scaled(baseX, rightSide ? child.x : child.right(), activeZoom);
                drawHorizontalConnector(
                    context,
                    parentEdgeX,
                    scaled(baseY, node.centerY(), activeZoom),
                    childEdgeX,
                    scaled(baseY, child.centerY(), activeZoom),
                    0xFF5D6C7C);
            }
            renderConnectors(context, child, baseX, baseY);
        }
    }

    private void renderNodes(RenderContext context, NodeLayout node, int baseX, int baseY) {
        float activeZoom = getActiveZoom();
        LytRect rect = new LytRect(
            scaled(baseX, node.x, activeZoom),
            scaled(baseY, node.y, activeZoom),
            Math.max(1, Math.round(node.width * activeZoom)),
            Math.max(1, Math.round(node.height * activeZoom)));
        LytRect boxRect = rect;
        NodeColors colors = resolveColors(node.node);
        context.fillRect(boxRect, colors.background);
        context.drawBorder(boxRect, colors.border, node.node.getShape() == MermaidNodeShape.BANG ? 2 : 1);
        context.fillRect(new LytRect(boxRect.x(), boxRect.y(), 3, boxRect.height()), colors.accent);

        ResolvedTextStyle style = getOrScaleStyle(node.depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE, activeZoom);
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        int badgePaddingX = Math.max(2, Math.round(4 * activeZoom));
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int textY = rect.y() + paddingY;
        if (node.showBadge && node.badgeText != null) {
            int badgeWidth = Math.max(1, context.getStringWidth(node.badgeText, badgeStyle) + badgePaddingX * 2);
            LytRect badge = new LytRect(
                rect.x() + paddingX,
                textY,
                badgeWidth,
                Math.max(1, context.getLineHeight(badgeStyle) + badgePaddingY * 2));
            context.fillRect(badge, 0x262A3340);
            context.drawBorder(badge, 0x66434C57, 1);
            context.drawText(node.badgeText, badge.x() + badgePaddingX, badge.y() + badgePaddingY, badgeStyle);
            textY = badge.bottom() + iconGapY;
        }

        if (node.contentLayout != null) {
            renderNodeContent(context, node, rect, paddingX, textY, activeZoom);
        } else {
            int lineHeight = context.getLineHeight(style);
            for (String line : node.lines) {
                int lineWidth = measureText(context, style, line);
                int textX = rect.x() + Math.max(paddingX, (rect.width() - lineWidth) / 2);
                context.drawText(line, textX, textY, style);
                textY += lineHeight;
            }
        }

        for (NodeLayout child : node.children) {
            renderNodes(context, child, baseX, baseY);
        }
    }

    private void renderNodeContent(RenderContext context, NodeLayout node, LytRect rect, int paddingX, int contentY,
        float activeZoom) {
        if (node.contentLayout == null) {
            return;
        }
        LytRect innerViewport = getInnerViewport();
        LytRect contentViewport = resolveNodeContentRect(node, rect, paddingX, contentY, activeZoom);
        LytRect clip = intersect(innerViewport, contentViewport);
        if (clip == null) {
            return;
        }
        context.pushLocalScissor(clip);
        try {
            int originX = contentViewport.x() - Math.round(
                node.contentLayout.visualBounds()
                    .x() * activeZoom);
            int originY = contentViewport.y() - Math.round(
                node.contentLayout.visualBounds()
                    .y() * activeZoom);
            NodeContentRenderContext nodeContext = new NodeContentRenderContext(
                context,
                clip,
                originX,
                originY,
                activeZoom);
            renderNodeContentBlock(node.contentLayout.block(), nodeContext, context, contentViewport);
        } finally {
            context.popScissor();
        }
    }

    private void renderNodeContentBlock(LytBlock block, NodeContentRenderContext nodeContext,
        RenderContext nativeContext, LytRect contentViewport) {
        if (block instanceof LytNode container && !container.getChildren()
            .isEmpty()) {
            for (var child : new ArrayList<>(container.getChildren())) {
                if (child instanceof LytBlock childBlock) {
                    renderNodeContentBlock(childBlock, nodeContext, nativeContext, contentViewport);
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

    private static boolean containsScene(@Nullable LytBlock block) {
        if (block == null) return false;
        if (block instanceof LytGuidebookScene) return true;
        if (block instanceof LytNode container) {
            for (var child : container.getChildren()) {
                if (child instanceof LytBlock childBlock && containsScene(childBlock)) return true;
            }
        }
        return false;
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

    private static boolean usesRawGl(LytBlock block) {
        return block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock
            || block instanceof LytItemImage
            || block instanceof LytNeiRecipeBox;
    }

    private @Nullable NodeHit pickNodeHit(int documentX, int documentY) {
        if (layout == null) {
            return null;
        }
        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();
        List<NodeLayout> contentNodes = layout.contentNodes();
        for (int index = contentNodes.size() - 1; index >= 0; index--) {
            NodeLayout node = contentNodes.get(index);
            LytRect contentScreenRect = getNodeContentScreenRect(node, baseX, baseY, activeZoom);
            if (contentScreenRect == null || !contentScreenRect.contains(documentX, documentY)) {
                continue;
            }
            int localX = unscaleCoordinate(documentX - contentScreenRect.x(), activeZoom);
            int localY = unscaleCoordinate(documentY - contentScreenRect.y(), activeZoom);
            DocumentInteractionSnapshot hit = LytDocument.pick(node.contentLayout.block(), localX, localY);
            if (hit != null) {
                return new NodeHit(hit.node(), hit.flowPath(), localX, localY);
            }
        }
        return null;
    }

    private @Nullable LytRect getNodeContentScreenRect(NodeLayout node, int baseX, int baseY, float activeZoom) {
        if (node.contentLayout == null) {
            return null;
        }
        LytRect nodeRect = new LytRect(
            scaled(baseX, node.x, activeZoom),
            scaled(baseY, node.y, activeZoom),
            Math.max(1, Math.round(node.width * activeZoom)),
            Math.max(1, Math.round(node.height * activeZoom)));
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int contentY = nodeRect.y() + Math.max(1, Math.round(NODE_PADDING_Y * activeZoom))
            + resolveNodeBadgeHeight(node, activeZoom);
        return resolveNodeContentRect(node, nodeRect, paddingX, contentY, activeZoom);
    }

    private int contextLineHeight(ResolvedTextStyle style) {
        return Math.max(1, Math.round((9 + 1) * style.fontScale()));
    }

    private LytRect resolveNodeContentRect(NodeLayout node, LytRect nodeRect, int paddingX, int contentY,
        float activeZoom) {
        return new LytRect(
            nodeRect.x() + paddingX,
            contentY,
            Math.max(
                1,
                Math.round(
                    node.contentLayout.visualBounds()
                        .width() * activeZoom)),
            Math.max(
                1,
                Math.round(
                    node.contentLayout.visualBounds()
                        .height() * activeZoom)));
    }

    private int resolveNodeBadgeHeight(NodeLayout node, float activeZoom) {
        if (!node.showBadge || node.badgeText == null) {
            return 0;
        }
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        return contextLineHeight(badgeStyle) + badgePaddingY * 2 + iconGapY;
    }

    private int unscaleCoordinate(int coordinate, float activeZoom) {
        return Math.max(0, Math.round(coordinate / Math.max(activeZoom, 0.0001f)));
    }

    private @Nullable LytRect intersect(LytRect a, LytRect b) {
        int left = Math.max(a.x(), b.x());
        int top = Math.max(a.y(), b.y());
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (right <= left || bottom <= top) {
            return null;
        }
        return new LytRect(left, top, right - left, bottom - top);
    }

    private void restoreViewportAfterLayout(@Nullable DiagramLayout previousLayout, int previousOffsetX,
        int previousOffsetY, int previousViewportWidth, int previousViewportHeight, int viewportWidth,
        int viewportHeight) {
        if (previousLayout == null) {
            centerDiagram(viewportWidth, viewportHeight, layout.diagramWidth(), layout.diagramHeight());
            return;
        }
        float curZoom = getRawZoom();
        float anchorX = previousLayout.contentBounds()
            .x() + (previousViewportWidth * 0.5f - previousOffsetX) / Math.max(curZoom, 0.0001f);
        float anchorY = previousLayout.contentBounds()
            .y() + (previousViewportHeight * 0.5f - previousOffsetY) / Math.max(curZoom, 0.0001f);
        setContentOffset(
            Math.round(
                viewportWidth * 0.5f - (anchorX - layout.contentBounds()
                    .x()) * curZoom),
            Math.round(
                viewportHeight * 0.5f - (anchorY - layout.contentBounds()
                    .y()) * curZoom));
        clampOffsets();
    }

    private LytRect resolveNodeVisualRect(NodeLayout node) {
        LytRect nodeRect = new LytRect(node.x, node.y, node.width, node.height);
        if (node.contentLayout == null) {
            return nodeRect;
        }
        int contentY = node.y + NODE_PADDING_Y + resolveNodeBadgeHeightUnscaled(node);
        LytRect contentRect = new LytRect(
            node.x + NODE_PADDING_X,
            contentY,
            node.contentLayout.visualBounds()
                .width(),
            node.contentLayout.visualBounds()
                .height());
        return LytRect.union(nodeRect, contentRect);
    }

    private int resolveNodeBadgeHeightUnscaled(NodeLayout node) {
        if (!node.showBadge || node.badgeText == null) {
            return 0;
        }
        return contextLineHeight(ICON_TEXT_STYLE) + 4 + ICON_GAP_Y;
    }

    private LytRect resolveBlockVisualBounds(LytBlock block) {
        LytRect[] result = { LytRect.empty() };
        block.visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                if (node instanceof LytBlock childBlock) {
                    result[0] = LytRect.union(result[0], resolveSelfVisualBounds(childBlock));
                }
                return Result.CONTINUE;
            }
        });
        return result[0];
    }

    private LytRect resolveSelfVisualBounds(LytBlock block) {
        LytRect bounds = block.getBounds();
        if (bounds == null) {
            return LytRect.empty();
        }
        if (block instanceof LytLatexBlock latexBlock) {
            return latexBlock.getVisualBounds();
        }
        if (block instanceof LytLatexDisplayBlock latexDisplayBlock) {
            return latexDisplayBlock.getVisualBounds();
        }
        return bounds;
    }

    private NodeColors resolveColors(MindmapNode node) {
        int accent = 0xFF7AA2F7;
        for (String className : node.getClasses()) {
            String lower = className.toLowerCase();
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = 0xFFF7768E;
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = 0xFF9ECE6A;
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = 0xFFE0AF68;
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = 0xFF8B949E;
            }
        }

        accent = switch (node.getShape()) {
            case CIRCLE -> 0xFF7DCFFF;
            case HEXAGON -> 0xFFE0AF68;
            case CLOUD -> 0xFF73DACA;
            case BANG -> 0xFFF7768E;
            default -> accent;
        };

        int border = accent;
        int background = node == mindmap.getRoot() ? 0xFF1F2A38 : 0xFF111922;
        return new NodeColors(background, border, accent);
    }

    private void drawHorizontalConnector(RenderContext context, int startX, int startY, int endX, int endY, int color) {
        int midX = (startX + endX) / 2;
        fillHorizontalLine(context, startX, midX, startY, color);
        fillVerticalLine(context, midX, startY, endY, color);
        fillHorizontalLine(context, midX, endX, endY, color);
    }

    private void drawVerticalConnector(RenderContext context, int startX, int startY, int endX, int endY, int color) {
        int midY = (startY + endY) / 2;
        fillVerticalLine(context, startX, startY, midY, color);
        fillHorizontalLine(context, startX, endX, midY, color);
        fillVerticalLine(context, endX, midY, endY, color);
    }

    private void fillHorizontalLine(RenderContext context, int startX, int endX, int y, int color) {
        int left = Math.min(startX, endX);
        int width = Math.abs(endX - startX) + 1;
        context.fillRect(new LytRect(left, y, width, CONNECTOR_THICKNESS), color);
    }

    private void fillVerticalLine(RenderContext context, int x, int startY, int endY, int color) {
        int top = Math.min(startY, endY);
        int height = Math.abs(endY - startY) + 1;
        context.fillRect(new LytRect(x, top, CONNECTOR_THICKNESS, height), color);
    }

    private int measureText(LayoutContext context, ResolvedTextStyle style, String text) {
        return measureTextInternal(style, text, context::getAdvance);
    }

    private int measureText(RenderContext context, ResolvedTextStyle style, String text) {
        return context.getStringWidth(text, style);
    }

    private int measureTextInternal(ResolvedTextStyle style, String text, AdvanceFunction advance) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += advance.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
    }

    private List<String> wrapText(LayoutContext context, ResolvedTextStyle style, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        GuideStringLines.visitLines(text != null ? text : "", (paragraph, lineIndex) -> {
            if (paragraph.isEmpty()) {
                result.add("");
                return true;
            }

            StringBuilder line = new StringBuilder();
            scanWords(paragraph, word -> appendWrappedWord(result, line, context, style, word, maxWidth));
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            return true;
        });
        return result;
    }

    private boolean appendWrappedWord(List<String> result, StringBuilder line, LayoutContext context,
        ResolvedTextStyle style, String word, int maxWidth) {
        if (line.isEmpty()) {
            if (measureText(context, style, word) <= maxWidth) {
                line.append(word);
            } else {
                appendBrokenWord(result, line, context, style, word, maxWidth);
            }
            return true;
        }

        String candidate = line + " " + word;
        if (measureText(context, style, candidate) <= maxWidth) {
            line.append(' ')
                .append(word);
            return true;
        }

        result.add(line.toString());
        line.setLength(0);
        if (measureText(context, style, word) <= maxWidth) {
            line.append(word);
        } else {
            appendBrokenWord(result, line, context, style, word, maxWidth);
        }
        return true;
    }

    private void scanWords(String text, WordVisitor visitor) {
        int start = -1;
        for (int index = 0, length = text.length(); index <= length; index++) {
            char value = index < length ? text.charAt(index) : ' ';
            if (Character.isWhitespace(value)) {
                if (start >= 0) {
                    if (!visitor.accept(text.substring(start, index))) {
                        return;
                    }
                    start = -1;
                }
            } else if (start < 0) {
                start = index;
            }
        }
    }

    private void appendBrokenWord(List<String> result, StringBuilder line, LayoutContext context,
        ResolvedTextStyle style, String word, int maxWidth) {
        StringBuilder fragment = new StringBuilder();
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String next = fragment + new String(Character.toChars(codePoint));
            if (!fragment.isEmpty() && measureText(context, style, next) > maxWidth) {
                result.add(fragment.toString());
                fragment.setLength(0);
            }
            fragment.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (!fragment.isEmpty()) {
            line.append(fragment);
        }
    }

    private String simplifyIcon(String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }

        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? trimmed : leaf;
    }

    private int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    private int resolvePreferredViewportWidth() {
        return preferredWidth > 0 ? preferredWidth : MIN_WIDTH;
    }

    LytRect getContentBoundsForTesting() {
        return layout != null ? layout.contentBounds() : LytRect.empty();
    }

    public interface AdvanceFunction {

        float getAdvance(int codePoint, ResolvedTextStyle style);
    }

    public static class NodeContentLayout {

        private final LytBlock block;
        private final LytRect visualBounds;

        public NodeContentLayout(LytBlock block, LytRect visualBounds) {
            this.block = block;
            this.visualBounds = visualBounds != null && !visualBounds.isEmpty() ? visualBounds : LytRect.empty();
        }

        public LytBlock block() {
            return block;
        }

        public LytRect visualBounds() {
            return visualBounds;
        }
    }

    public static class NodeHit {

        private final LytNode node;
        private final FlowInteractionPath flowPath;
        private final int localX;
        private final int localY;

        public NodeHit(LytNode node, @Nullable FlowInteractionPath flowPath, int localX, int localY) {
            this.node = node;
            this.flowPath = flowPath != null ? flowPath : FlowInteractionPath.empty();
            this.localX = localX;
            this.localY = localY;
        }

        public LytNode node() {
            return node;
        }

        public FlowInteractionPath flowPath() {
            return flowPath;
        }

        public int localX() {
            return localX;
        }

        public int localY() {
            return localY;
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

        public float getScale() {
            return scale;
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
                if (overlay) {
                    delegate.renderItem(stack, 0, 0);
                } else {
                    delegate.renderItemIcon(stack, 0, 0);
                }
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
            for (int index = 0; index < xs.length; index++) {
                scaledXs[index] = scaleFloatX(xs[index]);
                scaledYs[index] = scaleFloatY(ys[index]);
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
            // Override the default interface method to avoid double-scaling:
            // the default chains toScreenRect(scale) + pushScissor(scale again).
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

    private interface WordVisitor {

        boolean accept(String word);
    }

    public static class DiagramLayout {

        private final NodeLayout root;
        private final int diagramWidth;
        private final int diagramHeight;
        private final LytRect contentBounds;
        private final List<NodeLayout> contentNodes;

        public DiagramLayout(NodeLayout root, int diagramWidth, int diagramHeight, LytRect contentBounds,
            List<NodeLayout> contentNodes) {
            this.root = root;
            this.diagramWidth = diagramWidth;
            this.diagramHeight = diagramHeight;
            this.contentBounds = contentBounds;
            this.contentNodes = contentNodes;
        }

        public NodeLayout root() {
            return root;
        }

        public int diagramWidth() {
            return diagramWidth;
        }

        public int diagramHeight() {
            return diagramHeight;
        }

        public LytRect contentBounds() {
            return contentBounds;
        }

        public List<NodeLayout> contentNodes() {
            return contentNodes;
        }
    }

    public static class NodeColors {

        private final int background;
        private final int border;
        private final int accent;

        public NodeColors(int background, int border, int accent) {
            this.background = background;
            this.border = border;
            this.accent = accent;
        }
    }

    public static class NodeLayout {

        private final MindmapNode node;
        private final int depth;
        private final List<String> lines;
        private final String badgeText;
        private final boolean showBadge;
        @Nullable
        private final NodeContentLayout contentLayout;
        private final int width;
        private final int height;
        private final List<NodeLayout> children = new ArrayList<>();

        private int x;
        private int y;
        private int subtreeWidth;
        private int subtreeHeight;

        public NodeLayout(MindmapNode node, int depth, List<String> lines, String badgeText, boolean showBadge,
            @Nullable NodeContentLayout contentLayout, int width, int height) {
            this.node = node;
            this.depth = depth;
            this.lines = lines;
            this.badgeText = badgeText;
            this.showBadge = showBadge;
            this.contentLayout = contentLayout;
            this.width = width;
            this.height = height;
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public int centerX() {
            return x + width / 2;
        }

        public int centerY() {
            return y + height / 2;
        }
    }

    // Debug implementation

    @Override
    public List<ComponentEntry> getDebugComponents() {
        List<ComponentEntry> components = new ArrayList<>();

        if (layout == null || bounds == null) {
            return components;
        }

        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();

        // Collect all nodes from the layout
        collectNodeComponents(layout.root(), components, baseX, baseY, activeZoom);

        return components;
    }

    private void collectNodeComponents(NodeLayout node, List<ComponentEntry> components, int baseX, int baseY,
        float activeZoom) {
        if (node == null) {
            return;
        }

        // Calculate node bounds using the same formula as renderNodes
        int nodeScreenX = scaled(baseX, node.x, activeZoom);
        int nodeScreenY = scaled(baseY, node.y, activeZoom);
        int nodeScreenW = Math.max(1, Math.round(node.width * activeZoom));
        int nodeScreenH = Math.max(1, Math.round(node.height * activeZoom));

        LytRect nodeBounds = new LytRect(nodeScreenX, nodeScreenY, nodeScreenW, nodeScreenH);

        // Node info
        String nodeName = String.join(" ", node.lines);
        if (nodeName.length() > 30) {
            nodeName = nodeName.substring(0, 27) + "...";
        }

        String extra = "Depth: " + node.depth;
        if (node.children.size() > 0) {
            extra += ", Children: " + node.children.size();
        }

        int priority = 20 - node.depth;
        components.add(new SimpleComponentEntry("Node:" + nodeName, nodeBounds, extra, priority));

        // Add badge as separate component if present
        if (node.showBadge && node.badgeText != null) {
            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
            int badgePaddingX = Math.max(2, Math.round(4 * activeZoom));
            int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
            ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);

            // Calculate badge bounds (simplified from renderNodes)
            int badgeWidth = Math.max(1, 100 + badgePaddingX * 2); // Approximate
            int badgeHeight = Math.max(1, 10 + badgePaddingY * 2); // Approximate
            LytRect badgeBounds = new LytRect(nodeScreenX + paddingX, nodeScreenY + paddingY, badgeWidth, badgeHeight);

            components.add(
                new SimpleComponentEntry("Badge:" + node.badgeText, badgeBounds, "Node: " + nodeName, priority + 5));
        }

        // Add node content as separate component if present
        if (node.contentLayout != null) {
            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
            int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
            int contentY = nodeScreenY + paddingY;

            if (node.showBadge && node.badgeText != null) {
                int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
                int badgeHeight = Math.max(1, 10 + badgePaddingY * 2); // Approximate
                contentY += badgeHeight + iconGapY;
            }

            LytRect contentRect = resolveNodeContentRect(node, nodeBounds, paddingX, contentY, activeZoom);
            components.add(
                new SimpleComponentEntry("NodeContent", contentRect, "Block content for: " + nodeName, priority + 3));
        }

        // Recursively collect children
        for (NodeLayout child : node.children) {
            collectNodeComponents(child, components, baseX, baseY, activeZoom);
        }
    }
}
