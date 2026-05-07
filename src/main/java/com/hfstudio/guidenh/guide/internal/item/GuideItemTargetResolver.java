package com.hfstudio.guidenh.guide.internal.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.indices.ItemMultiIndex;
import com.hfstudio.guidenh.guide.indices.OreIndex;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.search.GuideItemLinksPage;

public class GuideItemTargetResolver {

    @Nullable
    public static GuideOpenTarget resolve(@Nullable ItemStack stack, Iterable<MutableGuide> guides) {
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
                if (anchor == null) {
                    anchor = guide.getIndex(OreIndex.class)
                        .findByStack(stack);
                }
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (anchor != null) {
                var allPages = guide.getIndex(ItemMultiIndex.class)
                    .findAllByStack(stack);
                PageAnchor target = allPages.size() > 1 ? GuideItemLinksPage.anchorForStack(stack) : anchor;
                return new GuideOpenTarget(guide.getId(), target);
            }
        }

        return null;
    }

    @Desugar
    public record GuideOpenTarget(ResourceLocation guideId, @Nullable PageAnchor anchor) {}
}
