package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.List;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.mediawiki.CategoryCompiler.CategoryPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageTitleResolver;

public class CategoryScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "Category"; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        CategoryPlaceholder ph;
        boolean isWrapped = node instanceof LytFlowInlineBlock w
            && w.getBlock() instanceof CategoryPlaceholder p;
        if (isWrapped) {
            ph = (CategoryPlaceholder) ((LytFlowInlineBlock) node).getBlock();
        } else if (node instanceof CategoryPlaceholder p) {
            ph = p;
        } else {
            return;
        }

        CategoryIndex index = ctx.getIndex(CategoryIndex.class);
        if (index == null) {
            ctx.replace(LytParagraph.error(GuidebookText.MediaWikiNoDataAvailable.text()));
            return;
        }

        List<PageAnchor> members = index.get(ph.name);
        if (members.isEmpty()) {
            ctx.replace(LytParagraph.error(GuidebookText.MediaWikiNoPagesInCategory.text()));
            return;
        }

        LytVBox box = new LytVBox();
        box.setGap(2);
        int count = 0;
        for (PageAnchor anchor : members) {
            if (ph.rows > 0 && count >= ph.rows) break;
            LytParagraph line = new LytParagraph();
            LytFlowLink link = new LytFlowLink();
            link.setGuideLink(ph.guideId, anchor);
            if (ctx.getPageCollection() instanceof Guide guide) {
                ParsedGuidePage page = guide.getParsedPage(anchor.pageId());
                if (page != null) {
                    link.appendText(MediaWikiPageTitleResolver.resolvePageTitle(guide, page));
                } else {
                    link.appendText(anchor.pageId().getResourcePath());
                }
            } else {
                link.appendText(anchor.pageId().getResourcePath());
            }
            line.append(link);
            box.append(line);
            count++;
        }
        ctx.replace(box);
    }
}
