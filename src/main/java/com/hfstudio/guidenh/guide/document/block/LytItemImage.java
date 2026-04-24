package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytItemImage extends LytBlock implements InteractiveElement {

    private static final int BASE_SIZE = 16;

    /**
     * Base upward render offset, in pixels, applied to <em>inline</em> item icons so they sit
     * optically centered with the surrounding text line instead of being anchored to the line top.
     * Negative values shift the icon up. Scales linearly with {@link #getScale()} at render time.
     * Authors can override globally by mutating this field during guide load, or per-element via
     * the {@code yOffset} MDX attribute on {@code <ItemImage>}.
     */
    public static int DEFAULT_INLINE_Y_OFFSET = -4;

    private final ItemStack stack;
    private float scale = 1f;
    private boolean tooltipSuppressed = false;
    private boolean inline = false;
    /** Per-instance override for {@link #DEFAULT_INLINE_Y_OFFSET}. {@code null} means "use default". */
    private Integer inlineYOffsetOverride = null;

    public LytItemImage(ItemStack stack) {
        this.stack = stack;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.125f, scale);
    }

    public float getScale() {
        return scale;
    }

    public void setTooltipSuppressed(boolean suppressed) {
        this.tooltipSuppressed = suppressed;
    }

    /**
     * Flag this image as being laid out inline with text. Only inline images receive the
     * {@link #DEFAULT_INLINE_Y_OFFSET} correction; block-level images render at their raw layout
     * position so they do not escape their reserved rect.
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public void setInlineYOffsetOverride(@javax.annotation.Nullable Integer override) {
        this.inlineYOffsetOverride = override;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int size = Math.round(BASE_SIZE * scale);
        return new LytRect(x, y, size, size);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (stack == null || stack.stackSize == 0) return;
        int x = bounds.x();
        int y = bounds.y();
        // Inline icons get a small upward nudge so their visual center lines up with the adjacent
        // text baseline. Offset scales with the icon scale so a 2x icon shifts roughly 4 px.
        if (inline) {
            int base = inlineYOffsetOverride != null ? inlineYOffsetOverride : DEFAULT_INLINE_Y_OFFSET;
            y += Math.round(base * scale);
        }
        if (scale == 1f) {
            context.renderItem(stack, x, y);
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);
        context.renderItem(stack, 0, 0);
        GL11.glPopMatrix();
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (tooltipSuppressed) return Optional.empty();
        if (stack == null || stack.stackSize == 0) return Optional.empty();
        return Optional.of(new ItemTooltip(stack));
    }

    public List<ItemStack> getStacks() {
        return stack == null ? Collections.emptyList() : Collections.singletonList(stack);
    }
}
