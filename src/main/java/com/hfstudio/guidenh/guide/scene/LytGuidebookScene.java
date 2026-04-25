package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import org.joml.Vector3f;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class LytGuidebookScene extends LytBlock {

    private static final float DRAG_ROTATE_SENSITIVITY = 0.5f;
    private static final float WHEEL_ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 10f;

    private int dragButton = -1;
    private int dragLastX;
    private int dragLastY;

    private boolean interactive = true;

    public static int SCENE_BG_COLOR = 0xFF0A0A10;
    public static int SCENE_BORDER_COLOR = 0xFF303040;

    private static final ResourceLocation BUTTONS_TEXTURE = new ResourceLocation(
        "guidenh",
        "textures/guide/buttons.png");

    private static final GuideIconButton.Role[] SCENE_BUTTONS_SHOWN = { GuideIconButton.Role.HIDE_ANNOTATIONS,
        GuideIconButton.Role.ZOOM_IN, GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };
    private static final GuideIconButton.Role[] SCENE_BUTTONS_HIDDEN = { GuideIconButton.Role.SHOW_ANNOTATIONS,
        GuideIconButton.Role.ZOOM_IN, GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };
    // Shown when the scene has no annotations at all: drop the annotation toggle entirely.
    private static final GuideIconButton.Role[] SCENE_BUTTONS_NO_ANNOTATIONS = { GuideIconButton.Role.ZOOM_IN,
        GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };

    private static final int DEFAULT_WIDTH = 256;
    private static final int DEFAULT_HEIGHT = 192;

    private GuidebookLevel level = new GuidebookLevel();
    private CameraSettings camera = new CameraSettings();
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private final List<SceneAnnotation> annotations = new ArrayList<>();

    // Reuse annotation partitions instead of allocating new lists every frame.
    private final List<InWorldAnnotation> inWorldScratch = new ArrayList<>();
    private final List<OverlayAnnotation> overlayScratch = new ArrayList<>();

    // Reuse hovered-block overlay objects across frames.
    private final Vector3f hoverBoxMin = new Vector3f();
    private final Vector3f hoverBoxMax = new Vector3f();
    private final ConstantColor hoverBoxColor = new ConstantColor(0xFFFFFFFF);
    private final InWorldBoxAnnotation hoverBoxAnnotation = new InWorldBoxAnnotation(
        hoverBoxMin,
        hoverBoxMax,
        hoverBoxColor,
        1f);
    private LytRect cachedOverlayViewport;
    private LytRect cachedScreenRect;
    private LytRect cachedSceneRect;
    private int sceneButtonsAbsX;
    private int sceneButtonsAbsY;
    private boolean cachedSceneButtonsVisible = true;
    private boolean cachedSceneHasAnnotations = true;
    private GuideIconButton.Role[] cachedSceneButtonRoles = SCENE_BUTTONS_SHOWN;

    private boolean annotationsVisible = true;

    private float[] initialCam = new float[] { 1f, 0f, 0f, 0f, 0f, 0f };

    @Nullable
    private int[] hoveredBlock;

    public LytGuidebookScene() {
        camera.setPerspectivePreset(PerspectivePreset.ISOMETRIC_NORTH_EAST);
        snapshotInitialCamera();
    }

    public GuidebookLevel getLevel() {
        return level;
    }

    public void setLevel(GuidebookLevel level) {
        this.level = level != null ? level : new GuidebookLevel();
        if (!this.level.isEmpty()) {
            var c = this.level.getCenter();
            camera.setRotationCenter(c[0], c[1], c[2]);
        }
        snapshotInitialCamera();
    }

    public CameraSettings getCamera() {
        return camera;
    }

    public void setCamera(CameraSettings camera) {
        this.camera = camera != null ? camera : new CameraSettings();
        snapshotInitialCamera();
    }

    public void snapshotInitialCamera() {
        initialCam[0] = camera.getZoom();
        initialCam[1] = camera.getRotationX();
        initialCam[2] = camera.getRotationY();
        initialCam[3] = camera.getRotationZ();
        initialCam[4] = camera.getOffsetX();
        initialCam[5] = camera.getOffsetY();
    }

    public int getSceneWidth() {
        return width;
    }

    public int getSceneHeight() {
        return height;
    }

    public void setSceneSize(int width, int height) {
        this.width = Math.max(16, width);
        this.height = Math.max(16, height);
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void addAnnotation(SceneAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    public List<SceneAnnotation> getAnnotations() {
        return annotations;
    }

    @Nullable
    public SceneAnnotation updateAnnotationHover(int mouseX, int mouseY) {
        if (!annotationsVisible) {
            clearAnnotationHover();
            return null;
        }
        SceneAnnotation hit = null;
        LytRect viewport = cachedScreenRect = updateCachedRect(cachedScreenRect, lastAbsX, lastAbsY, lastW, lastH);
        // Iterate top-down: overlays sit on top of in-world geometry.
        for (int i = annotations.size() - 1; i >= 0; i--) {
            var a = annotations.get(i);
            boolean hovered = false;
            if (hit == null) {
                if (a instanceof OverlayAnnotation ov) {
                    hovered = ov.getBoundingRect(camera, viewport)
                        .contains(mouseX, mouseY);
                } else if (a instanceof InWorldBoxAnnotation box) {
                    hovered = boxScreenRectContains(box, viewport, mouseX, mouseY);
                } else if (a instanceof InWorldLineAnnotation line) {
                    hovered = lineScreenDistance(line, viewport, mouseX, mouseY) <= LINE_HOVER_TOLERANCE_PX;
                }
            }
            a.setHovered(hovered);
            if (hovered) hit = a;
        }
        return hit;
    }

    private static final int LINE_HOVER_TOLERANCE_PX = 4;

    private boolean boxScreenRectContains(InWorldBoxAnnotation box, LytRect viewport, int mouseX, int mouseY) {
        var min = box.min();
        var max = box.max();
        int cx = viewport.x() + viewport.width() / 2;
        int cy = viewport.y() + viewport.height() / 2;
        int minSx = Integer.MAX_VALUE, minSy = Integer.MAX_VALUE;
        int maxSx = Integer.MIN_VALUE, maxSy = Integer.MIN_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            float x = ((corner & 1) == 0) ? min.x : max.x;
            float y = ((corner & 2) == 0) ? min.y : max.y;
            float z = ((corner & 4) == 0) ? min.z : max.z;
            var s = camera.worldToScreen(x, y, z);
            int sx = cx + Math.round(s.x);
            int sy = cy + Math.round(s.y);
            if (sx < minSx) minSx = sx;
            if (sy < minSy) minSy = sy;
            if (sx > maxSx) maxSx = sx;
            if (sy > maxSy) maxSy = sy;
        }
        return mouseX >= minSx && mouseX <= maxSx && mouseY >= minSy && mouseY <= maxSy;
    }

    private float lineScreenDistance(InWorldLineAnnotation line, LytRect viewport, int mouseX, int mouseY) {
        int cx = viewport.x() + viewport.width() / 2;
        int cy = viewport.y() + viewport.height() / 2;
        var a = camera.worldToScreen(line.from().x, line.from().y, line.from().z);
        var b = camera.worldToScreen(line.to().x, line.to().y, line.to().z);
        float ax = cx + a.x, ay = cy + a.y;
        float bx = cx + b.x, by = cy + b.y;
        float dx = bx - ax, dy = by - ay;
        float lenSq = dx * dx + dy * dy;
        float t = lenSq < 1e-4f ? 0f : Math.max(0f, Math.min(1f, ((mouseX - ax) * dx + (mouseY - ay) * dy) / lenSq));
        float px = ax + t * dx, py = ay + t * dy;
        float ex = mouseX - px, ey = mouseY - py;
        return (float) Math.sqrt(ex * ex + ey * ey);
    }

    public void clearAnnotationHover() {
        for (var a : annotations) a.setHovered(false);
    }

    public boolean isAnnotationsVisible() {
        return annotationsVisible;
    }

    public void setAnnotationsVisible(boolean visible) {
        this.annotationsVisible = visible;
        if (!visible) clearAnnotationHover();
    }

    // LytBlock

    /** Horizontal space the floating button column steals from the row when interactive. */
    private int buttonColumnReserve() {
        return interactive ? (BTN_OUTSIDE_GAP + BTN_SIZE) : 0;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int reserve = buttonColumnReserve();
        int totalDesired = width + reserve;
        int w = Math.min(totalDesired, Math.max(reserve + 16, availableWidth));
        int sceneW = Math.max(16, w - reserve);
        int buttonsTotalH = interactive
            ? (BTN_SIZE * SCENE_BUTTONS_SHOWN.length + BTN_GAP * (SCENE_BUTTONS_SHOWN.length - 1))
            : 0;
        int h = Math.max(height, buttonsTotalH);
        // Stash the inner scene width so render() stays consistent with layout.
        this.layoutSceneWidth = sceneW;
        return new LytRect(x, y, w, h);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        int sceneW = layoutSceneWidth > 0 ? layoutSceneWidth : getBounds().width() - buttonColumnReserve();
        if (sceneW < 16) sceneW = Math.max(16, getBounds().width() - buttonColumnReserve());
        LytRect sceneRect = cachedSceneRect = updateCachedRect(
            cachedSceneRect,
            getBounds().x(),
            getBounds().y(),
            sceneW,
            getBounds().height());
        if (level.isEmpty() && annotations.isEmpty()) {
            context.fillRect(sceneRect, SCENE_BG_COLOR);
            context.drawBorder(sceneRect, SCENE_BORDER_COLOR, 1);
            return;
        }

        context.fillRect(sceneRect, SCENE_BG_COLOR);

        int absX = sceneRect.x();
        int absY = sceneRect.y();
        int clipX = absX, clipY = absY, clipW = sceneRect.width(), clipH = sceneRect.height();
        if (context instanceof VanillaRenderContext mrc) {
            absX = mrc.getDocumentOriginX() + getBounds().x();
            absY = mrc.getDocumentOriginY() + getBounds().y() - mrc.getScrollOffsetY();
            LytRect vp = mrc.viewport();
            clipX = mrc.getDocumentOriginX();
            clipY = mrc.getDocumentOriginY();
            clipW = vp.width();
            clipH = vp.height();
        }
        this.renderedContentClip = updateCachedRect(this.renderedContentClip, clipX, clipY, clipW, clipH);

        int w = sceneRect.width();
        int h = sceneRect.height();
        camera.setViewportSize(w, h);

        List<InWorldAnnotation> inWorld = inWorldScratch;
        List<OverlayAnnotation> overlays = overlayScratch;
        inWorld.clear();
        overlays.clear();
        if (annotationsVisible) {
            for (var a : annotations) {
                if (a instanceof InWorldAnnotation iw) inWorld.add(iw);
                else if (a instanceof OverlayAnnotation ov) overlays.add(ov);
            }
        }
        if (hoveredBlock != null) {
            int bx = hoveredBlock[0], by = hoveredBlock[1], bz = hoveredBlock[2];
            Block block = level.getBlock(bx, by, bz);
            double minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
            if (block != null && block != Blocks.air) {
                try {
                    block.setBlockBoundsBasedOnState(level, bx, by, bz);
                    minX = block.getBlockBoundsMinX();
                    minY = block.getBlockBoundsMinY();
                    minZ = block.getBlockBoundsMinZ();
                    maxX = block.getBlockBoundsMaxX();
                    maxY = block.getBlockBoundsMaxY();
                    maxZ = block.getBlockBoundsMaxZ();
                    if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
                        minX = minY = minZ = 0;
                        maxX = maxY = maxZ = 1;
                    }
                } catch (Throwable t) {
                    minX = minY = minZ = 0;
                    maxX = maxY = maxZ = 1;
                }
            }
            float EPS = 0.002f;
            hoverBoxMin.set((float) (bx + minX) - EPS, (float) (by + minY) - EPS, (float) (bz + minZ) - EPS);
            hoverBoxMax.set((float) (bx + maxX) + EPS, (float) (by + maxY) + EPS, (float) (bz + maxZ) + EPS);
            inWorld.add(hoverBoxAnnotation);
        }

        GuidebookLevelRenderer.getInstance()
            .render(level, camera, absX, absY, w, h, clipX, clipY, clipW, clipH, 0f, inWorld, context.lightDarkMode());

        if (!overlays.isEmpty()) {
            LytRect viewport = cachedOverlayViewport = updateCachedRect(cachedOverlayViewport, absX, absY, w, h);
            // Scissor overlays to the scene rect so diamond icons etc. cannot escape.
            // NOTE: pushScissor expects SCREEN coords (same space as GuideScreen.cachedScissorRect),
            // not document-local coords. Passing sceneRect (doc-local) caused the scissor to clip
            // overlays out of sight, making diamond annotations disappear.
            context.pushScissor(viewport);
            try {
                for (var o : overlays) {
                    o.render(camera, context, viewport);
                }
            } finally {
                context.popScissor();
            }
        }

        // Draw border AFTER the 3D content so border pixels always sit on top.
        context.drawBorder(sceneRect, SCENE_BORDER_COLOR, 1);

        if (interactive) {
            drawSceneButtons(sceneRect.x(), sceneRect.y(), w, h, absX, absY);
        }
    }

    private static final int BTN_SIZE = 16;
    private static final int BTN_GAP = 2;
    private static final int BTN_OUTSIDE_GAP = 3;

    private int lastAbsX, lastAbsY, lastW, lastH;
    /** Width reserved for the inner 3D scene (bounds.width minus the button column). */
    private int layoutSceneWidth;

    @Nullable
    private LytRect renderedContentClip;

    // Reuse rect records when geometry is unchanged.
    private static LytRect updateCachedRect(@Nullable LytRect current, int x, int y, int w, int h) {
        if (current != null && current.x() == x && current.y() == y && current.width() == w && current.height() == h) {
            return current;
        }
        return new LytRect(x, y, w, h);
    }

    private void drawSceneButtons(int drawX, int drawY, int w, int h, int absX, int absY) {
        this.lastAbsX = absX;
        this.lastAbsY = absY;
        this.lastW = w;
        this.lastH = h;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        int bx = drawX + w + BTN_OUTSIDE_GAP;
        int by = drawY;
        int absBx = absX + w + BTN_OUTSIDE_GAP;
        int absBy = absY;
        sceneButtonsAbsX = absBx;
        sceneButtonsAbsY = absBy;
        cachedScreenRect = updateCachedRect(cachedScreenRect, absX, absY, w, h);
        int mx, my;
        try {
            var mc = Minecraft.getMinecraft();
            int sw = DisplayScale.scaledWidth(), sh = DisplayScale.scaledHeight();
            mx = Mouse.getX() * sw / mc.displayWidth;
            my = sh - Mouse.getY() * sh / mc.displayHeight - 1;
        } catch (Throwable t) {
            mx = -1;
            my = -1;
        }
        GuideIconButton.Role[] roles = cachedSceneButtonRoles();
        for (var role : roles) {
            boolean hover = mx >= absBx && my >= absBy && mx < absBx + BTN_SIZE && my < absBy + BTN_SIZE;
            drawOneSceneButton(bx, by, role, hover);
            by += BTN_SIZE + BTN_GAP;
            absBy += BTN_SIZE + BTN_GAP;
        }
    }

    private GuideIconButton.Role[] sceneButtonRoles() {
        if (annotations.isEmpty()) {
            return SCENE_BUTTONS_NO_ANNOTATIONS;
        }
        return annotationsVisible ? SCENE_BUTTONS_SHOWN : SCENE_BUTTONS_HIDDEN;
    }

    private GuideIconButton.Role[] cachedSceneButtonRoles() {
        boolean hasAnnotations = !annotations.isEmpty();
        if (cachedSceneButtonsVisible != annotationsVisible || cachedSceneHasAnnotations != hasAnnotations) {
            cachedSceneButtonsVisible = annotationsVisible;
            cachedSceneHasAnnotations = hasAnnotations;
            cachedSceneButtonRoles = sceneButtonRoles();
        }
        return cachedSceneButtonRoles;
    }

    private static void drawOneSceneButton(int x, int y, GuideIconButton.Role role, boolean hovered) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(BUTTONS_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (hovered) {
            GL11.glColor4f(0x00 / 255f, 0xCA / 255f, 0xF2 / 255f, 1f);
        } else {
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
        float texSize = 64f;
        float u0 = role.iconSrcX() / texSize;
        float v0 = role.iconSrcY() / texSize;
        float u1 = (role.iconSrcX() + 16) / texSize;
        float v1 = (role.iconSrcY() + 16) / texSize;
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + BTN_SIZE, 0, u0, v1);
        tess.addVertexWithUV(x + BTN_SIZE, y + BTN_SIZE, 0, u1, v1);
        tess.addVertexWithUV(x + BTN_SIZE, y, 0, u1, v0);
        tess.addVertexWithUV(x, y, 0, u0, v0);
        tess.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    @Nullable
    public GuideIconButton.Role sceneButtonAt(int mouseX, int mouseY) {
        if (lastW <= 0 || lastH <= 0) return null;
        if (renderedContentClip != null) {
            int cx0 = renderedContentClip.x();
            int cy0 = renderedContentClip.y();
            int cx1 = cx0 + renderedContentClip.width();
            int cy1 = cy0 + renderedContentClip.height();
            if (lastAbsX + lastW <= cx0 || lastAbsX >= cx1) return null;
            if (lastAbsY + lastH <= cy0 || lastAbsY >= cy1) return null;
            // Also reject when the mouse is outside the visible content viewport entirely.
            if (mouseX < cx0 || mouseX >= cx1 || mouseY < cy0 || mouseY >= cy1) return null;
        }
        // Vertically, the mouse must be within this scene's own rendered band; otherwise a scene
        // below the viewport could return a false-positive hit purely on X coincidence because
        // its stashed sceneButtonsAbsY still falls inside the button rect math.
        if (mouseY < lastAbsY || mouseY >= lastAbsY + lastH) return null;
        int bx = sceneButtonsAbsX;
        int by = sceneButtonsAbsY;
        // Early-out on X: the whole button column lives at [sceneButtonsAbsX, sceneButtonsAbsX + BTN_SIZE).
        if (mouseX < bx || mouseX >= bx + BTN_SIZE) return null;
        var roles = cachedSceneButtonRoles();
        for (var role : roles) {
            boolean visible = renderedContentClip == null || (bx + BTN_SIZE > renderedContentClip.x()
                && bx < renderedContentClip.x() + renderedContentClip.width()
                && by + BTN_SIZE > renderedContentClip.y()
                && by < renderedContentClip.y() + renderedContentClip.height());
            if (visible && mouseX >= bx && mouseX < bx + BTN_SIZE && mouseY >= by && mouseY < by + BTN_SIZE) {
                return role;
            }
            by += BTN_SIZE + BTN_GAP;
        }
        return null;
    }

    public void setHoveredBlock(@Nullable int[] xyz) {
        this.hoveredBlock = xyz;
    }

    @Nullable
    public int[] getHoveredBlock() {
        return hoveredBlock;
    }

    public LytRect getScreenRect() {
        return cachedScreenRect = updateCachedRect(cachedScreenRect, lastAbsX, lastAbsY, lastW, lastH);
    }

    public boolean containsSceneViewport(int mouseX, int mouseY) {
        if (lastW <= 0 || lastH <= 0) return false;

        int x0 = lastAbsX;
        int y0 = lastAbsY;
        int x1 = x0 + lastW;
        int y1 = y0 + lastH;

        if (renderedContentClip != null) {
            int cx0 = renderedContentClip.x();
            int cy0 = renderedContentClip.y();
            int cx1 = cx0 + renderedContentClip.width();
            int cy1 = cy0 + renderedContentClip.height();
            if (x1 <= cx0 || x0 >= cx1 || y1 <= cy0 || y0 >= cy1) return false;
            x0 = Math.max(x0, cx0);
            y0 = Math.max(y0, cy0);
            x1 = Math.min(x1, cx1);
            y1 = Math.min(y1, cy1);
        }

        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    @Nullable
    public int[] pickBlock(int mouseAbsX, int mouseAbsY) {
        if (level.isEmpty() || lastW <= 0 || lastH <= 0) return null;
        float relX = (mouseAbsX) - (lastAbsX + lastW * 0.5f);
        float relY = (mouseAbsY) - (lastAbsY + lastH * 0.5f);
        camera.setViewportSize(lastW, lastH);
        float[] ray = camera.screenToWorldRay(relX, relY);
        float ox = ray[0], oy = ray[1], oz = ray[2];
        float dx = ray[3], dy = ray[4], dz = ray[5];
        int[] best = null;
        float bestT = Float.POSITIVE_INFINITY;
        for (int[] b : level.getFilledBlocks()) {
            float t = rayAabb(ox, oy, oz, dx, dy, dz, b[0], b[1], b[2], b[0] + 1f, b[1] + 1f, b[2] + 1f);
            if (!Float.isNaN(t) && t < bestT) {
                bestT = t;
                best = b;
            }
        }
        return best;
    }

    private static float rayAabb(float ox, float oy, float oz, float dx, float dy, float dz, float minX, float minY,
        float minZ, float maxX, float maxY, float maxZ) {
        float tmin = Float.NEGATIVE_INFINITY;
        float tmax = Float.POSITIVE_INFINITY;
        // X
        if (Math.abs(dx) < 1e-6f) {
            if (ox < minX || ox > maxX) return Float.NaN;
        } else {
            float t1 = (minX - ox) / dx;
            float t2 = (maxX - ox) / dx;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        // Y
        if (Math.abs(dy) < 1e-6f) {
            if (oy < minY || oy > maxY) return Float.NaN;
        } else {
            float t1 = (minY - oy) / dy;
            float t2 = (maxY - oy) / dy;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        // Z
        if (Math.abs(dz) < 1e-6f) {
            if (oz < minZ || oz > maxZ) return Float.NaN;
        } else {
            float t1 = (minZ - oz) / dz;
            float t2 = (maxZ - oz) / dz;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        return tmin;
    }

    public void activateSceneButton(GuideIconButton.Role role) {
        switch (role) {
            case HIDE_ANNOTATIONS, SHOW_ANNOTATIONS -> setAnnotationsVisible(!annotationsVisible);
            case ZOOM_IN -> {
                float z = Math.min(10f, camera.getZoom() * 1.25f);
                camera.setZoom(z);
            }
            case ZOOM_OUT -> {
                float z = Math.max(0.1f, camera.getZoom() / 1.25f);
                camera.setZoom(z);
            }
            case RESET_VIEW -> {
                camera.setZoom(initialCam[0]);
                camera.setRotationX(initialCam[1]);
                camera.setRotationY(initialCam[2]);
                camera.setRotationZ(initialCam[3]);
                camera.setOffsetX(initialCam[4]);
                camera.setOffsetY(initialCam[5]);
            }
            default -> {}
        }
    }

    public void startDrag(int mouseX, int mouseY, int button) {
        if (!interactive) return;
        this.dragButton = button;
        this.dragLastX = mouseX;
        this.dragLastY = mouseY;
    }

    private static boolean isRotateButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 0 : button == 1;
    }

    private static boolean isPanButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 1 : button == 0;
    }

    public void drag(int mouseX, int mouseY) {
        if (dragButton < 0) return;
        int dx = mouseX - dragLastX;
        int dy = mouseY - dragLastY;
        dragLastX = mouseX;
        dragLastY = mouseY;

        if (isPanButton(dragButton)) {
            camera.setOffsetX(camera.getOffsetX() + dx);
            camera.setOffsetY(camera.getOffsetY() - dy);
        } else if (isRotateButton(dragButton)) {
            camera.setRotationY(camera.getRotationY() + dx * DRAG_ROTATE_SENSITIVITY);
            camera.setRotationX(camera.getRotationX() + dy * DRAG_ROTATE_SENSITIVITY);
        }
    }

    public void endDrag() {
        this.dragButton = -1;
    }

    public boolean isDragging() {
        return dragButton >= 0;
    }

    public void scroll(int dwheel) {
        if (!interactive) return;
        if (dwheel == 0) return;
        float z = camera.getZoom();
        if (dwheel > 0) z *= WHEEL_ZOOM_STEP;
        else z /= WHEEL_ZOOM_STEP;
        if (z < MIN_ZOOM) z = MIN_ZOOM;
        if (z > MAX_ZOOM) z = MAX_ZOOM;
        camera.setZoom(z);
    }
}
