package com.hfstudio.guidenh.guide.internal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class GuideIconButton extends GuiButton {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;

    public static final ResourceLocation TEX = new ResourceLocation("guidenh", "textures/guide/buttons.png");

    private Role role;
    private boolean active;

    public GuideIconButton(int id, int x, int y, Role role) {
        super(id, x, y, WIDTH, HEIGHT, "");
        this.role = role;
        this.active = false;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTooltip() {
        return role.tooltip();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        this.field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;

        int color = resolveIconColor(enabled, field_146123_n, active);

        drawIcon(mc, role, xPosition, yPosition, width, height, color);
    }

    public static int resolveIconColor(boolean enabled, boolean hovered, boolean active) {
        if (!enabled) {
            return 0x60FFFFFF;
        }
        if (active || hovered) {
            return 0xFF00CAF2;
        }
        return 0xC0FFFFFF;
    }

    public static void drawIcon(Minecraft mc, Role role, int x, int y, int width, int height, int color) {
        if (mc == null || role == null) {
            return;
        }

        mc.getTextureManager()
            .bindTexture(TEX);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);

        float texSize = 64f;
        float u0 = role.iconSrcX / texSize;
        float v0 = role.iconSrcY / texSize;
        float u1 = (role.iconSrcX + 16) / texSize;
        float v1 = (role.iconSrcY + 16) / texSize;

        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + height, 0, u0, v1);
        tess.addVertexWithUV(x + width, y + height, 0, u1, v1);
        tess.addVertexWithUV(x + width, y, 0, u1, v0);
        tess.addVertexWithUV(x, y, 0, u0, v0);
        tess.draw();

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public enum Role {

        BACK(GuidebookText.HistoryGoBack, 0, 0),
        FORWARD(GuidebookText.HistoryGoForward, 16, 0),
        CLOSE(GuidebookText.Close, 32, 0),
        SCENE_EDITOR_CLOSE(GuidebookText.SceneEditorClose, 32, 0),
        SEARCH(GuidebookText.Search, 48, 0),
        SCENE_EDITOR_AUTO_PICK(GuidebookText.SceneEditorAutoPick, 48, 0),
        HIDE_ANNOTATIONS(GuidebookText.HideAnnotations, 0, 16),
        SHOW_ANNOTATIONS(GuidebookText.ShowAnnotations, 16, 16),
        HIGHLIGHT_STRUCTURELIB_HATCHES(GuidebookText.HighlightStructureLibHatches, 32, 48),
        SCENE_EDITOR_HIDE_ELEMENT(GuidebookText.SceneEditorHideElement, 0, 16),
        SCENE_EDITOR_SHOW_ELEMENT(GuidebookText.SceneEditorShowElement, 16, 16),
        ZOOM_OUT(GuidebookText.ZoomOut, 32, 16),
        ZOOM_IN(GuidebookText.ZoomIn, 48, 16),
        SCENE_EDITOR_ADD_ELEMENT(GuidebookText.SceneEditorAddElement, 48, 16),
        RESET_VIEW(GuidebookText.ResetView, 0, 32),
        SCENE_EDITOR_RESET_PREVIEW(GuidebookText.SceneEditorResetPreview, 0, 32),
        OPEN_FULL_WIDTH_VIEW(GuidebookText.FullWidthView, 16, 32),
        SCENE_EDITOR_EXPORT(GuidebookText.SceneEditorExport, 0, 48),
        SCENE_EDITOR_IMPORT_STRUCTURE(GuidebookText.SceneEditorImportStructure, 16, 48),
        SCENE_EDITOR_SCREENSHOT(GuidebookText.SceneEditorScreenshot, 32, 48),
        CLOSE_FULL_WIDTH_VIEW(GuidebookText.CloseFullWidthView, 32, 32),
        SCENE_EDITOR_SNAP(GuidebookText.SceneEditorSnap, 32, 32),
        SCENE_EDITOR_DELETE_ELEMENT(GuidebookText.SceneEditorDeleteElement, 32, 0);

        private final GuidebookText textKey;
        final int iconSrcX;
        final int iconSrcY;

        Role(GuidebookText textKey, int iconSrcX, int iconSrcY) {
            this.textKey = textKey;
            this.iconSrcX = iconSrcX;
            this.iconSrcY = iconSrcY;
        }

        public String tooltip() {
            return textKey.text();
        }

        public int iconSrcX() {
            return iconSrcX;
        }

        public int iconSrcY() {
            return iconSrcY;
        }
    }
}
