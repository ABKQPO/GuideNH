package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Stores content-only history for the scene editor.
 * Selection-only changes and UI preference changes must never be pushed here.
 */
public final class SceneEditorUndoHistory {

    private final int maxEntries;
    private final List<Entry> entries;
    private int currentIndex;
    @Nullable
    private String currentMergeKey;
    private boolean currentMergeOpen;

    public SceneEditorUndoHistory(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.entries = new ArrayList<>();
        this.currentIndex = -1;
        this.currentMergeKey = null;
        this.currentMergeOpen = false;
    }

    public boolean canUndo() {
        return currentIndex > 0;
    }

    public boolean hasSnapshots() {
        return !entries.isEmpty();
    }

    public boolean canRedo() {
        return currentIndex >= 0 && currentIndex < entries.size() - 1;
    }

    public void pushStandalone(SceneEditorUndoSnapshot snapshot) {
        discardRedoEntries();
        entries.add(new Entry(snapshot, null, false));
        currentIndex = entries.size() - 1;
        currentMergeKey = null;
        currentMergeOpen = false;
        trimToMaxEntries();
    }

    public void pushOrMerge(String mergeKey, SceneEditorUndoSnapshot snapshot) {
        pushOrMerge(mergeKey, snapshot, true);
    }

    public void pushOrMerge(@Nullable String mergeKey, SceneEditorUndoSnapshot snapshot, boolean keepOpen) {
        boolean effectiveKeepOpen = keepOpen && mergeKey != null;
        if (entries.isEmpty()) {
            pushStandalone(snapshot);
            replaceCurrentMergeState(mergeKey, effectiveKeepOpen);
            return;
        }

        if (mergeKey != null && currentMergeOpen && mergeKey.equals(currentMergeKey)) {
            entries.set(currentIndex, new Entry(snapshot, mergeKey, effectiveKeepOpen));
            currentMergeKey = mergeKey;
            currentMergeOpen = effectiveKeepOpen;
            return;
        }

        discardRedoEntries();
        entries.add(new Entry(snapshot, mergeKey, effectiveKeepOpen));
        currentIndex = entries.size() - 1;
        currentMergeKey = mergeKey;
        currentMergeOpen = effectiveKeepOpen;
        trimToMaxEntries();
    }

    public SceneEditorUndoSnapshot undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Cannot undo without a previous snapshot");
        }
        currentIndex--;
        syncCurrentMergeState();
        return entries.get(currentIndex).snapshot;
    }

    public SceneEditorUndoSnapshot redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Cannot redo without a next snapshot");
        }
        currentIndex++;
        syncCurrentMergeState();
        return entries.get(currentIndex).snapshot;
    }

    private void discardRedoEntries() {
        while (entries.size() > currentIndex + 1) {
            entries.remove(entries.size() - 1);
        }
    }

    private void trimToMaxEntries() {
        while (entries.size() > maxEntries) {
            entries.remove(0);
            currentIndex--;
        }
        syncCurrentMergeState();
    }

    private void replaceCurrentMergeState(@Nullable String mergeKey, boolean keepOpen) {
        if (currentIndex < 0 || currentIndex >= entries.size()) {
            return;
        }
        Entry current = entries.get(currentIndex);
        entries.set(currentIndex, new Entry(current.snapshot, mergeKey, keepOpen));
        currentMergeKey = mergeKey;
        currentMergeOpen = keepOpen;
    }

    private void syncCurrentMergeState() {
        if (currentIndex >= 0 && currentIndex < entries.size()) {
            Entry current = entries.get(currentIndex);
            currentMergeKey = current.mergeKey;
            currentMergeOpen = current.mergeOpen;
            return;
        }
        currentMergeKey = null;
        currentMergeOpen = false;
    }

    private static final class Entry {

        private final SceneEditorUndoSnapshot snapshot;
        @Nullable
        private final String mergeKey;
        private final boolean mergeOpen;

        private Entry(SceneEditorUndoSnapshot snapshot, @Nullable String mergeKey, boolean mergeOpen) {
            this.snapshot = snapshot;
            this.mergeKey = mergeKey;
            this.mergeOpen = mergeOpen;
        }
    }
}
