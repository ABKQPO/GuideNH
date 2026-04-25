package com.hfstudio.guidenh.guide.scene.element;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
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
 * {@code <ImportStructure src="redstone_test.snbt" x="0" y="0" z="0" />}.
 *
 * <p>
 * Accepted file formats:
 * <ul>
 * <li>SNBT text (string-NBT, the {@code .snbt} format produced by the region wand);</li>
 * <li>Gzipped binary NBT (the vanilla {@code .nbt} structure layout);</li>
 * <li>Plain (uncompressed) binary NBT.</li>
 * </ul>
 *
 * <p>
 * <strong>SNBT dialect:</strong> 1.7.10's {@code JsonToNBT} parses an all-integer JSON-style array
 * directly as an {@code IntArray}, so {@code pos:[0,1,2]} and {@code size:[5,3,5]} are valid. Modern
 * typed-array prefixes such as {@code [I; ...]} / {@code [B; ...]} / {@code [L; ...]} are
 * <em>not</em> recognized by 1.7.10 and must be omitted. Numeric suffixes ({@code 5b}, {@code 12s},
 * {@code 1.5f}, {@code 7L}) are honored for the inner block compounds.
 *
 * <p>
 * Schema:
 *
 * <pre>
 * { size: [dx, dy, dz],
 *   palette: [ {Name: "minecraft:stone"}, {Name: "minecraft:chest"}, ... ],
 *   blocks: [ {pos: [rx, ry, rz], state: 0, meta: 0, nbt: {id:"Chest", Items:[...]}}, ... ] }
 * </pre>
 *
 * <p>
 * The optional {@code nbt} compound feeds {@link TileEntity#createAndLoadEntity(NBTTagCompound)} and
 * therefore must contain a vanilla {@code id} field (e.g. {@code "Chest"}, {@code "Furnace"}).
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
            root = readStructureNbt(data);
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

            int[] pos = b.getIntArray("pos");
            if (pos.length < 3) continue;
            int px = offsetX + pos[0];
            int py = offsetY + pos[1];
            int pz = offsetZ + pos[2];

            int meta = b.hasKey("meta") ? b.getInteger("meta") : 0;

            TileEntity te = null;
            if (b.hasKey("nbt", 10)) {
                try {
                    te = TileEntity.createAndLoadEntity(b.getCompoundTag("nbt"));
                } catch (Exception ignored) {}
            }
            level.setBlock(px, py, pz, block, meta, te);
            placed++;
        }

        if (placed == 0) {
            errorSink.appendError(compiler, "Structure had no placeable blocks: " + absSrc, el);
        }
    }

    private static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        if (looksLikeText(data)) {
            String text = new String(data, StandardCharsets.UTF_8);
            // Strip a UTF-8 BOM if present.
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            NBTBase parsed = JsonToNBT.func_150315_a(text);
            if (parsed instanceof NBTTagCompound c) return c;
            throw new IllegalStateException("SNBT root must be a Compound");
        }
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(data)); var dis = new DataInputStream(gzip)) {
            return CompressedStreamTools.read(dis);
        } catch (Exception ignored) {
            try (var dis = new DataInputStream(new ByteArrayInputStream(data))) {
                return CompressedStreamTools.read(dis);
            }
        }
    }

    private static boolean looksLikeText(byte[] data) {
        // Skip BOM + leading whitespace; SNBT roots always start with '{'.
        int i = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < data.length) {
            byte b = data[i];
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                i++;
                continue;
            }
            return b == '{';
        }
        return false;
    }
}
