package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.latex.GuideLatexRenderer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Inline-flow LaTeX block. When placed inside a
 * {@link com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock}, it renders a LaTeX formula at a
 * size proportional to the surrounding text, automatically expanding the line height when the formula is
 * taller than a single character (e.g. fractions).
 *
 * <p>
 * Vertical alignment is controlled by {@link LatexVerticalAlign}:
 * <ul>
 * <li>{@link LatexVerticalAlign#TOP} — formula top aligns with the text line top.</li>
 * <li>{@link LatexVerticalAlign#CENTER} — formula is centered on the text line (default).</li>
 * <li>{@link LatexVerticalAlign#BOTTOM} — formula bottom aligns with the text line bottom.</li>
 * </ul>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the alignment.
 */
public class LytLatexBlock extends LytBlock implements InteractiveElement {

    private final String formula;
    private final int fillColorArgb;
    private final float sourceScale;
    private final float userScale;
    private final boolean showTooltip;
    private final LatexVerticalAlign valign;
    private final int offsetX;
    private final int offsetY;

    /** Vertical pixel offset from aligned position, recomputed each layout pass. */
    private int renderYOffset;

    public LytLatexBlock(String formula, int fillColorArgb, float sourceScale, float userScale, boolean showTooltip,
        LatexVerticalAlign valign, int offsetX, int offsetY) {
        this.formula = formula;
        this.fillColorArgb = fillColorArgb;
        this.sourceScale = sourceScale;
        this.userScale = Math.max(0.1f, userScale);
        this.showTooltip = showTooltip;
        this.valign = valign;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale);
        if (size == null) {
            renderYOffset = 0;
            return new LytRect(x, y, 0, 0);
        }

        int lineHeight = context.getLineHeight(null);
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);

        int displayH = (int) Math.max(1, Math.ceil((double) size[1] * lineHeight * userScale / refH));
        int displayW = (int) Math.max(1, Math.ceil((double) size[0] * lineHeight * userScale / refH));

        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - displayH) / 2;
            case BOTTOM -> lineHeight - displayH;
            default -> 0;
        };
        renderYOffset = alignOffset + offsetY;

        return new LytRect(x, y, displayW, displayH);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
        if (tex == null) {
            return;
        }

        GuideLatexRenderer.INSTANCE
            .renderLatex(bounds.x() + offsetX, bounds.y() + renderYOffset, bounds.width(), bounds.height(), tex[0]);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (showTooltip) {
            return Optional.of(new TextTooltip(formula));
        }
        return Optional.empty();
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        return LytVisitor.Result.CONTINUE;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return Collections.emptyList();
    }

    public String getFormula() {
        return formula;
    }

    public int getFillColorArgb() {
        return fillColorArgb;
    }

    public float getSourceScale() {
        return sourceScale;
    }

    public float getUserScale() {
        return userScale;
    }

    public boolean isShowTooltip() {
        return showTooltip;
    }

    public LatexVerticalAlign getValign() {
        return valign;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    @Nullable
    @Override
    public LytRect getBounds() {
        return bounds;
    }
}
