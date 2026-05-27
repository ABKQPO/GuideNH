package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LatexRenderOptions;
import com.hfstudio.guidenh.guide.document.block.LatexVerticalAlign;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Compiles {@code <Latex formula="..." />} tags in guide Markdown pages into in-game LaTeX-rendered formulas.
 *
 * <p>
 * Supported attributes:
 * <ul>
 * <li>{@code formula} (String, required) — the LaTeX source string</li>
 * <li>{@code color} (String, optional, default {@code "#FFFFFF"}) — formula glyph colour as {@code #RRGGBB}
 * or {@code #AARRGGBB}</li>
 * <li>{@code scale} (float, optional, default {@code 1.0}) — multiplier applied to the automatic display size</li>
 * <li>{@code sourceScale} (float, optional, default {@code 100.0}) — jlatexmath internal render quality;
 * higher values produce crisper output at the cost of more memory</li>
 * <li>{@code showTooltip} (boolean, optional, default {@code false}) — when {@code true}, hovering over
 * the formula shows the raw LaTeX source in a tooltip</li>
 * <li>{@code tooltip} (String, optional) — plain tooltip text. Child Markdown content takes precedence
 * and is compiled as a rich tooltip.</li>
 * <li>{@code valign} (String, optional, default {@code "baseline"}) — inline-only; vertical alignment of
 * the formula within the text line: {@code baseline}, {@code top}, {@code center}, or {@code bottom}</li>
 * <li>{@code offsetX} (int, optional, default {@code 0}) — horizontal pixel offset applied after alignment</li>
 * <li>{@code offsetY} (int, optional, default {@code 0}) — vertical pixel offset applied after alignment</li>
 * </ul>
 *
 * <p>
 * When used in flow (inline) context the formula is placed inside a
 * {@link LytFlowInlineBlock} so the containing line expands to fit tall formulas such as fractions.
 * When used in block context the formula is centered and surrounded by a small vertical margin.
 */
public class LatexTagCompiler implements TagCompiler {

    private static final String TAG_NAME = "Latex";

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton(TAG_NAME);
    }

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        LytLatexBlock block = buildInlineBlock(compiler, parent, el);
        if (block != null) {
            parent.append(LytFlowInlineBlock.of(block));
        }
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        LytLatexDisplayBlock block = buildDisplayBlock(compiler, parent, el);
        if (block != null) {
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(block));
        }
    }

    private LytLatexBlock buildInlineBlock(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        String formula = MdxAttrs.getString(compiler, parent, el, "formula", null);
        if (formula == null || formula.trim()
            .isEmpty()) {
            parent.appendError(compiler, "Latex tag requires a non-empty 'formula' attribute.", el);
            return null;
        }

        int fillColor = parseColor(compiler, parent, el);
        float userScale = MdxAttrs.getFloat(compiler, parent, el, "scale", LatexRenderOptions.DEFAULT_USER_SCALE);
        float sourceScale = MdxAttrs
            .getFloat(compiler, parent, el, "sourceScale", LatexRenderOptions.DEFAULT_SOURCE_SCALE);
        GuideTooltip tooltip = buildInlineTooltip(compiler, parent, el, formula);
        LatexVerticalAlign valign = LatexVerticalAlign.parse(MdxAttrs.getString(compiler, parent, el, "valign", null));
        int offsetX = MdxAttrs.getInt(compiler, parent, el, "offsetX", 0);
        int offsetY = MdxAttrs.getInt(compiler, parent, el, "offsetY", 0);

        return new LytLatexBlock(
            formula,
            LatexRenderOptions.builder()
                .fillColorArgb(fillColor)
                .sourceScale(sourceScale)
                .userScale(userScale)
                .tooltip(tooltip)
                .valign(valign)
                .offset(offsetX, offsetY)
                .build());
    }

    private LytLatexDisplayBlock buildDisplayBlock(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields el) {
        String formula = MdxAttrs.getString(compiler, parent, el, "formula", null);
        if (formula == null || formula.trim()
            .isEmpty()) {
            parent.appendError(compiler, "Latex tag requires a non-empty 'formula' attribute.", el);
            return null;
        }

        int fillColor = parseColor(compiler, parent, el);
        float userScale = MdxAttrs.getFloat(compiler, parent, el, "scale", LatexRenderOptions.DEFAULT_USER_SCALE);
        float sourceScale = MdxAttrs
            .getFloat(compiler, parent, el, "sourceScale", LatexRenderOptions.DEFAULT_SOURCE_SCALE);
        GuideTooltip tooltip = buildBlockTooltip(compiler, parent, el, formula);
        int offsetX = MdxAttrs.getInt(compiler, parent, el, "offsetX", 0);
        int offsetY = MdxAttrs.getInt(compiler, parent, el, "offsetY", 0);

        return new LytLatexDisplayBlock(
            formula,
            LatexRenderOptions.builder()
                .fillColorArgb(fillColor)
                .sourceScale(sourceScale)
                .userScale(userScale)
                .tooltip(tooltip)
                .offset(offsetX, offsetY)
                .build());
    }

    private static GuideTooltip buildInlineTooltip(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String formula) {
        if (el.children() != null && !el.children()
            .isEmpty()) {
            var contentBox = new LytVBox();
            compiler.compileBlockContextInSourceContext(el.children(), contentBox);
            if (!contentBox.getChildren()
                .isEmpty()) {
                return new ContentTooltip(contentBox);
            }
        }
        return buildAttributeTooltip(compiler, errorSink, el, formula);
    }

    private static GuideTooltip buildBlockTooltip(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields el, String formula) {
        if (el.children() != null && !el.children()
            .isEmpty()) {
            var contentBox = new LytVBox();
            compiler.compileBlockTagChildren(el, contentBox);
            if (!contentBox.getChildren()
                .isEmpty()) {
                return new ContentTooltip(contentBox);
            }
        }
        return buildAttributeTooltip(compiler, parent, el, formula);
    }

    private static GuideTooltip buildAttributeTooltip(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String formula) {
        String tooltip = MdxAttrs.getString(compiler, errorSink, el, "tooltip", null);
        if (tooltip != null && !tooltip.trim()
            .isEmpty()) {
            return new TextTooltip(tooltip);
        }
        boolean showTooltip = MdxAttrs.getBoolean(compiler, errorSink, el, "showTooltip", false);
        return showTooltip ? new TextTooltip(formula) : null;
    }

    private static int parseColor(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String colorStr = MdxAttrs.getString(compiler, errorSink, el, "color", null);
        if (colorStr == null) {
            return LatexRenderOptions.DEFAULT_FILL_COLOR_ARGB;
        }
        colorStr = colorStr.trim();
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }
        try {
            if (colorStr.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(colorStr, 16);
            }
            if (colorStr.length() == 8) {
                return (int) Long.parseLong(colorStr, 16);
            }
        } catch (NumberFormatException ignored) {}

        errorSink.appendError(
            compiler,
            "Invalid color format '" + colorStr + "' for Latex tag; expected #RRGGBB or #AARRGGBB.",
            el);
        return LatexRenderOptions.DEFAULT_FILL_COLOR_ARGB;
    }
}
