package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemImageCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var stack = MdxAttrs.getRequiredItemStack(compiler, parent, el);
        if (stack == null) return;

        float scale = MdxAttrs.getFloat(compiler, parent, el, "scale", 1f);
        var img = new LytItemImage(stack);
        img.setScale(scale);
        img.setInline(true);

        // Allow MDX authors to nudge the icon after its default inline vertical centering.
        // e.g. <ItemImage id="minecraft:diamond" yOffset="2" /> to move it down by 2px.
        String yOffRaw = el.getAttributeString("yOffset", null);
        if (yOffRaw != null && !yOffRaw.isEmpty()) {
            try {
                img.setInlineYOffsetOverride(Integer.parseInt(yOffRaw.trim()));
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "yOffset must be an integer (pixels at scale=1)", el);
            }
        }

        // labelYOffset overrides the default inline vertical nudge for the label text only.
        // e.g. <ItemImage id="minecraft:diamond" label="right" labelYOffset="0" />
        String labelYOffRaw = el.getAttributeString("labelYOffset", null);
        if (labelYOffRaw != null && !labelYOffRaw.isEmpty()) {
            try {
                img.setLabelYOffsetOverride(Integer.parseInt(labelYOffRaw.trim()));
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "labelYOffset must be an integer (pixels at scale=1)", el);
            }
        }

        // noTooltip (legacy) and showTooltip — noTooltip=true or showTooltip=false both suppress.
        Boolean noTooltipAttr = MdxAttrs.getOptionalBoolean(el, "noTooltip");
        boolean noTooltip = MdxAttrs.getBoolean(noTooltipAttr, false);
        Boolean showTooltipAttr = MdxAttrs.getOptionalBoolean(el, "showTooltip");
        boolean showTooltip = showTooltipAttr != null ? showTooltipAttr : !noTooltip;
        img.setShowTooltip(showTooltip);

        // showIcon — whether to render the icon graphic (default true).
        Boolean showIcon = MdxAttrs.getOptionalBoolean(el, "showIcon");
        if (showIcon != null) {
            img.setShowIcon(showIcon);
        }

        // label — "left" or "right" to display the item name next to the icon.
        String labelRaw = el.getAttributeString("label", null);
        img.setLabelPosition(resolveLabelPosition(labelRaw));

        // format — Markdown-style format pattern for the label text.
        String formatRaw = el.getAttributeString("format", null);
        if (formatRaw != null && !formatRaw.isEmpty()) {
            img.setLabelFormat(formatRaw);
        }

        var inline = new LytFlowInlineBlock();
        inline.setBlock(img);
        parent.append(inline);
    }

    /**
     * Normalises a raw {@code label} attribute value to {@code "left"}, {@code "right"}, or
     * {@code null} (no label). Any truthy value that is not {@code "left"} is treated as
     * {@code "right"} (icon on left, text on right).
     */
    @Nullable
    public static String resolveLabelPosition(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String v = raw.trim()
            .toLowerCase(Locale.ROOT);
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("none")) return null;
        if (v.equals("left")) return "left";
        return "right";
    }

}
