package com.hfstudio.guidenh.guide.document.block;

import java.util.IdentityHashMap;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

import lombok.Setter;

public abstract class LytMermaidCanvas<T extends LytMermaidCanvas<T>> extends LytBlock
    implements DocumentDragTarget, InteractiveElement {

    private static final float ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;

    private int contentOffsetX;
    private int contentOffsetY;
    private final SmoothFloatState visualContentOffsetX = new SmoothFloatState();
    private final SmoothFloatState visualContentOffsetY = new SmoothFloatState();
    @Setter
    private float zoom = 1f;
    private final SmoothFloatState visualZoom = new SmoothFloatState();
    private boolean dragging;
    private int dragLastDocumentX;
    private int dragLastDocumentY;

    private final Map<ResolvedTextStyle, ResolvedTextStyle> scaledStyleCache = new IdentityHashMap<>();
    private float lastScaledStyleZoom = Float.NaN;

    protected abstract int canvasPadding();

    protected abstract int contentWidth();

    protected abstract int contentHeight();

    protected abstract int contentOriginX();

    protected abstract int contentOriginY();

    protected abstract boolean diagramReady();

    protected abstract void renderPanel(RenderContext context);

    protected abstract void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom);

    protected void onPreRender() {}

    public float getActiveZoom() {
        return visualZoom.value();
    }

    public int getVisualOffsetX() {
        return visualContentOffsetX.rounded();
    }

    public int getVisualOffsetY() {
        return visualContentOffsetY.rounded();
    }

    public int getScaledOriginX() {
        return Math.round(contentOriginX() * getActiveZoom());
    }

    public int getScaledOriginY() {
        return Math.round(contentOriginY() * getActiveZoom());
    }

    public LytRect getInnerViewport() {
        return new LytRect(
            bounds.x() + canvasPadding(),
            bounds.y() + canvasPadding(),
            Math.max(1, bounds.width() - canvasPadding() * 2),
            Math.max(1, bounds.height() - canvasPadding() * 2));
    }

    public void updateVisualState() {
        float boundsW = bounds.width();
        float boundsH = bounds.height();
        visualContentOffsetX.updateTowards(contentOffsetX, 26f, 0.05f, 0.01f, Math.max(128f, boundsW * 2f));
        visualContentOffsetY.updateTowards(contentOffsetY, 26f, 0.05f, 0.01f, Math.max(128f, boundsH * 2f));
        visualZoom.updateTowards(zoom, 24f, 0.05f, 0.0001f, 4f);
    }

    public void snapTo(int offsetX, int offsetY, float zoomValue) {
        contentOffsetX = offsetX;
        contentOffsetY = offsetY;
        zoom = zoomValue;
        visualContentOffsetX.snapTo(contentOffsetX);
        visualContentOffsetY.snapTo(contentOffsetY);
        visualZoom.snapTo(zoom);
    }

    public void centerDiagram(int diagramWidth, int diagramHeight) {
        LytRect vp = getInnerViewport();
        snapTo(
            (vp.width() - Math.round(diagramWidth * zoom)) / 2,
            (vp.height() - Math.round(diagramHeight * zoom)) / 2,
            zoom);
    }

    public void centerDiagram(int viewportWidth, int viewportHeight, int diagramWidth, int diagramHeight) {
        int innerWidth = Math.max(1, viewportWidth);
        int innerHeight = Math.max(1, viewportHeight);
        snapTo(
            (innerWidth - Math.round(diagramWidth * zoom)) / 2,
            (innerHeight - Math.round(diagramHeight * zoom)) / 2,
            zoom);
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!diagramReady()) return false;
        if (button != 0) return false;
        LytRect vp = getInnerViewport();
        if (!vp.contains(documentX, documentY)) return false;
        dragging = true;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (!dragging) return;
        contentOffsetX += documentX - dragLastDocumentX;
        contentOffsetY += documentY - dragLastDocumentY;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        clampOffsets();
    }

    @Override
    public void endDrag() {
        dragging = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (!diagramReady()) return false;
        if (wheelDelta == 0) return false;
        LytRect vp = getInnerViewport();
        if (!vp.contains(documentX, documentY)) return false;

        int previousOffsetX = contentOffsetX;
        int previousOffsetY = contentOffsetY;
        float previousZoom = zoom;

        zoom = wheelDelta > 0 ? Math.min(MAX_ZOOM, zoom * ZOOM_STEP) : Math.max(MIN_ZOOM, zoom / ZOOM_STEP);

        if (Math.abs(previousZoom - zoom) < 0.0001f) return false;

        float anchorX = contentOriginX() + (documentX - vp.x() - previousOffsetX) / Math.max(previousZoom, 0.0001f);
        float anchorY = contentOriginY() + (documentY - vp.y() - previousOffsetY) / Math.max(previousZoom, 0.0001f);
        contentOffsetX = Math.round((documentX - vp.x()) - (anchorX - contentOriginX()) * zoom);
        contentOffsetY = Math.round((documentY - vp.y()) - (anchorY - contentOriginY()) * zoom);
        clampOffsets();
        return true;
    }

    public void setContentOffset(int x, int y) {
        contentOffsetX = x;
        contentOffsetY = y;
    }

    public int getRawOffsetX() {
        return contentOffsetX;
    }

    public int getRawOffsetY() {
        return contentOffsetY;
    }

    public float getRawZoom() {
        return zoom;
    }

    public void clampOffsets() {
        int innerWidth = Math.max(1, bounds.width() - canvasPadding() * 2);
        int innerHeight = Math.max(1, bounds.height() - canvasPadding() * 2);
        contentOffsetX = clampAxis(contentOffsetX, innerWidth, Math.round(contentWidth() * zoom));
        contentOffsetY = clampAxis(contentOffsetY, innerHeight, Math.round(contentHeight() * zoom));
    }

    @Override
    public void render(RenderContext context) {
        if (!diagramReady()) return;
        onPreRender();
        updateVisualState();

        float activeZoom = getActiveZoom();
        if (Float.compare(lastScaledStyleZoom, activeZoom) != 0) {
            scaledStyleCache.clear();
            lastScaledStyleZoom = activeZoom;
        }

        renderPanel(context);

        LytRect inner = getInnerViewport();
        int baseX = inner.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = inner.y() + getVisualOffsetY() - getScaledOriginY();

        context.pushLocalScissor(inner);
        try {
            renderDiagram(context, baseX, baseY, activeZoom);
        } finally {
            context.popScissor();
        }
    }

    protected ResolvedTextStyle getOrScaleStyle(ResolvedTextStyle base, float zoom) {
        return MermaidNodeRenderer.getOrScaleStyle(scaledStyleCache, base, zoom);
    }

    public static int clampAxis(int offset, int viewportSize, int contentSize) {
        if (contentSize <= viewportSize) {
            return (viewportSize - contentSize) / 2;
        }
        return Math.clamp(offset, viewportSize - contentSize, 0);
    }

    public static int scaled(int base, int value, float activeZoom) {
        return base + Math.round(value * activeZoom);
    }
}
