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

public class SpecialCompiler extends BlockTagCompiler {

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

        var guide = MediaWikiTagCompilerSupport.resolveGuide(compiler, parent, el);
        if (guide == null) {
            return;
        }

        var block = new SpecialPlaceholder(
            rawSpecialName.trim(),
            MediaWikiTagCompilerSupport.readRows(el),
            guide.getId(),
            el.getAttributeString("page", null),
            el.getAttributeString("prefix", null),
            el.getAttributeString("language", null),
            el.getAttributeString("query", null));
        parent.append(block);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        // NB: Phase 2 indexed special page result entries resolved by
        // MediaWikiSpecialPageResolver.resolve(). Phase 3 defers resolution to
        // SpecialScript (MOUNT time), so index() only indexes the special page name string.
        // Full indexing requires a post-mount indexing pass (TBD).
        String specialName = el.getAttributeString("name", null);
        if (specialName != null && !specialName.trim()
            .isEmpty()) {
            sink.appendText(el, specialName.trim());
            sink.appendBreak();
        }
    }

    public static class SpecialPlaceholder extends LytParagraph {
        public final String name;
        public final int rows;
        public final ResourceLocation guideId;
        public final String page;
        public final String prefix;
        public final String language;
        public final String query;

        SpecialPlaceholder(String name, int rows, ResourceLocation guideId, String page, String prefix,
            String language, String query) {
            this.name = name;
            this.rows = rows;
            this.guideId = guideId;
            this.page = page;
            this.prefix = prefix;
            this.language = language;
            this.query = query;
            setStyleClass("Special");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[Special]");
        }
    }
}
