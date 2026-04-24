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

    private static final ResourceLocation TEX = new ResourceLocation("guidenh", "textures/guide/buttons.png");

    private Role role;

    public GuideIconButton(int id, int x, int y, Role role) {
        super(id, x, y, WIDTH, HEIGHT, "");
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

        int color;
        if (!enabled) color = 0x60FFFFFF;
        else if (field_146123_n) color = 0xFF00CAF2;
        else color = 0xC0FFFFFF;

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
        tess.addVertexWithUV(xPosition, yPosition + height, 0, u0, v1);
        tess.addVertexWithUV(xPosition + width, yPosition + height, 0, u1, v1);
        tess.addVertexWithUV(xPosition + width, yPosition, 0, u1, v0);
        tess.addVertexWithUV(xPosition, yPosition, 0, u0, v0);
        tess.draw();

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    public enum Role {

        BACK(GuidebookText.HistoryGoBack, 0, 0),
        FORWARD(GuidebookText.HistoryGoForward, 16, 0),
        CLOSE(GuidebookText.Close, 32, 0),
        SEARCH(GuidebookText.Search, 48, 0),
        HIDE_ANNOTATIONS(GuidebookText.HideAnnotations, 0, 16),
        SHOW_ANNOTATIONS(GuidebookText.ShowAnnotations, 16, 16),
        ZOOM_OUT(GuidebookText.ZoomOut, 32, 16),
        ZOOM_IN(GuidebookText.ZoomIn, 48, 16),
        RESET_VIEW(GuidebookText.ResetView, 0, 32),
        OPEN_FULL_WIDTH_VIEW(GuidebookText.FullWidthView, 16, 32),
        CLOSE_FULL_WIDTH_VIEW(GuidebookText.CloseFullWidthView, 32, 32);

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
