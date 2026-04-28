package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class RemoveBlocksExecutor {

    private RemoveBlocksExecutor() {}

    public static void execute(GuidebookLevel level, GuideBlockMatcher matcher) {
        List<int[]> toRemove = new ArrayList<>();
        for (int[] pos : level.getFilledBlocks()) {
            int meta = level.getBlockMetadata(pos[0], pos[1], pos[2]);
            String explicitBlockId = level.getExplicitBlockId(pos[0], pos[1], pos[2]);
            if (matcher.matchesResolvedBlockId(explicitBlockId, meta)
                || matcher.matches(level.getBlock(pos[0], pos[1], pos[2]), meta)) {
                toRemove.add(pos);
            }
        }

        for (int[] pos : toRemove) {
            level.setBlock(pos[0], pos[1], pos[2], Blocks.air, 0, null);
        }
    }
}
