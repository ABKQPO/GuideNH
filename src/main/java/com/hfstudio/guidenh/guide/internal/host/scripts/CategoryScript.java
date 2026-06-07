package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.tags.mediawiki.CategoryCompiler.CategoryPlaceholder;
import com.hfstudio.guidenh.guide.compiler.tags.mediawiki.MediaWikiTagCompilerSupport;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageListBuilder;

public class CategoryScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Category";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        CategoryPlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, CategoryPlaceholder.class);
        if (ph == null) return;

        if (!(ctx.getPageCollection() instanceof Guide guide)) {
            ctx.replace(LytParagraph.error(GuidebookText.MediaWikiNoDataAvailable.text()));
            return;
        }

        CategoryIndex index = ctx.getIndex(CategoryIndex.class);
        if (index == null) {
            ctx.replace(LytParagraph.error(GuidebookText.MediaWikiNoDataAvailable.text()));
            return;
        }

        var context = MediaWikiTagCompilerSupport.createListContext(guide, index);
        var entries = MediaWikiPageListBuilder.buildCategoryMembers(context, ph.name);
        var block = MediaWikiTagCompilerSupport
            .createBlock(entries, ph.rows, GuidebookText.MediaWikiNoPagesInCategory.text());
        ctx.replace(block);
    }
}
