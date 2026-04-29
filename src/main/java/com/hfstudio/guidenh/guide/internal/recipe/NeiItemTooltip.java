package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;

/**
 * An {@link ItemTooltip} that lets the NEI handler contribute extra lines via
 * {@code IRecipeHandler.handleItemTooltip}. The GuideScreen renderer checks for this subtype and
 * appends {@link #appendExtraLines(List)} output after the vanilla tooltip lines.
 */
public class NeiItemTooltip extends ItemTooltip implements ItemTooltipAppender {

    private final Object handler;
    private final int recipeIndex;

    public NeiItemTooltip(ItemStack stack, Object handler, int recipeIndex) {
        super(stack);
        this.handler = handler;
        this.recipeIndex = recipeIndex;
    }

    /** Append handler-specific lines to {@code base} using a fresh temp list as NEI expects. */
    public void appendExtraLines(List<String> base) {
        if (base == null) return;
        List<String> temp = new ArrayList<>();
        NeiRecipeLookup.appendItemTooltip(handler, getStack(), temp, recipeIndex);
        for (String line : temp) {
            if (line != null && !line.isEmpty() && !base.contains(line)) base.add(line);
        }
    }

    @Override
    public void appendTooltipLines(List<String> lines) {
        appendExtraLines(lines);
    }
}
