package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
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

    private boolean largeSlot;
    private boolean renderSlotBackground = true;
    private final List<ItemStack> stacks;

    public LytSlot(ItemStack stack) {
        this.stacks = stack == null ? Collections.emptyList() : Collections.singletonList(stack);
    }

    public LytSlot(List<ItemStack> stacks) {
        this.stacks = stacks != null ? stacks : Collections.emptyList();
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
            final int BORDER_DARK = 0xFF373737;
            final int BORDER_LIGHT = 0xFFFFFFFF;
            final int INNER_BG = 0xFF8B8B8B;
            context.fillRect(new LytRect(x, y, w, 1), BORDER_DARK);
            context.fillRect(new LytRect(x, y, 1, h), BORDER_DARK);
            context.fillRect(new LytRect(x, y + h - 1, w, 1), BORDER_LIGHT);
            context.fillRect(new LytRect(x + w - 1, y, 1, h), BORDER_LIGHT);
            context.fillRect(new LytRect(x + 1, y + 1, w - 2, h - 2), INNER_BG);
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
        var cycle = System.nanoTime() / TimeUnit.MILLISECONDS.toNanos(CYCLE_TIME);
        return stacks.get((int) (cycle % stacks.size()));
    }
}
