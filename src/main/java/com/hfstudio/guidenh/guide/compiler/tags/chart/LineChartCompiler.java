package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.LytLineChart;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class LineChartCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("LineChart");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytLineChart chart = new LytLineChart();
        CommonChartAttrs.apply(chart, compiler, parent, el);

        ChartAxisOptions xAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "xAxis", "showXGrid", "xGridColor");
        ChartAxisOptions yAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "yAxis", "showYGrid", "yGridColor");
        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        String categories = MdxAttrs.getString(compiler, parent, el, "categories", null);
        chart.setCategories(ChartAttrParser.parseStringArray(categories));

        boolean numericX = MdxAttrs.getBoolean(compiler, parent, el, "numericX", false);
        chart.setNumericX(numericX);
        boolean showPoints = MdxAttrs.getBoolean(compiler, parent, el, "showPoints", true);
        chart.setShowPoints(showPoints);

        if (numericX) {
            chart.setSeries(ChartChildParser.parsePointSeries(compiler, parent, el));
        } else {
            chart.setSeries(ChartChildParser.parseValueSeries(compiler, parent, el));
        }
        parent.append(chart);
    }
}
