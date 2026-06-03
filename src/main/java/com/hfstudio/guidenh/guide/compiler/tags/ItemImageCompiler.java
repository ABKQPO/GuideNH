package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
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
        String itemId = MdxAttrs.getString(compiler, parent, el, "id", null);
        String ore = MdxAttrs.getString(compiler, parent, el, "ore", null);
        if (itemId == null) return;
        itemId = itemId.trim();

        float scale = MdxAttrs.getFloat(compiler, parent, el, "scale", 1f);
        boolean showTooltip;
        Boolean showIcon;
        String labelPosition;
        String labelFormat;
        Integer yOffset = null;
        Integer labelYOffset = null;

        // Allow MDX authors to nudge the icon after its default inline vertical centering.
        // e.g. <ItemImage id="minecraft:diamond" yOffset="2" /> to move it down by 2px.
        String yOffRaw = el.getAttributeString("yOffset", null);
        if (yOffRaw != null && !yOffRaw.isEmpty()) {
            try {
                yOffset = Integer.parseInt(yOffRaw.trim());
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "yOffset must be an integer (pixels at scale=1)", el);
            }
        }

        // labelYOffset overrides the default inline vertical nudge for the label text only.
        // e.g. <ItemImage id="minecraft:diamond" label="right" labelYOffset="0" />
        String labelYOffRaw = el.getAttributeString("labelYOffset", null);
        if (labelYOffRaw != null && !labelYOffRaw.isEmpty()) {
            try {
                labelYOffset = Integer.parseInt(labelYOffRaw.trim());
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "labelYOffset must be an integer (pixels at scale=1)", el);
            }
        }

        // noTooltip (legacy) and showTooltip — noTooltip=true or showTooltip=false both suppress.
        Boolean noTooltipAttr = MdxAttrs.getOptionalBoolean(el, "noTooltip");
        boolean noTooltip = MdxAttrs.getBoolean(noTooltipAttr, false);
        Boolean showTooltipAttr = MdxAttrs.getOptionalBoolean(el, "showTooltip");
        showTooltip = showTooltipAttr != null ? showTooltipAttr : !noTooltip;

        // showIcon — whether to render the icon graphic (default true).
        showIcon = MdxAttrs.getOptionalBoolean(el, "showIcon");

        // label — "left" or "right" to display the item name next to the icon.
        String labelRaw = el.getAttributeString("label", null);
        labelPosition = resolveLabelPosition(labelRaw);

        // format — Markdown-style format pattern for the label text.
        String formatRaw = el.getAttributeString("format", null);
        labelFormat = (formatRaw != null && !formatRaw.isEmpty()) ? formatRaw : null;

        ItemImagePlaceholder placeholder = new ItemImagePlaceholder(
            itemId,
            scale,
            yOffset,
            labelYOffset,
            showTooltip,
            showIcon,
            labelPosition,
            labelFormat,
            ore);

        var inline = new LytFlowInlineBlock();
        inline.setBlock(placeholder);
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

    public static class ItemImagePlaceholder extends LytParagraph {

        public final String itemId;
        public final float scale;
        @Nullable
        public final Integer yOffset;
        @Nullable
        public final Integer labelYOffset;
        public final boolean showTooltip;
        @Nullable
        public final Boolean showIcon;
        @Nullable
        public final String labelPosition;
        @Nullable
        public final String labelFormat;
        @Nullable
        public final String ore;

        public ItemImagePlaceholder(String itemId, float scale, @Nullable Integer yOffset,
            @Nullable Integer labelYOffset, boolean showTooltip, @Nullable Boolean showIcon,
            @Nullable String labelPosition, @Nullable String labelFormat, @Nullable String ore) {
            this.itemId = itemId;
            this.scale = scale;
            this.yOffset = yOffset;
            this.labelYOffset = labelYOffset;
            this.showTooltip = showTooltip;
            this.showIcon = showIcon;
            this.labelPosition = labelPosition;
            this.labelFormat = labelFormat;
            this.ore = ore;
            setStyleClass("ItemImage");
        }
    }

}
