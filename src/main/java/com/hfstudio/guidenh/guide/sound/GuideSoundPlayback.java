package com.hfstudio.guidenh.guide.sound;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.FMLLog;

public class GuideSoundPlayback {

    private static final Map<String, Long> LAST_PLAYED_AT = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_WARNED_AT = new ConcurrentHashMap<>();
    private static final Set<ISound> ACTIVE_SOUNDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final long WARN_INTERVAL_MILLIS = 30000L;

    private GuideSoundPlayback() {}

    public static boolean play(@Nullable GuideSoundSpec sound) {
        return play(sound, sound != null ? sound.volume() : 0.0f);
    }

    public static boolean play(@Nullable GuideSoundSpec sound, float effectiveVolume) {
        if (sound == null || effectiveVolume <= 0.0f) {
            return false;
        }
        if (!checkCooldown(sound)) {
            return true;
        }
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.getSoundHandler() == null) {
                return false;
            }
            pruneInactive(minecraft.getSoundHandler());
            PositionedSoundRecord record = new PositionedSoundRecord(
                sound.soundId(),
                effectiveVolume,
                sound.pitch(),
                0.0f,
                0.0f,
                0.0f);
            minecraft.getSoundHandler()
                .playSound(record);
            ACTIVE_SOUNDS.add(record);
            rememberPlayed(sound);
            return true;
        } catch (RuntimeException e) {
            warnLowFrequency(sound.soundId(), e);
            return false;
        }
    }

    public static void stopAll() {
        Minecraft minecraft = Minecraft.getMinecraft();
        SoundHandler soundHandler = minecraft != null ? minecraft.getSoundHandler() : null;
        if (soundHandler == null) {
            ACTIVE_SOUNDS.clear();
            return;
        }
        for (ISound sound : ACTIVE_SOUNDS) {
            try {
                soundHandler.stopSound(sound);
            } catch (RuntimeException ignored) {}
        }
        ACTIVE_SOUNDS.clear();
    }

    public static float attenuate(GuideSoundSpec sound, float distance, float fallbackRadius) {
        float radius = sound.radius() > 0.0f ? sound.radius() : fallbackRadius;
        if (radius <= 0.0f) {
            return sound.volume();
        }
        float factor = 1.0f - Math.max(0.0f, distance) / radius;
        factor = Math.max(sound.minVolume(), Math.min(1.0f, factor));
        return sound.volume() * factor;
    }

    private static boolean checkCooldown(GuideSoundSpec sound) {
        int cooldown = sound.cooldownMillis();
        if (cooldown <= 0) {
            return true;
        }
        Long last = LAST_PLAYED_AT.get(cooldownKey(sound));
        return last == null || System.currentTimeMillis() - last >= cooldown;
    }

    private static void rememberPlayed(GuideSoundSpec sound) {
        LAST_PLAYED_AT.put(cooldownKey(sound), System.currentTimeMillis());
    }

    private static void pruneInactive(SoundHandler soundHandler) {
        ACTIVE_SOUNDS.removeIf(sound -> !soundHandler.isSoundPlaying(sound));
    }

    private static String cooldownKey(GuideSoundSpec sound) {
        return sound.soundId()
            .toString();
    }

    private static void warnLowFrequency(ResourceLocation soundId, RuntimeException e) {
        long now = System.currentTimeMillis();
        String key = soundId.toString();
        Long last = LAST_WARNED_AT.get(key);
        if (last != null && now - last < WARN_INTERVAL_MILLIS) {
            return;
        }
        LAST_WARNED_AT.put(key, now);
        FMLLog.getLogger()
            .warn("[GuideNH] [GuideSoundPlayback] Failed to play sound {}", soundId, e);
    }
}
