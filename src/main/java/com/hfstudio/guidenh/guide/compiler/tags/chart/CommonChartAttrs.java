package com.hfstudio.guidenh.guide.compiler.tags.chart;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLabelPosition;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.LytChartBase;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Applies the common attributes shared by all charts (title/size/color/legend/label position).
 */
public final class CommonChartAttrs {

    private CommonChartAttrs() {}

    public static void apply(LytChartBase chart, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        chart.setTitle(MdxAttrs.getString(compiler, errorSink, el, "title", null));
        int w = MdxAttrs.getInt(compiler, errorSink, el, "width", -1);
        int h = MdxAttrs.getInt(compiler, errorSink, el, "height", -1);
        chart.setExplicitSize(w, h);

        String bg = MdxAttrs.getString(compiler, errorSink, el, "background", null);
        if (bg != null) {
            chart.setBackgroundColor(ChartAttrParser.parseColor(bg, chart.getBackgroundColor()));
        }
        String border = MdxAttrs.getString(compiler, errorSink, el, "border", null);
        if (border != null) {
            chart.setBorderColor(ChartAttrParser.parseColor(border, chart.getBorderColor()));
        }
        String titleColor = MdxAttrs.getString(compiler, errorSink, el, "titleColor", null);
        if (titleColor != null) {
            chart.setTitleColor(ChartAttrParser.parseColor(titleColor, chart.getTitleColor()));
        }
        String labelColor = MdxAttrs.getString(compiler, errorSink, el, "labelColor", null);
        if (labelColor != null) {
            chart.setLabelColor(ChartAttrParser.parseColor(labelColor, chart.getLabelColor()));
        }
        chart.setLegendPosition(
            ChartAttrParser.parseLegendPosition(
                MdxAttrs.getString(compiler, errorSink, el, "legend", null),
                chart.getLegendPosition()));
        chart.setLabelPosition(
            ChartAttrParser.parseLabelPosition(
                MdxAttrs.getString(compiler, errorSink, el, "labelPosition", null),
                ChartLabelPosition.NONE));
        // Static empty read to suppress unused warning (no-op).
        if (chart.getLegendPosition() == null) {
            chart.setLegendPosition(ChartLegendPosition.NONE);
        }
    }
}
