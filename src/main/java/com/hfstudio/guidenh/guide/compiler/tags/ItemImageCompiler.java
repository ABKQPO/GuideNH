package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

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
        boolean noTooltip = parseBool(el.getAttributeString("noTooltip", null));
        var img = new LytItemImage(stack);
        img.setScale(scale);
        img.setInline(true);
        // Allow MDX authors to override the default inline vertical nudge on a per-element basis,
        // e.g. <ItemImage id="minecraft:diamond" yOffset="0" /> to disable the upward shift.
        String yOffRaw = el.getAttributeString("yOffset", null);
        if (yOffRaw != null && !yOffRaw.isEmpty()) {
            try {
                img.setInlineYOffsetOverride(Integer.parseInt(yOffRaw.trim()));
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "yOffset must be an integer (pixels at scale=1)", el);
            }
        }
        if (noTooltip) img.setTooltipSuppressed(true);

        var inline = new LytFlowInlineBlock();
        inline.setBlock(img);
        parent.append(inline);
    }

    private static boolean parseBool(String raw) {
        if (raw == null) return false;
        if (raw.isEmpty()) return true;
        var v = raw.trim()
            .toLowerCase(Locale.ROOT);
        return !(v.equals("false") || v.equals("0") || v.equals("no"));
    }
}
