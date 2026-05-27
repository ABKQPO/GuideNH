package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.LinkParser;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.indices.OreIndex;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemLink");
    }

    @Override
    public void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var itemAndId = MdxAttrs.getRequiredItemStackAndId(compiler, parent, el);
        if (itemAndId == null) {
            return;
        }
        var stack = itemAndId.getRight();

        // showTooltip — default true for ItemLink
        Boolean noTooltipAttr = MdxAttrs.getOptionalBoolean(el, "noTooltip");
        boolean noTooltip = MdxAttrs.getBoolean(noTooltipAttr, false);
        Boolean showTooltipAttr = MdxAttrs.getOptionalBoolean(el, "showTooltip");
        boolean showTooltip = showTooltipAttr != null ? showTooltipAttr : !noTooltip;

        // showIcon — null/falsy = no icon; "left", "right", or any truthy = icon at that side
        String showIconRaw = el.getAttributeString("showIcon", null);
        String iconPosition = ItemImageCompiler.resolveLabelPosition(showIconRaw);

        // Manual link target override: linksTo="page.md#heading" or "#heading"
        PageAnchor linksTo = null;
        String linksToAttr = el.getAttributeString("linksTo", null);
        if (linksToAttr != null) {
            PageAnchor[] holder = new PageAnchor[1];
            LinkParser.parseLink(compiler, linksToAttr, new LinkParser.Visitor() {

                @Override
                public void handlePage(PageAnchor page) {
                    holder[0] = page;
                }

                @Override
                public void handleError(String error) {
                    parent.appendError(compiler, error, el);
                }
            });
            linksTo = holder[0];
        } else {
            var itemAnchor = compiler.getIndex(ItemIndex.class)
                .findByStack(stack);
            linksTo = itemAnchor != null ? itemAnchor
                : compiler.getIndex(OreIndex.class)
                    .findByStack(stack);
        }

        // Build icon inline block if requested.
        LytFlowInlineBlock iconBlock = null;
        if (iconPosition != null) {
            var img = new LytItemImage(stack);
            img.setScale(1f);
            img.setInline(true);
            img.setShowTooltip(showTooltip);
            iconBlock = new LytFlowInlineBlock();
            iconBlock.setBlock(img);
        }

        // If the item link is already on the page we're linking to, or no page exists,
        // render as an underlined tooltip span instead of a clickable link.
        if (linksTo == null || linksTo.anchor() == null && compiler.getPageId()
            .equals(linksTo.pageId())) {
            var span = new LytTooltipSpan();
            span.modifyStyle(style -> style.italic(true));
            span.appendText(stack.getDisplayName());
            if (showTooltip) {
                span.setTooltip(new ItemTooltip(stack));
            }
            if ("left".equals(iconPosition)) {
                parent.append(iconBlock);
            }
            parent.append(span);
            if ("right".equals(iconPosition)) {
                parent.append(iconBlock);
            }
        } else {
            var link = new LytFlowLink();
            link.setPageLink(linksTo);
            link.appendText(stack.getDisplayName());
            if (showTooltip) {
                link.setTooltip(new ItemTooltip(stack));
            }
            if ("left".equals(iconPosition)) {
                parent.append(iconBlock);
            }
            parent.append(link);
            if ("right".equals(iconPosition)) {
                parent.append(iconBlock);
            }
        }
    }

}
