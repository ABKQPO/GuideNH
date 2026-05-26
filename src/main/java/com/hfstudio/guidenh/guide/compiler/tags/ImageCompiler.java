package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytImage;
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
        LytImage image = new LytImage();
        String src = el.getAttributeString("src", "");
        ResourceLocation imageId = compiler.resolveId(src);
        if (imageId != null) {
            byte[] imageContent = compiler.loadAsset(imageId);
            if (imageContent != null) {
                image.setImage(imageId, imageContent);
            }
        }
        String alt = el.getAttributeString("alt", "");
        String title = el.getAttributeString("title", "");
        if (!alt.isEmpty()) image.setAlt(alt);
        if (!title.isEmpty()) image.setTitle(title);

        var inlineBlock = new LytFlowInlineBlock();
        inlineBlock.setBlock(image);
        parent.append(inlineBlock);
    }
}
