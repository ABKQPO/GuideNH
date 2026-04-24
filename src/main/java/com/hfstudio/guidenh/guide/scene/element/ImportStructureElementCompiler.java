package com.hfstudio.guidenh.guide.scene.element;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code <ImportStructure src="redstone_test.nbt" x="0" y="0" z="0" />}。
 *
 * <pre>
 * { size: [x,y,z],
 *   palette: [ {Name:"minecraft:stone"}, {Name:"minecraft:chest"}, ... ],
 *   blocks: [ {pos:[x,y,z], state: int, nbt: {...} }, ... ]  }
 * </pre>
 *
 */
public class ImportStructureElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ImportStructure");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {

        var src = MdxAttrs.getString(compiler, errorSink, el, "src", null);
        if (src == null || src.isEmpty()) {
            errorSink.appendError(compiler, "Missing src attribute", el);
            return;
        }
        ResourceLocation absSrc;
        try {
            absSrc = IdUtils.resolveLink(src, compiler.getPageId());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Invalid structure path: " + src, el);
            return;
        }
        byte[] data = compiler.loadAsset(absSrc);
        if (data == null) {
            errorSink.appendError(compiler, "Missing structure file: " + absSrc, el);
            return;
        }

        NBTTagCompound root;
        try {
            root = readCompound(data);
        } catch (Exception e) {
            errorSink.appendError(compiler, "Couldn't read structure: " + e.getMessage(), el);
            return;
        }

        int offsetX = MdxAttrs.getInt(compiler, errorSink, el, "x", 0);
        int offsetY = MdxAttrs.getInt(compiler, errorSink, el, "y", 0);
        int offsetZ = MdxAttrs.getInt(compiler, errorSink, el, "z", 0);

        if (!root.hasKey("palette") || !root.hasKey("blocks")) {
            errorSink.appendError(compiler, "Unsupported structure format (missing palette/blocks)", el);
            return;
        }

        NBTTagList paletteTag = root.getTagList("palette", 10);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < paletteTag.tagCount(); i++) {
            var entry = paletteTag.getCompoundTagAt(i);
            palette[i] = entry.getString("Name");
        }

        NBTTagList blocksTag = root.getTagList("blocks", 10);
        int placed = 0;
        for (int i = 0; i < blocksTag.tagCount(); i++) {
            var b = blocksTag.getCompoundTagAt(i);
            int state = b.getInteger("state");
            if (state < 0 || state >= palette.length) continue;
            String name = palette[state];
            Block block = (Block) Block.blockRegistry.getObject(name);
            if (block == null) continue;

            NBTTagList posTag = b.getTagList("pos", 3); // 3 = int
            if (posTag.tagCount() < 3) continue;
            int px = offsetX + intAt(posTag, 0);
            int py = offsetY + intAt(posTag, 0);
            int pz = offsetZ + intAt(posTag, 0);

            TileEntity te = null;
            if (b.hasKey("nbt", 10)) {
                try {
                    te = TileEntity.createAndLoadEntity(b.getCompoundTag("nbt"));
                } catch (Exception ignored) {}
            }
            level.setBlock(px, py, pz, block, 0, te);
            placed++;
        }

        if (placed == 0) {
            errorSink.appendError(compiler, "Structure had no placeable blocks: " + absSrc, el);
        }
    }

    private static int intAt(NBTTagList list, int i) {
        var base = list.removeTag(0);
        if (base instanceof NBTTagInt ti) {
            return ti.func_150287_d();
        }
        return 0;
    }

    private static NBTTagCompound readCompound(byte[] data) throws Exception {
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(data)); var dis = new DataInputStream(gzip)) {
            return CompressedStreamTools.read(dis);
        } catch (Exception gzipErr) {
            try (var dis = new DataInputStream(new ByteArrayInputStream(data))) {
                return CompressedStreamTools.read(dis);
            }
        }
    }
}
