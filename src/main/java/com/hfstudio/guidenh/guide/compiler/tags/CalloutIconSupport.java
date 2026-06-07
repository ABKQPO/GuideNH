package com.hfstudio.guidenh.guide.compiler.tags;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;

public class CalloutIconSupport {

    private static final int INLINE_PNG_SIZE = 8;
    private static final float INLINE_ITEM_SCALE = 0.75f;

    private CalloutIconSupport() {}

    public static @Nullable LytFlowContent buildFlowIcon(PageCompiler compiler, @Nullable QuoteIconSpec icon) {
        if (icon == null || icon.value() == null
            || icon.value()
                .trim()
                .isEmpty()) {
            return null;
        }
        String value = icon.value()
            .trim();
        return switch (icon.kind()) {
            case TEXT -> LytFlowText.of(value);
            case PNG -> buildPngIcon(compiler, value);
            case ITEM -> buildItemIcon(compiler, value);
        };
    }

    private static LytFlowContent buildPngIcon(PageCompiler compiler, String value) {
        LytImage image = new LytImage();
        image.setExplicitWidth(INLINE_PNG_SIZE);
        image.setExplicitHeight(INLINE_PNG_SIZE);
        try {
            ResourceLocation imageId = IdUtils.resolveLink(value, compiler.getPageId());
            image.setImage(imageId, compiler.loadAsset(imageId));
        } catch (IllegalArgumentException ignored) {
            image.setTitle("Invalid image: " + value);
        }
        return LytFlowInlineBlock.of(image);
    }

    private static LytFlowContent buildItemIcon(PageCompiler compiler, String value) {
        ItemStack stack = GuideDisplayItemStacks.resolveItemStack(
            value,
            compiler.getPageId()
                .getResourceDomain());
        if (stack == null) {
            return LytFlowText.of(value);
        }
        LytItemImage image = new LytItemImage(stack);
        image.setInline(true);
        image.setScale(INLINE_ITEM_SCALE);
        return LytFlowInlineBlock.of(image);
    }
}
