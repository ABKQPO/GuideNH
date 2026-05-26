package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ListItemCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("li");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytListItem listItem = new LytListItem();
        compiler.compileBlockContext(el.children(), listItem);

        // Fix up top/bottom margin for list item children
        var children = listItem.getChildren();
        if (!children.isEmpty()) {
            var firstChild = children.get(0);
            if (firstChild instanceof LytBlock firstBlock) {
                firstBlock.setMarginTop(0);
                firstBlock.setMarginBottom(0);
            }
        }
        parent.append(listItem);
    }
}
