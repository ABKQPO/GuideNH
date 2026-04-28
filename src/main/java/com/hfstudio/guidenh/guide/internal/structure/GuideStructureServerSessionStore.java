package com.hfstudio.guidenh.guide.internal.structure;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuideStructureServerSessionStore {

    private final GuideStructurePlacementService placementService;
    private final ConcurrentHashMap<UUID, GuideStructureMemoryStore> stores = new ConcurrentHashMap<>();

    public GuideStructureServerSessionStore(GuideStructurePlacementService placementService) {
        this.placementService = placementService;
    }

    public GuideStructureMemoryStore.Entry remember(UUID playerId, String label, String structureText)
        throws Exception {
        return getOrCreate(playerId).remember(label, structureText);
    }

    public List<GuideStructureData> snapshotData(UUID playerId) {
        return getOrCreate(playerId).snapshotData();
    }

    public int size(UUID playerId) {
        return getOrCreate(playerId).size();
    }

    public boolean isEmpty(UUID playerId) {
        return getOrCreate(playerId).isEmpty();
    }

    public void clear(UUID playerId) {
        stores.remove(playerId);
    }

    public void reset(UUID playerId) {
        stores.put(playerId, new GuideStructureMemoryStore(placementService));
    }

    private GuideStructureMemoryStore getOrCreate(UUID playerId) {
        return stores.computeIfAbsent(playerId, ignored -> new GuideStructureMemoryStore(placementService));
    }
}
