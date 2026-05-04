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

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.compat.neicustomdiagram.NeiCustomDiagramBridge;
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

    /**
     * Extra transparent border around the NEI body rectangle. GregTech (and similar) ModularUI / nine-patch chrome
     * often draws a few pixels outside
     * {@link NeiRecipeLookup#lookupHandlerWidth}/{@link NeiRecipeLookup#lookupHandlerHeight};
     * a flush viewport clips top/right bezel lines and truncates footer text unless we pad here. Site overlays use the
     * same
     * inset — see {@link com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeExporter#renderNeiPositionedSlots}.
     */
    public static final int VIEWPORT_MARGIN_PX = 6;

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

        int m = VIEWPORT_MARGIN_PX;
        int vw = bodyW + 2 * m;
        int vh = bodyH + 2 * m;
        if (vw > MAX_EXPORT_EDGE || vh > MAX_EXPORT_EDGE) {
            LOG.debug("Skip NEI Phase1 export: {}x{} exceeds cap", vw, vh);
            return null;
        }

        String cacheKey = cacheKey(handler, recipeIndex, vw, vh);
        Result existing = cache.get(cacheKey);
        if (existing != null) {
            return existing;
        }

        try {
            int bodyYShiftPx = NeiRecipeLookup.lookupHandlerYShift(handler);
            byte[] png = renderPng(handler, recipeIndex, vw, vh);
            String rel = GuideSitePageAssetExporter.ROOT_PREFIX + assets.writeShared("nei-phase1-bg", ".png", png);
            Result res = new Result(rel, vw, vh, bodyYShiftPx);
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

    private static byte[] renderPng(Object handler, int recipeIndex, int viewportW, int viewportH) throws Exception {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for NEI Phase1 export.");
        }

        boolean skipForeground = NeiRecipeLookup.otherStacksThrows(handler, recipeIndex);
        int yShift = NeiRecipeLookup.lookupHandlerYShift(handler);
        int m = VIEWPORT_MARGIN_PX;

        Framebuffer framebuffer = new Framebuffer(viewportW, viewportH, true);
        framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        boolean projectionPushed = false;
        boolean modelViewPushed = false;

        try {
            minecraft.displayWidth = viewportW;
            minecraft.displayHeight = viewportH;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, viewportW, viewportH);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, viewportW, viewportH, 0.0D, 1000.0D, 3000.0D);

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
                GL11.glTranslatef((float) m, (float) (yShift + m), 0f);
                NeiRecipeLookup.callDrawBackground(handler, recipeIndex);
                if (!skipForeground) {
                    NeiRecipeLookup.callDrawForeground(handler, recipeIndex);
                    NeiRecipeLookup.callDrawExtras(handler, recipeIndex);
                }
            } finally {
                GL11.glPopMatrix();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(readPixels(viewportW, viewportH), "png", out);
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
        /**
         * Width/height incl. symmetric {@link GuideSiteNeiPhase1BackgroundExporter#VIEWPORT_MARGIN_PX} padding per
         * edge.
         */
        public final int pixelWidth;
        public final int pixelHeight;
        /**
         * {@link NeiRecipeLookup#lookupHandlerYShift} applied when rendering Phase1; site overlays must align to the
         * same shift or icons drift from the raster background.
         */
        public final int bodyYShiftPx;

        public Result(String relativeUrl, int pixelWidth, int pixelHeight, int bodyYShiftPx) {
            this.relativeUrl = relativeUrl;
            this.pixelWidth = pixelWidth;
            this.pixelHeight = pixelHeight;
            this.bodyYShiftPx = bodyYShiftPx;
        }
    }
}
