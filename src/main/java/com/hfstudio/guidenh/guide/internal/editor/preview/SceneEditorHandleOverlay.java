package com.hfstudio.guidenh.guide.internal.editor.preview;

import javax.annotation.Nullable;

import net.minecraft.client.gui.Gui;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

public final class SceneEditorHandleOverlay {

    public static final String CENTER_HANDLE_ID = "center";
    public static final String X_AXIS_HANDLE_ID = "axis_x";
    public static final String Y_AXIS_HANDLE_ID = "axis_y";
    public static final String Z_AXIS_HANDLE_ID = "axis_z";
    public static final String XY_PLANE_HANDLE_ID = "plane_xy";
    public static final String YZ_PLANE_HANDLE_ID = "plane_yz";
    public static final String ZX_PLANE_HANDLE_ID = "plane_zx";
    public static final String LINE_FROM_HANDLE_ID = "line_from";
    public static final String LINE_TO_HANDLE_ID = "line_to";
    public static final String BOX_CORNER_MIN_MIN_MIN_HANDLE_ID = "box_corner_min_min_min";
    public static final String BOX_CORNER_MIN_MIN_MAX_HANDLE_ID = "box_corner_min_min_max";
    public static final String BOX_CORNER_MIN_MAX_MIN_HANDLE_ID = "box_corner_min_max_min";
    public static final String BOX_CORNER_MIN_MAX_MAX_HANDLE_ID = "box_corner_min_max_max";
    public static final String BOX_CORNER_MAX_MIN_MIN_HANDLE_ID = "box_corner_max_min_min";
    public static final String BOX_CORNER_MAX_MIN_MAX_HANDLE_ID = "box_corner_max_min_max";
    public static final String BOX_CORNER_MAX_MAX_MIN_HANDLE_ID = "box_corner_max_max_min";
    public static final String BOX_CORNER_MAX_MAX_MAX_HANDLE_ID = "box_corner_max_max_max";

    private static final int CENTER_HANDLE_RADIUS = 5;
    private static final int CENTER_HANDLE_DIAMETER = CENTER_HANDLE_RADIUS * 2 + 1;
    private static final int AXIS_HANDLE_RADIUS = 5;
    private static final int AXIS_HANDLE_DIAMETER = AXIS_HANDLE_RADIUS * 2 + 1;
    private static final int POINT_HANDLE_RADIUS = 4;
    private static final int POINT_HANDLE_DIAMETER = POINT_HANDLE_RADIUS * 2 + 1;
    private static final int PLANE_HANDLE_SIZE = 7;
    private static final float AXIS_WORLD_LENGTH = 1.5f;
    private static final float PLANE_WORLD_OFFSET = 0.8f;
    private static final float PLANE_GUIDE_FACTOR = 0.28f;
    private static final float AXIS_LINE_WIDTH = 3.25f;
    private static final float PLANE_LINE_WIDTH = 2.25f;
    private static final float ARROW_LENGTH = 8f;
    private static final float ARROW_HALF_WIDTH = 4f;
    private static final int CENTER_HANDLE_FILL = 0x8A00CAF2;
    private static final int CENTER_HANDLE_OUTLINE = 0xFFF4FBFF;
    private static final int X_AXIS_COLOR = 0xFFFF5A5A;
    private static final int Y_AXIS_COLOR = 0xFF67E26C;
    private static final int Z_AXIS_COLOR = 0xFF64A8FF;
    private static final int XY_PLANE_COLOR = 0xD8FFD45A;
    private static final int YZ_PLANE_COLOR = 0xD85AE9FF;
    private static final int ZX_PLANE_COLOR = 0xD8F16BFF;
    private static final String[] POINT_HANDLE_IDS = new String[] { CENTER_HANDLE_ID, XY_PLANE_HANDLE_ID,
        YZ_PLANE_HANDLE_ID, ZX_PLANE_HANDLE_ID, X_AXIS_HANDLE_ID, Y_AXIS_HANDLE_ID, Z_AXIS_HANDLE_ID };
    private static final String[] LINE_HANDLE_IDS = new String[] { LINE_FROM_HANDLE_ID, LINE_TO_HANDLE_ID };
    private static final String[] BOX_HANDLE_IDS = new String[] { BOX_CORNER_MIN_MIN_MIN_HANDLE_ID,
        BOX_CORNER_MIN_MIN_MAX_HANDLE_ID, BOX_CORNER_MIN_MAX_MIN_HANDLE_ID, BOX_CORNER_MIN_MAX_MAX_HANDLE_ID,
        BOX_CORNER_MAX_MIN_MIN_HANDLE_ID, BOX_CORNER_MAX_MIN_MAX_HANDLE_ID, BOX_CORNER_MAX_MAX_MIN_HANDLE_ID,
        BOX_CORNER_MAX_MAX_MAX_HANDLE_ID };

