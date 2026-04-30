package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SubscriptTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("sub");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(style -> style.fontScale(0.85f));
        compiler.compileFlowContext(el.children(), span);
        parent.append(span);
    }
}
