package com.hfstudio.guidenh.guide.internal.structure;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public class GuideStructurePlacementService {

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

    public static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        return GuideTextNbtCodec.readStructureNbt(data);
    }

    @FunctionalInterface
    interface StructureBlockResolver {

        Block resolve(String name);
    }
}
