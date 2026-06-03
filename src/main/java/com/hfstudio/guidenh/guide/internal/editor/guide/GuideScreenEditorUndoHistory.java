package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.ArrayList;
import java.util.List;

public class GuideScreenEditorUndoHistory {

    private final int limit;
    private final List<Entry> states = new ArrayList<>();
    private int index = -1;

    public GuideScreenEditorUndoHistory(int limit) {
        this.limit = Math.max(1, limit);
    }

    public void reset(String text, int selectionStart, int selectionEnd) {
        states.clear();
        states.add(new Entry(text, selectionStart, selectionEnd));
        index = 0;
    }

    public void push(String text, int selectionStart, int selectionEnd) {
        Entry entry = new Entry(text, selectionStart, selectionEnd);
        if (index >= 0 && index < states.size() && entry.equals(states.get(index))) {
            return;
        }
        while (states.size() > index + 1) {
            states.removeLast();
        }
        states.add(entry);
        index = states.size() - 1;
        while (states.size() > limit) {
            states.removeFirst();
            index--;
        }
        if (index < 0 && !states.isEmpty()) {
            index = 0;
        }
    }

    public boolean canUndo() {
        return index > 0;
    }

    public boolean canRedo() {
        return index >= 0 && index < states.size() - 1;
    }

    public Entry undo() {
        if (!canUndo()) {
            return current();
        }
        index--;
        return current();
    }

    public Entry redo() {
        if (!canRedo()) {
            return current();
        }
        index++;
        return current();
    }

    public Entry current() {
        if (index < 0 || index >= states.size()) {
            return new Entry("", 0, 0);
        }
        return states.get(index);
    }

    public static final class Entry {

        private final String text;
        private final int selectionStart;
        private final int selectionEnd;

        private Entry(String text, int selectionStart, int selectionEnd) {
            String safeText = text != null ? text : "";
            this.text = safeText;
            int safeSelectionStart = Math.clamp(selectionStart, 0, safeText.length());
            int safeSelectionEnd = Math.clamp(selectionEnd, safeSelectionStart, safeText.length());
            this.selectionStart = safeSelectionStart;
            this.selectionEnd = safeSelectionEnd;
        }

        public String getText() {
            return text;
        }

        public int getSelectionStart() {
            return selectionStart;
        }

        public int getSelectionEnd() {
            return selectionEnd;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Entry other)) {
                return false;
            }
            return selectionStart == other.selectionStart && selectionEnd == other.selectionEnd
                && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + selectionStart;
            result = 31 * result + selectionEnd;
            return result;
        }
    }
}
