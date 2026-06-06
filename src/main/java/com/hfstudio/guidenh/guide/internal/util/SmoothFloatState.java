package com.hfstudio.guidenh.guide.internal.util;

public final class SmoothFloatState {

    private float value;
    private long lastUpdateNanos;

    public float value() {
        return value;
    }

    public int rounded() {
        return Math.round(value);
    }

    public void snapTo(float target) {
        value = target;
        lastUpdateNanos = System.nanoTime();
    }

    public void updateTowards(float target, float response, float maxDeltaSeconds, float snapThreshold,
        float jumpThreshold) {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            value = target;
            lastUpdateNanos = now;
            return;
        }

        float deltaSeconds = Math.min((now - lastUpdateNanos) / 1_000_000_000f, maxDeltaSeconds);
        lastUpdateNanos = now;
        if (Math.abs(value - target) > jumpThreshold) {
            value = target;
        } else if (deltaSeconds > 0f) {
            float blend = 1f - (float) Math.exp(-response * deltaSeconds);
            value += (target - value) * blend;
        }
        if (Math.abs(value - target) < snapThreshold) {
            value = target;
        }
    }
}
