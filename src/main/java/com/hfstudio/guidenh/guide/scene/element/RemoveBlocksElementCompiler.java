package com.hfstudio.guidenh.guide.scene.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class RemoveBlocksElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("RemoveBlocks");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var pair = MdxAttrs.getRequiredBlockAndId(compiler, errorSink, el, "id");
        if (pair == null) return;
        Block target = pair.getRight();

        List<int[]> toRemove = new ArrayList<>();
        for (int[] pos : level.getFilledBlocks()) {
            if (level.getBlock(pos[0], pos[1], pos[2]) == target) {
                toRemove.add(pos);
            }
        }
        for (int[] p : toRemove) {
            level.setBlock(p[0], p[1], p[2], Blocks.air, 0, null);
        }
    }
}
