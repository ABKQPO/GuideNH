package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class CategoryIndexCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("CategoryIndex");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {

        var category = el.getAttributeString("category", null);
        if (category == null) {
            parent.appendError(compiler, "Missing category", el);
            return;
        }

        var categories = compiler.getIndex(CategoryIndex.class)
            .get(category);

        var list = new LytList(false, 0);
        for (var pageAnchor : categories) {
            var page = compiler.getPageCollection()
                .getParsedPage(pageAnchor.pageId());

            var listItem = new LytListItem();
            var listItemPar = new LytParagraph();
            if (page == null) {
                listItemPar.appendText("Unknown page id: " + pageAnchor.pageId());
            } else {
                var link = new LytFlowLink();
                link.setClickCallback(guideScreen -> guideScreen.navigateTo(pageAnchor));
                link.appendText(
                    page.getFrontmatter()
                        .navigationEntry()
                        .title());
                listItemPar.append(link);
            }
            listItem.append(listItemPar);
            list.append(listItem);
        }
        parent.append(list);
    }
}
