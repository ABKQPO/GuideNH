package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
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
        String oreName = GuideItemReferenceResolver.trimToNull(MdxAttrs.getString(compiler, parent, el, "ore", null));
        var id = itemAndId.getLeft();
        var stack = itemAndId.getRight();

        var linksTo = compiler.getIndex(ItemIndex.class)
            .findByStack(stack);
        // We'll error out for item-links to our own mod because we expect them to have a page
        // while we don't have pages for Vanilla items or items from other mods.
        if (linksTo == null && oreName == null
            && id.getResourceDomain()
                .equals(
                    compiler.getPageId()
                        .getResourceDomain())) {
            parent.append(compiler.createErrorFlowContent("No page found for item " + id, el));
            return;
        }

        // If the item link is already on the page we're linking to, replace it with an underlined
        // text that has a tooltip.
        if (linksTo == null || linksTo.anchor() == null && compiler.getPageId()
            .equals(linksTo.pageId())) {
            var span = new LytTooltipSpan();
            span.modifyStyle(style -> style.italic(true));
            span.appendText(stack.getDisplayName());
            span.setTooltip(new ItemTooltip(stack));
            parent.append(span);
        } else {
            var link = new LytFlowLink();
            link.setPageLink(linksTo);
            link.appendText(stack.getDisplayName());
            link.setTooltip(new ItemTooltip(stack));
            parent.append(link);
        }
    }

}
