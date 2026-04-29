package com.hfstudio.guidenh.guide.internal.tooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;

public class AppendedItemTooltip extends ItemTooltip implements ItemTooltipAppender {

    private final List<String> extraLines;

    public AppendedItemTooltip(ItemStack stack, List<String> extraLines) {
        super(stack);
        if (extraLines == null || extraLines.isEmpty()) {
            this.extraLines = Collections.emptyList();
        } else {
            this.extraLines = Collections.unmodifiableList(new ArrayList<>(extraLines));
        }
    }

    @Override
    public void appendTooltipLines(List<String> lines) {
        if (lines == null || extraLines.isEmpty()) {
            return;
        }
        for (String line : extraLines) {
            if (line != null && !line.isEmpty() && !lines.contains(line)) {
                lines.add(line);
            }
        }
    }
}
