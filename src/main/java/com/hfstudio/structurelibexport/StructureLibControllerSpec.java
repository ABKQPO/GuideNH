package com.hfstudio.structurelibexport;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;

public class StructureLibControllerSpec {

    private final String blockId;
    private final Block block;
    private final int meta;
    private final ItemStack displayStack;
    private final String displayName;

    public StructureLibControllerSpec(String blockId, Block block, int meta) {
        this(blockId, block, meta, createDisplayStack(block, meta));
    }

    public StructureLibControllerSpec(String blockId, Block block, int meta, @Nullable ItemStack displayStack) {
        this.blockId = blockId;
        this.block = block;
        this.meta = meta;
        this.displayStack = displayStack != null ? displayStack.copy() : createDisplayStack(block, meta);
        this.displayName = resolveDisplayName(displayStack, blockId, meta);
    }

    public static StructureLibControllerSpec parse(String raw) {
        GuideBlockMatcher matcher = GuideBlockMatcher.parse(raw);
        Block block = (Block) Block.blockRegistry.getObject(matcher.getBlockId());
        if (block == null || block == Blocks.air) {
            throw new IllegalArgumentException("Could not resolve controller block: " + raw);
        }
        return new StructureLibControllerSpec(
            matcher.getBlockId(),
            block,
            matcher.getMeta() != null ? matcher.getMeta() : 0);
    }

    public String getBlockId() {
        return blockId;
    }

    public Block getBlock() {
        return block;
    }

    public int getMeta() {
        return meta;
    }

    public ItemStack getDisplayStack() {
        return displayStack.copy();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getControllerArgument() {
        return blockId + ":" + meta;
    }

    private static ItemStack createDisplayStack(Block block, int meta) {
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return new ItemStack(block, 1, meta);
        }
        return new ItemStack(item, 1, meta);
    }

    private static String resolveDisplayName(@Nullable ItemStack stack, String blockId, int meta) {
        if (stack != null) {
            try {
                String name = stack.getDisplayName();
                if (name != null && !name.trim()
                    .isEmpty()) {
                    return name.trim();
                }
            } catch (Throwable ignored) {}
        }
        return blockId + ":" + meta;
    }
}
