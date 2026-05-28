package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;

public class GuideSiteSoundExport {

    private GuideSiteSoundExport() {}

    @Nullable
    public static GuideSoundSpec parse(MdxSoundAttributes attributes, String defaultNamespace,
        @Nullable ResourceLocation currentPageId) {
        String rawSound = attributes.sound();
        String rawSource = attributes.source();
        try {
            ResourceLocation soundId;
            if (hasText(rawSound)) {
                soundId = IdUtils.resolveId(rawSound.trim(), defaultNamespace);
            } else if (hasText(rawSource) && currentPageId != null) {
                soundId = GuideSoundSpec.soundIdFromSource(IdUtils.resolveLink(rawSource.trim(), currentPageId));
            } else {
                return null;
            }
            GuideSoundSpec.Builder builder = GuideSoundSpec.builder(soundId)
                .volume(attributes.floatValue("volume", GuideSoundSpec.DEFAULT_VOLUME))
                .pitch(attributes.floatValue("pitch", GuideSoundSpec.DEFAULT_PITCH))
                .cooldownMillis(attributes.intValue("cooldown", GuideSoundSpec.DEFAULT_COOLDOWN_MILLIS))
                .radius(attributes.floatValue("radius", GuideSoundSpec.DEFAULT_RADIUS))
                .minVolume(attributes.floatValue("minVolume", GuideSoundSpec.DEFAULT_MIN_VOLUME));
            Float x = attributes.optionalFloat("x");
            Float y = attributes.optionalFloat("y");
            Float z = attributes.optionalFloat("z");
            if (x != null || y != null || z != null) {
                builder.position(x != null ? x : 0.0f, y != null ? y : 0.0f, z != null ? z : 0.0f);
            }
            return builder.build();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String exportSource(GuideSoundSpec sound, MdxSoundAttributes attributes,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter) {
        if (assetExporter == null) {
            return "";
        }
        ResourceLocation assetId = resolveSoundAsset(sound, attributes, currentPageId);
        return assetExporter.exportSound(assetId);
    }

    public static @NotNull ResourceLocation resolveSoundAsset(GuideSoundSpec sound, MdxSoundAttributes attributes,
        @Nullable ResourceLocation currentPageId) {
        String rawSource = attributes.source();
        if (hasText(rawSource) && currentPageId != null) {
            try {
                return IdUtils.resolveLink(rawSource.trim(), currentPageId);
            } catch (IllegalArgumentException ignored) {}
        }
        String path = "sounds/" + sound.soundId()
            .getResourcePath()
            .replace('.', '/') + ".ogg";
        return new ResourceLocation(
            sound.soundId()
                .getResourceDomain(),
            path);
    }

    public static void appendDataAttributes(StringBuilder html, GuideSoundSpec sound, GuideSoundTrigger trigger,
        @Nullable String src, AttributeEscaper escaper) {
        html.append(" data-guide-sound=\"")
            .append(
                escaper.escape(
                    sound.soundId()
                        .toString()))
            .append("\"");
        if (src != null && !src.isEmpty()) {
            html.append(" data-guide-sound-src=\"")
                .append(escaper.escape(src))
                .append("\"");
        }
        html.append(" data-guide-sound-trigger=\"")
            .append(
                trigger.name()
                    .toLowerCase())
            .append("\" data-guide-sound-volume=\"")
            .append(sound.volume())
            .append("\" data-guide-sound-pitch=\"")
            .append(sound.pitch())
            .append("\" data-guide-sound-cooldown=\"")
            .append(sound.cooldownMillis())
            .append("\" data-guide-sound-radius=\"")
            .append(sound.radius())
            .append("\" data-guide-sound-min-volume=\"")
            .append(sound.minVolume())
            .append("\"");
        if (sound.hasPosition()) {
            html.append(" data-guide-sound-x=\"")
                .append(sound.x())
                .append("\" data-guide-sound-y=\"")
                .append(sound.y())
                .append("\" data-guide-sound-z=\"")
                .append(sound.z())
                .append("\"");
        }
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim()
            .isEmpty();
    }

    public interface MdxSoundAttributes {

        @Nullable
        String value(String name);

        default @Nullable String sound() {
            return value("sound");
        }

        default @Nullable String source() {
            return value("src");
        }

        default float floatValue(String name, float fallback) {
            String raw = value(name);
            if (raw == null || raw.trim()
                .isEmpty()) {
                return fallback;
            }
            try {
                return Float.parseFloat(raw.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        default int intValue(String name, int fallback) {
            String raw = value(name);
            if (raw == null || raw.trim()
                .isEmpty()) {
                return fallback;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        default @Nullable Float optionalFloat(String name) {
            String raw = value(name);
            if (raw == null || raw.trim()
                .isEmpty()) {
                return null;
            }
            try {
                return Float.parseFloat(raw.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    public interface AttributeEscaper {

        String escape(String value);
    }
}
