package com.hfstudio.guidenh.guide.sound;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeSound;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSoundParsers {

    private GuideSoundParsers() {}

    @Nullable
    public static GuideSoundSpec parseAttributes(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        return parseAttributes(compiler, errorSink, el, "src");
    }

    @Nullable
    public static GuideSoundSpec parseAttributes(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String sourceAttributeName) {
        String rawSound = MdxAttrs.getString(compiler, errorSink, el, "sound", null);
        String rawSource = MdxAttrs.getString(compiler, errorSink, el, sourceAttributeName, null);
        if ((rawSound == null || rawSound.trim()
            .isEmpty()) && (rawSource == null
                || rawSource.trim()
                    .isEmpty())) {
            return null;
        }
        if (hasText(rawSound) && hasText(rawSource)) {
            errorSink.appendError(
                compiler,
                "Use either sound or " + sourceAttributeName + " for guide sound playback, not both.",
                el);
            return null;
        }

        ResourceLocation soundId = resolveSoundId(compiler, errorSink, el, rawSound, rawSource);
        if (soundId == null) {
            return null;
        }
        GuideSoundSpec.Builder builder = GuideSoundSpec.builder(soundId)
            .volume(MdxAttrs.getFloat(compiler, errorSink, el, "volume", GuideSoundSpec.DEFAULT_VOLUME))
            .pitch(MdxAttrs.getFloat(compiler, errorSink, el, "pitch", GuideSoundSpec.DEFAULT_PITCH))
            .cooldownMillis(
                MdxAttrs.getInt(compiler, errorSink, el, "cooldown", GuideSoundSpec.DEFAULT_COOLDOWN_MILLIS))
            .radius(MdxAttrs.getFloat(compiler, errorSink, el, "radius", GuideSoundSpec.DEFAULT_RADIUS))
            .minVolume(MdxAttrs.getFloat(compiler, errorSink, el, "minVolume", GuideSoundSpec.DEFAULT_MIN_VOLUME));

        Float x = getOptionalFloat(compiler, errorSink, el, "x");
        Float y = getOptionalFloat(compiler, errorSink, el, "y");
        Float z = getOptionalFloat(compiler, errorSink, el, "z");
        if (x != null || y != null || z != null) {
            builder.position(x != null ? x : 0.0f, y != null ? y : 0.0f, z != null ? z : 0.0f);
        }
        return builder.build();
    }

    @Nullable
    public static GuideSoundSpec parseActionUri(PageCompiler compiler, String rawUri) {
        if (rawUri == null) {
            return null;
        }
        if (rawUri.startsWith("sound-src:")) {
            String payload = rawUri.substring("sound-src:".length());
            return parseUriPayload(compiler, payload, true);
        }
        if (rawUri.startsWith("sound:")) {
            String payload = rawUri.substring("sound:".length());
            return parseUriPayload(compiler, payload, false);
        }
        return null;
    }

    public static boolean isSoundActionUri(@Nullable String rawUri) {
        return rawUri != null && (rawUri.startsWith("sound:") || rawUri.startsWith("sound-src:"));
    }

    @Nullable
    public static GuideSoundSpec parsePonderSound(PageCompiler compiler, PonderKeyframeSound raw) {
        ResourceLocation soundId;
        try {
            if (raw.getSound() != null && !raw.getSound()
                .trim()
                .isEmpty()) {
                soundId = IdUtils.resolveId(
                    raw.getSound()
                        .trim(),
                    compiler.getPageId()
                        .getResourceDomain());
            } else if (raw.getSrc() != null && !raw.getSrc()
                .trim()
                .isEmpty()) {
                    soundId = GuideSoundSpec.soundIdFromSource(
                        IdUtils.resolveLink(
                            raw.getSrc()
                                .trim(),
                            compiler.getPageId()));
                } else {
                    return null;
                }
        } catch (IllegalArgumentException e) {
            return null;
        }

        GuideSoundSpec.Builder builder = GuideSoundSpec.builder(soundId)
            .volume(raw.getVolume())
            .pitch(raw.getPitch())
            .cooldownMillis(raw.getCooldown())
            .radius(raw.getRadius())
            .minVolume(raw.getMinVolume());
        if (raw.getX() != null || raw.getY() != null || raw.getZ() != null) {
            builder.position(
                raw.getX() != null ? raw.getX() : 0.0f,
                raw.getY() != null ? raw.getY() : 0.0f,
                raw.getZ() != null ? raw.getZ() : 0.0f);
        }
        return builder.build();
    }

    @Nullable
    private static ResourceLocation resolveSoundId(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, @Nullable String rawSound, @Nullable String rawSource) {
        try {
            if (rawSound != null && !rawSound.trim()
                .isEmpty()) {
                return IdUtils.resolveId(
                    rawSound.trim(),
                    compiler.getPageId()
                        .getResourceDomain());
            }
            ResourceLocation sourceId = IdUtils.resolveLink(rawSource.trim(), compiler.getPageId());
            return GuideSoundSpec.soundIdFromSource(sourceId);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Invalid sound id: " + e.getMessage(), el);
            return null;
        }
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim()
            .isEmpty();
    }

    @Nullable
    private static GuideSoundSpec parseUriPayload(PageCompiler compiler, String payload, boolean sourceMode) {
        String path = payload;
        String query = "";
        int queryIdx = payload.indexOf('?');
        if (queryIdx >= 0) {
            path = payload.substring(0, queryIdx);
            query = payload.substring(queryIdx + 1);
        }
        try {
            ResourceLocation soundId;
            if (sourceMode) {
                soundId = GuideSoundSpec.soundIdFromSource(IdUtils.resolveLink(decode(path), compiler.getPageId()));
            } else {
                soundId = IdUtils.resolveId(
                    decode(path),
                    compiler.getPageId()
                        .getResourceDomain());
            }
            GuideSoundSpec.Builder builder = GuideSoundSpec.builder(soundId);
            applyQuery(builder, query);
            return builder.build();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void applyQuery(GuideSoundSpec.Builder builder, String query) {
        if (query == null || query.isEmpty()) {
            return;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = decode(pair.substring(0, eq));
            String value = decode(pair.substring(eq + 1));
            applyQueryValue(builder, key, value);
        }
    }

    private static void applyQueryValue(GuideSoundSpec.Builder builder, String key, String value) {
        try {
            switch (key) {
                case "volume":
                    builder.volume(Float.parseFloat(value));
                    break;
                case "pitch":
                    builder.pitch(Float.parseFloat(value));
                    break;
                case "cooldown":
                    builder.cooldownMillis(Integer.parseInt(value));
                    break;
                case "radius":
                    builder.radius(Float.parseFloat(value));
                    break;
                case "minVolume":
                    builder.minVolume(Float.parseFloat(value));
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException ignored) {}
    }

    @Nullable
    private static Float getOptionalFloat(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name) {
        if (el.getAttribute(name) == null) {
            return null;
        }
        return MdxAttrs.getFloat(compiler, errorSink, el, name, 0.0f);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
