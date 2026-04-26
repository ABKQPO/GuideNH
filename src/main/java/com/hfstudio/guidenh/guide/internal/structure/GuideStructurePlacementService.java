package com.hfstudio.guidenh.guide.internal.structure;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public final class GuideStructurePlacementService {

    private final StructureBlockResolver blockResolver;

    public GuideStructurePlacementService() {
        this(name -> (Block) Block.blockRegistry.getObject(name));
    }

    GuideStructurePlacementService(StructureBlockResolver blockResolver) {
        this.blockResolver = blockResolver;
    }

    public GuideStructureData parse(String structureText) throws Exception {
        NBTTagCompound root = readStructureNbt(structureText.getBytes(StandardCharsets.UTF_8));
        int[] size = root.getIntArray("size");
        int sizeX = size.length > 0 ? size[0] : 0;
        int sizeY = size.length > 1 ? size[1] : 0;
        int sizeZ = size.length > 2 ? size[2] : 0;
        return new GuideStructureData(root, sizeX, sizeY, sizeZ);
    }

    public void place(GuideStructurePlacementTarget target, GuideStructureData structure, int offsetX, int offsetY,
        int offsetZ) {
        NBTTagCompound root = structure.getRoot();
        NBTTagList paletteTag = root.getTagList("palette", 10);
        String[] palette = new String[paletteTag.tagCount()];
        for (int index = 0; index < paletteTag.tagCount(); index++) {
            palette[index] = paletteTag.getCompoundTagAt(index)
                .getString("Name");
        }

        NBTTagList blocksTag = root.getTagList("blocks", 10);
        for (int index = 0; index < blocksTag.tagCount(); index++) {
            NBTTagCompound blockTag = blocksTag.getCompoundTagAt(index);
            int state = blockTag.getInteger("state");
            if (state < 0 || state >= palette.length) {
                continue;
            }
            Block block = blockResolver.resolve(palette[state]);
            if (block == null) {
                continue;
            }
            int[] pos = blockTag.getIntArray("pos");
            if (pos.length < 3) {
                continue;
            }
            int meta = blockTag.hasKey("meta") ? blockTag.getInteger("meta") : 0;
            TileEntity tileEntity = null;
            if (blockTag.hasKey("nbt", 10)) {
                try {
                    tileEntity = TileEntity.createAndLoadEntity(blockTag.getCompoundTag("nbt"));
                } catch (Exception ignored) {}
            }
            target.placeBlock(offsetX + pos[0], offsetY + pos[1], offsetZ + pos[2], block, meta, tileEntity);
        }
    }

    public void placeAll(GuideStructurePlacementTarget target, List<GuideStructureData> structures, int startX,
        int startY, int startZ) {
        int offsetX = startX;
        for (GuideStructureData structure : structures) {
            place(target, structure, offsetX, startY, startZ);
            offsetX += Math.max(structure.getSizeX(), 0);
        }
    }

    private static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        if (looksLikeText(data)) {
            String text = new String(data, StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            NBTBase parsed = JsonToNBT.func_150315_a(text);
            if (parsed instanceof NBTTagCompound compound) {
                return compound;
            }
            throw new IllegalStateException("SNBT root must be a Compound");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
            DataInputStream input = new DataInputStream(gzip)) {
            return CompressedStreamTools.read(input);
        } catch (Exception ignored) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                return CompressedStreamTools.read(input);
            }
        }
    }

    private static boolean looksLikeText(byte[] data) {
        int index = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            index = 3;
        }
        while (index < data.length) {
            byte next = data[index];
            if (next == ' ' || next == '\t' || next == '\r' || next == '\n') {
                index++;
                continue;
            }
            return next == '{';
        }
        return false;
    }

    @FunctionalInterface
    interface StructureBlockResolver {

        Block resolve(String name);
    }
}
