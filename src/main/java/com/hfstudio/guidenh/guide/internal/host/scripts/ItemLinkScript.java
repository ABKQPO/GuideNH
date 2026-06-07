package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.internal.item.GuideItemTargetResolver;

public class ItemLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "ItemLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowLink link) {
            String itemId = (String) link.getData("itemId");
            String ore = (String) link.getData("ore");
            Boolean showTooltip = (Boolean) link.getData("showTooltip");
            String linksTo = (String) link.getData("linksTo");
            String iconPosition = (String) link.getData("showIcon");
            String currentPage = (String) link.getData("pageId");
            String currentGuide = (String) link.getData("guideId");
            Boolean showText = (Boolean) link.getData("showText");

            // Neither target specified
            if ((itemId == null || itemId.isEmpty()) && (ore == null || ore.isEmpty())) {
                replaceFlowError(ctx, "[ItemLink] Link has no target");
                return;
            }

            ItemStack stack = GuideDisplayItemStacks.resolveItemStack(itemId, "minecraft");
            if (stack == null && ore != null && !ore.isEmpty()) {
                stack = GuideDisplayItemStacks.resolveOreStack(ore);
            }
            if (stack == null) {
                String detail = (itemId != null && !itemId.isEmpty()) ? itemId : ore;
                replaceFlowError(ctx, "[ItemLink] Link target not found: " + detail);
                return;
            }

            GuideAnchor target = findLinkTarget(stack, linksTo, currentPage, currentGuide, ctx);
            if (Boolean.TRUE.equals(showTooltip)) {
                link.setTooltip(new ItemTooltip(stack));
            }

            boolean samePageWithoutAnchor = target != null && currentPage != null
                && target.page()
                    .anchor() == null
                && target.page()
                    .pageId()
                    .toString()
                    .equals(currentPage);
            if (target == null || samePageWithoutAnchor) {
                LytTooltipSpan span = new LytTooltipSpan();
                span.setStyleClass("ItemLink");
                moveChildren(link, span);
                appendIconAndFallbackText(span, stack, iconPosition, Boolean.TRUE.equals(showTooltip), showText);
                if (Boolean.TRUE.equals(showTooltip)) {
                    span.setTooltip(new ItemTooltip(stack));
                }
                span.modifyStyle(
                    style -> style.color(SymbolicColor.GRAY)
                        .italic(true));
                ctx.replace(span);
                return;
            }

            ResourceLocation guideId = target.guideId();
            if (guideId != null) {
                link.setGuideLink(guideId, target.page());
            } else {
                link.setPageLink(target.page());
            }
            appendIconAndFallbackText(link, stack, iconPosition, Boolean.TRUE.equals(showTooltip), showText);
        }
    }

    @Nullable
    private static GuideAnchor findLinkTarget(ItemStack stack, @Nullable String linksTo, @Nullable String currentPage,
        @Nullable String currentGuide, ScriptContext ctx) {
        if (linksTo != null && !linksTo.trim()
            .isEmpty()) {
            ResourceLocation currentPageId;
            try {
                currentPageId = currentPage != null ? new ResourceLocation(currentPage) : null;
            } catch (IllegalArgumentException ignored) {
                currentPageId = null;
            }
            ResourceLocation currentGuideId;
            try {
                currentGuideId = currentGuide != null ? new ResourceLocation(currentGuide) : null;
            } catch (IllegalArgumentException ignored) {
                currentGuideId = null;
            }
            if (currentPageId == null) {
                return null;
            }
            String pagePart = linksTo.trim();
            String anchorPart = null;
            int hashIdx = pagePart.indexOf('#');
            if (hashIdx >= 0) {
                anchorPart = pagePart.substring(hashIdx + 1);
                pagePart = pagePart.substring(0, hashIdx);
            }
            try {
                ResourceLocation pageId = pagePart.isEmpty() ? currentPageId
                    : IdUtils.resolveLink(pagePart, currentPageId);
                ResourceLocation guideId = resolveGuideId(currentPageId, currentGuideId, pageId);
                if (!pageExists(guideId, pageId, ctx)) {
                    return null;
                }
                return new GuideAnchor(guideId, new PageAnchor(pageId, anchorPart));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        var resolved = GuideItemTargetResolver.resolve(stack, GuideRegistry.getAll());
        if (resolved == null || resolved.anchor() == null) {
            return null;
        }
        return new GuideAnchor(resolved.guideId(), resolved.anchor());
    }

    private static ResourceLocation resolveGuideId(ResourceLocation currentPageId,
        @Nullable ResourceLocation currentGuideId, ResourceLocation pageId) {
        if (currentGuideId != null && pageId.getResourceDomain()
            .equals(currentPageId.getResourceDomain())) {
            return currentGuideId;
        }
        return new ResourceLocation(
            pageId.getResourceDomain(),
            currentGuideId != null ? currentGuideId.getResourcePath() : "guidenh");
    }

    private static boolean pageExists(ResourceLocation guideId, ResourceLocation pageId, ScriptContext ctx) {
        PageCollection pageCollection = ctx.getPageCollection();
        if (pageCollection != null && guideId.equals(pageCollection.getId())) {
            return pageCollection.pageExists(pageId);
        }
        var guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(pageId);
    }

    private static void appendIconAndFallbackText(LytFlowSpan span, ItemStack stack, @Nullable String iconPosition,
        boolean showTooltip, @Nullable Boolean showText) {
        boolean shouldShowText = showText == null || showText;
        boolean hasText = hasTextChild(span.getChildren());
        LytFlowInlineBlock icon = iconPosition == null || iconPosition.isEmpty() ? null
            : createIcon(stack, showTooltip);
        if (icon != null && "left".equals(iconPosition)) {
            span.getChildren()
                .add(0, icon);
            icon.setParent(span);
        }
        if (shouldShowText && !hasText) {
            span.appendText(stack.getDisplayName());
        }
        if (icon != null && !"left".equals(iconPosition)) {
            span.append(icon);
        }
    }

    @Nullable
    private static LytFlowInlineBlock createIcon(ItemStack stack, boolean showTooltip) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        var img = new LytItemImage(stack);
        img.setScale(1f);
        img.setInline(true);
        img.setShowTooltip(showTooltip);
        var wrapper = new LytFlowInlineBlock();
        wrapper.setBlock(img);
        return wrapper;
    }

    private static boolean hasTextChild(List<LytFlowContent> children) {
        for (LytFlowContent child : children) {
            if (child instanceof LytFlowText text && !text.getText()
                .isEmpty()) {
                return true;
            }
            if (child instanceof LytFlowSpan span && hasTextChild(span.getChildren())) {
                return true;
            }
        }
        return false;
    }

    private static void moveChildren(LytFlowSpan from, LytFlowSpan to) {
        List<LytFlowContent> linkChildren = new ArrayList<>(from.getChildren());
        from.getChildren()
            .clear();
        for (LytFlowContent child : linkChildren) {
            child.setParent(null);
            to.append(child);
        }
    }

    private void replaceFlowError(ScriptContext ctx, String message) {
        LytFlowInlineBlock wrapper = new LytFlowInlineBlock();
        wrapper.setBlock(LytParagraph.error(message));
        ctx.replace(wrapper);
    }
}
