package com.hfstudio.guidenh.guide.scene;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;

public class SceneSoundCue {

    private final GuideSoundTrigger trigger;
    private final GuideSoundSpec sound;
    @Nullable
    private StructureLibSceneCondition structureLibCondition;
    private boolean entered;
    private boolean hovered;

    public SceneSoundCue(GuideSoundTrigger trigger, GuideSoundSpec sound) {
        this.trigger = trigger != null ? trigger : GuideSoundTrigger.CLICK;
        this.sound = sound;
    }

    public GuideSoundTrigger getTrigger() {
        return trigger;
    }

    public GuideSoundSpec getSound() {
        return sound;
    }

    @Nullable
    public StructureLibSceneCondition getStructureLibCondition() {
        return structureLibCondition;
    }

    public void setStructureLibCondition(@Nullable StructureLibSceneCondition structureLibCondition) {
        this.structureLibCondition = structureLibCondition;
    }

    public boolean isEntered() {
        return entered;
    }

    public void setEntered(boolean entered) {
        this.entered = entered;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean matches(GuideSoundTrigger trigger) {
        return this.trigger == trigger;
    }
}
