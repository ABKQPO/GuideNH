package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.chart.ChartIcon;
import com.hfstudio.guidenh.guide.document.block.chart.ChartSeries;
import com.hfstudio.guidenh.guide.document.block.chart.PieInsetSpec;
import com.hfstudio.guidenh.guide.document.block.chart.PieSlice;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * Parses {@code <Series>} / {@code <Slice>} child elements inside chart tags.
 */
public class ChartChildParser {

    protected ChartChildParser() {}

    public static List<ChartSeries> parseValueSeries(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parseValueSeries(compiler, errorSink, childElements(compiler, parentEl));
    }

    public static List<ChartSeries> parsePointSeries(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parsePointSeries(compiler, errorSink, childElements(compiler, parentEl));
    }

    public static List<ChartSeries> parseValueSeries(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        return parseSeriesInternal(compiler, errorSink, children, false);
    }

    public static List<ChartSeries> parsePointSeries(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        return parseSeriesInternal(compiler, errorSink, children, true);
    }

    private static List<ChartSeries> parseSeriesInternal(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children, boolean usePoints) {
        List<ChartSeries> result = new ArrayList<>();
        int colorIdx = 0;
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(child);
            if (childEl == null) {
                continue;
            }
            String name = childEl.name();
            if (name == null) {
                continue;
            }
            if (!"Series".equals(name)) {
                // Recognized sibling tags inside chart blocks (combo chart support) are silently skipped
                // here because they are parsed by separate helpers below.
                if ("LineSeries".equals(name) || "PieInset".equals(name)) {
                    continue;
                }
                errorSink.appendError(compiler, "Expected <Series> child but got <" + name + ">", child);
                continue;
            }
            String seriesName = MdxAttrs.getString(compiler, errorSink, childEl, "name", "");
            String colorStr = MdxAttrs.getString(compiler, errorSink, childEl, "color", null);
            int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, ChartAttrParser.paletteColor(colorIdx))
                : ChartAttrParser.paletteColor(colorIdx);
            colorIdx++;
            if (usePoints) {
                String points = MdxAttrs.getString(compiler, errorSink, childEl, "points", "");
                double[][] arr = ChartAttrParser.parsePointArray(points);
                ChartSeries series = new ChartSeries(seriesName, color, arr[0], arr[1]);
                applyIconAndTooltip(compiler, errorSink, childEl, series);
                result.add(series);
            } else {
                String data = MdxAttrs.getString(compiler, errorSink, childEl, "data", "");
                double[] values = ChartAttrParser.parseDoubleArray(data);
                ChartSeries series = ChartSeries.fromValues(seriesName, color, values);
                applyIconAndTooltip(compiler, errorSink, childEl, series);
                result.add(series);
            }
        }
        return result;
    }

    public static List<PieSlice> parseSlices(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parseSlices(compiler, errorSink, childElements(compiler, parentEl));
    }

    public static List<PieSlice> parseSlices(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        List<PieSlice> result = new ArrayList<>();
        int colorIdx = 0;
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(child);
            if (childEl == null) {
                continue;
            }
            String name = childEl.name();
            if (name == null) {
                continue;
            }
            if (!"Slice".equals(name)) {
                // Allow the slice list to coexist with combo siblings without producing spurious errors.
                if ("LineSeries".equals(name) || "Series".equals(name) || "PieInset".equals(name)) {
                    continue;
                }
                errorSink.appendError(compiler, "Expected <Slice> child but got <" + name + ">", child);
                continue;
            }
            String label = MdxAttrs.getString(compiler, errorSink, childEl, "label", "");
            float value = MdxAttrs.getFloat(compiler, errorSink, childEl, "value", 0f);
            String colorStr = MdxAttrs.getString(compiler, errorSink, childEl, "color", null);
            int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, ChartAttrParser.paletteColor(colorIdx))
                : ChartAttrParser.paletteColor(colorIdx);
            colorIdx++;
            PieSlice slice = new PieSlice(label, value, color);
            applyIconAndTooltip(compiler, errorSink, childEl, slice);
            result.add(slice);
        }
        return result;
    }

    /**
     * Parses {@code <LineSeries>} children inside a column/bar chart. Each entry produces a
     * {@link ChartSeries} drawn as a polyline overlay sharing the host chart's value axis.
     * Unknown sibling tags are silently skipped so combo charts can mix {@code <Series>},
     * {@code <LineSeries>} and {@code <PieInset>} freely.
     */
    public static List<ChartSeries> parseLineOverlays(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parseLineOverlays(compiler, errorSink, childElements(compiler, parentEl));
    }

    public static List<ChartSeries> parseLineOverlays(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        List<ChartSeries> result = new ArrayList<>();
        // Use a dedicated palette offset so line colors do not collide with the bar palette by default.
        int colorIdx = 0;
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(child);
            if (childEl == null) continue;
            String name = childEl.name();
            if (!"LineSeries".equals(name)) continue;
            String seriesName = MdxAttrs.getString(compiler, errorSink, childEl, "name", "");
            String colorStr = MdxAttrs.getString(compiler, errorSink, childEl, "color", null);
            int color = colorStr != null
                ? ChartAttrParser.parseColor(colorStr, ChartAttrParser.paletteColor(colorIdx + 4))
                : ChartAttrParser.paletteColor(colorIdx + 4);
            colorIdx++;
            String data = MdxAttrs.getString(compiler, errorSink, childEl, "data", "");
            double[] values = ChartAttrParser.parseDoubleArray(data);
            ChartSeries series = ChartSeries.fromValues(seriesName, color, values);
            applyIconAndTooltip(compiler, errorSink, childEl, series);
            result.add(series);
        }
        return result;
    }

    /**
     * Parses the first {@code <PieInset>} child of the given chart, including its nested {@code <Slice>} entries.
     * Returns {@code null} when no inset is declared.
     */
    public static PieInsetSpec parsePieInset(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parsePieInset(compiler, errorSink, childElements(compiler, parentEl));
    }

    public static PieInsetSpec parsePieInset(PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(child);
            if (childEl == null) continue;
            String name = childEl.name();
            if (!"PieInset".equals(name)) continue;
            PieInsetSpec spec = new PieInsetSpec();
            int size = (int) MdxAttrs.getFloat(compiler, errorSink, childEl, "size", spec.getSize());
            spec.setSize(size);
            String pos = MdxAttrs.getString(compiler, errorSink, childEl, "position", null);
            spec.setPosition(PieInsetSpec.parsePosition(pos, spec.getPosition()));
            float startAngle = MdxAttrs
                .getFloat(compiler, errorSink, childEl, "startAngleDeg", spec.getStartAngleDeg());
            spec.setStartAngleDeg(startAngle);
            String dir = MdxAttrs.getString(compiler, errorSink, childEl, "direction", null);
            if (dir != null) {
                spec.setClockwise(
                    !"counterclockwise".equalsIgnoreCase(dir.trim()) && !"ccw".equalsIgnoreCase(dir.trim()));
            }
            String title = MdxAttrs.getString(compiler, errorSink, childEl, "title", "");
            spec.setTitle(title);
            String titleColor = MdxAttrs.getString(compiler, errorSink, childEl, "titleColor", null);
            if (titleColor != null) {
                spec.setTitleColor(ChartAttrParser.parseColor(titleColor, spec.getTitleColor()));
            }
            // Reuse the already reparsed inset subtree so nested tags do not need another source slice.
            spec.setSlices(parseSlices(compiler, errorSink, childEl.children()));
            return spec;
        }
        return null;
    }

    public static List<? extends MdAstAnyContent> childElements(PageCompiler compiler, MdxJsxElementFields parentEl) {
        return compiler.reparseBlockTagChildren(parentEl);
    }

    private static void applyIconAndTooltip(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        ChartSeries series) {
        series.setIcon(parseIcon(compiler, errorSink, el));
        String extra = MdxAttrs.getString(compiler, errorSink, el, "tooltip", null);
        if (extra != null && !extra.isEmpty()) {
            series.setTooltipExtra(extra);
        }
    }

    private static void applyIconAndTooltip(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        PieSlice slice) {
        slice.setIcon(parseIcon(compiler, errorSink, el));
        String extra = MdxAttrs.getString(compiler, errorSink, el, "tooltip", null);
        if (extra != null && !extra.isEmpty()) {
            slice.setTooltipExtra(extra);
        }
    }

    /**
     * Parses {@code icon="modid:item"} or {@code iconImage="path/to.png"}. Returns {@code null} when neither is
     * present.
     */
    public static ChartIcon parseIcon(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String iconId = MdxAttrs.getString(compiler, errorSink, el, "icon", null);
        if (iconId != null && !iconId.trim()
            .isEmpty()) {
            ChartIcon stackIcon = parseItemStackIcon(compiler, errorSink, el, iconId.trim());
            if (stackIcon != null) {
                return stackIcon;
            }
        }
        String iconImage = MdxAttrs.getString(compiler, errorSink, el, "iconImage", null);
        if (iconImage != null && !iconImage.trim()
            .isEmpty()) {
            return parseImageIcon(compiler, errorSink, el, iconImage.trim());
        }
        return null;
    }

    private static ChartIcon parseItemStackIcon(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String idStr) {
        IdUtils.ParsedItemRef ref;
        try {
            ref = IdUtils.parseItemRef(
                idStr,
                compiler.getPageId()
                    .getResourceDomain());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Malformed icon id " + idStr + ": " + e.getMessage(), el);
            return null;
        }
        if (ref == null) {
            return null;
        }
        // Deferred resolution: store raw key + meta, resolve at render time
        return ChartIcon.ofDeferredItem(ref.rawKey(), ref.concreteMeta());
    }

    private static ChartIcon parseImageIcon(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String src) {
        try {
            ResourceLocation imgId = IdUtils.resolveLink(src, compiler.getPageId());
            // Deferred resolution: store resolved id, load image data at render time
            return ChartIcon.ofDeferredImage(imgId);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Invalid icon image " + src + ": " + e.getMessage(), el);
            return null;
        }
    }
}
