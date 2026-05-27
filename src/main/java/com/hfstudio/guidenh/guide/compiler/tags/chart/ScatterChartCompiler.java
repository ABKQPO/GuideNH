package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.LytScatterChart;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

public class ScatterChartCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ScatterChart");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytScatterChart chart = new LytScatterChart();
        CommonChartAttrs.apply(chart, compiler, parent, el);

        ChartAxisOptions xAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "xAxis", "showXGrid", "xGridColor");
        ChartAxisOptions yAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "yAxis", "showYGrid", "yGridColor");
        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        List<? extends MdAstAnyContent> children = ChartChildParser.childElements(compiler, el);
        chart.setSeries(ChartChildParser.parsePointSeries(compiler, parent, children));
        parent.append(chart);
    }
}
