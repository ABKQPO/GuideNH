package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.SymbolicColorResolver;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ColorTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Color");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var id = MdxAttrs.getString(compiler, parent, el, "id", null);
        ColorValue color;
        if (id != null) {
            color = SymbolicColorResolver.resolve(compiler, id);
            if (color == null) {
                parent.appendError(compiler, "Cannot resolve symbolic color", el);
                return;
            }
        } else if (el.hasAttribute("color")) {
            color = MdxAttrs.getColor(compiler, parent, el, "color", null);
            if (color == null) {
                parent.appendError(compiler, "Malformed color value", el);
                return;
            }
        } else {
            parent.appendError(compiler, "Must either specify 'id' or 'color' attribute", el);
            return;
        }

        var span = new LytFlowSpan();
        span.setStyle(
            TextStyle.builder()
                .color(color)
                .build());
        parent.append(span);
        compiler.compileFlowContext(el.children(), span);
    }
}
