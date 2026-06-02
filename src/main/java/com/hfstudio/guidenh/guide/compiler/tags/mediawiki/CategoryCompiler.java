package com.hfstudio.guidenh.guide.compiler.tags.mediawiki;

import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
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

        var block = new CategoryPlaceholder(
            categoryName.trim(),
            MediaWikiTagCompilerSupport.readRows(el),
            guide.getId());
        parent.append(block);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        // NB: Phase 2 indexed category member titles resolved by
        // MediaWikiPageListBuilder.buildCategoryMembers(). Phase 3 defers member resolution to
        // CategoryScript (MOUNT time), so index() only indexes the category name string.
        // Full indexing requires a post-mount indexing pass (TBD).
        String categoryName = el.getAttributeString("name", null);
        if (categoryName != null && !categoryName.trim()
            .isEmpty()) {
            sink.appendText(el, categoryName.trim());
            sink.appendBreak();
        }
    }

    public static class CategoryPlaceholder extends LytParagraph {
        public final String name;
        public final int rows;
        public final ResourceLocation guideId;

        CategoryPlaceholder(String name, int rows, ResourceLocation guideId) {
            this.name = name;
            this.rows = rows;
            this.guideId = guideId;
            setStyleClass("Category");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[Category]");
        }
    }
}
