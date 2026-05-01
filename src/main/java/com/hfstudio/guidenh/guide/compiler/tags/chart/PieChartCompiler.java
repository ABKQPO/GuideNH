package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.chart.LytPieChart;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class PieChartCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("PieChart");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytPieChart chart = new LytPieChart();
        CommonChartAttrs.apply(chart, compiler, parent, el);

        float startAngle = MdxAttrs.getFloat(compiler, parent, el, "startAngle", -90f);
        chart.setStartAngleDeg(startAngle);
        boolean clockwise = MdxAttrs.getBoolean(compiler, parent, el, "clockwise", true);
        chart.setClockwise(clockwise);

        chart.setSlices(ChartChildParser.parseSlices(compiler, parent, el));
        parent.append(chart);
    }
}
