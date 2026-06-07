package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemGridCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemGrid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        List<String> itemIds = new ArrayList<>();

        // We expect children to only contain ItemIcon elements
        for (var childNode : el.children()) {
            if (childNode instanceof MdxJsxElementFields jsxChild && "ItemIcon".equals(jsxChild.name())) {
                var itemId = MdxAttrs.getString(compiler, parent, jsxChild, "id", null);
                if (itemId != null) {
                    itemIds.add(itemId);
                }
                continue;
            }
            parent.appendError(compiler, "Unsupported child-element in ItemGrid", childNode);
        }

        ItemGridPlaceholder placeholder = new ItemGridPlaceholder(itemIds);
        parent.append(placeholder);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {}

    public static class ItemGridPlaceholder extends LytParagraph {

        public final List<String> itemIds;

        public ItemGridPlaceholder(List<String> itemIds) {
            this.itemIds = itemIds;
            setStyleClass("ItemGrid");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[ItemGrid]");
        }
    }
}
