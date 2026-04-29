package com.hfstudio.guidenh.guide.scene.level;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;

public interface GuidebookChunkSource {

    @Nullable
    GuidebookChunk getChunk(int chunkX, int chunkZ, boolean create);

    @Nullable
    default GuidebookChunk getChunk(ChunkCoordIntPair pair, boolean create) {
        return getChunk(pair.chunkXPos, pair.chunkZPos, create);
    }

    default void setBlock(int x, int y, int z, Block block, int meta) {
        var chunk = getChunk(x >> 4, z >> 4, true);
        if (chunk != null) {
            chunk.setBlock(x, y, z, block, meta);
        }
    }
}
