package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuideSiteItemIconExporter implements GuideSiteItemIconResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSiteItemIconExporter.class);
    private static final int ICON_SIZE = 32;

    private final GuideSiteAssetRegistry assets;
    private final Map<String, String> exportedIcons = new LinkedHashMap<String, String>();

    public GuideSiteItemIconExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    @Override
    public synchronized String exportIcon(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }

        String cacheKey = cacheKey(stack);
        String cached = exportedIcons.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String exportedPath = GuideSitePageAssetExporter.ROOT_PREFIX
                + assets.writeShared("item-icons", ".png", renderPng(stack.copy()));
            exportedIcons.put(cacheKey, exportedPath);
            return exportedPath;
        } catch (Throwable t) {
            LOG.debug("Failed to export offline icon for {}", GuideSiteItemSupport.itemId(stack), t);
            exportedIcons.put(cacheKey, "");
            return "";
        }
    }

    private String cacheKey(ItemStack stack) {
        StringBuilder key = new StringBuilder();
        key.append(GuideSiteItemSupport.itemId(stack))
            .append('#')
            .append(stack.getItemDamage())
            .append('#')
            .append(stack.stackSize);
        if (stack.getTagCompound() != null) {
            key.append('#')
                .append(stack.getTagCompound());
        }
        return key.toString();
    }

    private byte[] renderPng(ItemStack stack) throws Exception {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null || minecraft.fontRenderer == null) {
            throw new IllegalStateException("Minecraft client is not ready for item icon export.");
        }

        Framebuffer framebuffer = new Framebuffer(ICON_SIZE, ICON_SIZE, true);
        framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        boolean projectionPushed = false;
        boolean modelViewPushed = false;

        try {
            minecraft.displayWidth = ICON_SIZE;
            minecraft.displayHeight = ICON_SIZE;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, ICON_SIZE, ICON_SIZE);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, ICON_SIZE, ICON_SIZE, 0.0D, 1000.0D, 3000.0D);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            modelViewPushed = true;
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            float scale = (ICON_SIZE - 2f) / 16f;
            float origin = (ICON_SIZE - 16f * scale) / 2f;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(origin, origin, 0f);
                GL11.glScalef(scale, scale, 1f);

                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL11.GL_NORMALIZE);
                GL11.glEnable(GL11.GL_DEPTH_TEST);

                RenderItem itemRenderer = RenderItem.getInstance();
                itemRenderer.zLevel = 100f;
                itemRenderer
                    .renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer
                    .renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer.zLevel = 0f;
            } finally {
                GL11.glPopMatrix();
                RenderHelper.disableStandardItemLighting();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(readPixels(), "png", out);
            return out.toByteArray();
        } finally {
            if (modelViewPushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }

            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private BufferedImage readPixels() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(ICON_SIZE * ICON_SIZE * 4);
        GL11.glReadPixels(0, 0, ICON_SIZE, ICON_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < ICON_SIZE; y++) {
            int flippedY = ICON_SIZE - 1 - y;
            for (int x = 0; x < ICON_SIZE; x++) {
                int index = (x + y * ICON_SIZE) * 4;
                int red = buffer.get(index) & 0xFF;
                int green = buffer.get(index + 1) & 0xFF;
                int blue = buffer.get(index + 2) & 0xFF;
                int alpha = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }
}
