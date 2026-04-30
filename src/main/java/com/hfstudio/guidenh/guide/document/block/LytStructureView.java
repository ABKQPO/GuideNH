package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytStructureView extends LytBlock {

    public static class BlockEntry {

        public final int x;
        public final int y;
        public final int z;
        public final ItemStack stack;

        public BlockEntry(int x, int y, int z, ItemStack stack) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.stack = stack;
        }
    }

    public static final int DEFAULT_WIDTH = 192;
    public static final int DEFAULT_HEIGHT = 144;
    public static final int ICON = 16;
    public static final int TILE_W = 14;
    public static final int TILE_H = 7;
    public static final int LAYER_H = 12;

    private int viewWidth = DEFAULT_WIDTH;
    private int viewHeight = DEFAULT_HEIGHT;
    private final List<BlockEntry> blocks = new ArrayList<>();

    public void setViewSize(int width, int height) {
        this.viewWidth = Math.max(32, width);
        this.viewHeight = Math.max(32, height);
    }

    public void addBlock(int x, int y, int z, ItemStack stack) {
        if (stack != null) {
            blocks.add(new BlockEntry(x, y, z, stack));
        }
    }

    public List<BlockEntry> getBlocks() {
        return blocks;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int w = Math.min(viewWidth, availableWidth);
        return new LytRect(x, y, w, viewHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        var bounds = getBounds();
        context.fillRect(bounds, 0xFF1E1E1E);
        context.drawBorder(bounds, 0xFF555555, 1);

        if (blocks.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockEntry b : blocks) {
            if (b.x < minX) minX = b.x;
            if (b.x > maxX) maxX = b.x;
            if (b.y < minY) minY = b.y;
            if (b.y > maxY) maxY = b.y;
            if (b.z < minZ) minZ = b.z;
            if (b.z > maxZ) maxZ = b.z;
        }

        int sxMin = Integer.MAX_VALUE, sxMax = Integer.MIN_VALUE;
        int syMin = Integer.MAX_VALUE, syMax = Integer.MIN_VALUE;
        for (BlockEntry b : blocks) {
            int px = projectX(b.x, b.z);
            int py = projectY(b.x, b.y, b.z);
            if (px < sxMin) sxMin = px;
            if (px + ICON > sxMax) sxMax = px + ICON;
            if (py < syMin) syMin = py;
            if (py + ICON > syMax) syMax = py + ICON;
        }
        int contentW = sxMax - sxMin;
        int contentH = syMax - syMin;
        int offsetX = bounds.x() + (bounds.width() - contentW) / 2 - sxMin;
        int offsetY = bounds.y() + (bounds.height() - contentH) / 2 - syMin;

        List<BlockEntry> sorted = new ArrayList<>(blocks);
        sorted.sort(
            Comparator.<BlockEntry>comparingInt(b -> b.y)
                .thenComparing(
                    Comparator.<BlockEntry>comparingInt(b -> b.z)
                        .reversed())
                .thenComparingInt(b -> b.x));

        context.pushLocalScissor(bounds);
        try {
            for (BlockEntry b : sorted) {
                int px = projectX(b.x, b.z) + offsetX;
                int py = projectY(b.x, b.y, b.z) + offsetY;
                context.renderItem(b.stack, px, py);
            }
        } finally {
            context.popScissor();
        }
    }

    public static int projectX(int x, int z) {
        return (x - z) * TILE_W;
    }

    public static int projectY(int x, int y, int z) {
        return (x + z) * TILE_H - y * LAYER_H;
    }
}
