package com.hfstudio.guidenh.guide.scene.annotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

/**
 * Renders a mouse-input icon (LMB, RMB, or scroll wheel) anchored to a world position,
 * ported from Ponder's InputWindowElement.
 * Supports optional modifier prefix text ("Sneak +" / "Ctrl +") and an item icon.
 */
public class PonderInputAnnotation extends OverlayAnnotation {

    /** The type of input gesture to display. */
    public enum InputType {

        /** Left mouse button. Sprite at x=0, y=64 in ponder_widgets.png. */
        LMB(0, 64),
        /** Scroll wheel. Sprite at x=16, y=64 in ponder_widgets.png. */
        SCROLL(16, 64),
        /** Right mouse button. Sprite at x=32, y=64 in ponder_widgets.png. */
        RMB(32, 64);

        public final int srcX;
        public final int srcY;

        InputType(int srcX, int srcY) {
            this.srcX = srcX;
            this.srcY = srcY;
        }
    }

    private static final int ICON_SIZE = 16;
    private static final int BOX_PAD = 4;
    private static final int ITEM_GAP = 2;
    private static final ResourceLocation PONDER_WIDGETS = GuideIconButton.PONDER_WIDGETS_TEX;

    private static final RenderItem ITEM_RENDERER = new RenderItem();

    private final Vector3f worldPos;
    private final InputType inputType;
    @Nullable
    private String modifier;
    @Nullable
    private ItemStack item;

    public PonderInputAnnotation(Vector3f worldPos, InputType inputType) {
        this.worldPos = worldPos;
        this.inputType = inputType;
    }

    public PonderInputAnnotation setModifier(@Nullable String modifier) {
        this.modifier = modifier;
        return this;
    }

    public PonderInputAnnotation setItemStack(@Nullable ItemStack item) {
        this.item = item;
        return this;
    }

    public Vector3f getWorldPos() {
        return worldPos;
    }

    public InputType getInputType() {
        return inputType;
    }

    @Nullable
    public String getModifier() {
        return modifier;
    }

    @Nullable
    public ItemStack getItemStack() {
        return item;
    }

    private int totalBoxWidth() {
        int iconCols = item != null ? 2 : 1;
        return ICON_SIZE * iconCols + (item != null ? ITEM_GAP : 0) + BOX_PAD * 2;
    }

    private int totalBoxHeight() {
        return ICON_SIZE + BOX_PAD * 2;
    }

    @Override
    public LytRect getBoundingRect(CameraSettings camera, LytRect viewport) {
        Vector3f screen = camera.worldToScreen(worldPos.x, worldPos.y, worldPos.z);
        int boxW = totalBoxWidth();
        int boxH = totalBoxHeight();
        int cx = viewport.x() + viewport.width() / 2 + Math.round(screen.x);
        int cy = viewport.y() + viewport.height() / 2 + Math.round(screen.y);
        return new LytRect(cx - boxW / 2, cy - boxH / 2, boxW, boxH);
    }

    @Override
    public void render(CameraSettings camera, RenderContext context, LytRect viewport) {
        Vector3f screen = camera.worldToScreen(worldPos.x, worldPos.y, worldPos.z);

        int docOx = 0, docOy = 0, scroll = 0;
        if (context instanceof VanillaRenderContext vrc) {
            docOx = vrc.getDocumentOriginX();
            docOy = vrc.getDocumentOriginY();
            scroll = vrc.getScrollOffsetY();
        }

        int cx = viewport.x() + viewport.width() / 2 + Math.round(screen.x) - docOx;
        int cy = viewport.y() + viewport.height() / 2 + Math.round(screen.y) - docOy + scroll;

        int boxW = totalBoxWidth();
        int boxH = totalBoxHeight();
        int bx = cx - boxW / 2;
        int by = cy - boxH / 2;

        Minecraft mc = Minecraft.getMinecraft();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float fade = getFade();

        if (modifier != null && !modifier.isEmpty()) {
            String modText = modifier.equalsIgnoreCase("sneak") ? "Sneak +" : "Ctrl +";
            int textW = mc.fontRenderer.getStringWidth(modText);
            int textX = cx - textW / 2;
            int textY = by - mc.fontRenderer.FONT_HEIGHT - 2;
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.fontRenderer.drawStringWithShadow(modText, textX, textY, applyFade(0xFFCCCCCC, fade));
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        TextAnnotation.drawFilledRect(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, applyFade(0x80AAAADD, fade));
        TextAnnotation.drawFilledRect(bx, by, bx + boxW, by + boxH, applyFade(0xCC0E0E20, fade));

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        int iconX = bx + BOX_PAD;
        int iconY = by + BOX_PAD;

        if (item != null) {
            try {
                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_NORMALIZE);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                ITEM_RENDERER.zLevel = 100f;
                ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), item, iconX, iconY);
                ITEM_RENDERER.zLevel = 0f;
                RenderHelper.disableStandardItemLighting();
            } finally {
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_NORMALIZE);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            iconX += ICON_SIZE + ITEM_GAP;
        }

        mc.getTextureManager()
            .bindTexture(GuideIconButton.PONDER_WIDGETS_TEX);

        GL11.glColor4f(1f, 1f, 1f, fade);
        float texSize = 256f;
        float u0 = inputType.srcX / texSize;
        float v0 = inputType.srcY / texSize;
        float u1 = (inputType.srcX + 16) / texSize;
        float v1 = (inputType.srcY + 16) / texSize;

        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(iconX, iconY + ICON_SIZE, 0, u0, v1);
        tess.addVertexWithUV(iconX + ICON_SIZE, iconY + ICON_SIZE, 0, u1, v1);
        tess.addVertexWithUV(iconX + ICON_SIZE, iconY, 0, u1, v0);
        tess.addVertexWithUV(iconX, iconY, 0, u0, v0);
        tess.draw();

        GL11.glColor4f(1f, 1f, 1f, fade);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
