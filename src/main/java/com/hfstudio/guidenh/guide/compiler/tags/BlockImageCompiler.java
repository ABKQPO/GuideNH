package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockImageCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var blockReference = MdxAttrs.getRequiredBlockReference(compiler, parent, el, "id");
        if (blockReference == null) return;

        if (blockReference.stack() == null || blockReference.stack()
            .getItem() == null) {
            parent.appendError(compiler, "No item form for block " + blockReference.registryId(), el);
            return;
        }

        float scale = MdxAttrs.getFloat(compiler, parent, el, "scale", 1f);
        var img = new LytItemImage(blockReference.stack());
        img.setScale(scale);
        parent.append(img);
    }
}
