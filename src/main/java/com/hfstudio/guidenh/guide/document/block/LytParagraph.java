package com.hfstudio.guidenh.guide.document.block;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.flow.FlowBuilder;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytParagraph extends LytBlock implements LytFlowContainer {

    protected final FlowBuilder content = new FlowBuilder();

    protected int paddingLeft;
    protected int paddingTop;
    protected int paddingRight;
    protected int paddingBottom;

    @Nullable
    protected FlowInteractionPath hoveredPath = FlowInteractionPath.empty();
    @Nullable
    protected FlowInteractionPath revealedPath = FlowInteractionPath.empty();

    @Override
    public void append(LytFlowContent child) {
        content.append(child);
        child.setParent(this);
    }

    @Override
    public boolean isCulled(LytRect viewport) {
        // If we have floating content, account for its bounding box exceeding our content box
        if (content.floatsIntersect(viewport)) {
            return false;
        }

        return super.isCulled(viewport);
    }

    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Apply padding to paragraph content
        x += paddingLeft;
        availableWidth -= paddingLeft + paddingRight;
        y += paddingTop;

        var style = resolveStyle();

        var bounds = content.computeLayout(context, x, y, availableWidth, style.alignment());

        if (paddingBottom != 0) {
            return bounds.withHeight(bounds.height() + paddingBottom);
        }
        return bounds;
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        content.move(deltaX, deltaY);
    }

    @Override
    public void onMouseEnter(@Nullable LytFlowContent hoveredContent) {
        super.onMouseEnter(hoveredContent);
        this.hoveredPath = FlowInteractionPath.fromPrimary(hoveredContent);
    }

    public void setInteractionPaths(@Nullable FlowInteractionPath hoveredPath,
        @Nullable FlowInteractionPath revealedPath) {
        this.hoveredPath = hoveredPath != null ? hoveredPath : FlowInteractionPath.empty();
        this.revealedPath = revealedPath != null ? revealedPath : FlowInteractionPath.empty();
    }

    @Override
    public void onMouseLeave() {
        super.onMouseLeave();
        this.hoveredPath = FlowInteractionPath.empty();
        this.revealedPath = FlowInteractionPath.empty();
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        // If we are the host for any floating elements, those can exceed our own bounds
        var fl = content.pickFloatingElement(x, y);
        if (fl != null) {
            return this;
        }

        return super.pickNode(x, y);
    }

    @Override
    public void render(RenderContext context) {
        // Since we overwrite isCulled, we render even if our actual line content is culled, for floats
        if (context.intersectsViewport(bounds)) {
            content.render(context, hoveredPath, revealedPath);
        }

        content.renderFloats(context, hoveredPath, revealedPath);
    }

    @Override
    public @Nullable FlowInteractionPath pickContent(int x, int y) {
        return content.pickPath(x, y);
    }

    public @Nullable LytRect getFirstLineBounds() {
        return content.getFirstLineBounds();
    }

    public @Nullable LytRect getFirstTextRunBounds() {
        return content.getFirstTextRunBounds();
    }

    @Override
    public Stream<LytRect> enumerateContentBounds(LytFlowContent content) {
        return this.content.enumerateContentBounds(content);
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        if (super.visitChildren(visitor, includeOutOfTreeContent) == LytVisitor.Result.STOP) {
            return LytVisitor.Result.STOP;
        }

        for (var flowContent : getContent()) {
            flowContent.visit(visitor);
        }

        return LytVisitor.Result.CONTINUE;
    }

    public Iterable<LytFlowContent> getContent() {
        return content.getContent();
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public void clearContent() {
        content.clear();
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    /**
     * Quick shorthand to create a paragrpah of plain text.
     */
    public static LytParagraph of(String text) {
        var paragraph = new LytParagraph();
        paragraph.appendText(text);
        return paragraph;
    }
}
