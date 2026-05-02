package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class KbdTagCompiler extends FlowTagCompiler {

    private static final ConstantColor KEY_COLOR = new ConstantColor(0xFFE8EDF5);

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("kbd");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(
            style -> style.bold(true)
                .color(KEY_COLOR));
        compiler.compileFlowContext(el.children(), span);
        parent.append(span);
    }
}
