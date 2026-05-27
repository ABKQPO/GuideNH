package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Per-coordinate opaque supplement bytes keyed by {@link #supplementId()}, for server-authoritative preview data.
 */
public class GuidebookPreviewAuthorityStore {

    private final HashMap<Long, HashMap<String, byte[]>> byPos = new HashMap<>();

    public void put(long packedPos, String supplementId, @Nullable byte[] payload) {
        if (payload == null || payload.length == 0) {
            remove(packedPos, supplementId);
            return;
        }
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        if (slot == null) {
            slot = new HashMap<>();
            byPos.put(packedPos, slot);
        }
        slot.put(supplementId, payload.clone());
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

    /** Clears every supplement slot in this store. */
    public void clear() {
        byPos.clear();
    }

    public void restoreAt(long packedPos, Map<String, byte[]> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            clearAt(packedPos);
            return;
        }
        HashMap<String, byte[]> restored = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : snapshot.entrySet()) {
            String supplementId = entry.getKey();
            byte[] payload = entry.getValue();
            if (supplementId != null && payload != null && payload.length > 0) {
                restored.put(supplementId, payload.clone());
            }
        }
        if (restored.isEmpty()) {
            clearAt(packedPos);
            return;
        }
        byPos.put(packedPos, restored);
    }

    @Nullable
    public byte[] get(long packedPos, String supplementId) {
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        if (slot == null) {
            return null;
        }
        byte[] raw = slot.get(supplementId);
        return raw != null && raw.length > 0 ? raw.clone() : null;
    }

    /** For diagnostics only. */
    public Map<String, byte[]> snapshotAt(long packedPos) {
        HashMap<String, byte[]> slot = byPos.get(packedPos);
        if (slot == null || slot.isEmpty()) {
            return Collections.emptyMap();
        }
        HashMap<String, byte[]> snapshot = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : slot.entrySet()) {
            byte[] payload = entry.getValue();
            if (payload != null && payload.length > 0) {
                snapshot.put(entry.getKey(), payload.clone());
            }
        }
        return snapshot.isEmpty() ? Collections.emptyMap() : snapshot;
    }

    public Map<Long, Map<String, byte[]>> snapshotAll() {
        if (byPos.isEmpty()) {
            return Collections.emptyMap();
        }
        HashMap<Long, Map<String, byte[]>> snapshot = new HashMap<>();
        for (Long packedPos : byPos.keySet()) {
            snapshot.put(packedPos, snapshotAt(packedPos.longValue()));
        }
        return snapshot.isEmpty() ? Collections.emptyMap() : snapshot;
    }

    public void restoreAll(Map<Long, Map<String, byte[]>> snapshot) {
        clear();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Map<String, byte[]>> entry : snapshot.entrySet()) {
            if (entry.getKey() != null) {
                restoreAt(
                    entry.getKey()
                        .longValue(),
                    entry.getValue());
            }
        }
    }
}
