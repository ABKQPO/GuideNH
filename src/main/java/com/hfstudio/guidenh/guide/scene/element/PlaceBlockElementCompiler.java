package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.ScenePreviewFormedState;
import com.hfstudio.guidenh.guide.scene.support.SceneStructureOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class PlaceBlockElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("PlaceBlock");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }
        String idRaw = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (idRaw == null || idRaw.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing id attribute.", el);
            return;
        }

        GuideBlockMatcher blockMatcher;
        try {
            blockMatcher = GuideBlockMatcher.parse(idRaw.trim());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return;
        }

        Block block = (Block) Block.blockRegistry.getObject(blockMatcher.getBlockId());
        if (block == null) {
            errorSink.appendError(compiler, "Unknown block: " + blockMatcher.getBlockId(), el);
            return;
        }
        int meta = blockMatcher.getMeta() != null ? blockMatcher.getMeta() : 0;

        String nbtStr = MdxAttrs.getString(compiler, errorSink, el, "nbt", null);
        NBTTagCompound tileTag = null;
        if (nbtStr != null && !nbtStr.trim()
            .isEmpty()) {
            try {
                tileTag = GuideTextNbtCodec.readTextSafeCompound(nbtStr.trim());
            } catch (Exception e) {
                errorSink.appendError(compiler, "Bad nbt: " + e.getMessage(), el);
                return;
            }
        }

        int x = MdxAttrs.getInt(compiler, errorSink, el, "x", 0);
        int y = MdxAttrs.getInt(compiler, errorSink, el, "y", 0);
        int z = MdxAttrs.getInt(compiler, errorSink, el, "z", 0);
        int dx = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dx", 1));
        int dy = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dy", 1));
        int dz = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dz", 1));
        boolean formed = SceneStructureOptions.isFormed(compiler, errorSink, el);

        String explicitId = blockMatcher.getBlockId();
        int endX = x + dx;
        int endY = y + dy;
        int endZ = z + dz;
        for (int bx = x; bx < endX; bx++) {
            for (int by = y; by < endY; by++) {
                if (by < 0 || by >= level.getHeight()) {
                    continue;
                }
                for (int bz = z; bz < endZ; bz++) {
                    NBTTagCompound tagCopy = tileTag != null ? (NBTTagCompound) tileTag.copy() : null;
                    GuidebookPreviewBlockPlacer.place(level, bx, by, bz, block, meta, tagCopy, explicitId);
                    ScenePreviewFormedState.updateAfterPlacement(level, bx, by, bz, formed);
                }
            }
        }
    }
}
