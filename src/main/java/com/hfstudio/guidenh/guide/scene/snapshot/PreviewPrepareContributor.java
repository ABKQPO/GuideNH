package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Runs after tiles are bound into the fake world, before scene rendering (TESR-facing fixes).
 */
public interface PreviewPrepareContributor {

    int priority();

    void prepare(GuidebookLevel level);
}
