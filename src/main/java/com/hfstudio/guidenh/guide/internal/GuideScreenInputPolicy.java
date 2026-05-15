package com.hfstudio.guidenh.guide.internal;

public class GuideScreenInputPolicy {

    private GuideScreenInputPolicy() {}

    public static boolean shouldRouteNavigationClickBeforeEditor(boolean navigationOpen, boolean navigationContains,
        int button) {
        return button == 0 && navigationOpen && navigationContains;
    }

    public static boolean shouldSuppressNavigationHoverForEditorMouseDown(boolean guideEditorActive,
        boolean editorContainsMouse, boolean editorMouseButtonDown, boolean navigationOpen,
        boolean navigationContains) {
        return guideEditorActive && editorContainsMouse
            && editorMouseButtonDown
            && !(navigationOpen && navigationContains);
    }
}
