package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        // Extract raw attributes (no registry lookups)
        String itemId = MdxAttrs.getString(compiler, parent, el, "id", null);
        String ore = MdxAttrs.getString(compiler, parent, el, "ore", null);
        if (itemId == null && ore == null) {
            parent.appendError(compiler, "Missing id or ore attribute.", el);
            return;
        }

        // showTooltip — default true for ItemLink
        Boolean noTooltipAttr = MdxAttrs.getOptionalBoolean(el, "noTooltip");
        boolean noTooltip = MdxAttrs.getBoolean(noTooltipAttr, false);
        Boolean showTooltipAttr = MdxAttrs.getOptionalBoolean(el, "showTooltip");
        boolean showTooltip = showTooltipAttr != null ? showTooltipAttr : !noTooltip;
        Boolean showTextAttr = MdxAttrs.getOptionalBoolean(el, "showText");
        boolean showText = showTextAttr == null || showTextAttr;

        // showIcon — null/falsy = no icon; "left", "right", or any truthy = icon at that side
        String showIconRaw = el.getAttributeString("showIcon", null);
        String iconPosition = ItemImageCompiler.resolveLabelPosition(showIconRaw);

        // Manual link target override: linksTo="page.md#heading" or "#heading"
        String linksTo = el.getAttributeString("linksTo", null);

        // Create placeholder link for runtime resolution by ItemLinkScript
        var link = new LytFlowLink();
        link.setStyleClass("ItemLink");
        link.setData("itemId", itemId != null ? itemId.trim() : null);
        link.setData("ore", ore != null ? ore.trim() : null);
        link.setData("showTooltip", showTooltip);
        link.setData("showText", showText);
        link.setData("showIcon", iconPosition);
        link.setData("linksTo", linksTo);
        link.setData(
            "guideId",
            compiler.getGuideId()
                .toString());
        link.setData(
            "pageId",
            compiler.getPageId()
                .toString());

        compiler.compileFlowContext(el.children(), link);
        parent.append(link);
    }

}
