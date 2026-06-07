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

    /** Formula display width in GUI pixels, recomputed each layout pass. */
    private int formulaDisplayW;
    /** Formula display height in GUI pixels, recomputed each layout pass. */
    private int formulaDisplayH;
    /** Vertical pixel offset inside the layout bounds, recomputed each layout pass. */
    private int renderYOffset;
    private boolean sourceMetricsResolved;
    private int sourceWidthPx;
    private int sourceHeightPx;
    private int sourceDepthPx;
    private int sourceRefHeightPx;

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
        if (!resolveSourceMetrics()) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            renderYOffset = 0;
            return new LytRect(x, y, 0, 0);
        }

        int lineHeight = context.getLineHeight(null);
        formulaDisplayH = scaleSourceMetricCeil(sourceHeightPx, lineHeight);
        formulaDisplayW = scaleSourceMetricCeil(sourceWidthPx, lineHeight);

        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - formulaDisplayH) / 2;
            case BOTTOM -> lineHeight - formulaDisplayH;
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
                int depthDisplay = scaleSourceMetricRound(sourceDepthPx, lineHeight);
                yield lineHeight - formulaDisplayH + depthDisplay;
            }
            default -> 0; // TOP
        };
        int desiredRenderYOffset = alignOffset + offsetY;
        int topInset = Math.max(0, -desiredRenderYOffset);
        int bottomInset = Math.max(0, desiredRenderYOffset);
        renderYOffset = desiredRenderYOffset + topInset;

        return new LytRect(x, y - topInset, formulaDisplayW, topInset + formulaDisplayH + bottomInset);
    }

    private boolean resolveSourceMetrics() {
        if (sourceMetricsResolved) {
            return sourceWidthPx > 0 && sourceHeightPx > 0;
        }
        sourceMetricsResolved = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale);
        if (size == null) {
            return false;
        }
        sourceWidthPx = size[0];
        sourceHeightPx = size[1];
        sourceDepthPx = size[2];
        sourceRefHeightPx = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        return sourceWidthPx > 0 && sourceHeightPx > 0;
    }

    private int scaleSourceMetricCeil(int sourceMetric, int lineHeight) {
        return (int) Math.max(1, Math.ceil((double) sourceMetric * lineHeight * userScale / sourceRefHeightPx));
    }

    private int scaleSourceMetricRound(int sourceMetric, int lineHeight) {
        return (int) Math.round((double) sourceMetric * lineHeight * userScale / sourceRefHeightPx);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (formulaDisplayW <= 0 || formulaDisplayH <= 0) {
            return;
        }

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
        if (tex == null) {
            return;
        }

        GuideLatexRenderer.INSTANCE
            .renderLatex(bounds.x() + offsetX, bounds.y() + renderYOffset, formulaDisplayW, formulaDisplayH, tex[0]);
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

    public LytRect getVisualBounds() {
        if (bounds == null || bounds.isEmpty()) {
            return LytRect.empty();
        }
        return new LytRect(bounds.x() + offsetX, bounds.y() + renderYOffset, formulaDisplayW, formulaDisplayH);
    }

    @Nullable
    @Override
    public LytRect getBounds() {
        return bounds;
    }
}
