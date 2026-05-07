package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytCyclingItemImage extends LytItemImage {

    private final List<ItemStack> stacks;

    private long cachedSecond = -1;
    private int cachedIdx = 0;

    public LytCyclingItemImage(List<ItemStack> stacks) {
        super(stacks.get(0));
        this.stacks = stacks;
    }

    private ItemStack currentStack() {
        long second = System.currentTimeMillis() / 1000L;
        if (second != cachedSecond) {
            cachedSecond = second;
            cachedIdx = (int) (second % stacks.size());
        }
        return stacks.get(cachedIdx);
    }

    @Override
    public void render(RenderContext context) {
        this.stack = currentStack();
        super.render(context);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        this.stack = currentStack();
        return super.getTooltip(x, y);
    }
}
