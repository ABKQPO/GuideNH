package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

public class StructureLibImportResult {

    private final boolean success;
    private final List<PlacedBlock> blocks;
    private final List<String> warnings;
    private final List<String> errors;
    @Nullable
    private final StructureLibSceneMetadata metadata;

    public StructureLibImportResult(boolean success, List<PlacedBlock> blocks, List<String> warnings,
        List<String> errors, @Nullable StructureLibSceneMetadata metadata) {
        this.success = success;
        this.blocks = immutableCopy(blocks);
        this.warnings = immutableCopy(warnings);
        this.errors = immutableCopy(errors);
        this.metadata = metadata;
    }

    public static StructureLibImportResult success(List<PlacedBlock> blocks, List<String> warnings,
        @Nullable StructureLibSceneMetadata metadata) {
        return new StructureLibImportResult(true, blocks, warnings, Collections.<String>emptyList(), metadata);
    }

    public static StructureLibImportResult failure(String error) {
        return failure(error, Collections.<String>emptyList(), null);
    }

    public static StructureLibImportResult failure(String error, List<String> warnings,
        @Nullable StructureLibSceneMetadata metadata) {
        String normalized = normalizeMessage(error);
        return new StructureLibImportResult(
            false,
            Collections.<PlacedBlock>emptyList(),
            warnings,
            Collections.singletonList(normalized),
            metadata);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<PlacedBlock> getBlocks() {
        return blocks;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Nullable
    public StructureLibSceneMetadata getMetadata() {
        return metadata;
    }

    private static <T> List<T> immutableCopy(@Nullable List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static String normalizeMessage(@Nullable String message) {
        if (message == null) {
            return "Unknown StructureLib import error";
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? "Unknown StructureLib import error" : trimmed;
    }

    public static class PlacedBlock {

        private final int x;
        private final int y;
        private final int z;
        private final Block block;
        private final int meta;
        @Nullable
        private final NBTTagCompound tileTag;
        @Nullable
        private final String blockId;

        public PlacedBlock(int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound tileTag,
            @Nullable String blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.tileTag = tileTag != null ? (NBTTagCompound) tileTag.copy() : null;
            this.blockId = normalizeBlockId(blockId);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public Block getBlock() {
            return block;
        }

        public int getMeta() {
            return meta;
        }

        @Nullable
        public NBTTagCompound getTileTag() {
            return tileTag;
        }

        @Nullable
        public String getBlockId() {
            return blockId;
        }

        @Nullable
        private static String normalizeBlockId(@Nullable String blockId) {
            if (blockId == null) {
                return null;
            }
            String trimmed = blockId.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
