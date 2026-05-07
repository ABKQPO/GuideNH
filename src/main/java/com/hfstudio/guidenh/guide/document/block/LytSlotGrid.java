package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

public class LytSlotGrid extends LytBox {

    private final int width;
    private final int height;
    private final LytSlot[] slots;
    private boolean renderEmptySlots = true;
    private boolean renderSlotBackground = true;

    public LytSlotGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.slots = new LytSlot[width * height];
    }

    public static LytSlotGrid columnFromStacks(List<ItemStack> items, boolean skipEmpty) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!skipEmpty || (s != null && s.stackSize > 0)) count++;
        }
        var grid = new LytSlotGrid(1, count);
        int row = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!skipEmpty || (s != null && s.stackSize > 0)) {
                grid.setItem(0, row++, s);
            }
        }
        return grid;
    }

    public static LytSlotGrid rowFromStacks(List<ItemStack> items, boolean skipEmpty) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!skipEmpty || (s != null && s.stackSize > 0)) count++;
        }
        var grid = new LytSlotGrid(count, 1);
        int col = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!skipEmpty || (s != null && s.stackSize > 0)) {
                grid.setItem(col++, 0, s);
            }
        }
        return grid;
    }

    public void setItem(int x, int y, @Nullable ItemStack stack) {
        int idx = y * width + x;
        if (idx >= 0 && idx < slots.length) {
            if (slots[idx] == null) {
                slots[idx] = new LytSlot(stack);
                configureSlot(slots[idx]);
                append(slots[idx]);
            }
        }
    }

    public void setItems(int x, int y, List<ItemStack> stacks) {
        int idx = y * width + x;
        if (idx >= 0 && idx < slots.length) {
            if (slots[idx] == null) {
                slots[idx] = new LytSlot(stacks);
                configureSlot(slots[idx]);
                append(slots[idx]);
            }
        }
    }

    public boolean isRenderEmptySlots() {
        return renderEmptySlots;
    }

    public void setRenderEmptySlots(boolean renderEmptySlots) {
        this.renderEmptySlots = renderEmptySlots;
    }

    public boolean isRenderSlotBackground() {
        return renderSlotBackground;
    }

    public void setRenderSlotBackground(boolean renderSlotBackground) {
        this.renderSlotBackground = renderSlotBackground;
        for (LytSlot slot : slots) {
            if (slot != null) {
                configureSlot(slot);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Nullable
    public LytSlot getSlot(int x, int y) {
        int idx = y * width + x;
        if (idx >= 0 && idx < slots.length) {
            return slots[idx];
        }
        return null;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (renderEmptySlots) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == null) {
                    slots[i] = new LytSlot((ItemStack) null);
                    configureSlot(slots[i]);
                    append(slots[i]);
                }
            }
        }
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < this.width; col++) {
                int idx = row * this.width + col;
                if (slots[idx] != null) {
                    slots[idx]
                        .layout(context, x + col * LytSlot.OUTER_SIZE, y + row * LytSlot.OUTER_SIZE, availableWidth);
                }
            }
        }
        return new LytRect(x, y, this.width * LytSlot.OUTER_SIZE, height * LytSlot.OUTER_SIZE);
    }

    private void configureSlot(LytSlot slot) {
        slot.setRenderSlotBackground(renderSlotBackground);
    }
}
