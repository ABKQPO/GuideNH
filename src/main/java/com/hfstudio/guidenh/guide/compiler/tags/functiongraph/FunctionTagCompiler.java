package com.hfstudio.guidenh.guide.compiler.tags.functiongraph;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Shorthand for a function graph that contains a single curve. {@code <Function expr="x^2" />}
 * expands to a {@link LytFunctionGraph} with one {@link FunctionPlot} so authors do not need to
 * spell out the wrapper element when only one curve is needed.
 */
public class FunctionTagCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Function");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytFunctionGraph graph = new LytFunctionGraph();
        FunctionGraphAttrs.applyContainerAttrs(graph, compiler, parent, el);
        FunctionPlot plot = FunctionGraphAttrs.parsePlot(compiler, parent, el, 0);
        if (plot == null) {
            parent.appendError(compiler, "<Function> requires an 'expr' attribute", el);
            return;
        }
        graph.addPlot(plot);
        parent.append(graph);
    }
}
