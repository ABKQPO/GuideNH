package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SubPagesCompiler extends BlockTagCompiler {

    public static final Comparator<NavigationNode> ALPHABETICAL_COMPARATOR = Comparator
        .comparing(NavigationNode::title);

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("SubPages");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var pageIdStr = el.getAttributeString("id", null);
        var alphabetical = MdxAttrs.getBoolean(compiler, parent, el, "alphabetical", false);

        var navigationTree = compiler.getPageCollection()
            .getNavigationTree();

        List<NavigationNode> subNodes;
        if ("".equals(pageIdStr)) {
            subNodes = navigationTree.getRootNodes();
        } else {
            ResourceLocation pageId;
            try {
                pageId = pageIdStr == null ? compiler.getPageId() : compiler.resolveId(pageIdStr);
            } catch (Exception e) {
                parent.appendError(compiler, "Invalid id", el);
                return;
            }

            var node = navigationTree.getNodeById(pageId);
            if (node == null) {
                parent.appendError(compiler, "Couldn't find page " + pageId + " in the navigation tree", el);
                return;
            }

            subNodes = node.children();
        }

        if (alphabetical) {
            subNodes = new ArrayList<>(subNodes);
            subNodes.sort(ALPHABETICAL_COMPARATOR);
        }

        var list = new LytList(false, 0);
        for (var childNode : subNodes) {
            if (!childNode.hasPage()) {
                continue;
            }

            var listItem = new LytListItem();
            var listItemPar = new LytParagraph();

            var link = new LytFlowLink();
            link.setPageLink(PageAnchor.page(childNode.pageId()));
            link.appendText(childNode.title());
            listItemPar.append(link);

            listItem.append(listItemPar);
            list.append(listItem);
        }
        parent.append(list);
    }
}
