package com.hfstudio.guidenh.guide.internal.item;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

public class GuideItemTargetResolver {

    private GuideItemTargetResolver() {}

    @Nullable
    static GuideOpenTarget resolve(@Nullable ItemStack stack, Iterable<MutableGuide> guides) {
        var guideId = GuideItem.getGuideId(stack);
        if (guideId != null) {
            return new GuideOpenTarget(guideId, null);
        }

        if (stack == null || stack.getItem() == null) {
            return null;
        }

        for (var guide : guides) {
            PageAnchor anchor;
            try {
                anchor = guide.getIndex(ItemIndex.class)
                    .findByStack(stack);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (anchor != null) {
                return new GuideOpenTarget(guide.getId(), anchor);
            }
        }

        return null;
    }

    @Desugar
    record GuideOpenTarget(ResourceLocation guideId, @Nullable PageAnchor anchor) {}
}
