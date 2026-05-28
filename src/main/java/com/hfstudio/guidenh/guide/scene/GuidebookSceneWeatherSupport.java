package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuidebookSceneWeatherSupport {

    public static final int[] EMPTY_BOUNDS = { 0, 0, 0, 0, 0, 0 };
    public static final int DEFAULT_RAIN_DENSITY = 10;
    public static final int DEFAULT_SNOW_DENSITY = 7;
    public static final float RAIN_ALPHA = 0.72f;
    public static final float SNOW_ALPHA = 0.88f;
    public static final float RAIN_HALF_WIDTH = 0.5f;
    public static final float SNOW_HALF_WIDTH = 0.5f;
    public static final float RAIN_FALL_SPEED = 0.03125f;
    public static final float SNOW_FALL_SPEED = 0.001953125f;
    public static final float RAIN_DRIFT = 0.0f;
    public static final float SNOW_DRIFT = 0.01f;
    public static final float WEATHER_SURFACE_OFFSET = 0.02f;
    public static final float SNOW_SURFACE_OFFSET = 0.0f;
    public static final float RAIN_SPAWN_HEADROOM = 0.0f;
    public static final float SNOW_SPAWN_HEADROOM = 0.0f;
    public static final float RAIN_LIGHT_RADIUS_SCALE = 0.5f;
    public static final float SNOW_LIGHT_RADIUS_SCALE = 0.3f;
    public static final float RAIN_ALPHA_RADIUS_SCALE = 0.5f;
    public static final float SNOW_ALPHA_RADIUS_SCALE = 0.3f;
    private static final int MIN_TRANSITION_TICKS = 8;

    private GuidebookSceneWeatherSupport() {}

    public static int defaultDensity(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? DEFAULT_SNOW_DENSITY : DEFAULT_RAIN_DENSITY;
    }

    public static int resolveTransitionTicks(int durationTicks) {
        return Math.max(4, Math.min(durationTicks / 4, MIN_TRANSITION_TICKS));
    }

    public static float resolveHalfWidth(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_HALF_WIDTH : RAIN_HALF_WIDTH;
    }

    public static float resolveAlpha(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_ALPHA : RAIN_ALPHA;
    }

    public static float resolveDriftAmplitude(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_DRIFT : RAIN_DRIFT;
    }

    public static float resolveFallSpeed(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_FALL_SPEED : RAIN_FALL_SPEED;
    }

    public static float resolveSpawnHeadroom(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_SPAWN_HEADROOM : RAIN_SPAWN_HEADROOM;
    }

    public static float resolveSurfaceOffset(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_SURFACE_OFFSET : WEATHER_SURFACE_OFFSET;
    }

    public static float resolveLightRadiusScale(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_LIGHT_RADIUS_SCALE : RAIN_LIGHT_RADIUS_SCALE;
    }

    public static float resolveAlphaRadiusScale(GuidebookSceneWeatherType weatherType) {
        return weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_ALPHA_RADIUS_SCALE : RAIN_ALPHA_RADIUS_SCALE;
    }

    @Nullable
    public static int[] parseAxisValues(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("[,\\s]+");
        int[] values = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            try {
                values[count++] = (int) Math.floor(Float.parseFloat(part));
            } catch (NumberFormatException ignored) {}
        }
        if (count <= 0) {
            return null;
        }
        if (count == values.length) {
            return values;
        }
        int[] trimmedValues = new int[count];
        System.arraycopy(values, 0, trimmedValues, 0, count);
        return trimmedValues;
    }

    public static List<GuidebookSceneWeatherArea> resolveWeatherAreas(int[] bounds, @Nullable int[] xValues,
        @Nullable int[] zValues) {
        int[] resolvedBounds = bounds != null && bounds.length >= 6 ? bounds : EMPTY_BOUNDS;
        if ((xValues == null || xValues.length <= 0) && (zValues == null || zValues.length <= 0)) {
            return buildFallbackWeatherAreas(resolvedBounds);
        }
        int[] normalizedX = normalizeWeatherAxisValues(xValues, resolvedBounds[0], resolvedBounds[3]);
        int[] normalizedZ = normalizeWeatherAxisValues(zValues, resolvedBounds[2], resolvedBounds[5]);
        int pairCount = resolveWeatherAreaPairCount(normalizedX, normalizedZ);
        if (pairCount <= 0) {
            return buildFallbackWeatherAreas(resolvedBounds);
        }
        List<GuidebookSceneWeatherArea> areas = new ArrayList<>(pairCount);
        for (int index = 0; index < pairCount; index++) {
            int x0 = resolveWeatherAxisEndpoint(normalizedX, index, true, resolvedBounds[0], resolvedBounds[3]);
            int x1 = resolveWeatherAxisEndpoint(normalizedX, index, false, resolvedBounds[0], resolvedBounds[3]);
            int z0 = resolveWeatherAxisEndpoint(normalizedZ, index, true, resolvedBounds[2], resolvedBounds[5]);
            int z1 = resolveWeatherAxisEndpoint(normalizedZ, index, false, resolvedBounds[2], resolvedBounds[5]);
            areas.add(
                new GuidebookSceneWeatherArea(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)));
        }
        return areas;
    }

    public static List<GuidebookSceneWeatherEffect> resolveRenderableEffects(List<GuidebookSceneWeatherEffect> effects,
        int[] bounds) {
        return resolveRenderableEffects(effects, bounds, null);
    }

    public static List<GuidebookSceneWeatherEffect> resolveRenderableEffects(List<GuidebookSceneWeatherEffect> effects,
        int[] bounds, @Nullable Integer activeTick) {
        if (effects == null || effects.isEmpty()) {
            return List.of();
        }
        if (effects.size() == 1) {
            GuidebookSceneWeatherEffect singleEffect = effects.get(0);
            if (singleEffect == null || activeTick != null && !singleEffect.isActiveAt(activeTick)) {
                return List.of();
            }
            List<GuidebookSceneWeatherArea> singleAreas = singleEffect.resolveAreas(bounds);
            if (singleAreas.size() <= 1 || singleEffect.hasExplicitAreas()) {
                return List.of(singleEffect);
            }
        }
        Set<Long> occupiedColumns = new HashSet<>();
        List<GuidebookSceneWeatherEffect> resolved = new ArrayList<>(effects.size());
        for (GuidebookSceneWeatherEffect effect : effects) {
            if (effect == null || activeTick != null && !effect.isActiveAt(activeTick)) {
                continue;
            }
            List<GuidebookSceneWeatherArea> trimmedAreas = trimWeatherAreas(
                effect.resolveAreas(bounds),
                occupiedColumns);
            if (trimmedAreas.isEmpty()) {
                continue;
            }
            resolved.add(effect.withResolvedAreas(trimmedAreas));
        }
        return resolved;
    }

    public static List<GuidebookSceneWeatherEffect> resolveRenderableEffects(List<GuidebookSceneWeatherEffect> effects,
        int[] bounds, @Nullable Integer activeTick, @Nullable GuidebookSceneLayerSelection layerSelection,
        @Nullable GuidebookLevel level) {
        List<GuidebookSceneWeatherEffect> resolvedEffects = resolveRenderableEffects(effects, bounds, activeTick);
        if (resolvedEffects.isEmpty() || layerSelection == null
            || level == null
            || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL) {
            return resolvedEffects;
        }
        List<GuidebookSceneWeatherEffect> visibleEffects = new ArrayList<>(resolvedEffects.size());
        for (GuidebookSceneWeatherEffect effect : resolvedEffects) {
            if (intersectsVisibleLayer(effect, bounds, layerSelection, level)) {
                visibleEffects.add(effect);
            }
        }
        if (visibleEffects.isEmpty()) {
            return List.of();
        }
        return visibleEffects;
    }

    public static int countColumns(List<GuidebookSceneWeatherArea> areas) {
        if (areas == null || areas.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (GuidebookSceneWeatherArea area : areas) {
            if (area == null) {
                continue;
            }
            total += Math.max(1, (area.getMaxX() - area.getMinX() + 1) * (area.getMaxZ() - area.getMinZ() + 1));
        }
        return total;
    }

    public static boolean intersectsVisibleLayer(GuidebookSceneWeatherEffect effect, int[] bounds,
        GuidebookSceneLayerSelection layerSelection, GuidebookLevel level) {
        if (effect == null || layerSelection == null
            || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL
            || level == null) {
            return true;
        }
        int[] resolvedBounds = bounds != null && bounds.length >= 6 ? bounds : EMPTY_BOUNDS;
        for (GuidebookSceneWeatherArea area : effect.resolveAreas(resolvedBounds)) {
            if (area == null) {
                continue;
            }
            for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                    int precipitationBottom = Math.max(
                        resolvedBounds[1],
                        level.getPrecipitationHeight(x, z, resolvedBounds[1], resolvedBounds[4]));
                    int precipitationTop = Math.max(precipitationBottom, resolvedBounds[4] + 1);
                    for (int y = precipitationBottom; y <= precipitationTop; y++) {
                        if (!layerSelection.isLayerVisible(y)) {
                            continue;
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<GuidebookSceneWeatherArea> trimWeatherAreas(List<GuidebookSceneWeatherArea> areas,
        Set<Long> occupiedColumns) {
        if (areas == null || areas.isEmpty()) {
            return List.of();
        }
        List<GuidebookSceneWeatherArea> trimmed = new ArrayList<>();
        for (GuidebookSceneWeatherArea area : areas) {
            if (area == null || area.getMinX() > area.getMaxX() || area.getMinZ() > area.getMaxZ()) {
                continue;
            }
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int rowZ = Integer.MIN_VALUE;
            for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                    long key = GuidebookLevel.packPos(x, 0, z);
                    if (occupiedColumns.contains(key)) {
                        if (rowZ != Integer.MIN_VALUE && minX <= maxX) {
                            trimmed.add(new GuidebookSceneWeatherArea(minX, rowZ, maxX, rowZ));
                        }
                        minX = Integer.MAX_VALUE;
                        maxX = Integer.MIN_VALUE;
                        rowZ = Integer.MIN_VALUE;
                        continue;
                    }
                    occupiedColumns.add(key);
                    if (rowZ == Integer.MIN_VALUE) {
                        rowZ = z;
                        minX = x;
                    }
                    maxX = x;
                }
                if (rowZ != Integer.MIN_VALUE && minX <= maxX) {
                    trimmed.add(new GuidebookSceneWeatherArea(minX, rowZ, maxX, rowZ));
                }
                minX = Integer.MAX_VALUE;
                maxX = Integer.MIN_VALUE;
                rowZ = Integer.MIN_VALUE;
            }
        }
        return trimmed;
    }

    private static List<GuidebookSceneWeatherArea> buildFallbackWeatherAreas(int[] bounds) {
        return List.of(new GuidebookSceneWeatherArea(bounds[0], bounds[2], bounds[3], bounds[5]));
    }

    @Nullable
    private static int[] normalizeWeatherAxisValues(@Nullable int[] values, int minBound, int maxBound) {
        if (values == null || values.length <= 0) {
            return null;
        }
        int[] normalized = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = Math.max(minBound, Math.min(maxBound, values[i]));
        }
        return normalized;
    }

    private static int resolveWeatherAxisPairCount(@Nullable int[] values) {
        if (values == null || values.length <= 0) {
            return 0;
        }
        if (values.length == 1) {
            return 1;
        }
        return values.length / 2;
    }

    private static int resolveWeatherAreaPairCount(@Nullable int[] xValues, @Nullable int[] zValues) {
        int xPairs = resolveWeatherAxisPairCount(xValues);
        int zPairs = resolveWeatherAxisPairCount(zValues);
        boolean xBroadcast = xValues == null || xValues.length <= 1;
        boolean zBroadcast = zValues == null || zValues.length <= 1;
        if (xBroadcast && zBroadcast) {
            return Math.max(xPairs, zPairs);
        }
        if (xBroadcast) {
            return zPairs;
        }
        if (zBroadcast) {
            return xPairs;
        }
        return Math.min(xPairs, zPairs);
    }

    private static int resolveWeatherAxisEndpoint(@Nullable int[] values, int pairIndex, boolean first, int minBound,
        int maxBound) {
        if (values == null || values.length <= 0) {
            return first ? minBound : maxBound;
        }
        if (values.length == 1) {
            return values[0];
        }
        int baseIndex = pairIndex * 2;
        if (baseIndex >= values.length) {
            int fallback = values[values.length - 1];
            return Math.max(minBound, Math.min(maxBound, fallback));
        }
        int resolvedIndex = first ? baseIndex : Math.min(values.length - 1, baseIndex + 1);
        return Math.max(minBound, Math.min(maxBound, values[resolvedIndex]));
    }
}
