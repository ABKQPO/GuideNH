package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;

import lombok.Getter;

@Getter
public class PonderKeyframeSound {

    @Nullable
    private String sound;
    @Nullable
    private String src;
    private float volume = GuideSoundSpec.DEFAULT_VOLUME;
    private float pitch = GuideSoundSpec.DEFAULT_PITCH;
    private int cooldown = GuideSoundSpec.DEFAULT_COOLDOWN_MILLIS;
    private float radius = GuideSoundSpec.DEFAULT_RADIUS;
    private float minVolume = GuideSoundSpec.DEFAULT_MIN_VOLUME;
    @Nullable
    private Float x;
    @Nullable
    private Float y;
    @Nullable
    private Float z;

}
