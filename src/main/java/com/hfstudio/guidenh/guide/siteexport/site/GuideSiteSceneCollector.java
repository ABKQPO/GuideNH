package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;

public final class GuideSiteSceneCollector {

    private GuideSiteSceneCollector() {}

    public static GuideSiteCollectedScenes collect(GuidePage compiledPage) {
        if (compiledPage == null || compiledPage.document() == null) {
            return new GuideSiteCollectedScenes(Collections.emptyList(), Collections.emptyList());
        }

        CollectorState state = new CollectorState();
        collectHtmlScenesFromNode(compiledPage.document(), state);

        while (!state.annotationPending.isEmpty()) {
            collectAnnotationTooltipScenes(state.annotationPending.removeFirst(), state);
        }

        return new GuideSiteCollectedScenes(state.htmlSceneSequence, state.uniqueScenes);
    }

    private static void collectHtmlScenesFromNode(LytNode node, CollectorState state) {
        collectScenesFromNode(node, state, true);
    }

    private static void collectScenesFromNode(LytNode node, CollectorState state, boolean includeInHtmlSequence) {
        if (node == null) {
            return;
        }

        if (node instanceof LytGuidebookScene scene) {
            addScene(scene, state, includeInHtmlSequence);
        }

        collectTooltipScenesFromInteractive(node, state, includeInHtmlSequence);

        for (LytNode child : node.getChildren()) {
            collectScenesFromNode(child, state, includeInHtmlSequence);
        }

        if (node instanceof LytParagraph paragraph) {
            for (LytFlowContent content : paragraph.getContent()) {
                collectScenesFromFlowContent(content, state, includeInHtmlSequence);
            }
        }
    }

    private static void collectScenesFromFlowContent(LytFlowContent content, CollectorState state,
        boolean includeInHtmlSequence) {
        if (content == null) {
            return;
        }

        if (content instanceof LytFlowInlineBlock inlineBlock && inlineBlock.getBlock() != null) {
            collectScenesFromNode(inlineBlock.getBlock(), state, includeInHtmlSequence);
        }

        if (content instanceof InteractiveElement interactiveElement) {
            collectTooltipScenes(resolveTooltip(interactiveElement), state, includeInHtmlSequence);
        }

        if (content instanceof LytFlowSpan span) {
            for (LytFlowContent child : span.getChildren()) {
                collectScenesFromFlowContent(child, state, includeInHtmlSequence);
            }
        }
    }

    private static void collectAnnotationTooltipScenes(LytGuidebookScene scene, CollectorState state) {
        if (scene == null) {
            return;
        }
        for (SceneAnnotation annotation : scene.getAnnotations()) {
            if (annotation == null) {
                continue;
            }
            collectTooltipScenes(annotation.getTooltip(), state, false);
        }
    }

    private static void collectTooltipScenesFromInteractive(Object candidate, CollectorState state,
        boolean includeInHtmlSequence) {
        if (!(candidate instanceof InteractiveElement interactiveElement)) {
            return;
        }
        collectTooltipScenes(resolveTooltip(interactiveElement), state, includeInHtmlSequence);
    }

    private static void collectTooltipScenes(GuideTooltip tooltip, CollectorState state,
        boolean includeInHtmlSequence) {
        if (!(tooltip instanceof ContentTooltip contentTooltip)) {
            return;
        }
        if (state.visitedTooltipContents.put(contentTooltip, Boolean.TRUE) != null) {
            return;
        }
        collectScenesFromNode(contentTooltip.getContent(), state, includeInHtmlSequence);
    }

    private static GuideTooltip resolveTooltip(InteractiveElement interactiveElement) {
        try {
            Optional<GuideTooltip> tooltip = interactiveElement.getTooltip(0, 0);
            return tooltip.orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void addScene(LytGuidebookScene scene, CollectorState state, boolean includeInHtmlSequence) {
        if (includeInHtmlSequence) {
            state.htmlSceneSequence.add(scene);
        }
        if (state.uniqueSceneSet.put(scene, Boolean.TRUE) != null) {
            return;
        }
        state.uniqueScenes.add(scene);
        state.annotationPending.addLast(scene);
    }

    private static final class CollectorState {

        private final ArrayList<LytGuidebookScene> htmlSceneSequence = new ArrayList<>();
        private final ArrayList<LytGuidebookScene> uniqueScenes = new ArrayList<>();
        private final IdentityHashMap<LytGuidebookScene, Boolean> uniqueSceneSet = new IdentityHashMap<>();
        private final IdentityHashMap<ContentTooltip, Boolean> visitedTooltipContents = new IdentityHashMap<>();
        private final ArrayDeque<LytGuidebookScene> annotationPending = new ArrayDeque<>();
    }
}
