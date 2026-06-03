package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytTaskListItem;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownListSemantics;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ListItemCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("li");
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytListItem listItem;
        var taskMarker = MarkdownListSemantics.extractTaskMarker((List) el.children());
        if (taskMarker != null) {
            LytTaskListItem taskItem = new LytTaskListItem();
            taskItem.setChecked(taskMarker.checked());
            listItem = taskItem;
        } else {
            listItem = new LytListItem();
        }
        compiler.compileBlockContext(el.children(), listItem);

        // Normalize first child margins
        var children = listItem.getChildren();
        if (!children.isEmpty()) {
            var firstChild = children.get(0);
            if (firstChild instanceof LytBlock) {
                ((LytBlock) firstChild).setMarginTop(0);
                ((LytBlock) firstChild).setMarginBottom(0);
            }
        }
        parent.append(listItem);
    }
}
