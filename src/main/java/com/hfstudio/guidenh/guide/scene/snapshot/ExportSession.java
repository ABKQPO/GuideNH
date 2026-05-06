package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * One SNBT export run; contributors may stash batch state in {@link #shared}.
 */
public final class ExportSession {

    private final StructureExportAccess access;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final ConcurrentHashMap<String, Object> shared = new ConcurrentHashMap<>();

    public ExportSession(StructureExportAccess access, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        int sizeX, int sizeY, int sizeZ) {
        this.access = access;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public StructureExportAccess access() {
        return access;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public ConcurrentHashMap<String, Object> shared() {
        return shared;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getShared(String key, Class<T> type) {
        Object o = shared.get(key);
        return type.isInstance(o) ? (T) o : null;
    }
}
