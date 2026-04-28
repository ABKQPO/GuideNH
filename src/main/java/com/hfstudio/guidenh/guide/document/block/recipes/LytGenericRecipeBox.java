package com.hfstudio.guidenh.guide.document.block.recipes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.internal.recipe.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytGenericRecipeBox extends LytBox {

    private static final int TITLE_HEIGHT = 10;
    private static final int TITLE_COLOR = 0xFFAAAAAA;
    private static final int SLOT_INSET = (LytSlot.OUTER_SIZE - 16) / 2;

    private final String title;
    private final int normX;
    private final int normY;

    public LytGenericRecipeBox(NeiRecipeLookup.Entry entry) {
        this.title = entry.recipeName == null ? "" : entry.recipeName;
        int mnx = Integer.MAX_VALUE;
        int mny = Integer.MAX_VALUE;
        for (NeiRecipeLookup.Slot s : collect(entry)) {
            if (s.relx < mnx) mnx = s.relx;
            if (s.rely < mny) mny = s.rely;
        }
        if (mnx == Integer.MAX_VALUE) {
            mnx = 0;
            mny = 0;
        }
        this.normX = mnx;
        this.normY = mny;

        for (NeiRecipeLookup.Slot s : entry.ingredients) {
            append(new PositionedSlot(s, false));
        }
        for (NeiRecipeLookup.Slot s : entry.others) {
            append(new PositionedSlot(s, false));
        }
        if (entry.result != null) {
            append(new PositionedSlot(entry.result, false));
        }
    }

    private static List<NeiRecipeLookup.Slot> collect(NeiRecipeLookup.Entry entry) {
        List<NeiRecipeLookup.Slot> all = new ArrayList<>(entry.ingredients.size() + entry.others.size() + 1);
        all.addAll(entry.ingredients);
        all.addAll(entry.others);
        if (entry.result != null) all.add(entry.result);
        return all;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int maxX = x;
        int maxY = y;
        int topOffset = hasTitle() ? TITLE_HEIGHT : 0;
        for (var child : children) {
            PositionedSlot ps = (PositionedSlot) child;
            int cellX = x + (ps.src.relx - normX) - SLOT_INSET;
            int cellY = y + topOffset + (ps.src.rely - normY) - SLOT_INSET;
            ps.layout(context, cellX, cellY, availableWidth);
            int right = cellX + LytSlot.OUTER_SIZE;
            int bottom = cellY + LytSlot.OUTER_SIZE;
            if (right > maxX) maxX = right;
            if (bottom > maxY) maxY = bottom;
        }
        int w = Math.max(maxX - x, 1);
        int h = Math.max(maxY - y, topOffset);
        return new LytRect(x, y, w, h);
    }

    @Override
    public void render(RenderContext context) {
        if (hasTitle()) {
            context.renderText(title, DefaultStyles.BASE_STYLE, bounds.x(), bounds.y());
        }
        super.render(context);
    }

    private boolean hasTitle() {
        return title != null && !title.isEmpty();
    }

    public static class PositionedSlot extends LytSlot {

        final NeiRecipeLookup.Slot src;

        PositionedSlot(NeiRecipeLookup.Slot src, boolean large) {
            super(src.stacks);
            this.src = src;
            if (large) setLargeSlot(true);
        }
    }

    public static @Nullable List<LytGenericRecipeBox> forAll(List<NeiRecipeLookup.Entry> entries, int limit) {
        if (entries == null || entries.isEmpty()) return null;
        int cap = limit <= 0 ? entries.size() : Math.min(limit, entries.size());
        List<LytGenericRecipeBox> out = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) {
            out.add(new LytGenericRecipeBox(entries.get(i)));
        }
        return out;
    }
}
