package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Collections;
import java.util.List;

import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public final class GuideSiteCollectedScenes {

    private final List<LytGuidebookScene> htmlSceneSequence;
    private final List<LytGuidebookScene> uniqueScenes;

    public GuideSiteCollectedScenes(List<LytGuidebookScene> htmlSceneSequence, List<LytGuidebookScene> uniqueScenes) {
        this.htmlSceneSequence = htmlSceneSequence == null ? Collections.emptyList()
            : Collections.unmodifiableList(htmlSceneSequence);
        this.uniqueScenes = uniqueScenes == null ? Collections.emptyList() : Collections.unmodifiableList(uniqueScenes);
    }

    public List<LytGuidebookScene> htmlSceneSequence() {
        return htmlSceneSequence;
    }

    public List<LytGuidebookScene> uniqueScenes() {
        return uniqueScenes;
    }
}
