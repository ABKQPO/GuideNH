package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SubPagesCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("SubPages");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var pageIdStr = el.getAttributeString("id", null);
        var alphabetical = MdxAttrs.getBoolean(compiler, parent, el, "alphabetical", false);
        var currentPageId = compiler.getPageId().toString();

        SubPagesPlaceholder placeholder = new SubPagesPlaceholder(pageIdStr, alphabetical, currentPageId);
        parent.append(placeholder);
    }

    public static class SubPagesPlaceholder extends LytParagraph {
        public final String pageIdStr;
        public final boolean alphabetical;
        public final String currentPageId;

        public SubPagesPlaceholder(String pageIdStr, boolean alphabetical, String currentPageId) {
            this.pageIdStr = pageIdStr;
            this.alphabetical = alphabetical;
            this.currentPageId = currentPageId;
            setStyleClass("SubPages");
        }
    }
}
