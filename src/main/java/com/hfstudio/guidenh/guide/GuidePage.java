package com.hfstudio.guidenh.guide;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

@Desugar
public record GuidePage(String sourcePack, ResourceLocation id, LytDocument document) {

    public void prepareForDisplay() {
        document.setHoveredElement(null);
        document.invalidateLayout();
        document.visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                if (node instanceof LytGuidebookScene scene) {
                    scene.resetInteractiveState();
                }
                return Result.CONTINUE;
            }
        });
    }
}
