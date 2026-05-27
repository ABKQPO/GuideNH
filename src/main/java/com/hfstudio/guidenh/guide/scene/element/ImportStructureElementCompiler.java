package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.support.ScenePreviewFormedState;
import com.hfstudio.guidenh.guide.scene.support.SceneStructureOptions;
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
 *   blocks: [ {pos: [rx, ry, rz], state: 0, meta: 0, nbt: {...},
 *              guidenh_server_preview_supplement: { guidenh.ae2.cable_bus: { v: 1, b64: "<payload>" } } }, ... ] }
 * </pre>
 *
 * <p>
 * The optional {@code nbt} compound is applied to the block entity. Supplying an {@code id} is preferred,
 * but if deserialization by id fails and the block can create its own tile entity, GuideNH falls back to
 * constructing the tile from the block and then applying the NBT manually.
 */
public class ImportStructureElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ImportStructure");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }

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

        int offsetX = MdxAttrs.getString(compiler, errorSink, el, "offsetX", null) != null
            ? MdxAttrs.getInt(compiler, errorSink, el, "offsetX", 0)
            : MdxAttrs.getInt(compiler, errorSink, el, "x", 0);
        int offsetY = MdxAttrs.getString(compiler, errorSink, el, "offsetY", null) != null
            ? MdxAttrs.getInt(compiler, errorSink, el, "offsetY", 0)
            : MdxAttrs.getInt(compiler, errorSink, el, "y", 0);
        int offsetZ = MdxAttrs.getString(compiler, errorSink, el, "offsetZ", null) != null
            ? MdxAttrs.getInt(compiler, errorSink, el, "offsetZ", 0)
            : MdxAttrs.getInt(compiler, errorSink, el, "z", 0);
        boolean formed = SceneStructureOptions.isFormed(compiler, errorSink, el);

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
            int py = Math.max(0, Math.min(offsetY + pos[1], level.getHeight() - 1));
            int pz = offsetZ + pos[2];

            int meta = b.hasKey("meta") ? b.getInteger("meta") : 0;

            NBTTagCompound tileTag = b.hasKey("nbt", 10) ? b.getCompoundTag("nbt") : null;
            GuidebookPreviewBlockPlacer.place(level, px, py, pz, block, meta, tileTag, name, b);
            ScenePreviewFormedState.updateAfterPlacement(level, px, py, pz, formed);
            placed++;
        }

        if (placed == 0) {
            errorSink.appendError(compiler, "Structure had no placeable blocks: " + absSrc, el);
        }

        // Spawn entities stored by the region-wand exporter (snbt+entities mode).
        if (root.hasKey("entities", 9)) {
            World fakeWorld = null;
            try {
                fakeWorld = level.getOrCreateFakeWorld();
            } catch (IllegalStateException ignored) {}
            // Proceed even if fakeWorld is null; entities bind to the preview world lazily on first render,
            // matching how EntityElementCompiler handles scene parsing before a client world exists.
            NBTTagList entitiesTag = root.getTagList("entities", 10);
            for (int i = 0; i < entitiesTag.tagCount(); i++) {
                NBTTagCompound et = entitiesTag.getCompoundTagAt(i);
                GuidebookSceneEntityImportSupport.ImportedSceneEntity importedEntity = GuidebookSceneEntityImportSupport
                    .loadImportedEntityRecord(fakeWorld, et, offsetX, offsetY, offsetZ, 0f, level.getHeight() - 1f);
                if (importedEntity != null) {
                    level.addEntity(importedEntity.entity(), importedEntity.sceneEntityId());
                    if (MdxAttrs.getBoolean(importedEntity.unmount(), false)) {
                        level.clearSceneEntityMount(importedEntity.sceneEntityId());
                    } else if (importedEntity.mountTargetSceneEntityId() != null) {
                        level.setSceneEntityMount(
                            importedEntity.sceneEntityId(),
                            importedEntity.mountTargetSceneEntityId());
                    }
                }
            }
        }
    }

    public static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        return GuideTextNbtCodec.readStructureNbt(data);
    }
}
