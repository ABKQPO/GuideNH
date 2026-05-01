package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.LytColumnChart;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ColumnChartCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ColumnChart");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytColumnChart chart = new LytColumnChart();
        CommonChartAttrs.apply(chart, compiler, parent, el);

        ChartAxisOptions xAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "xAxis", "showXGrid", "xGridColor");
        ChartAxisOptions yAxis = ChartAttrParser
            .parseAxisOptions(compiler, parent, el, "yAxis", "showYGrid", "yGridColor");
        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        String categories = MdxAttrs.getString(compiler, parent, el, "categories", null);
        chart.setCategories(ChartAttrParser.parseStringArray(categories));

        float ratio = MdxAttrs.getFloat(compiler, parent, el, "barWidthRatio", 0.7f);
        chart.setBarWidthRatio(ratio);

        chart.setSeries(ChartChildParser.parseValueSeries(compiler, parent, el));
        chart.setLineOverlays(ChartChildParser.parseLineOverlays(compiler, parent, el));
        chart.setPieInset(ChartChildParser.parsePieInset(compiler, parent, el));
        parent.append(chart);
    }
}
