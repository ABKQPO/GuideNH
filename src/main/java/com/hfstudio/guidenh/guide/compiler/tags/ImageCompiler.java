package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ImageCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("img");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        LytImageBlock block = new LytImageBlock();
        block.setStyleClass("Img");

        String src = el.getAttributeString("src", "");
        if (!src.isEmpty()) {
            var imageId = compiler.resolveId(src);
            if (imageId != null) {
                block.setSrc(imageId.toString());
            }
        }

        String alt = el.getAttributeString("alt", "");
        String title = el.getAttributeString("title", "");
        if (!alt.isEmpty()) block.setAlt(alt);
        if (!title.isEmpty()) block.setTitle(title);

        block.appendText("Loading image...");
        block.setStyle(LytParagraph.LOADING_STYLE);

        var inlineBlock = new LytFlowInlineBlock();
        inlineBlock.setBlock(block);
        parent.append(inlineBlock);
    }
}
