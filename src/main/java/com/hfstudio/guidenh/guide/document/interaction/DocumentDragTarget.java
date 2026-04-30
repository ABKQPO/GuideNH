package com.hfstudio.guidenh.guide.document.interaction;

public interface DocumentDragTarget {

    default boolean beginDrag(int documentX, int documentY, int button) {
        return false;
    }

    default void dragTo(int documentX, int documentY) {}

    default void endDrag() {}

    default boolean scroll(int documentX, int documentY, int wheelDelta) {
        return false;
    }
}
