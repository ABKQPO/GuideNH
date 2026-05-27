package com.hfstudio.guidenh.guide.scene.cache;

import java.util.Objects;

public class GuideSceneStructureCacheKey {

    private final String fingerprint;

    public GuideSceneStructureCacheKey(String fingerprint) {
        this.fingerprint = fingerprint != null ? fingerprint : "";
    }

    public String fingerprint() {
        return fingerprint;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GuideSceneStructureCacheKey that)) {
            return false;
        }
        return Objects.equals(fingerprint, that.fingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fingerprint);
    }

    @Override
    public String toString() {
        return fingerprint;
    }
}
