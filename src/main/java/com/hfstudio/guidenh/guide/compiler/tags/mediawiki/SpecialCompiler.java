package com.hfstudio.guidenh.guide.compiler.tags.mediawiki;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageQuery;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResolver;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResult;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SpecialCompiler extends BlockTagCompiler {

    private final MediaWikiSpecialPageResolver resolver = new MediaWikiSpecialPageResolver();

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Special");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String rawSpecialName = el.getAttributeString("name", null);
        if (rawSpecialName == null || rawSpecialName.trim()
            .isEmpty()) {
            parent.appendError(compiler, GuidebookText.MediaWikiMissingSpecialPageName.text(), el);
            return;
        }
        String specialName = resolver.normalizeSupportedName(rawSpecialName);
        if (specialName == null) {
            parent.appendError(compiler, GuidebookText.MediaWikiUnsupportedSpecialPage.text(rawSpecialName), el);
            return;
        }

        var guide = MediaWikiTagCompilerSupport.resolveGuide(compiler, parent, el);
        if (guide == null) {
            return;
        }

        var context = MediaWikiTagCompilerSupport.createListContext(guide, compiler.getIndex(CategoryIndex.class));
        MediaWikiSpecialPageQuery specialQuery = MediaWikiTagCompilerSupport.readSpecialQuery(el);
        MediaWikiSpecialPageResult result = resolver
            .resolve(context, specialName, specialQuery.withVisibleCount(Integer.MAX_VALUE));
        parent.append(
            MediaWikiTagCompilerSupport
                .createSpecialBlock(result, MediaWikiTagCompilerSupport.readRows(el), context, specialQuery, resolver));
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        String specialName = resolver.normalizeSupportedName(el.getAttributeString("name", null));
        if (specialName == null) {
            return;
        }

        var guide = MediaWikiTagCompilerSupport.resolveGuide(indexer);
        if (guide == null) {
            return;
        }

        var context = MediaWikiTagCompilerSupport.createListContext(guide, indexer.getIndex(CategoryIndex.class));
        sink.appendText(el, specialName);
        sink.appendBreak();
        MediaWikiSpecialPageQuery specialQuery = MediaWikiTagCompilerSupport.readSpecialQuery(el);
        MediaWikiTagCompilerSupport.indexSpecialResult(
            sink,
            el,
            resolver.resolve(context, specialName, specialQuery.withVisibleCount(MediaWikiSpecialPageQuery.PAGE_SIZE)));
    }
}
