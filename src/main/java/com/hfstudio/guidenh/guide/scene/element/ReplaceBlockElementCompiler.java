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
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.ReplaceBlockExecutor;
import com.hfstudio.guidenh.guide.scene.support.SceneStructureOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ReplaceBlockElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ReplaceBlock");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }
        String fromRaw = MdxAttrs.getString(compiler, errorSink, el, "from", null);
        if (fromRaw == null || fromRaw.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing from attribute.", el);
            return;
        }

        GuideBlockMatcher fromMatcher;
        try {
            fromMatcher = GuideBlockMatcher.parse(fromRaw.trim());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return;
        }

        String fromNbtStr = MdxAttrs.getString(compiler, errorSink, el, "from_nbt", null);
        NBTTagCompound fromNbt = null;
        if (fromNbtStr != null && !fromNbtStr.trim()
            .isEmpty()) {
            try {
                fromNbt = GuideTextNbtCodec.readTextSafeCompound(fromNbtStr.trim());
            } catch (Exception e) {
                errorSink.appendError(compiler, "Bad from_nbt: " + e.getMessage(), el);
                return;
            }
        }

        String toRaw = MdxAttrs.getString(compiler, errorSink, el, "to", null);
        if (toRaw == null || toRaw.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing to attribute.", el);
            return;
        }

        GuideBlockMatcher toMatcher;
        try {
            toMatcher = GuideBlockMatcher.parse(toRaw.trim());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return;
        }

        Block toBlock = (Block) Block.blockRegistry.getObject(toMatcher.getBlockId());
        if (toBlock == null) {
            errorSink.appendError(compiler, "Unknown block: " + toMatcher.getBlockId(), el);
            return;
        }
        int toMeta = toMatcher.getMeta() != null ? toMatcher.getMeta() : 0;

        String toNbtStr = MdxAttrs.getString(compiler, errorSink, el, "to_nbt", null);
        NBTTagCompound toNbt = null;
        if (toNbtStr != null && !toNbtStr.trim()
            .isEmpty()) {
            try {
                toNbt = GuideTextNbtCodec.readTextSafeCompound(toNbtStr.trim());
            } catch (Exception e) {
                errorSink.appendError(compiler, "Bad to_nbt: " + e.getMessage(), el);
                return;
            }
        }

        boolean hasBounds = el.getAttribute("x") != null || el.getAttribute("y") != null
            || el.getAttribute("z") != null
            || el.getAttribute("dx") != null
            || el.getAttribute("dy") != null
            || el.getAttribute("dz") != null;

        int bx = MdxAttrs.getInt(compiler, errorSink, el, "x", 0);
        int by = MdxAttrs.getInt(compiler, errorSink, el, "y", 0);
        int bz = MdxAttrs.getInt(compiler, errorSink, el, "z", 0);
        int bdx = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dx", 1));
        int bdy = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dy", 1));
        int bdz = Math.max(1, MdxAttrs.getInt(compiler, errorSink, el, "dz", 1));
        boolean formed = SceneStructureOptions.isFormed(compiler, errorSink, el);

        ReplaceBlockExecutor.execute(
            level,
            fromMatcher,
            fromNbt,
            toBlock,
            toMeta,
            toNbt,
            toMatcher.getBlockId(),
            hasBounds,
            bx,
            by,
            bz,
            bdx,
            bdy,
            bdz,
            formed);
    }
}
