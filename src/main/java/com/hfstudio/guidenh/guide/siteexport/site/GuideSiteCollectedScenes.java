package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;

import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public class GuideSiteCollectedScenes {

    private final List<LytGuidebookScene> htmlSceneSequence;
    private final List<LytGuidebookScene> uniqueScenes;

    public GuideSiteCollectedScenes(List<LytGuidebookScene> htmlSceneSequence, List<LytGuidebookScene> uniqueScenes) {
        this.htmlSceneSequence = htmlSceneSequence == null ? List.of() : List.copyOf(htmlSceneSequence);
        this.uniqueScenes = uniqueScenes == null ? List.of() : List.copyOf(uniqueScenes);
    }

    public List<LytGuidebookScene> htmlSceneSequence() {
        return htmlSceneSequence;
    }

    public List<LytGuidebookScene> uniqueScenes() {
        return uniqueScenes;
    }
}
