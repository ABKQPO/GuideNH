package com.hfstudio.guidenh.guide.scene.ponder;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * A single keyframe in a Ponder timeline.
 * At the specified {@code time} (in game ticks) the camera and annotations transition to this state.
 *
 * <p>
 * Optional {@code blockChanges} list specifies blocks to place or remove when this keyframe
 * first becomes active during forward playback or when seeking. All positions are restored to their
 * initial structure state before changes from keyframes 0..current are re-applied, ensuring correct
 * appearance at any seek point.
 */
public class PonderKeyframe {

    private int time;
    @Nullable
    private String label;
    @Nullable
    private String labelKey;
    @Nullable
    private PonderKeyframeCameraState camera;
    @Nullable
    private Integer layer;
    @Nullable
    private List<PonderKeyframeAnnotation> annotations;
    @Nullable
    private List<PonderKeyframeSound> sounds;
    @Nullable
    private List<PonderKeyframeParticle> particles;
    @Nullable
    private List<PonderKeyframeBlockChange> blockChanges;
    @Nullable
    private List<PonderKeyframeTileNbtOperation> mergeTileNBT;
    @Nullable
    private List<PonderKeyframeTileNbtOperation> modifyTileNBT;
    @Nullable
    private List<PonderKeyframeTileNbtOperation> removeTileNBT;
    @Nullable
    private List<PonderKeyframeEntityAction> createEntities;
    @Nullable
    private List<PonderKeyframeEntityAction> setEntityNBT;
    @Nullable
    private List<PonderKeyframeEntityAction> mergeEntityNBT;
    @Nullable
    private List<PonderKeyframeEntityAction> modifyEntityNBT;
    @Nullable
    private List<PonderKeyframeEntityAction> removeEntityNBT;
    @Nullable
    private List<PonderKeyframeEntityAction> removeEntities;
    @Nullable
    private List<PonderKeyframeEntityAnimation> animateEntities;
    /**
     * Maximum number of ticks over which the camera eases from the previous keyframe's position
     * to this keyframe's position.
     * <ul>
     * <li>{@code null} (absent in JSON) — ease over the full duration of the segment (default behaviour).</li>
     * <li>{@code 0} — instant snap to this keyframe's camera with no interpolation.</li>
     * <li>{@code N > 0} — ease completes in {@code N} ticks; camera holds at the target for the
     * rest of the segment.</li>
     * </ul>
     */
    @Nullable
    private Integer cameraEaseTicks;

    public int getTime() {
        return time;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getLabelKey() {
        return labelKey;
    }

    public void applyLocalizedLabel(@Nullable String localizedLabel) {
        if (localizedLabel == null || localizedLabel.isEmpty()) {
            return;
        }
        this.label = localizedLabel;
    }

    @Nullable
    public PonderKeyframeCameraState getCamera() {
        return camera;
    }

    /**
     * The visible layer override for this keyframe.
     * {@code null} means show all layers; a non-negative integer restricts to that layer.
     */
    @Nullable
    public Integer getLayer() {
        return layer;
    }

    public List<PonderKeyframeAnnotation> getAnnotations() {
        return annotations != null ? annotations : List.of();
    }

    public List<PonderKeyframeSound> getSounds() {
        return sounds != null ? sounds : List.of();
    }

    public List<PonderKeyframeParticle> getParticles() {
        return particles != null ? particles : List.of();
    }

    /**
     * Block replacements to apply when this keyframe becomes active.
     * The runtime restores all changed positions to their initial state before re-applying
     * changes from keyframes 0..active, so seeking backwards works correctly.
     */
    public List<PonderKeyframeBlockChange> getBlockChanges() {
        return blockChanges != null ? blockChanges : List.of();
    }

    public List<PonderKeyframeTileNbtOperation> getMergeTileNBT() {
        return mergeTileNBT != null ? mergeTileNBT : List.of();
    }

    public List<PonderKeyframeTileNbtOperation> getModifyTileNBT() {
        return modifyTileNBT != null ? modifyTileNBT : List.of();
    }

    public List<PonderKeyframeTileNbtOperation> getRemoveTileNBT() {
        return removeTileNBT != null ? removeTileNBT : List.of();
    }

    public List<PonderKeyframeEntityAction> getCreateEntities() {
        return createEntities != null ? createEntities : List.of();
    }

    public List<PonderKeyframeEntityAction> getSetEntityNBT() {
        return setEntityNBT != null ? setEntityNBT : List.of();
    }

    public List<PonderKeyframeEntityAction> getMergeEntityNBT() {
        return mergeEntityNBT != null ? mergeEntityNBT : List.of();
    }

    public List<PonderKeyframeEntityAction> getModifyEntityNBT() {
        return modifyEntityNBT != null ? modifyEntityNBT : List.of();
    }

    public List<PonderKeyframeEntityAction> getRemoveEntityNBT() {
        return removeEntityNBT != null ? removeEntityNBT : List.of();
    }

    public List<PonderKeyframeEntityAction> getRemoveEntities() {
        return removeEntities != null ? removeEntities : List.of();
    }

    public List<PonderKeyframeEntityAnimation> getAnimateEntities() {
        return animateEntities != null ? animateEntities : List.of();
    }

    /**
     * Camera easing duration for the transition from the previous keyframe into this one.
     * {@code null} means full-segment ease; {@code 0} means instant snap.
     */
    @Nullable
    public Integer getCameraEaseTicks() {
        return cameraEaseTicks;
    }
}
