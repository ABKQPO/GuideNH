package com.hfstudio.guidenh.guide.internal.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;
import com.hfstudio.guidenh.integration.Mods;

public class GuideItemTooltipLines {

    protected GuideItemTooltipLines() {}

    private static String safeDisplayName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (Throwable t) {
            return stack.getItem()
                .getClass()
                .getSimpleName();
        }
    }

    public static List<String> build(ItemTooltip tooltip, Minecraft mc) {
        ItemStack stack = tooltip.getStack();
        List<String> rawLines;
        try {
            rawLines = new ArrayList<>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        } catch (Throwable t) {
            rawLines = new ArrayList<>();
        }

        List<String> lines = new ArrayList<>(rawLines.size());
        for (String rawLine : rawLines) {
            if (Mods.ChromaticTooltips.isModLoaded()) {
                lines.addAll(GuideChromaticTooltipCompat.expandLine(rawLine));
            } else {
                lines.add(rawLine);
            }
        }

        if (lines.isEmpty()) {
            lines.add(safeDisplayName(stack));
        } else if (lines.get(0) == null || lines.get(0)
            .isEmpty()) {
                lines.set(0, safeDisplayName(stack));
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
