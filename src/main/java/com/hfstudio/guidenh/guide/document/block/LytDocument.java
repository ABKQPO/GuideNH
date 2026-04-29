package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Layout document. Has a viewport and an overall size which may exceed the document size vertically, but not
 * horizontally.
 */
public class LytDocument extends LytNode implements LytBlockContainer {

    private final List<LytBlock> blocks = new ArrayList<>();

    @Nullable
    private Layout layout;

    @Nullable
    private HitTestResult hoveredElement;

    public int getAvailableWidth() {
        return layout != null ? layout.availableWidth() : 0;
    }

    public int getContentHeight() {
        return layout != null ? layout.contentHeight() : 0;
    }

    public List<LytBlock> getBlocks() {
        return blocks;
    }

    @Override
    public List<LytBlock> getChildren() {
        return blocks;
    }

    @Override
    public @Nullable LytRect getBounds() {
        return layout != null ? layout.bounds() : null;
    }

    @Override
    public void removeChild(LytNode node) {
        if (node instanceof LytBlock block) {
            if (block.parent == this) {
                block.parent = null;
            }
            blocks.remove(block);
            invalidateLayout();
        }
    }

    @Override
    public void append(LytBlock block) {
        if (block.parent != null) {
            block.parent.removeChild(block);
        }
        block.parent = this;
        blocks.add(block);
        invalidateLayout();
    }

    public void clearContent() {
        for (var block : blocks) {
            block.parent = null;
        }
        blocks.clear();
        invalidateLayout();
    }

    public boolean hasLayout() {
        return layout != null;
    }

    public void invalidateLayout() {
        layout = null;
    }

    public void updateLayout(LayoutContext context, int availableWidth) {
        if (layout != null && layout.availableWidth == availableWidth) {
            return;
        }

        layout = createLayout(context, availableWidth);
    }

    private Layout createLayout(LayoutContext context, int availableWidth) {
        var bounds = Layouts.verticalLayout(context, blocks, 0, 0, availableWidth, 5, 5, 5, 5, 0, AlignItems.START);
        var cachedBounds = new LytRect(0, 0, availableWidth, bounds.height());

        return new Layout(availableWidth, bounds.height(), cachedBounds);
    }

    public void render(RenderContext context) {
        for (var block : blocks) {
            if (block.isCulled(context.viewport())) {
                continue;
            }
            block.render(context);
        }
    }

    public HitTestResult getHoveredElement() {
        return hoveredElement;
    }

    public void setHoveredElement(HitTestResult hoveredElement) {
        if (!Objects.equals(hoveredElement, this.hoveredElement)) {
            if (this.hoveredElement != null) {
                this.hoveredElement.node.onMouseLeave();
            }
            this.hoveredElement = hoveredElement;
            if (this.hoveredElement != null) {
                this.hoveredElement.node.onMouseEnter(hoveredElement.content());
            }
        }
    }

    public HitTestResult pick(int x, int y) {
        return pick(this, x, y);
    }

    public static HitTestResult pick(LytNode root, int x, int y) {
        var node = root.pickNode(x, y);
        if (node != null) {
            LytFlowContent content = null;
            if (node instanceof LytFlowContainer container) {
                content = container.pickContent(x, y);

                // If the content is an inline-block, we descend into it! (This can go on and on and on...)
                if (content instanceof LytFlowInlineBlock inlineBlock && inlineBlock.getBlock() != null) {
                    return pick(inlineBlock.getBlock(), x, y);
                }
            }
            return new HitTestResult(node, content);
        }

        return null;
    }

    @Override
    public void onMouseEnter(@Nullable LytFlowContent hoveredContent) {}

    @Desugar
    public record Layout(int availableWidth, int contentHeight, LytRect bounds) {}

    @Desugar
    public record HitTestResult(LytNode node, @Nullable LytFlowContent content) {}
}
