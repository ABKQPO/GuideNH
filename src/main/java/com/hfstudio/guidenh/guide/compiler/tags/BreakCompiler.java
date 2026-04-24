package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BreakCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("br");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var br = new LytFlowBreak();
        var clear = el.getAttributeString("clear", "none");
        switch (clear) {
            case "left" -> br.setClearLeft(true);
            case "right" -> br.setClearRight(true);
            case "all" -> {
                br.setClearLeft(true);
                br.setClearRight(true);
            }
            case "none" -> {}
            default -> parent.append(compiler.createErrorFlowContent("Invalid 'clear' attribute", el));
        }

        parent.append(br);
    }
}
