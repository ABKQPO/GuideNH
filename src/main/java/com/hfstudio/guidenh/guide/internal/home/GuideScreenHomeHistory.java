package com.hfstudio.guidenh.guide.internal.home;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

public class GuideScreenHomeHistory {

    private static final GuideScreenHomeHistory SHARED = new GuideScreenHomeHistory();

    public static GuideScreenHomeHistory shared() {
        return SHARED;
    }

    private final LinkedList<Entry> entries = new LinkedList<>();
    private int version;

    public void record(ResourceLocation guideId, ResourceLocation pageId) {
        if (guideId == null || pageId == null) {
            return;
        }
        Entry entry = new Entry(guideId, pageId);
        if (!entries.isEmpty() && entry.equals(entries.getFirst())) {
            return;
        }
        entries.remove(entry);
        entries.addFirst(entry);
        version++;
    }

    public List<Entry> snapshot() {
        return List.copyOf(entries);
    }

    public int version() {
        return version;
    }

    public void clear() {
        if (entries.isEmpty()) {
            return;
        }
        entries.clear();
        version++;
    }

    public static class Entry {

        private final ResourceLocation guideId;
        private final ResourceLocation pageId;

        public Entry(ResourceLocation guideId, ResourceLocation pageId) {
            this.guideId = guideId;
            this.pageId = pageId;
        }

        public ResourceLocation guideId() {
            return guideId;
        }

        public ResourceLocation pageId() {
            return pageId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Entry other)) {
                return false;
            }
            return guideId.equals(other.guideId) && pageId.equals(other.pageId);
        }

        @Override
        public int hashCode() {
            int result = guideId.hashCode();
            result = 31 * result + pageId.hashCode();
            return result;
        }
    }
}
