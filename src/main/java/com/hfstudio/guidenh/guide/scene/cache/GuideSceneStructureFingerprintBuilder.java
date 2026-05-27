package com.hfstudio.guidenh.guide.scene.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuideSceneStructureFingerprintBuilder {

    private final List<String> parts = new ArrayList<>();

    public void add(String key, Object value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        parts.add(key + "=" + value);
    }

    public void addHashedText(String key, String value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        add(key, sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    public void addHashedBytes(String key, byte[] value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        add(key, sha256(value));
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public GuideSceneStructureCacheKey build() {
        Collections.sort(parts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update(part.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '|');
            }
            return new GuideSceneStructureCacheKey(toHex(digest.digest()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash GameScene structure fingerprint", e);
        }
    }

    private static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash GameScene structure fingerprint", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte next : bytes) {
            builder.append(Character.forDigit((next >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(next & 0xF, 16));
        }
        return builder.toString();
    }
}
