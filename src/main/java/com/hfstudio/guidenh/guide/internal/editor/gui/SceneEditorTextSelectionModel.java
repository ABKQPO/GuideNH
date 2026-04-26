package com.hfstudio.guidenh.guide.internal.editor.gui;

public final class SceneEditorTextSelectionModel {

    private String text;
    private int cursorIndex;
    private int selectionAnchor;
    private boolean selectionActive;

    public SceneEditorTextSelectionModel() {
        this.text = "";
        this.cursorIndex = 0;
        this.selectionAnchor = 0;
        this.selectionActive = false;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursorIndex = Math.min(cursorIndex, this.text.length());
        if (selectionActive) {
            this.selectionAnchor = Math.min(selectionAnchor, this.text.length());
            if (selectionAnchor == cursorIndex) {
                selectionActive = false;
            }
        }
    }

    public String getText() {
        return text;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(int cursorIndex) {
        this.cursorIndex = clampIndex(cursorIndex);
        this.selectionAnchor = this.cursorIndex;
        this.selectionActive = false;
    }

    public void beginSelection(int anchorIndex) {
        this.selectionAnchor = clampIndex(anchorIndex);
        this.cursorIndex = this.selectionAnchor;
        this.selectionActive = false;
    }

    public void updateSelection(int cursorIndex) {
        this.cursorIndex = clampIndex(cursorIndex);
        this.selectionActive = this.cursorIndex != this.selectionAnchor;
    }

    public void moveCursor(int cursorIndex, boolean keepSelection) {
        int clamped = clampIndex(cursorIndex);
        if (keepSelection) {
            if (!selectionActive) {
                selectionAnchor = this.cursorIndex;
            }
            this.cursorIndex = clamped;
            this.selectionActive = this.cursorIndex != selectionAnchor;
            return;
        }
        this.cursorIndex = clamped;
        this.selectionAnchor = clamped;
        this.selectionActive = false;
    }

    public boolean hasSelection() {
        return selectionActive && getSelectionStart() != getSelectionEnd();
    }

    public int getSelectionStart() {
        return Math.min(selectionAnchor, cursorIndex);
    }

    public int getSelectionEnd() {
        return Math.max(selectionAnchor, cursorIndex);
    }

    public String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }
        return text.substring(getSelectionStart(), getSelectionEnd());
    }

    public void selectAll() {
        selectionAnchor = 0;
        cursorIndex = text.length();
        selectionActive = !text.isEmpty();
    }

    public void insertText(String insertion) {
        String replacement = insertion != null ? insertion : "";
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (hasSelection()) {
            text = text.substring(0, start) + replacement + text.substring(end);
            cursorIndex = start + replacement.length();
        } else {
            text = text.substring(0, cursorIndex) + replacement + text.substring(cursorIndex);
            cursorIndex += replacement.length();
        }
        selectionAnchor = cursorIndex;
        selectionActive = false;
    }

    public String cutSelection() {
        String selected = getSelectedText();
        if (selected.isEmpty()) {
            return "";
        }
        deleteSelection();
        return selected;
    }

    public void deleteBackward() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorIndex <= 0) {
            return;
        }
        text = text.substring(0, cursorIndex - 1) + text.substring(cursorIndex);
        cursorIndex--;
        selectionAnchor = cursorIndex;
    }

    public void deleteForward() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorIndex >= text.length()) {
            return;
        }
        text = text.substring(0, cursorIndex) + text.substring(cursorIndex + 1);
        selectionAnchor = cursorIndex;
    }

    private void deleteSelection() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        text = text.substring(0, start) + text.substring(end);
        cursorIndex = start;
        selectionAnchor = start;
        selectionActive = false;
    }

    private int clampIndex(int index) {
        if (index < 0) {
            return 0;
        }
        if (index > text.length()) {
            return text.length();
        }
        return index;
    }
}
