package com.hfstudio.guidenh.guide.compiler.tags.mediawiki;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageListBuilder;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class CategoryCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Category");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String categoryName = el.getAttributeString("name", null);
        if (categoryName == null || categoryName.trim()
            .isEmpty()) {
            parent.appendError(compiler, GuidebookText.MediaWikiMissingCategoryName.text(), el);
            return;
        }

        var guide = MediaWikiTagCompilerSupport.resolveGuide(compiler, parent, el);
        if (guide == null) {
            return;
        }

        var context = MediaWikiTagCompilerSupport.createListContext(guide, compiler.getIndex(CategoryIndex.class));
        List<MediaWikiListEntry> entries = MediaWikiPageListBuilder.buildCategoryMembers(context, categoryName.trim());
        parent.append(
            MediaWikiTagCompilerSupport.createBlock(
                entries,
                MediaWikiTagCompilerSupport.readRows(el),
                GuidebookText.MediaWikiNoPagesInCategory.text()));
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        String categoryName = el.getAttributeString("name", null);
        if (categoryName == null || categoryName.trim()
            .isEmpty()) {
            return;
        }

        var guide = MediaWikiTagCompilerSupport.resolveGuide(indexer);
        if (guide == null) {
            return;
        }

        var context = MediaWikiTagCompilerSupport.createListContext(guide, indexer.getIndex(CategoryIndex.class));
        sink.appendText(el, categoryName);
        sink.appendBreak();
        MediaWikiTagCompilerSupport
            .indexEntries(sink, el, MediaWikiPageListBuilder.buildCategoryMembers(context, categoryName.trim()));
    }
}
