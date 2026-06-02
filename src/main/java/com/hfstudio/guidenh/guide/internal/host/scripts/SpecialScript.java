package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.tags.mediawiki.MediaWikiTagCompilerSupport;
import com.hfstudio.guidenh.guide.compiler.tags.mediawiki.SpecialCompiler.SpecialPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageQuery;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResolver;

public class SpecialScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "Special"; }

    @Override
    public boolean isAsync() { return true; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof SpecialPlaceholder ph)) return;

        PageCollection pc = ctx.getPageCollection();
        if (!(pc instanceof Guide guide)) {
            ctx.replace(LytParagraph.error("[Special] Special page not available: collection is not a guide"));
            return;
        }

        CategoryIndex categoryIndex = ctx.getIndex(CategoryIndex.class);

        var resolver = new MediaWikiSpecialPageResolver();
        String specialName = resolver.normalizeSupportedName(ph.name);
        if (specialName == null) {
            ctx.replace(LytParagraph.error("[Special] Unsupported special page: " + ph.name));
            return;
        }

        MediaWikiListContext context = MediaWikiTagCompilerSupport.createListContext(guide, categoryIndex);
        MediaWikiSpecialPageQuery query = new MediaWikiSpecialPageQuery("",
            MediaWikiSpecialPageQuery.PAGE_SIZE);
        if (ph.page != null) query = query.withParameter("page", ph.page);
        if (ph.prefix != null) query = query.withParameter("prefix", ph.prefix);
        if (ph.language != null) query = query.withParameter("language", ph.language);
        if (ph.query != null) query = query.withSearchText(ph.query);

        var result = resolver.resolve(context, specialName,
            query.withVisibleCount(Integer.MAX_VALUE));
        var block = MediaWikiTagCompilerSupport.createSpecialBlock(
            result, ph.rows, context, query, resolver);
        ctx.replace(block);
        ctx.markComplete();
    }
}
