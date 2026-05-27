package com.hfstudio.guidenh.integration.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.integration.Mods;

public class GuideScreenNeiBridge {

    protected GuideScreenNeiBridge() {}

    public static int reservedSidePixels(EditorAccess editorAccess) {
        return isNeiLoaded() ? GuideScreenNeiNativeBridge.reservedSidePixels(editorAccess) : 0;
    }

    public static int reservedBottomPixels(EditorAccess editorAccess) {
        return isNeiLoaded() ? GuideScreenNeiNativeBridge.reservedBottomPixels(editorAccess) : 0;
    }

    public static int layoutStateVersion(EditorAccess editorAccess) {
        return isNeiLoaded() ? GuideScreenNeiNativeBridge.layoutStateVersion(editorAccess) : 0;
    }

    public static boolean isDraggingItem() {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.isDraggingItem();
    }

    public static boolean mouseClicked(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.mouseClicked(editorAccess, mouseX, mouseY, button);
    }

    public static boolean mouseDragged(EditorAccess editorAccess, int mouseX, int mouseY, int button, long heldTime) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.mouseDragged(editorAccess, mouseX, mouseY, button, heldTime);
    }

    public static boolean mouseReleased(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.mouseReleased(editorAccess, mouseX, mouseY, button);
    }

    public static boolean mouseScrolled(EditorAccess editorAccess, int mouseX, int mouseY, int wheelDelta) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.mouseScrolled(editorAccess, mouseX, mouseY, wheelDelta);
    }

    public static boolean handleItemDrop(EditorAccess editorAccess, int mouseX, int mouseY) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.handleItemDrop(editorAccess, mouseX, mouseY);
    }

    public static boolean keyTyped(EditorAccess editorAccess, char typedChar, int keyCode) {
        return isNeiLoaded() && GuideScreenNeiNativeBridge.keyTyped(editorAccess, typedChar, keyCode);
    }

    public static boolean keyTypedForHoveredGuideItem(EditorAccess editorAccess, char typedChar, int keyCode) {
        return isNeiLoaded()
            && GuideScreenNeiNativeBridge.keyTypedForHoveredGuideItem(editorAccess, typedChar, keyCode);
    }

    public static void tick(EditorAccess editorAccess) {
        if (isNeiLoaded()) {
            GuideScreenNeiNativeBridge.tick(editorAccess);
        }
    }

    public static void drawNativeNei(EditorAccess editorAccess, int mouseX, int mouseY) {
        if (isNeiLoaded()) {
            GuideScreenNeiNativeBridge.drawNativeNei(editorAccess, mouseX, mouseY);
        }
    }

    public static void drawNativeNeiTooltip(EditorAccess editorAccess, int mouseX, int mouseY) {
        if (isNeiLoaded()) {
            GuideScreenNeiNativeBridge.drawNativeNeiTooltip(editorAccess, mouseX, mouseY);
        }
    }

    public static void init() {
        if (isNeiLoaded()) {
            GuideScreenNeiNativeBridge.init();
        }
    }

    private static boolean isNeiLoaded() {
        return Mods.NotEnoughItems.isModLoaded();
    }

    public interface EditorAccess {

        boolean isEditorActive();

        boolean isFullWidth();

        GuiContainer container();

        int containerLeft();

        int containerTop();

        int neiLayoutWidth();

        int neiLayoutLeft();

        int neiLayoutVersion();

        void beginNeiLayout();

        void endNeiLayout();

        @Nullable
        SceneEditorMultilineTextArea textArea();

        boolean canDropIntoEditor(int mouseX, int mouseY);

        boolean canInsertRichTagAtMouse(int mouseX, int mouseY);

        void insertAtMouse(String text, int mouseX, int mouseY);

        void insertAtSelection(String text);

        void returnToEditorScreen();

        default void prepareForTemporaryScreenChange() {}

        default void cancelTemporaryScreenChange() {}
    }
}
