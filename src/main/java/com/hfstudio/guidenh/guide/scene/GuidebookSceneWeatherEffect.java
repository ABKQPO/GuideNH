package com.hfstudio.guidenh.guide.scene;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class GuidebookSceneWeatherEffect {

    public static final int INFINITE_DURATION = -1;

    private final GuidebookSceneWeatherType weatherType;
    private final int @Nullable [] xValues;
    private final int @Nullable [] zValues;
    @Nullable
    private final List<GuidebookSceneWeatherArea> areas;
    private final int startTick;
    private final int durationTicks;
    private final int density;
    private final boolean transitionEnabled;
    private int @Nullable [] cachedBounds;
    @Nullable
    private List<GuidebookSceneWeatherArea> cachedResolvedAreas;

    public GuidebookSceneWeatherEffect(GuidebookSceneWeatherType weatherType, int @Nullable [] xValues,
        int @Nullable [] zValues, int startTick, int durationTicks, int density, boolean transitionEnabled) {
        this(weatherType, xValues, zValues, null, startTick, durationTicks, density, transitionEnabled);
    }

    public GuidebookSceneWeatherEffect(GuidebookSceneWeatherType weatherType, List<GuidebookSceneWeatherArea> areas,
        int startTick, int durationTicks, int density, boolean transitionEnabled) {
        this(weatherType, null, null, areas, startTick, durationTicks, density, transitionEnabled);
    }

    private GuidebookSceneWeatherEffect(GuidebookSceneWeatherType weatherType, int @Nullable [] xValues,
        int @Nullable [] zValues, @Nullable List<GuidebookSceneWeatherArea> areas, int startTick, int durationTicks,
        int density, boolean transitionEnabled) {
        this.weatherType = weatherType != null ? weatherType : GuidebookSceneWeatherType.RAIN;
        this.xValues = xValues != null ? Arrays.copyOf(xValues, xValues.length) : null;
        this.zValues = zValues != null ? Arrays.copyOf(zValues, zValues.length) : null;
        this.areas = areas != null ? List.copyOf(areas) : null;
        this.startTick = Math.max(0, startTick);
        this.durationTicks = durationTicks == INFINITE_DURATION ? INFINITE_DURATION : Math.max(1, durationTicks);
        this.density = Math.max(1, density);
        this.transitionEnabled = transitionEnabled;
    }

    public GuidebookSceneWeatherType getWeatherType() {
        return weatherType;
    }

    public int getStartTick() {
        return startTick;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getDensity() {
        return density;
    }

    public boolean isTransitionEnabled() {
        return transitionEnabled;
    }

    public boolean isInfinite() {
        return durationTicks == INFINITE_DURATION;
    }

    public boolean isActiveAt(int tick) {
        if (tick < startTick) {
            return false;
        }
        return isInfinite() || tick < startTick + durationTicks;
    }

    public float resolveIntensity(float animationTick) {
        if (!transitionEnabled || isInfinite()) {
            return 1.0f;
        }
        float localTick = animationTick - startTick;
        if (localTick <= 0.0f) {
            return 0.0f;
        }
        float duration = durationTicks;
        if (localTick >= duration) {
            return 0.0f;
        }
        float transitionTicks = GuidebookSceneWeatherSupport.resolveTransitionTicks(durationTicks);
        if (transitionTicks <= 0.0f) {
            return 1.0f;
        }
        float intensity = 1.0f;
        if (localTick < transitionTicks) {
            intensity = Math.min(intensity, localTick / transitionTicks);
        }
        float tailTicks = duration - localTick;
        if (tailTicks < transitionTicks) {
            intensity = Math.min(intensity, tailTicks / transitionTicks);
        }
        return Math.clamp(intensity, 0.0f, 1.0f);
    }

    public float resolveCoverage() {
        float baseline = GuidebookSceneWeatherSupport.defaultDensity(weatherType);
        if (baseline <= 0.0f) {
            return 1.0f;
        }
        return Math.clamp(density / baseline, 0.08f, 1.0f);
    }

    public List<GuidebookSceneWeatherArea> resolveAreas(int[] bounds) {
        if (areas != null) {
            return areas;
        }
        int[] resolvedBounds = bounds != null ? Arrays.copyOf(bounds, bounds.length)
            : GuidebookSceneWeatherSupport.EMPTY_BOUNDS;
        if (cachedResolvedAreas != null && cachedBounds != null && Arrays.equals(cachedBounds, resolvedBounds)) {
            return cachedResolvedAreas;
        }
        cachedResolvedAreas = GuidebookSceneWeatherSupport.resolveWeatherAreas(resolvedBounds, xValues, zValues);
        cachedBounds = resolvedBounds;
        return cachedResolvedAreas;
    }

    public GuidebookSceneWeatherEffect withResolvedAreas(List<GuidebookSceneWeatherArea> resolvedAreas) {
        return new GuidebookSceneWeatherEffect(
            weatherType,
            resolvedAreas,
            startTick,
            durationTicks,
            density,
            transitionEnabled);
    }

    public boolean hasExplicitAreas() {
        return areas != null;
    }
}
