package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class MarkTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("mark");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        String color = MdxAttrs.getString(compiler, parent, el, "color", null);
        int background = color != null ? ChartAttrParser.parseColor(color, PageCompiler.DEFAULT_MARK_BACKGROUND_COLOR)
            : PageCompiler.DEFAULT_MARK_BACKGROUND_COLOR;
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(style -> style.backgroundColor(new ConstantColor(background)));
        compiler.compileFlowContext(el.children(), span);
        parent.append(span);
    }
}
