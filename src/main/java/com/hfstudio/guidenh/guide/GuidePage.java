package com.hfstudio.guidenh.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public final class GuidePage {

    private final String sourcePack;
    private final ResourceLocation id;
    private final LytDocument document;
    private final List<LytGuidebookScene> scenes;

    public GuidePage(String sourcePack, ResourceLocation id, LytDocument document) {
        this.sourcePack = sourcePack;
        this.id = id;
        this.document = document;
        this.scenes = collectScenes(document);
    }

    public String sourcePack() {
        return sourcePack;
    }

    public ResourceLocation id() {
        return id;
    }

    public LytDocument document() {
        return document;
    }

    public void prepareForDisplay() {
        document.setHoveredElement(null);
        for (var scene : scenes) {
            scene.resetInteractiveState();
        }
    }

    private static List<LytGuidebookScene> collectScenes(LytDocument document) {
        ArrayList<LytGuidebookScene> scenes = new ArrayList<>();
        ArrayDeque<LytNode> pending = new ArrayDeque<>();
        pending.add(document);

        while (!pending.isEmpty()) {
            var node = pending.removeLast();
            if (node instanceof LytGuidebookScene scene) {
                scenes.add(scene);
            }

            var children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                pending.addLast(children.get(i));
            }
        }

        return scenes;
    }
}
