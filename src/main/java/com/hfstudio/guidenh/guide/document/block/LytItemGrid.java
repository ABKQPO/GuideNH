package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

public class LytItemGrid extends LytBox {

    private final List<LytSlot> slots = new ArrayList<>();

    public LytItemGrid() {
        setPadding(5);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        var cols = Math.max(1, availableWidth / LytSlot.OUTER_SIZE);
        var rows = (slots.size() + cols - 1) / cols;

        for (int i = 0; i < slots.size(); i++) {
            var slotX = i % cols;
            var slotY = i / cols;
            slots.get(i)
                .layout(context, x + slotX * LytSlot.OUTER_SIZE, y + slotY * LytSlot.OUTER_SIZE, availableWidth);
        }

        return new LytRect(x, y, cols * LytSlot.OUTER_SIZE, rows * LytSlot.OUTER_SIZE);
    }

    public void addItem(Item item) {
        addItem(new ItemStack(item));
    }

    public void addItem(ItemStack stack) {
        var slot = new LytSlot(stack);
        slots.add(slot);
        append(slot);
    }

    public void addItems(List<ItemStack> stacks) {
        var slot = new LytSlot(stacks);
        slots.add(slot);
        append(slot);
    }
}
