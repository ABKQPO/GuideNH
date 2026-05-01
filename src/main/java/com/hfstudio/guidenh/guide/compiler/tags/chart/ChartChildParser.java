package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.chart.ChartIcon;
import com.hfstudio.guidenh.guide.document.block.chart.ChartSeries;
import com.hfstudio.guidenh.guide.document.block.chart.PieInsetSpec;
import com.hfstudio.guidenh.guide.document.block.chart.PieSlice;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Parses {@code <Series>} / {@code <Slice>} child elements inside chart tags.
 */
public final class ChartChildParser {

    private ChartChildParser() {}

    public static List<ChartSeries> parseValueSeries(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parseSeriesInternal(compiler, errorSink, parentEl, false);
    }

    public static List<ChartSeries> parsePointSeries(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl) {
        return parseSeriesInternal(compiler, errorSink, parentEl, true);
    }

    private static List<ChartSeries> parseSeriesInternal(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parentEl, boolean usePoints) {
        List<ChartSeries> result = new ArrayList<>();
        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(parentEl);
        int colorIdx = 0;
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
            if (!"Series".equals(name)) {
                // Recognized sibling tags inside chart blocks (combo chart support) are silently skipped
                // here because they are parsed by separate helpers below.
                if ("LineSeries".equals(name) || "PieInset".equals(name)) {
                    continue;
                }
                errorSink.appendError(compiler, "Expected <Series> child but got <" + name + ">", node);
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
        List<PieSlice> result = new ArrayList<>();
        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(parentEl);
        int colorIdx = 0;
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
            if (!"Slice".equals(name)) {
                // Allow the slice list to coexist with combo siblings without producing spurious errors.
                if ("LineSeries".equals(name) || "Series".equals(name) || "PieInset".equals(name)) {
                    continue;
                }
                errorSink.appendError(compiler, "Expected <Slice> child but got <" + name + ">", node);
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
        List<ChartSeries> result = new ArrayList<>();
        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(parentEl);
        // Use a dedicated palette offset so line colors do not collide with the bar palette by default.
        int colorIdx = 0;
        for (MdAstAnyContent child : children) {
            UnistNode node = child;
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(node);
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
        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(parentEl);
        for (MdAstAnyContent child : children) {
            UnistNode node = child;
            MdxJsxElementFields childEl = SceneTagCompiler.unwrapSceneElement(node);
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
            // Parse nested <Slice> children of the inset itself.
            spec.setSlices(parseSlices(compiler, errorSink, childEl));
            return spec;
        }
        return null;
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
        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) {
            errorSink.appendError(compiler, "Missing item for icon: " + ref.id(), el);
            return null;
        }
        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (NBTTagCompound) ref.nbt()
                .copy();
        }
        return ChartIcon.ofItemStack(stack);
    }

    private static ChartIcon parseImageIcon(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String src) {
        try {
            ResourceLocation imgId = IdUtils.resolveLink(src, compiler.getPageId());
            byte[] data = compiler.loadAsset(imgId);
            if (data == null) {
                errorSink.appendError(compiler, "Missing icon image: " + src, el);
                return null;
            }
            return ChartIcon.ofImage(imgId, GuidePageTexture.load(imgId, data));
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Invalid icon image " + src + ": " + e.getMessage(), el);
            return null;
        }
    }
}
