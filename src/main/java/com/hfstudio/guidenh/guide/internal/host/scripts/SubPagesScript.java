package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.tags.SubPagesCompiler.SubPagesPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class SubPagesScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "SubPages"; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        SubPagesPlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, SubPagesPlaceholder.class);
        if (ph == null) return;

        NavigationTree tree = GuideRegistry.getMergedNavigationTree();

        List<NavigationNode> subNodes;
        if (ph.pageIdStr == null) {
            ResourceLocation currentPageId = new ResourceLocation(ph.currentPageId);
            NavigationNode current = tree.getNodeById(currentPageId);
            subNodes = current != null ? new ArrayList<>(current.children()) : tree.getRootNodes();
        } else if (ph.pageIdStr.isEmpty()) {
            subNodes = tree.getRootNodes();
        } else {
            ResourceLocation pageId = new ResourceLocation(ph.pageIdStr);
            NavigationNode navNode = tree.getNodeById(pageId);
            if (navNode == null) {
                ctx.replace(LytParagraph.error("[SubPages] Page not found in navigation: " + ph.pageIdStr));
                return;
            }
            subNodes = navNode.children();
        }

        if (ph.alphabetical) {
            subNodes = new ArrayList<>(subNodes);
            subNodes.sort(Comparator.comparing(NavigationNode::title));
        }

        if (subNodes.isEmpty()) {
            ctx.replace(LytParagraph.error("[SubPages] No sub-pages found"));
            return;
        }

        LytList list = new LytList(false, 0);
        for (NavigationNode childNode : subNodes) {
            if (!childNode.hasPage()) continue;

            LytListItem listItem = new LytListItem();
            LytParagraph listItemPar = new LytParagraph();
            LytFlowLink link = new LytFlowLink();
            link.setGuideLink(childNode.guideId(), PageAnchor.page(childNode.pageId()));
            link.appendText(childNode.title());
            listItemPar.append(link);
            listItem.append(listItemPar);
            list.append(listItem);
        }
        ctx.replace(list);
    }
}
