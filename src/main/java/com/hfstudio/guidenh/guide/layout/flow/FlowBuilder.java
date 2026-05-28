package com.hfstudio.guidenh.guide.layout.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.TextAlignment;

public class FlowBuilder {

    /** Sentinel used to force containsMouse recalculation after a layout rebuild. */
    private static final LytFlowContent LAYOUT_DIRTY = new LytFlowContent();

    private final List<Line> lines = new ArrayList<>();

    private final List<LytFlowContent> rootContent = new ArrayList<>();

    // Bounding rectangles for any floats in this flow
    private final List<LineBlock> floats = new ArrayList<>();

    /** Tracks the last hovered content so containsMouse is only recalculated on changes. */
    private LytFlowContent lastHoveredForContainsMouse = LAYOUT_DIRTY;

    public void append(LytFlowContent content) {
        this.rootContent.add(content);
    }

    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth, TextAlignment alignment) {
        lines.clear();
        floats.clear();
        lastHoveredForContainsMouse = LAYOUT_DIRTY;
        var lineBuilder = new LineBuilder(context, x, y, availableWidth, lines, floats, alignment);
        for (var content : rootContent) {
            visitInDocumentOrder(content, lineBuilder);
        }
        lineBuilder.end();

        // Build bounding box around all lines
        return lineBuilder.getBounds();
    }

    public void render(RenderContext context, @Nullable LytFlowContent hoveredContent) {
        updateContainsMouse(hoveredContent);
        for (var line : lines) {
            for (var el = line.firstElement; el != null; el = el.next) {
                el.render(context);
            }
        }
    }

    public void renderFloats(RenderContext context, @Nullable LytFlowContent hoveredContent) {
        updateContainsMouse(hoveredContent);
        for (var el : floats) {
            el.render(context);
        }
    }

    @Nullable
    public LytRect getFirstLineBounds() {
        return lines.isEmpty() ? null : lines.getFirst().bounds;
    }

    @Nullable
    public LytRect getFirstTextRunBounds() {
        if (lines.isEmpty()) {
            return null;
        }
        for (var el = lines.getFirst().firstElement; el != null; el = el.next) {
            if (el instanceof LineTextRun textRun && !textRun.text.isEmpty()) {
                return textRun.bounds;
            }
        }
        return null;
    }

    private void updateContainsMouse(@Nullable LytFlowContent hoveredContent) {
        if (lastHoveredForContainsMouse == hoveredContent) {
            return;
        }
        lastHoveredForContainsMouse = hoveredContent;
        for (var line : lines) {
            for (var el = line.firstElement; el != null; el = el.next) {
                el.containsMouse = hoveredContent != null && hoveredContent.isInclusiveAncestor(el.getFlowContent());
            }
        }
        for (var el : floats) {
            el.containsMouse = hoveredContent != null && hoveredContent.isInclusiveAncestor(el.getFlowContent());
        }
    }

    private void visitInDocumentOrder(LytFlowContent content, Consumer<LytFlowContent> visitor) {
        if (content instanceof LytFlowSpan flowSpan) {
            for (var child : flowSpan.getChildren()) {
                visitInDocumentOrder(child, visitor);
            }
        } else {
            visitor.accept(content);
        }
    }

    @Nullable
    public LineElement pick(int x, int y) {
        var floatEl = pickFloatingElement(x, y);
        if (floatEl != null) {
            return floatEl;
        }

        for (var line : lines) {
            // Floating content overflows the line-box, but still belongs to the line
            // otherwise only hit-test line-elements if the line itself is hit
            if (line.bounds.contains(x, y)) {
                for (var el = line.firstElement; el != null; el = el.next) {
                    if (el.bounds.contains(x, y)) {
                        return el;
                    }
                }
            }
        }

        return null;
    }

    public Stream<LytRect> enumerateContentBounds(LytFlowContent content) {
        Stream.Builder<LytRect> builder = Stream.builder();
        for (var line : lines) {
            for (var el = line.firstElement; el != null; el = el.next) {
                if (el.getFlowContent() == content) {
                    builder.accept(el.bounds);
                }
            }
        }
        for (var el : floats) {
            if (el.getFlowContent() == content) {
                builder.accept(el.bounds);
            }
        }
        return builder.build();
    }

    @Nullable
    public LineBlock pickFloatingElement(int x, int y) {
        for (var el : floats) {
            if (el.bounds.contains(x, y)) {
                return el;
            }
        }
        return null;
    }

    public boolean floatsIntersect(LytRect bounds) {
        for (var el : floats) {
            if (el.bounds.intersects(bounds)) {
                return true;
            }
        }
        return false;
    }

    public Iterable<LytFlowContent> getContent() {
        return rootContent;
    }

    public boolean isEmpty() {
        return rootContent.isEmpty();
    }

    public void clear() {
        this.lines.clear();
        this.rootContent.clear();
        this.floats.clear();
    }

    public void move(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return;
        }
        for (var line : this.lines) {
            line.bounds = line.bounds.move(deltaX, deltaY);
            for (var el = line.firstElement; el != null; el = el.next) {
                el.bounds = el.bounds.move(deltaX, deltaY);
                if (el instanceof LineBlock lineBlock) {
                    lineBlock.getBlock()
                        .moveLayoutPos(deltaX, deltaY);
                }
            }
        }
    }
}
