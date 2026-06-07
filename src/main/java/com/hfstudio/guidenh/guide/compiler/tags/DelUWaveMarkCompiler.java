package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.LinkedHashSet;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class DelUWaveMarkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        var tags = new LinkedHashSet<String>();
        tags.add("del");
        tags.add("u");
        tags.add("wavy");
        tags.add("dotted");
        return tags;
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var span = new LytFlowSpan();
        String name = el.name();
        if (name != null) {
            switch (name) {
                case "del" -> span.modifyStyle(s -> s.strikethrough(true));
                case "u" -> span.modifyStyle(s -> s.underlined(true));
                case "wavy" -> span.modifyStyle(s -> s.wavyUnderline(true));
                case "dotted" -> span.modifyStyle(s -> s.dottedUnderline(true));
                default -> {}
            }
        }
        compiler.compileFlowContext(el.children(), span);
        parent.append(span);
    }
}
