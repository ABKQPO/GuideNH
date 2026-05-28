package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;

public class ReplaceBlockExecutor {

    public static final int MAX_PREALLOCATED_TARGETS = 4096;

    private ReplaceBlockExecutor() {}

    /**
     * Replaces every block in {@code level} that matches {@code fromMatcher} (and optionally whose
     * tile entity NBT contains all entries from {@code fromNbt}) with {@code toBlock}/{@code toMeta}/
     * {@code toNbt}.
     *
     * <p>
     * When {@code hasBounds} is {@code false} the executor operates on all filled blocks in the
     * level. When {@code hasBounds} is {@code true} the search is restricted to the axis-aligned box
     * starting at {@code (x, y, z)} with size {@code (dx, dy, dz)}.
     */
    public static void execute(GuidebookLevel level, GuideBlockMatcher fromMatcher, @Nullable NBTTagCompound fromNbt,
        Block toBlock, int toMeta, @Nullable NBTTagCompound toNbt, String toExplicitId, boolean hasBounds, int x, int y,
        int z, int dx, int dy, int dz, boolean formed) {
        List<int[]> targets;

        if (!hasBounds) {
            Collection<int[]> filledBlocks = level.getFilledBlocks();
            targets = new ArrayList<>(filledBlocks.size());
            for (int[] pos : filledBlocks) {
                if (blockMatches(level, pos[0], pos[1], pos[2], fromMatcher, fromNbt)) {
                    targets.add(pos);
                }
            }
        } else {
            targets = new ArrayList<>(estimateBoundedTargetCapacity(dx, dy, dz));
            int endX = x + dx;
            int endY = y + dy;
            int endZ = z + dz;
            for (int bx = x; bx < endX; bx++) {
                for (int by = y; by < endY; by++) {
                    for (int bz = z; bz < endZ; bz++) {
                        if (level.getBlock(bx, by, bz) != Blocks.air
                            && blockMatches(level, bx, by, bz, fromMatcher, fromNbt)) {
                            targets.add(new int[] { bx, by, bz });
                        }
                    }
                }
            }
        }

        for (int[] pos : targets) {
            NBTTagCompound tagCopy = toNbt != null ? (NBTTagCompound) toNbt.copy() : null;
            GuidebookPreviewBlockPlacer.place(level, pos[0], pos[1], pos[2], toBlock, toMeta, tagCopy, toExplicitId);
            ScenePreviewFormedState.updateAfterPlacement(level, pos[0], pos[1], pos[2], formed);
        }
    }

    private static int estimateBoundedTargetCapacity(int dx, int dy, int dz) {
        if (dx <= 0 || dy <= 0 || dz <= 0) {
            return 0;
        }
        long volume = (long) dx * (long) dy * (long) dz;
        return volume > MAX_PREALLOCATED_TARGETS ? MAX_PREALLOCATED_TARGETS : (int) volume;
    }

    private static boolean blockMatches(GuidebookLevel level, int bx, int by, int bz, GuideBlockMatcher matcher,
        @Nullable NBTTagCompound fromNbt) {
        int meta = level.getBlockMetadata(bx, by, bz);
        String explicitId = level.getExplicitBlockId(bx, by, bz);
        if (!matcher.matchesResolvedBlockId(explicitId, meta) && !matcher.matches(level.getBlock(bx, by, bz), meta)) {
            return false;
        }
        if (fromNbt == null) {
            return true;
        }
        TileEntity te = level.getTileEntity(bx, by, bz);
        if (te == null) {
            return false;
        }
        NBTTagCompound teTag = new NBTTagCompound();
        te.writeToNBT(teTag);
        return nbtContains(fromNbt, teTag);
    }

    /**
     * Returns {@code true} when every key-value entry in {@code pattern} is also present in
     * {@code actual}. For nested compounds the check recurses so partial matching applies at every
     * depth level.
     */
    private static boolean nbtContains(NBTTagCompound pattern, NBTTagCompound actual) {
        for (String key : pattern.func_150296_c()) {
            NBTBase patternValue = pattern.getTag(key);
            NBTBase actualValue = actual.getTag(key);
            if (actualValue == null) {
                return false;
            }
            if (patternValue instanceof NBTTagCompound && actualValue instanceof NBTTagCompound) {
                if (!nbtContains((NBTTagCompound) patternValue, (NBTTagCompound) actualValue)) {
                    return false;
                }
            } else if (!patternValue.equals(actualValue)) {
                return false;
            }
        }
        return true;
    }
}
