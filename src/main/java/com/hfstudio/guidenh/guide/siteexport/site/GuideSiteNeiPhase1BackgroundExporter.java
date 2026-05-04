package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.compat.neicustomdiagram.NeiCustomDiagramBridge;
import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.internal.recipe.NeiRecipeLayoutMetrics;

/**
 * Renders NEI handler Phase1 ({@code drawBackground} / optionally {@code drawForeground} /
 * {@code drawExtras}) off-screen and writes a PNG shared asset for static site overlays.
 *
 * @see com.hfstudio.guidenh.guide.internal.recipe.NeiHandlerRenderer
 */
public final class GuideSiteNeiPhase1BackgroundExporter {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSiteNeiPhase1BackgroundExporter.class);

    /** Upper bound avoids extreme handler sizes blowing memory during export. */
    private static final int MAX_EXPORT_EDGE = 1024;

    private final GuideSiteAssetRegistry assets;
    private final Map<String, Result> cache = new LinkedHashMap<>(64, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return size() > 256;
        }
    };

    public GuideSiteNeiPhase1BackgroundExporter(GuideSiteAssetRegistry assets) {
        this.assets = Objects.requireNonNull(assets);
    }

    /**
     * @return png site-relative URL (with {@link GuideSitePageAssetExporter#ROOT_PREFIX}), or {@code null} if skipped
     */
    public synchronized @Nullable Result capture(@Nullable Object handler, int recipeIndex) {
        if (handler == null || !NeiRecipeLookup.isAvailable()) {
            return null;
        }
        if (NeiCustomDiagramBridge.isDiagramGroupHandler(handler)) {
            return null;
        }

        int bodyW = Math.max(1, NeiRecipeLookup.lookupHandlerWidth(handler));
        int handlerPlatH = NeiRecipeLookup.lookupHandlerHeight(handler);
        int recipePlatH = NeiRecipeLookup.lookupRecipeHeight(handler, recipeIndex);
        int bodyH = NeiRecipeLayoutMetrics
            .resolveBodyHeight(handlerPlatH, recipePlatH, LytNeiRecipeBox.DEFAULT_BODY_HEIGHT);
        if (bodyW > MAX_EXPORT_EDGE || bodyH > MAX_EXPORT_EDGE) {
            LOG.debug("Skip NEI Phase1 export: {}x{} exceeds cap", bodyW, bodyH);
            return null;
        }

        String cacheKey = cacheKey(handler, recipeIndex, bodyW, bodyH);
        Result existing = cache.get(cacheKey);
        if (existing != null) {
            return existing;
        }

        try {
            byte[] png = renderPng(handler, recipeIndex, bodyW, bodyH);
            String rel = GuideSitePageAssetExporter.ROOT_PREFIX + assets.writeShared("nei-phase1-bg", ".png", png);
            Result res = new Result(rel, bodyW, bodyH);
            cache.put(cacheKey, res);
            return res;
        } catch (Throwable t) {
            LOG.debug("NEI Phase1 snapshot failed for {} recipe {}", handler.getClass(), recipeIndex, t);
            return null;
        }
    }

    private static String cacheKey(Object handler, int recipeIndex, int bodyW, int bodyH) {
        String overlay = NeiRecipeLookup.lookupOverlayIdentifier(handler);
        if (overlay == null || overlay.isEmpty()) {
            overlay = handler.getClass()
                .getName();
        }
        return overlay + '|' + System.identityHashCode(handler) + '|' + recipeIndex + '|' + bodyW + 'x' + bodyH;
    }

    private static byte[] renderPng(Object handler, int recipeIndex, int bodyW, int bodyH) throws Exception {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for NEI Phase1 export.");
        }

        boolean skipForeground = NeiRecipeLookup.otherStacksThrows(handler, recipeIndex);
        int yShift = NeiRecipeLookup.lookupHandlerYShift(handler);

        Framebuffer framebuffer = new Framebuffer(bodyW, bodyH, true);
        framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        boolean projectionPushed = false;
        boolean modelViewPushed = false;

        try {
            minecraft.displayWidth = bodyW;
            minecraft.displayHeight = bodyH;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, bodyW, bodyH);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, bodyW, bodyH, 0.0D, 1000.0D, 3000.0D);

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

            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(0f, yShift, 0f);
                NeiRecipeLookup.callDrawBackground(handler, recipeIndex);
                if (!skipForeground) {
                    NeiRecipeLookup.callDrawForeground(handler, recipeIndex);
                    NeiRecipeLookup.callDrawExtras(handler, recipeIndex);
                }
            } finally {
                GL11.glPopMatrix();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(readPixels(bodyW, bodyH), "png", out);
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

    private static BufferedImage readPixels(int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int flippedY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int index = (x + y * width) * 4;
                int red = buffer.get(index) & 0xFF;
                int green = buffer.get(index + 1) & 0xFF;
                int blue = buffer.get(index + 2) & 0xFF;
                int alpha = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    /** Non-null fields when capture succeeds; use {@link #relativeUrl} in HTML/CSS. */
    public static final class Result {

        public final String relativeUrl;
        public final int pixelWidth;
        public final int pixelHeight;

        public Result(String relativeUrl, int pixelWidth, int pixelHeight) {
            this.relativeUrl = relativeUrl;
            this.pixelWidth = pixelWidth;
            this.pixelHeight = pixelHeight;
        }
    }
}
