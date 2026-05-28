package com.hfstudio.guidenh.guide.internal;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

public class GuideBookmarkState {

    private static GuideBookmarkState sharedInstance;

    public static GuideBookmarkState getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new GuideBookmarkState(new GuideBookmarkStore());
        }
        return sharedInstance;
    }

    private final GuideBookmarkStore store;
    private final Set<ResourceLocation> bookmarks = new LinkedHashSet<ResourceLocation>();
    private final Set<ResourceLocation> bookmarkView = new AbstractSet<ResourceLocation>() {

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Set.copyOf(bookmarks)
                .iterator();
        }

        @Override
        public int size() {
            return bookmarks.size();
        }

        @Override
        public boolean contains(Object object) {
            return bookmarks.contains(object);
        }
    };
    private boolean loaded;
    private int version;

    public GuideBookmarkState(GuideBookmarkStore store) {
        this.store = store;
    }

    public boolean isBookmarked(ResourceLocation pageId) {
        ensureLoaded();
        return pageId != null && bookmarks.contains(pageId);
    }

    public Set<ResourceLocation> getBookmarks() {
        ensureLoaded();
        return Set.copyOf(bookmarks);
    }

    public Set<ResourceLocation> getBookmarksView() {
        ensureLoaded();
        return bookmarkView;
    }

    public boolean isEmpty() {
        ensureLoaded();
        return bookmarks.isEmpty();
    }

    public int version() {
        ensureLoaded();
        return version;
    }

    public boolean toggle(ResourceLocation pageId) {
        ensureLoaded();
        if (pageId == null) {
            return false;
        }
        boolean bookmarked;
        if (bookmarks.contains(pageId)) {
            bookmarks.remove(pageId);
            bookmarked = false;
        } else {
            bookmarks.add(pageId);
            bookmarked = true;
        }
        version++;
        store.save(bookmarks);
        return bookmarked;
    }

    public boolean pruneInvalid(Set<ResourceLocation> validPageIds) {
        ensureLoaded();
        if (validPageIds == null) {
            return false;
        }
        boolean changed = bookmarks.removeIf(pageId -> pageId == null || !validPageIds.contains(pageId));
        if (changed) {
            version++;
            store.save(bookmarks);
        }
        return changed;
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        bookmarks.clear();
        bookmarks.addAll(store.load());
        loaded = true;
        version++;
    }
}
