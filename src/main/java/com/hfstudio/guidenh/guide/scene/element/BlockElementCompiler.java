package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Block");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var pair = MdxAttrs.getRequiredBlockAndId(compiler, errorSink, el, "id");
        if (pair == null) return;
        Block block = pair.getRight();

        int x = MdxAttrs.getInt(compiler, errorSink, el, "x", 0);
        int y = MdxAttrs.getInt(compiler, errorSink, el, "y", 0);
        int z = MdxAttrs.getInt(compiler, errorSink, el, "z", 0);
        int meta = MdxAttrs.getInt(compiler, errorSink, el, "meta", Integer.MIN_VALUE);
        String facing = MdxAttrs.getString(compiler, errorSink, el, "facing", null);
        if (meta == Integer.MIN_VALUE) {
            meta = defaultMetaFor(block, facing);
        }

        NBTTagCompound tileTag = null;
        String nbtStr = MdxAttrs.getString(compiler, errorSink, el, "nbt", null);
        if (nbtStr != null && !nbtStr.isEmpty()) {
            try {
                tileTag = GuideTextNbtCodec.readTextSafeCompound(nbtStr);
            } catch (Exception e) {
                errorSink.appendError(compiler, "Bad NBT: " + e.getMessage(), el);
            }
        }
        String explicitBlockId = pair.getLeft()
            .toString();
        GuidebookPreviewBlockPlacer.place(level, x, y, z, block, meta, tileTag, explicitBlockId);
        level.setExplicitBlockId(x, y, z, explicitBlockId);
    }

    private static int defaultMetaFor(Block block, String facing) {
        int facingMeta = parseFacing(facing);
        if (facingMeta >= 0) return facingMeta;
        if (block == Blocks.furnace || block == Blocks.lit_furnace
            || block == Blocks.dispenser
            || block == Blocks.dropper
            || block == Blocks.chest
            || block == Blocks.trapped_chest
            || block == Blocks.ender_chest
            || block == Blocks.hopper) {
            return 3;
        }
        return 0;
    }

    private static int parseFacing(String facing) {
        if (facing == null || facing.isEmpty()) return -1;
        return switch (facing.toLowerCase(Locale.ROOT)) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> -1;
        };
    }
}
