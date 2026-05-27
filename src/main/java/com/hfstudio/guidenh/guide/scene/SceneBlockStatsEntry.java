package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import org.jetbrains.annotations.Nullable;

public class SceneBlockStatsEntry {

    private final String key;
    private final ItemStack stack;
    private final String label;
    private final List<BlockStatsPlacement> placements = new ArrayList<>();
    private int count;
    private int cachedTextWidth = -1;
    private int cachedEllipsizedTextMaxWidth = -1;
    private String cachedEllipsizedSource = "";
    private String cachedEllipsizedText = "";

    public SceneBlockStatsEntry(String key, ItemStack stack, String label, int count) {
        this.key = key != null ? key : "";
        this.stack = stack;
        this.label = label != null ? label : "";
        this.count = Math.max(0, count);
        updateStackDisplayCount();
    }

    public String getKey() {
        return key;
    }

    public ItemStack getStack() {
        return stack;
    }

    public String getLabel() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public void addCount(int delta) {
        this.count = Math.max(0, this.count + delta);
        updateStackDisplayCount();
    }

    public void addPlacement(int x, int y, int z, @Nullable AxisAlignedBB bounds, int count) {
        placements.add(new BlockStatsPlacement(x, y, z, bounds, Math.max(1, count)));
    }

    public ItemStack getDisplayStack() {
        updateStackDisplayCount();
        return stack;
    }

    public ItemStack copyDisplayStack() {
        if (stack == null) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = displayCount(count);
        return copy;
    }

    private void updateStackDisplayCount() {
        if (stack != null) {
            stack.stackSize = displayCount(count);
        }
    }

    private static int displayCount(int count) {
        return Math.max(1, count);
    }

    public List<BlockStatsPlacement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

    public int getPlacementCount() {
        int total = 0;
        for (BlockStatsPlacement placement : placements) {
            total += placement.count();
        }
        return total;
    }

    public int getCachedTextWidth() {
        return cachedTextWidth;
    }

    public void setCachedTextWidth(int cachedTextWidth) {
        this.cachedTextWidth = cachedTextWidth;
    }

    public String getCachedEllipsizedText(String source, int maxWidth) {
        if (source == null) {
            source = "";
        }
        if (maxWidth == cachedEllipsizedTextMaxWidth && source.equals(cachedEllipsizedSource)) {
            return cachedEllipsizedText;
        }
        return null;
    }

    public void setCachedEllipsizedText(String source, int maxWidth, String text) {
        cachedEllipsizedSource = source != null ? source : "";
        cachedEllipsizedTextMaxWidth = maxWidth;
        cachedEllipsizedText = text != null ? text : "";
    }

    public static class BlockStatsPlacement {

        private final int x;
        private final int y;
        private final int z;
        @Nullable
        private final AxisAlignedBB bounds;
        private final int count;

        public BlockStatsPlacement(int x, int y, int z, @Nullable AxisAlignedBB bounds, int count) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bounds = bounds != null
                ? AxisAlignedBB
                    .getBoundingBox(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
                : null;
            this.count = Math.max(1, count);
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        @Nullable
        public AxisAlignedBB bounds() {
            return bounds;
        }

        public int count() {
            return count;
        }
    }

}
