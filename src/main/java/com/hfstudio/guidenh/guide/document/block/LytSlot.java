package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytSlot extends LytBlock implements InteractiveElement {

    public static final int ITEM_SIZE = 16;
    public static final int PADDING = 1;
    public static final int LARGE_PADDING = 5;
    public static final int OUTER_SIZE = ITEM_SIZE + 2 * PADDING;
    public static final int OUTER_SIZE_LARGE = ITEM_SIZE + 2 * LARGE_PADDING;
    public static final int CYCLE_TIME = 2000;

    /** Precomputed nanosecond period for item cycling to avoid repeated TimeUnit conversion. */
    private static final long CYCLE_NANOS = TimeUnit.MILLISECONDS.toNanos(CYCLE_TIME);

    private static final int SLOT_BORDER_DARK = 0xFF373737;
    private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_INNER_BG = 0xFF8B8B8B;

    private boolean largeSlot;
    private boolean renderSlotBackground = true;
    private final List<ItemStack> stacks;
    private long cachedCycleId = -1;
    private int cachedStackIdx = 0;

    public LytSlot(ItemStack stack) {
        this.stacks = stack == null ? List.of() : List.of(stack);
    }

    public LytSlot(List<ItemStack> stacks) {
        this.stacks = stacks != null ? stacks : List.of();
    }

    public boolean isLargeSlot() {
        return largeSlot;
    }

    public void setLargeSlot(boolean largeSlot) {
        this.largeSlot = largeSlot;
    }

    public boolean isRenderSlotBackground() {
        return renderSlotBackground;
    }

    public void setRenderSlotBackground(boolean renderSlotBackground) {
        this.renderSlotBackground = renderSlotBackground;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (largeSlot) {
            return new LytRect(x, y, OUTER_SIZE_LARGE, OUTER_SIZE_LARGE);
        } else {
            return new LytRect(x, y, OUTER_SIZE, OUTER_SIZE);
        }
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        var x = bounds.x();
        var y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();

        if (renderSlotBackground) {
            context.fillRect(x, y, w, 1, SLOT_BORDER_DARK);
            context.fillRect(x, y, 1, h, SLOT_BORDER_DARK);
            context.fillRect(x, y + h - 1, w, 1, SLOT_BORDER_LIGHT);
            context.fillRect(x + w - 1, y, 1, h, SLOT_BORDER_LIGHT);
            context.fillRect(x + 1, y + 1, w - 2, h - 2, SLOT_INNER_BG);
        }

        var padding = largeSlot ? LARGE_PADDING : PADDING;
        var stack = getDisplayedStack();
        if (stack != null) {
            if (stack.stackSize > 0) {
                context.renderItem(stack, x + padding, y + padding);
            } else {
                context.renderItemIcon(stack, x + padding, y + padding);
            }
        }
    }

    public Optional<GuideTooltip> getTooltip(float x, float y) {
        var stack = getDisplayedStack();
        if (stack == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemTooltip(stack));
    }

    private ItemStack getDisplayedStack() {
        if (stacks.isEmpty()) {
            return null;
        }
        long cycle = System.nanoTime() / CYCLE_NANOS;
        if (cycle != cachedCycleId) {
            cachedCycleId = cycle;
            cachedStackIdx = (int) (cycle % stacks.size());
        }
        return stacks.get(cachedStackIdx);
    }
}
