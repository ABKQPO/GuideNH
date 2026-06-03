package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class CodeCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("code");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var text = new LytFlowText();
        // Extract text from child — <code> has one MdAstText child from converter
        String value = "";
        if (!el.children()
            .isEmpty()
            && el.children()
                .get(0) instanceof MdAstText t) {
            value = t.value;
        }
        text.setText(value);
        text.modifyStyle(
            style -> style.italic(true)
                .whiteSpace(WhiteSpaceMode.PRE));
        parent.append(text);
    }
}