    public boolean supportsPointHandle(@Nullable SceneEditorElementModel element) {
        return getHandleIds(element).length > 0;
    }

    @Nullable
    public String pickHandle(SceneEditorElementModel element, CameraSettings camera, LytRect viewport, int mouseX,
        int mouseY) {
        if (!supportsPointHandle(element)) {
            return null;
        }
        String bestHandleId = null;
        float bestDistanceSq = Float.POSITIVE_INFINITY;
        for (String handleId : getHandleIds(element)) {
            LytRect bounds = getHandleBounds(element, camera, viewport, handleId).expand(2);
            if (!bounds.contains(mouseX, mouseY)) {
                continue;
            }
            float distanceSq = measureHitDistanceSq(element, camera, viewport, handleId, mouseX, mouseY);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestHandleId = handleId;
            }
        }
        return bestHandleId;
    }

    public LytRect getCenterHandleBounds(SceneEditorElementModel element, CameraSettings camera, LytRect viewport) {
        return getHandleBounds(element, camera, viewport, CENTER_HANDLE_ID);
    }

    public LytRect getHandleBounds(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String handleId) {
        Vector3f handleCenter = projectHandlePoint(element, camera, viewport, handleId);
        if (CENTER_HANDLE_ID.equals(handleId)) {
            return new LytRect(
                Math.round(handleCenter.x) - CENTER_HANDLE_RADIUS,
                Math.round(handleCenter.y) - CENTER_HANDLE_RADIUS,
                CENTER_HANDLE_DIAMETER,
                CENTER_HANDLE_DIAMETER);
        }
        if (isAxisHandle(handleId)) {
            return getAxisHandleBounds(element, camera, viewport, handleId);
        }
        if (isWorldPointHandle(handleId)) {
            return new LytRect(
                Math.round(handleCenter.x) - POINT_HANDLE_RADIUS,
                Math.round(handleCenter.y) - POINT_HANDLE_RADIUS,
                POINT_HANDLE_DIAMETER,
                POINT_HANDLE_DIAMETER);
        }
        return new LytRect(
            Math.round(handleCenter.x) - PLANE_HANDLE_SIZE / 2,
            Math.round(handleCenter.y) - PLANE_HANDLE_SIZE / 2,
            PLANE_HANDLE_SIZE,
            PLANE_HANDLE_SIZE);
    }

    public Vector3f projectHandlePoint(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String handleId) {
        if (XY_PLANE_HANDLE_ID.equals(handleId)) {
            return projectPlaneHandlePoint(element, camera, viewport, X_AXIS_HANDLE_ID, Y_AXIS_HANDLE_ID);
        }
        if (YZ_PLANE_HANDLE_ID.equals(handleId)) {
            return projectPlaneHandlePoint(element, camera, viewport, Y_AXIS_HANDLE_ID, Z_AXIS_HANDLE_ID);
        }
        if (ZX_PLANE_HANDLE_ID.equals(handleId)) {
            return projectPlaneHandlePoint(element, camera, viewport, Z_AXIS_HANDLE_ID, X_AXIS_HANDLE_ID);
        }
        Vector3f worldPoint = getHandleWorldPoint(element, handleId);
        return projectWorldPoint(camera, viewport, worldPoint.x, worldPoint.y, worldPoint.z);
    }

    public void render(SceneEditorElementModel element, CameraSettings camera, LytRect viewport) {
        if (!supportsPointHandle(element)) {
            return;
        }
        if (element.getType() == SceneEditorElementType.LINE) {
            renderWorldPointHandles(element, camera, viewport, LINE_HANDLE_IDS);
            return;
        }
        if (element.getType() == SceneEditorElementType.BOX) {
            renderWorldPointHandles(element, camera, viewport, BOX_HANDLE_IDS);
            return;
        }
        renderPointHandles(element, camera, viewport);
    }

    private void renderWorldPointHandles(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String[] handleIds) {
        int color = element.getType()
            .getAccentColor();
        for (String handleId : handleIds) {
            drawHandleRect(getHandleBounds(element, camera, viewport, handleId), color);
        }
    }

    private void renderPointHandles(SceneEditorElementModel element, CameraSettings camera, LytRect viewport) {
        Vector3f center = projectHandlePoint(element, camera, viewport, CENTER_HANDLE_ID);
        Vector3f xAxis = projectHandlePoint(element, camera, viewport, X_AXIS_HANDLE_ID);
        Vector3f yAxis = projectHandlePoint(element, camera, viewport, Y_AXIS_HANDLE_ID);
        Vector3f zAxis = projectHandlePoint(element, camera, viewport, Z_AXIS_HANDLE_ID);
        Vector3f xyPlane = projectHandlePoint(element, camera, viewport, XY_PLANE_HANDLE_ID);
        Vector3f yzPlane = projectHandlePoint(element, camera, viewport, YZ_PLANE_HANDLE_ID);
        Vector3f zxPlane = projectHandlePoint(element, camera, viewport, ZX_PLANE_HANDLE_ID);
        drawLine(center, xAxis, X_AXIS_COLOR, AXIS_LINE_WIDTH);
        drawLine(center, yAxis, Y_AXIS_COLOR, AXIS_LINE_WIDTH);
        drawLine(center, zAxis, Z_AXIS_COLOR, AXIS_LINE_WIDTH);
        drawAxisArrow(center, xAxis, X_AXIS_COLOR);
        drawAxisArrow(center, yAxis, Y_AXIS_COLOR);
        drawAxisArrow(center, zAxis, Z_AXIS_COLOR);
        drawPlaneGuide(center, xAxis, yAxis, XY_PLANE_COLOR);
        drawPlaneGuide(center, yAxis, zAxis, YZ_PLANE_COLOR);
        drawPlaneGuide(center, zAxis, xAxis, ZX_PLANE_COLOR);

        drawHandleRect(getHandleBounds(element, camera, viewport, XY_PLANE_HANDLE_ID), XY_PLANE_COLOR);
        drawHandleRect(getHandleBounds(element, camera, viewport, YZ_PLANE_HANDLE_ID), YZ_PLANE_COLOR);
        drawHandleRect(getHandleBounds(element, camera, viewport, ZX_PLANE_HANDLE_ID), ZX_PLANE_COLOR);

        LytRect bounds = getCenterHandleBounds(element, camera, viewport);
        int centerX = bounds.x() + bounds.width() / 2;
        int centerY = bounds.y() + bounds.height() / 2;

        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), CENTER_HANDLE_FILL);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), CENTER_HANDLE_OUTLINE);
        Gui.drawRect(centerX - 1, bounds.y() - 3, centerX + 1, bounds.bottom() + 3, CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.x() - 3, centerY - 1, bounds.right() + 3, centerY + 1, CENTER_HANDLE_OUTLINE);
    }

    private boolean isAxisHandle(String handleId) {
        return X_AXIS_HANDLE_ID.equals(handleId) || Y_AXIS_HANDLE_ID.equals(handleId)
            || Z_AXIS_HANDLE_ID.equals(handleId);
    }

    private boolean isWorldPointHandle(String handleId) {
        return LINE_FROM_HANDLE_ID.equals(handleId) || LINE_TO_HANDLE_ID.equals(handleId) || isBoxHandle(handleId);
    }

    private boolean isBoxHandle(String handleId) {
        for (String boxHandleId : BOX_HANDLE_IDS) {
            if (boxHandleId.equals(handleId)) {
                return true;
            }
        }
        return false;
    }

    private float measureHitDistanceSq(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String handleId, int mouseX, int mouseY) {
        if (isAxisHandle(handleId)) {
            Vector3f center = projectHandlePoint(element, camera, viewport, CENTER_HANDLE_ID);
            Vector3f tip = projectHandlePoint(element, camera, viewport, handleId);
            return distanceSqToSegment(mouseX, mouseY, center.x, center.y, tip.x, tip.y);
        }
        Vector3f handleCenter = projectHandlePoint(element, camera, viewport, handleId);
        float dx = mouseX - handleCenter.x;
        float dy = mouseY - handleCenter.y;
        return dx * dx + dy * dy;
    }

    private Vector3f getHandleWorldPoint(SceneEditorElementModel element, String handleId) {
        float x = element.getPrimaryX();
        float y = element.getPrimaryY();
        float z = element.getPrimaryZ();
        if (X_AXIS_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x + AXIS_WORLD_LENGTH, y, z);
        }
        if (Y_AXIS_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x, y + AXIS_WORLD_LENGTH, z);
        }
        if (Z_AXIS_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x, y, z + AXIS_WORLD_LENGTH);
        }
        if (XY_PLANE_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x + PLANE_WORLD_OFFSET, y + PLANE_WORLD_OFFSET, z);
        }
        if (YZ_PLANE_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x, y + PLANE_WORLD_OFFSET, z + PLANE_WORLD_OFFSET);
        }
        if (ZX_PLANE_HANDLE_ID.equals(handleId)) {
            return new Vector3f(x + PLANE_WORLD_OFFSET, y, z + PLANE_WORLD_OFFSET);
        }
        if (LINE_TO_HANDLE_ID.equals(handleId)) {
            return new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ());
        }
        if (isBoxHandle(handleId)) {
            return getBoxCornerPoint(element, handleId);
        }
        return new Vector3f(x, y, z);
    }

    private Vector3f getBoxCornerPoint(SceneEditorElementModel element, String handleId) {
        float minX = Math.min(element.getPrimaryX(), element.getSecondaryX());
        float minY = Math.min(element.getPrimaryY(), element.getSecondaryY());
        float minZ = Math.min(element.getPrimaryZ(), element.getSecondaryZ());
        float maxX = Math.max(element.getPrimaryX(), element.getSecondaryX());
        float maxY = Math.max(element.getPrimaryY(), element.getSecondaryY());
        float maxZ = Math.max(element.getPrimaryZ(), element.getSecondaryZ());

        if (BOX_CORNER_MIN_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, minY, minZ);
        }
        if (BOX_CORNER_MIN_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, minY, maxZ);
        }
        if (BOX_CORNER_MIN_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, maxY, minZ);
        }
        if (BOX_CORNER_MIN_MAX_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, maxY, maxZ);
        }
        if (BOX_CORNER_MAX_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, minY, minZ);
        }
        if (BOX_CORNER_MAX_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, minY, maxZ);
        }
        if (BOX_CORNER_MAX_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, maxY, minZ);
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    private Vector3f projectWorldPoint(CameraSettings camera, LytRect viewport, float x, float y, float z) {
        Vector3f projected = camera.worldToScreen(x, y, z);
        return new Vector3f(
            viewport.x() + viewport.width() / 2f + projected.x,
            viewport.y() + viewport.height() / 2f + projected.y,
            projected.z);
    }

    private LytRect getAxisHandleBounds(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String handleId) {
        Vector3f center = projectHandlePoint(element, camera, viewport, CENTER_HANDLE_ID);
        Vector3f tip = projectHandlePoint(element, camera, viewport, handleId);
        AxisArrowGeometry geometry = createAxisArrowGeometry(center, tip);
        float minX = Math.min(tip.x, Math.min(geometry.leftX, geometry.rightX));
        float minY = Math.min(tip.y, Math.min(geometry.leftY, geometry.rightY));
        float maxX = Math.max(tip.x, Math.max(geometry.leftX, geometry.rightX));
        float maxY = Math.max(tip.y, Math.max(geometry.leftY, geometry.rightY));
        return new LytRect(
            (int) Math.floor(minX) - 2,
            (int) Math.floor(minY) - 2,
            Math.max(AXIS_HANDLE_DIAMETER, (int) Math.ceil(maxX - minX) + 5),
            Math.max(AXIS_HANDLE_DIAMETER, (int) Math.ceil(maxY - minY) + 5));
    }

    private Vector3f projectPlaneHandlePoint(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        String axisHandleA, String axisHandleB) {
        Vector3f center = projectHandlePoint(element, camera, viewport, CENTER_HANDLE_ID);
        Vector3f axisA = projectHandlePoint(element, camera, viewport, axisHandleA);
        Vector3f axisB = projectHandlePoint(element, camera, viewport, axisHandleB);
        return createPlaneGuideCorner(center, axisA, axisB);
    }

    private String[] getHandleIds(@Nullable SceneEditorElementModel element) {
        if (element == null) {
            return new String[0];
        }
        if (element.getType() == SceneEditorElementType.BLOCK || element.getType() == SceneEditorElementType.DIAMOND) {
            return POINT_HANDLE_IDS;
        }
        if (element.getType() == SceneEditorElementType.LINE) {
            return LINE_HANDLE_IDS;
        }
        if (element.getType() == SceneEditorElementType.BOX) {
            return BOX_HANDLE_IDS;
        }
        return new String[0];
    }

    private void drawHandleRect(LytRect bounds, int fillColor) {
        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fillColor);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), CENTER_HANDLE_OUTLINE);
        Gui.drawRect(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), CENTER_HANDLE_OUTLINE);
    }

    private Vector3f createPlaneGuideCorner(Vector3f center, Vector3f axisA, Vector3f axisB) {
        Vector3f armA = interpolate(center, axisA, PLANE_GUIDE_FACTOR);
        Vector3f armB = interpolate(center, axisB, PLANE_GUIDE_FACTOR);
        return new Vector3f(armA.x + armB.x - center.x, armA.y + armB.y - center.y, center.z);
    }

    private void drawPlaneGuide(Vector3f center, Vector3f axisA, Vector3f axisB, int color) {
        Vector3f armA = interpolate(center, axisA, PLANE_GUIDE_FACTOR);
        Vector3f armB = interpolate(center, axisB, PLANE_GUIDE_FACTOR);
        Vector3f corner = new Vector3f(armA.x + armB.x - center.x, armA.y + armB.y - center.y, center.z);
        drawLine(armA, corner, color, PLANE_LINE_WIDTH);
        drawLine(corner, armB, color, PLANE_LINE_WIDTH);
    }

    private Vector3f interpolate(Vector3f from, Vector3f to, float factor) {
        return new Vector3f(
            from.x + (to.x - from.x) * factor,
            from.y + (to.y - from.y) * factor,
            from.z + (to.z - from.z) * factor);
    }

    private float distanceSqToSegment(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float lengthSq = dx * dx + dy * dy;
        if (lengthSq <= 1e-6f) {
            float ox = px - ax;
            float oy = py - ay;
            return ox * ox + oy * oy;
        }
        float t = ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        float closestX = ax + dx * t;
        float closestY = ay + dy * t;
        float ox = px - closestX;
        float oy = py - closestY;
        return ox * ox + oy * oy;
    }

    private void drawAxisArrow(Vector3f from, Vector3f to, int color) {
        AxisArrowGeometry geometry = createAxisArrowGeometry(from, to);
        drawFilledTriangle(to.x, to.y, geometry.leftX, geometry.leftY, geometry.rightX, geometry.rightY, color);
    }

    private AxisArrowGeometry createAxisArrowGeometry(Vector3f from, Vector3f to) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-4f) {
            return new AxisArrowGeometry(to.x, to.y, to.x, to.y, to.x, to.y);
        }
        float ux = dx / length;
        float uy = dy / length;
        float px = -uy;
        float py = ux;
        float arrowLength = Math.min(ARROW_LENGTH, Math.max(3f, length * 0.55f));
        if (arrowLength > length) {
            arrowLength = length;
        }
        float arrowHalfWidth = Math.min(ARROW_HALF_WIDTH, Math.max(2f, arrowLength * 0.5f));
        float baseX = to.x - ux * arrowLength;
        float baseY = to.y - uy * arrowLength;
        return new AxisArrowGeometry(
            baseX + px * arrowHalfWidth,
            baseY + py * arrowHalfWidth,
            baseX - px * arrowHalfWidth,
            baseY - py * arrowHalfWidth,
            baseX,
            baseY);
    }

    private void drawFilledTriangle(float ax, float ay, float bx, float by, float cx, float cy, int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r, g, b, a);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex3f(ax, ay, 0f);
            GL11.glVertex3f(bx, by, 0f);
            GL11.glVertex3f(cx, cy, 0f);
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private void drawLine(Vector3f from, Vector3f to, int color, float lineWidth) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r, g, b, a);
            GL11.glLineWidth(lineWidth);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3f(from.x, from.y, 0f);
            GL11.glVertex3f(to.x, to.y, 0f);
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private static final class AxisArrowGeometry {

        private final float leftX;
        private final float leftY;
        private final float rightX;
        private final float rightY;
        @SuppressWarnings("unused")
        private final float baseX;
        @SuppressWarnings("unused")
        private final float baseY;

        private AxisArrowGeometry(float leftX, float leftY, float rightX, float rightY, float baseX, float baseY) {
            this.leftX = leftX;
            this.leftY = leftY;
            this.rightX = rightX;
            this.rightY = rightY;
            this.baseX = baseX;
            this.baseY = baseY;
        }
    }
}
