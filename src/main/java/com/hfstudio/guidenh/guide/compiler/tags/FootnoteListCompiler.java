package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytWidthBox;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class FootnoteListCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("FootnoteList");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytWidthBox box = new LytWidthBox();
        box.setPreferredWidth(MdxAttrs.getInt(compiler, parent, el, "width", 220));
        compiler.compileBlockTagChildren(el, box);
        parent.append(box);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        indexer.indexContent(el.children(), sink);
    }
}
