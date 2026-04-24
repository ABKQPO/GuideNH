package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemGridCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemGrid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var itemGrid = new LytItemGrid();

        // We expect children to only contain ItemIcon elements
        for (var childNode : el.children()) {
            if (childNode instanceof MdxJsxElementFields jsxChild && "ItemIcon".equals(jsxChild.name())) {
                var stack = MdxAttrs.getRequiredItemStack(compiler, parent, jsxChild);
                if (stack != null) {
                    itemGrid.addItem(stack);
                }

                continue;
            }
            parent.appendError(compiler, "Unsupported child-element in ItemGrid", childNode);
        }

        parent.append(itemGrid);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {}
}
