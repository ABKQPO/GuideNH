package com.hfstudio.guidenh.guide.internal.structure;

import java.util.ArrayList;
import java.util.List;

public class GuideStructureMemoryStore {

    private final GuideStructurePlacementService placementService;
    private final List<Entry> entries = new ArrayList<>();

    public GuideStructureMemoryStore(GuideStructurePlacementService placementService) {
        this.placementService = placementService;
    }

    public synchronized Entry remember(String label, String structureText) throws Exception {
        GuideStructureData data = placementService.parse(structureText);
        Entry entry = new Entry(label, structureText, data);
        entries.add(entry);
        return entry;
    }

    public synchronized List<Entry> snapshotEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized List<GuideStructureData> snapshotData() {
        List<GuideStructureData> snapshot = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            snapshot.add(entry.getData());
        }
        return snapshot;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized void clear() {
        entries.clear();
    }

    public static class Entry {

        private final String label;
        private final String structureText;
        private final GuideStructureData data;

        private Entry(String label, String structureText, GuideStructureData data) {
            this.label = label;
            this.structureText = structureText;
            this.data = data;
        }

        public String getLabel() {
            return label;
        }

        public String getStructureText() {
            return structureText;
        }

        public GuideStructureData getData() {
            return data;
        }
    }
}
