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
 * Block-level (display) LaTeX element. Occupies the full available width and centers the formula
 * horizontally, with a small vertical margin above and below.
 *
 * <p>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the default centered position.
 */
public class LytLatexDisplayBlock extends LytBlock implements InteractiveElement {

    private static final int VERTICAL_MARGIN = 4;

    private final String formula;
    private final int fillColorArgb;
    private final float sourceScale;
    private final float userScale;
    @Nullable
    private final GuideTooltip tooltip;
    private final int offsetX;
    private final int offsetY;

    /** Cached formula display width (pixels in GUI units), set during layout. */
    private int formulaDisplayW;
    /** Cached formula display height (pixels in GUI units), set during layout. */
    private int formulaDisplayH;

    public LytLatexDisplayBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, int offsetX, int offsetY) {
        this(
            formula,
            new LatexRenderOptions(
                fillColorArgb,
                sourceScale,
                userScale,
                tooltip,
                LatexVerticalAlign.BASELINE,
                offsetX,
                offsetY));
    }

    public LytLatexDisplayBlock(String formula, LatexRenderOptions options) {
        this.formula = formula;
        this.fillColorArgb = options.fillColorArgb();
        this.sourceScale = options.sourceScale();
        this.userScale = options.userScale();
        this.tooltip = options.tooltip();
        this.offsetX = options.offsetX();
        this.offsetY = options.offsetY();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale);
        if (size == null) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            return new LytRect(x, y, availableWidth, 0);
        }

        int lineHeight = context.getLineHeight(null);
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);

        formulaDisplayH = (int) Math.max(1, Math.ceil((double) size[1] * lineHeight * userScale / refH));
        formulaDisplayW = (int) Math.max(1, Math.ceil((double) size[0] * lineHeight * userScale / refH));

        return new LytRect(x, y, availableWidth, formulaDisplayH + 2 * VERTICAL_MARGIN);
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

        int centeredX = bounds.x() + (bounds.width() - formulaDisplayW) / 2;
        int formulaY = bounds.y() + VERTICAL_MARGIN;
        GuideLatexRenderer.INSTANCE
            .renderLatex(centeredX + offsetX, formulaY + offsetY, formulaDisplayW, formulaDisplayH, tex[0]);
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
