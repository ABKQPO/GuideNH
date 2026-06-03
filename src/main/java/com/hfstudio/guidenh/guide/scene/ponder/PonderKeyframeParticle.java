package com.hfstudio.guidenh.guide.scene.ponder;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.hfstudio.guidenh.guide.color.Colors;

/**
 * A particle effect entry triggered when a Ponder keyframe becomes active during forward playback.
 */
public class PonderKeyframeParticle {

    public static final int MAX_COUNT = 256;
    public static final int MAX_LIFETIME_TICKS = 200;
    public static final int MAX_WEATHER_DURATION_TICKS = 600;
    public static final int MAX_WEATHER_DENSITY_PER_TICK = 64;
    public static final float MAX_POWER = 12f;
    public static final float MAX_SIZE = 4f;
    public static final int DEFAULT_INDICATOR_COLOR = 0xFFFF0000;

    @Nullable
    private String preset;
    @Nullable
    private String name;
    @Nullable
    private String particle;
    @Nullable
    private String kind;
    @Nullable
    private Integer amount;
    @Nullable
    private Integer time;
    @Nullable
    private Integer lifetime;
    @Nullable
    private Float power;
    @Nullable
    private JsonElement x;
    @Nullable
    private JsonElement y;
    @Nullable
    private JsonElement z;
    @Nullable
    private Float vx;
    @Nullable
    private Float vy;
    @Nullable
    private Float vz;
    @Nullable
    private Float motionX;
    @Nullable
    private Float motionY;
    @Nullable
    private Float motionZ;
    @Nullable
    private Float size;
    @Nullable
    private String weather;
    @Nullable
    private String color;

    @Nullable
    public String getPreset() {
        return preset;
    }

    public boolean isExplosionPreset() {
        return "explosion".equals(normalize(preset));
    }

    public boolean isWeatherPreset() {
        String normalizedPreset = normalize(preset);
        return "rain".equals(normalizedPreset) || "weather".equals(normalizedPreset);
    }

    public boolean isIndicatorPreset() {
        String normalizedPreset = normalize(preset);
        return "indicator".equals(normalizedPreset) || "redstone".equals(normalizedPreset);
    }

    @Nullable
    public String getParticleName() {
        String normalized = normalize(name);
        if (normalized != null) {
            return normalized;
        }
        normalized = normalize(particle);
        if (normalized != null) {
            return normalized;
        }
        return normalize(kind);
    }

    public int getAmount(int defaultValue) {
        return clampInt(amount != null ? amount : defaultValue, 1, MAX_COUNT);
    }

    public int getLifetimeTicks(int defaultValue) {
        if (lifetime != null) {
            return clampInt(lifetime, 1, MAX_LIFETIME_TICKS);
        }
        if (time != null) {
            return clampInt(time, 1, MAX_LIFETIME_TICKS);
        }
        return clampInt(defaultValue, 1, MAX_LIFETIME_TICKS);
    }

    public float getPower(float defaultValue) {
        return clampFloat(power != null ? power : defaultValue, 0.1f, MAX_POWER);
    }

    public int getWeatherDurationTicks(int defaultValue) {
        int resolved = lifetime != null ? lifetime : time != null ? time : defaultValue;
        return clampInt(resolved, 4, MAX_WEATHER_DURATION_TICKS);
    }

    public int getWeatherDensityPerTick(int defaultValue) {
        return clampInt(amount != null ? amount : defaultValue, 1, MAX_WEATHER_DENSITY_PER_TICK);
    }

    public String getWeatherType() {
        return "snow".equals(normalize(weather)) ? "snow" : "rain";
    }

    public float getX() {
        return getScalarCoordinate(x, 0f);
    }

    public float getY() {
        return getScalarCoordinate(y, 0f);
    }

    public float getZ() {
        return getScalarCoordinate(z, 0f);
    }

    @Nullable
    public int[] getWeatherXValues() {
        return getCoordinateValues(x);
    }

    @Nullable
    public int[] getWeatherZValues() {
        return getCoordinateValues(z);
    }

    @Nullable
    public int[] getIndicatorXValues() {
        return getCoordinateValues(x);
    }

    @Nullable
    public int[] getIndicatorYValues() {
        return getCoordinateValues(y);
    }

    @Nullable
    public int[] getIndicatorZValues() {
        return getCoordinateValues(z);
    }

    public float getVelocityX() {
        return motionX != null ? motionX : vx != null ? vx : 0f;
    }

    public float getVelocityY() {
        return motionY != null ? motionY : vy != null ? vy : 0f;
    }

    public float getVelocityZ() {
        return motionZ != null ? motionZ : vz != null ? vz : 0f;
    }

    public float getSize(float defaultValue) {
        return clampFloat(size != null ? size : defaultValue, 0.01f, MAX_SIZE);
    }

    public int getIndicatorColor() {
        String rawColor = color;
        if (rawColor == null) {
            return DEFAULT_INDICATOR_COLOR;
        }
        String trimmed = rawColor.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_INDICATOR_COLOR;
        }
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            trimmed = "#" + trimmed.substring(2);
        }
        return Colors.hexToRgb(trimmed);
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim()
            .toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }

    private static float getScalarCoordinate(@Nullable JsonElement element, float defaultValue) {
        Float parsed = parseCoordinateValue(element);
        return parsed != null ? parsed : defaultValue;
    }

    @Nullable
    private static int[] getCoordinateValues(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            int[] values = new int[array.size()];
            int count = 0;
            for (JsonElement child : array) {
                Float parsed = parseCoordinateValue(child);
                if (parsed == null) {
                    continue;
                }
                values[count++] = (int) Math.floor(parsed);
            }
            if (count <= 0) {
                return null;
            }
            if (count == values.length) {
                return values;
            }
            int[] trimmed = new int[count];
            System.arraycopy(values, 0, trimmed, 0, count);
            return trimmed;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
            .isString()) {
            return getCoordinateValuesFromString(element.getAsString());
        }
        Float parsed = parseCoordinateValue(element);
        if (parsed == null) {
            return null;
        }
        return new int[] { (int) Math.floor(parsed) };
    }

    @Nullable
    private static Float parseCoordinateValue(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.size() <= 0) {
                    return null;
                }
                return parseCoordinateValue(array.get(0));
            }
            if (!element.isJsonPrimitive()) {
                return null;
            }
            if (element.getAsJsonPrimitive()
                .isNumber()) {
                return element.getAsFloat();
            }
            if (!element.getAsJsonPrimitive()
                .isString()) {
                return null;
            }
            String[] parts = element.getAsString()
                .trim()
                .split("[,\\s]+");
            if (parts.length <= 0 || parts[0].isEmpty()) {
                return null;
            }
            return Float.parseFloat(parts[0]);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static int[] getCoordinateValuesFromString(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim()
            .split("[,\\s]+");
        if (parts.length <= 0) {
            return null;
        }
        int[] values = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            try {
                values[count++] = (int) Math.floor(Float.parseFloat(part));
            } catch (RuntimeException ignored) {}
        }
        if (count <= 0) {
            return null;
        }
        if (count == values.length) {
            return values;
        }
        int[] trimmed = new int[count];
        System.arraycopy(values, 0, trimmed, 0, count);
        return trimmed;
    }
}
