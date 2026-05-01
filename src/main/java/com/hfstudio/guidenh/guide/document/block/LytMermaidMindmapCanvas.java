package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapLayoutMode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNodeShape;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class LytMermaidMindmapCanvas extends LytBlock implements DocumentDragTarget {

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

    private static final ResolvedTextStyle ROOT_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        true,
        false,
        false,
        false,
        false,
        null,
        new ConstantColor(0xFFF1F6FB),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false);
    private static final ResolvedTextStyle NODE_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        false,
        false,
        false,
        false,
        false,
        null,
        new ConstantColor(0xFFD7DEE7),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false);
    private static final ResolvedTextStyle ICON_TEXT_STYLE = new ResolvedTextStyle(
        0.85f,
        false,
        false,
        false,
        false,
        false,
        null,
        new ConstantColor(0xFFB8C2CF),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false);

    private final MermaidMindmapDocument mindmap;

    private DiagramLayout layout;
    private int contentOffsetX;
    private int contentOffsetY;
    private float zoom = 1f;
    private int preferredWidth;
    private int preferredHeight;

    private boolean dragging;
    private int dragLastDocumentX;
    private int dragLastDocumentY;

    public LytMermaidMindmapCanvas(MermaidMindmapDocument mindmap) {
        this.mindmap = mindmap;
    }

    public MermaidMindmapDocument getMindmap() {
        return mindmap;
    }

    public void setPreferredSize(int width, int height) {
        preferredWidth = Math.max(0, width);
        preferredHeight = Math.max(0, height);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = preferredWidth > 0 ? Math.max(MIN_WIDTH, Math.min(preferredWidth, availableWidth))
            : Math.max(MIN_WIDTH, availableWidth);
        layout = buildLayout(context, safeWidth);
        int desiredHeight = layout.diagramHeight() + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, desiredHeight));
        centerDiagram(safeWidth, viewportHeight);
        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (layout == null) {
            return;
        }

        context.fillRect(bounds, 0x1A0C1117);
        context.drawBorder(bounds, 0x66434C57, 1);

        LytRect viewport = getInnerViewport();
        int baseX = viewport.x() + contentOffsetX
            - Math.round(
                layout.contentBounds()
                    .x() * zoom);
        int baseY = viewport.y() + contentOffsetY
            - Math.round(
                layout.contentBounds()
                    .y() * zoom);

        context.pushLocalScissor(viewport);
        try {
            renderConnectors(context, layout.root(), baseX, baseY);
            renderNodes(context, layout.root(), baseX, baseY);
        } finally {
            context.popScissor();
        }
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (button != 0 || layout == null) {
            return false;
        }
        LytRect viewport = getInnerViewport();
        if (!viewport.contains(documentX, documentY)) {
            return false;
        }
        dragging = true;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (!dragging || layout == null) {
            return;
        }
        int dx = documentX - dragLastDocumentX;
        int dy = documentY - dragLastDocumentY;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        contentOffsetX += dx;
        contentOffsetY += dy;
        clampOffsets();
    }

    @Override
    public void endDrag() {
        dragging = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (wheelDelta == 0 || layout == null || !getInnerViewport().contains(documentX, documentY)) {
            return false;
        }
        float previousZoom = zoom;
        if (wheelDelta > 0) {
            zoom = Math.min(MAX_ZOOM, zoom * ZOOM_STEP);
        } else {
            zoom = Math.max(MIN_ZOOM, zoom / ZOOM_STEP);
        }
        if (Math.abs(previousZoom - zoom) < 0.0001f) {
            return false;
        }
        clampOffsets();
        return true;
    }

    private DiagramLayout buildLayout(LayoutContext context, int availableWidth) {
        int innerWidth = Math.max(72, availableWidth - CANVAS_PADDING * 2);
        int maxNodeTextWidth = Math.max(72, Math.min(180, innerWidth / 3));
        NodeLayout root = prepareLayout(context, mindmap.getRoot(), 0, maxNodeTextWidth);

        if (mindmap.getLayoutMode() == MermaidMindmapLayoutMode.TIDY_TREE) {
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
        LytRect contentBounds = collectContentBounds(root);
        return new DiagramLayout(
            root,
            Math.max(1, contentBounds.width()),
            Math.max(1, contentBounds.height()),
            contentBounds);
    }

    private LytRect collectContentBounds(NodeLayout node) {
        LytRect bounds = new LytRect(node.x, node.y, node.width, node.height);
        for (NodeLayout child : node.children) {
            bounds = LytRect.union(bounds, collectContentBounds(child));
        }
        return bounds;
    }

    private NodeLayout prepareLayout(LayoutContext context, MermaidMindmapNode node, int depth, int maxNodeTextWidth) {
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

        ResolvedTextStyle style = depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
        List<String> lines = wrapText(context, style, primaryText, maxNodeTextWidth);
        if (lines.isEmpty()) {
            lines.add(" ");
        }

        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, measureText(context, style, line));
        }
        int lineHeight = context.getLineHeight(style);
        int textHeight = Math.max(1, lines.size()) * lineHeight;

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

        NodeLayout layout = new NodeLayout(node, depth, lines, badgeText, showBadge, width, height);
        for (MermaidMindmapNode child : node.getChildren()) {
            layout.children.add(prepareLayout(context, child, depth + 1, maxNodeTextWidth));
        }
        return layout;
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
        for (NodeLayout child : node.children) {
            if (mindmap.getLayoutMode() == MermaidMindmapLayoutMode.TIDY_TREE) {
                drawVerticalConnector(
                    context,
                    scaled(baseX, node.centerX()),
                    scaled(baseY, node.bottom()),
                    scaled(baseX, child.centerX()),
                    scaled(baseY, child.y),
                    0xFF5D6C7C);
            } else {
                boolean rightSide = child.centerX() >= node.centerX();
                int parentEdgeX = scaled(baseX, rightSide ? node.right() : node.x);
                int childEdgeX = scaled(baseX, rightSide ? child.x : child.right());
                drawHorizontalConnector(
                    context,
                    parentEdgeX,
                    scaled(baseY, node.centerY()),
                    childEdgeX,
                    scaled(baseY, child.centerY()),
                    0xFF5D6C7C);
            }
            renderConnectors(context, child, baseX, baseY);
        }
    }

    private void renderNodes(RenderContext context, NodeLayout node, int baseX, int baseY) {
        LytRect rect = new LytRect(
            scaled(baseX, node.x),
            scaled(baseY, node.y),
            Math.max(1, Math.round(node.width * zoom)),
            Math.max(1, Math.round(node.height * zoom)));
        NodeColors colors = resolveColors(node.node);
        context.fillRect(rect, colors.background);
        context.drawBorder(rect, colors.border, node.node.getShape() == MermaidMindmapNodeShape.BANG ? 2 : 1);
        context.fillRect(new LytRect(rect.x(), rect.y(), 3, rect.height()), colors.accent);

        int textY = rect.y() + NODE_PADDING_Y;
        if (node.showBadge && node.badgeText != null) {
            int badgeWidth = Math
                .max(1, Math.round((measureText(context, ICON_TEXT_STYLE, node.badgeText) + 8) * zoom));
            LytRect badge = new LytRect(
                rect.x() + Math.round(NODE_PADDING_X * zoom),
                textY,
                badgeWidth,
                Math.max(1, Math.round((context.getLineHeight(ICON_TEXT_STYLE) + 4) * zoom)));
            context.fillRect(badge, 0x262A3340);
            context.drawBorder(badge, 0x66434C57, 1);
            context.drawText(node.badgeText, badge.x() + 4, badge.y() + 2, ICON_TEXT_STYLE);
            textY = badge.bottom() + ICON_GAP_Y;
        }

        ResolvedTextStyle style = node.depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
        int lineHeight = context.getLineHeight(style);
        for (String line : node.lines) {
            int lineWidth = measureText(context, style, line);
            int textX = rect.x() + Math.max(Math.round(NODE_PADDING_X * zoom), (rect.width() - lineWidth) / 2);
            context.drawText(line, textX, textY, style);
            textY += lineHeight;
        }

        for (NodeLayout child : node.children) {
            renderNodes(context, child, baseX, baseY);
        }
    }

    private NodeColors resolveColors(MermaidMindmapNode node) {
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
        String normalized = text != null ? text.replace("\r\n", "\n")
            .replace('\r', '\n') : "";
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() == 0) {
                    if (measureText(context, style, word) <= maxWidth) {
                        line.append(word);
                    } else {
                        appendBrokenWord(result, line, context, style, word, maxWidth);
                    }
                    continue;
                }

                String candidate = line + " " + word;
                if (measureText(context, style, candidate) <= maxWidth) {
                    line.append(' ')
                        .append(word);
                    continue;
                }

                result.add(line.toString());
                line.setLength(0);
                if (measureText(context, style, word) <= maxWidth) {
                    line.append(word);
                } else {
                    appendBrokenWord(result, line, context, style, word, maxWidth);
                }
            }
            if (line.length() > 0) {
                result.add(line.toString());
            }
        }
        return result;
    }

    private void appendBrokenWord(List<String> result, StringBuilder line, LayoutContext context,
        ResolvedTextStyle style, String word, int maxWidth) {
        StringBuilder fragment = new StringBuilder();
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String next = fragment + new String(Character.toChars(codePoint));
            if (fragment.length() > 0 && measureText(context, style, next) > maxWidth) {
                result.add(fragment.toString());
                fragment.setLength(0);
            }
            fragment.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (fragment.length() > 0) {
            line.append(fragment);
        }
    }

    private String simplifyIcon(String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }

        String[] parts = icon.trim()
            .split("\\s+");
        String leaf = parts[parts.length - 1];
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? icon.trim() : leaf;
    }

    private void centerDiagram(int viewportWidth, int viewportHeight) {
        if (layout == null) {
            return;
        }
        int innerWidth = Math.max(1, viewportWidth - CANVAS_PADDING * 2);
        int innerHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);
        contentOffsetX = (innerWidth - Math.round(layout.diagramWidth * zoom)) / 2;
        contentOffsetY = (innerHeight - Math.round(layout.diagramHeight * zoom)) / 2;
        clampOffsets();
    }

    private void clampOffsets() {
        if (layout == null || bounds == null) {
            return;
        }
        int innerWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int innerHeight = Math.max(1, bounds.height() - CANVAS_PADDING * 2);

        contentOffsetX = clampAxis(contentOffsetX, innerWidth, Math.round(layout.diagramWidth * zoom));
        contentOffsetY = clampAxis(contentOffsetY, innerHeight, Math.round(layout.diagramHeight * zoom));
    }

    private int clampAxis(int offset, int viewportSize, int contentSize) {
        if (contentSize <= viewportSize) {
            return (viewportSize - contentSize) / 2;
        }
        int min = viewportSize - contentSize;
        int max = 0;
        return Math.max(min, Math.min(max, offset));
    }

    private LytRect getInnerViewport() {
        return new LytRect(
            bounds.x() + CANVAS_PADDING,
            bounds.y() + CANVAS_PADDING,
            Math.max(1, bounds.width() - CANVAS_PADDING * 2),
            Math.max(1, bounds.height() - CANVAS_PADDING * 2));
    }

    private int scaled(int base, int value) {
        return base + Math.round(value * zoom);
    }

    LytRect getContentBoundsForTesting() {
        return layout != null ? layout.contentBounds() : LytRect.empty();
    }

    public interface AdvanceFunction {

        float getAdvance(int codePoint, ResolvedTextStyle style);
    }

    public static final class DiagramLayout {

        private final NodeLayout root;
        private final int diagramWidth;
        private final int diagramHeight;
        private final LytRect contentBounds;

        public DiagramLayout(NodeLayout root, int diagramWidth, int diagramHeight, LytRect contentBounds) {
            this.root = root;
            this.diagramWidth = diagramWidth;
            this.diagramHeight = diagramHeight;
            this.contentBounds = contentBounds;
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
    }

    public static final class NodeColors {

        private final int background;
        private final int border;
        private final int accent;

        public NodeColors(int background, int border, int accent) {
            this.background = background;
            this.border = border;
            this.accent = accent;
        }
    }

    public static final class NodeLayout {

        private final MermaidMindmapNode node;
        private final int depth;
        private final List<String> lines;
        private final String badgeText;
        private final boolean showBadge;
        private final int width;
        private final int height;
        private final List<NodeLayout> children = new ArrayList<>();

        private int x;
        private int y;
        private int subtreeWidth;
        private int subtreeHeight;

        public NodeLayout(MermaidMindmapNode node, int depth, List<String> lines, String badgeText, boolean showBadge,
            int width, int height) {
            this.node = node;
            this.depth = depth;
            this.lines = lines;
            this.badgeText = badgeText;
            this.showBadge = showBadge;
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
}
