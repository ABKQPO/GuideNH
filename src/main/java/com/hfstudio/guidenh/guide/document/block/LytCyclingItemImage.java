package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytCyclingItemImage extends LytItemImage {

    private final List<ItemStack> stacks;

    public LytCyclingItemImage(List<ItemStack> stacks) {
        super(stacks.get(0));
        this.stacks = stacks;
    }

    private ItemStack currentStack() {
        int idx = (int) ((System.currentTimeMillis() / 1000L) % stacks.size());
        return stacks.get(idx);
    }

    @Override
    public void render(RenderContext context) {
        this.stack = currentStack();
        super.render(context);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        this.stack = currentStack();
        return Optional.of(new ItemTooltip(this.stack));
    }
}
