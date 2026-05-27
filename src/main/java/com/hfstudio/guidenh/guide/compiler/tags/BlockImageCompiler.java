package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockImageCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String id = MdxAttrs.getString(compiler, parent, el, "id", null);
        String ore = MdxAttrs.getString(compiler, parent, el, "ore", null);

        if ((id == null || id.trim().isEmpty()) && (ore == null || ore.trim().isEmpty())) {
            parent.appendError(compiler, "Missing id attribute (or ore).", el);
            return;
        }

        int meta = MdxAttrs.getInt(compiler, parent, el, "meta", 0);
        String nbt = MdxAttrs.getString(compiler, parent, el, "nbt", null);
        float scale = MdxAttrs.getFloat(compiler, parent, el, "scale", 1f);
        String perspective = MdxAttrs.getString(compiler, parent, el, "perspective", null);
        int width = MdxAttrs.getInt(compiler, parent, el, "width", 128);
        int height = MdxAttrs.getInt(compiler, parent, el, "height", 128);

        var placeholder = LytParagraph.of("[BlockImage]");
        placeholder.setStyleClass("BlockImage");
        parent.append(placeholder);
    }
}
