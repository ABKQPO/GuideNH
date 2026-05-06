package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Per-coordinate opaque supplement bytes keyed by {@link #supplementId()}, for server-authoritative preview data.
 */
public final class GuidebookPreviewAuthorityStore {

    private final HashMap<Long, HashMap<String, byte[]>> byPos = new HashMap<>();

    public void put(long packedPos, String supplementId, @Nullable byte[] payload) {
        if (payload == null || payload.length == 0) {
            remove(packedPos, supplementId);
            return;
        }
        HashMap<String, byte[]> slot = byPos.computeIfAbsent(packedPos, k -> new HashMap<>());
        slot.put(supplementId, payload);
    }

    public void remove(long packedPos, String supplementId) {
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        if (slot == null) {
            return;
        }
        slot.remove(supplementId);
        if (slot.isEmpty()) {
            byPos.remove(packedPos);
        }
    }

    /** Clears every supplement slot at this coordinate. */
    public void clearAt(long packedPos) {
        byPos.remove(packedPos);
    }

    @Nullable
    public byte[] get(long packedPos, String supplementId) {
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        if (slot == null) {
            return null;
        }
        byte[] raw = slot.get(supplementId);
        return raw != null && raw.length > 0 ? raw : null;
    }

    /** For diagnostics only. */
    public Map<String, byte[]> snapshotAt(long packedPos) {
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        return slot != null ? new HashMap<>(slot) : Collections.emptyMap();
    }
}
