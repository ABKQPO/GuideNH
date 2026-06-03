package com.hfstudio.guidenh.guide.scene.ponder;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Root data object loaded from an {@code ImportPonder} JSON file.
 * Holds the total animation duration in ticks and the ordered list of keyframes.
 */
public class PonderSceneData {

    private int totalTime;
    @Nullable
    private List<PonderKeyframe> keyframes;

    public int getTotalTime() {
        return Math.max(1, totalTime);
    }

    public List<PonderKeyframe> getKeyframes() {
        return keyframes != null ? keyframes : List.of();
    }

    public int getKeyframeCount() {
        return getKeyframes().size();
    }

    /**
     * Returns the keyframe at the given index, or {@code null} when out of bounds.
     */
    @Nullable
    public PonderKeyframe getKeyframe(int index) {
        List<PonderKeyframe> kfs = getKeyframes();
        if (index < 0 || index >= kfs.size()) {
            return null;
        }
        return kfs.get(index);
    }

    /**
     * Returns the index of the last keyframe whose {@code time} is &le; {@code tick},
     * or {@code -1} when no keyframe has been reached yet.
     */
    public int resolveActiveKeyframeIndex(int tick) {
        List<PonderKeyframe> kfs = getKeyframes();
        int active = -1;
        for (int i = 0; i < kfs.size(); i++) {
            if (kfs.get(i)
                .getTime() <= tick) {
                active = i;
            } else {
                break;
            }
        }
        return active;
    }

    /**
     * Returns the progress as a float in {@code [0, 1]} for the given tick.
     */
    public float getProgress(int tick) {
        int total = getTotalTime();
        if (total <= 0) {
            return 1f;
        }
        return Math.clamp(tick / (float) total, 0f, 1f);
    }
}
