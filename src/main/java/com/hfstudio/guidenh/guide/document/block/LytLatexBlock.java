package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
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
 * <li>{@link LatexVerticalAlign#BASELINE} — formula math baseline aligns with the text baseline (default).
 * This is the best choice for most inline formulas: letters and superscripts sit flush with
 * surrounding text, while fractions and integrals extend above/below the baseline naturally.</li>
 * <li>{@link LatexVerticalAlign#TOP} — formula top aligns with the text line top.</li>
 * <li>{@link LatexVerticalAlign#CENTER} — formula is centered on the text line.</li>
 * <li>{@link LatexVerticalAlign#BOTTOM} — formula bottom aligns with the text line bottom.</li>
 * </ul>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the alignment.
 */
public class LytLatexBlock extends LytBlock implements InteractiveElement {

    private final String formula;
    private final int fillColorArgb;
    private final float sourceScale;
    private final float userScale;
    @Nullable
    private final GuideTooltip tooltip;
    private final LatexVerticalAlign valign;
    private final int offsetX;
    private final int offsetY;

    /** Vertical pixel offset from aligned position, recomputed each layout pass. */
    private int renderYOffset;

    public LytLatexBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, LatexVerticalAlign valign, int offsetX, int offsetY) {
        this(formula, new LatexRenderOptions(fillColorArgb, sourceScale, userScale, tooltip, valign, offsetX, offsetY));
    }

    public LytLatexBlock(String formula, LatexRenderOptions options) {
        this.formula = formula;
        this.fillColorArgb = options.fillColorArgb();
        this.sourceScale = options.sourceScale();
        this.userScale = options.userScale();
        this.tooltip = options.tooltip();
        this.valign = options.valign();
        this.offsetX = options.offsetX();
        this.offsetY = options.offsetY();
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
            case BASELINE -> {
                // Align the formula's math baseline with the text baseline.
                //
                // Both calibrateRefHeight() and measureSize() apply the same Insets value,
                // so the bottom-inset term B cancels out in the algebra:
                //
                // text_baseline = (refH - B) * lineHeight / refH
                // formula_ascent = (size[1] - B - size[2]) * lineHeight * userScale / refH
                // alignOffset = text_baseline - formula_ascent
                // = (lineHeight - displayH) + size[2] * lineHeight * userScale / refH
                // = (lineHeight - displayH) + depthDisplay
                //
                // For depth-zero formulas (size[2]==0) this is identical to BOTTOM.
                int depthDisplay = (int) Math.round((double) size[2] * lineHeight * userScale / refH);
                yield lineHeight - displayH + depthDisplay;
            }
            default -> 0; // TOP
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
        return Optional.ofNullable(tooltip);
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        return LytVisitor.Result.CONTINUE;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of();
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
        return tooltip != null;
    }

    @Nullable
    public GuideTooltip getLatexTooltip() {
        return tooltip;
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
