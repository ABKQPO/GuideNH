package com.hfstudio.guidenh.guide.internal.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;

public final class GuideItemTooltipLines {

    private GuideItemTooltipLines() {}

    public static List<String> build(ItemTooltip tooltip, Minecraft mc) {
        ItemStack stack = tooltip.getStack();
        List<String> lines;
        try {
            lines = new ArrayList<>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        } catch (Throwable t) {
            lines = new ArrayList<>();
        }

        if (lines.isEmpty()) {
            lines.add(stack.getDisplayName());
        } else if (lines.get(0) == null || lines.get(0).isEmpty()) {
            lines.set(0, stack.getDisplayName());
        }

        var rarity = stack.getRarity();
        if (!lines.isEmpty() && rarity != null) {
            lines.set(0, rarity.rarityColor.toString() + lines.get(0));
        }
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            lines.set(i, line == null ? "" : EnumChatFormatting.GRAY + line);
        }

        if (tooltip instanceof ItemTooltipAppender appender) {
            appender.appendTooltipLines(lines);
        }
        return lines;
    }
}
