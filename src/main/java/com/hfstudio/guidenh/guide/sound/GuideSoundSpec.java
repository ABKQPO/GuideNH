package com.hfstudio.guidenh.guide.sound;

import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class GuideSoundSpec {

    public static final float DEFAULT_VOLUME = 1.0f;
    public static final float DEFAULT_PITCH = 1.0f;
    public static final int DEFAULT_COOLDOWN_MILLIS = 250;
    public static final float DEFAULT_RADIUS = -1.0f;
    public static final float DEFAULT_MIN_VOLUME = 0.15f;

    private final ResourceLocation soundId;
    private final float volume;
    private final float pitch;
    private final int cooldownMillis;
    private final float radius;
    private final float minVolume;
    @Nullable
    private final Float x;
    @Nullable
    private final Float y;
    @Nullable
    private final Float z;

    public GuideSoundSpec(ResourceLocation soundId, float volume, float pitch, int cooldownMillis, float radius,
        float minVolume, @Nullable Float x, @Nullable Float y, @Nullable Float z) {
        if (soundId == null) {
            throw new IllegalArgumentException("soundId must not be null");
        }
        this.soundId = soundId;
        this.volume = Math.max(0.0f, volume);
        this.pitch = Math.max(0.01f, pitch);
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.radius = radius;
        this.minVolume = Math.clamp(minVolume, 0.0f, 1.0f);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Builder builder(ResourceLocation soundId) {
        return new Builder(soundId);
    }

    public static ResourceLocation soundIdFromSource(ResourceLocation sourceId) {
        String path = sourceId.getResourcePath()
            .replace('\\', '/');
        if (path.startsWith("sounds/")) {
            path = path.substring("sounds/".length());
        }
        if (path.endsWith(".ogg")) {
            path = path.substring(0, path.length() - ".ogg".length());
        }
        path = path.replace('/', '.')
            .toLowerCase(Locale.ROOT);
        return new ResourceLocation(sourceId.getResourceDomain(), path);
    }

    public boolean hasPosition() {
        return x != null && y != null && z != null;
    }

    public ResourceLocation soundId() {
        return soundId;
    }

    public float volume() {
        return volume;
    }

    public float pitch() {
        return pitch;
    }

    public int cooldownMillis() {
        return cooldownMillis;
    }

    public float radius() {
        return radius;
    }

    public float minVolume() {
        return minVolume;
    }

    @Nullable
    public Float x() {
        return x;
    }

    @Nullable
    public Float y() {
        return y;
    }

    @Nullable
    public Float z() {
        return z;
    }

    public GuideSoundSpec withVolume(float newVolume) {
        return new GuideSoundSpec(soundId, newVolume, pitch, cooldownMillis, radius, minVolume, x, y, z);
    }

    public static class Builder {

        private final ResourceLocation soundId;
        private float volume = DEFAULT_VOLUME;
        private float pitch = DEFAULT_PITCH;
        private int cooldownMillis = DEFAULT_COOLDOWN_MILLIS;
        private float radius = DEFAULT_RADIUS;
        private float minVolume = DEFAULT_MIN_VOLUME;
        @Nullable
        private Float x;
        @Nullable
        private Float y;
        @Nullable
        private Float z;

        public Builder(ResourceLocation soundId) {
            this.soundId = soundId;
        }

        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder cooldownMillis(int cooldownMillis) {
            this.cooldownMillis = cooldownMillis;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder minVolume(float minVolume) {
            this.minVolume = minVolume;
            return this;
        }

        public Builder position(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public GuideSoundSpec build() {
            return new GuideSoundSpec(soundId, volume, pitch, cooldownMillis, radius, minVolume, x, y, z);
        }
    }
}
