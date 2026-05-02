package com.hfstudio.guidenh.guide.compiler.tags.functiongraph;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Compiles {@code <FunctionGraph>} elements containing {@code <Plot>} and {@code <Point>} children.
 */
public class FunctionGraphTagCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("FunctionGraph");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytFunctionGraph graph = new LytFunctionGraph();
        FunctionGraphAttrs.applyContainerAttrs(graph, compiler, parent, el);

        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(el);
        int plotIndex = 0;
        for (MdAstAnyContent child : children) {
            UnistNode node = child;
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(node);
            if (childEl == null) {
                continue;
            }
            String name = childEl.name();
            if (name == null) {
                continue;
            }
            if ("Plot".equals(name) || "Function".equals(name)) {
                FunctionPlot plot = FunctionGraphAttrs.parsePlot(compiler, parent, childEl, plotIndex);
                if (plot != null) {
                    graph.addPlot(plot);
                    plotIndex++;
                }
            } else if ("Point".equals(name)) {
                MarkedPoint point = FunctionGraphAttrs.parsePoint(compiler, parent, childEl);
                if (point != null) {
                    graph.addPoint(point);
                }
            } else {
                parent.appendError(compiler, "Expected <Plot>, <Function> or <Point> but got <" + name + ">", node);
            }
        }
        parent.append(graph);
    }
}
