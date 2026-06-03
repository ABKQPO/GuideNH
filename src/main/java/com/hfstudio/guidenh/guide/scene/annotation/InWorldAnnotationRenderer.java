package com.hfstudio.guidenh.guide.scene.annotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;

public class InWorldAnnotationRenderer {

    private InWorldAnnotationRenderer() {}

    public static void render(Iterable<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        // Single up-front scan: bail out early when there is nothing to draw (the common case for
        // scenes without highlights), and at the same time figure out whether any non-occluded
        // pass / always-on-top pass actually has work to do. Avoids paying for a full GL state
        // swap + 2-3 empty drawAll iterations on every frame for empty scenes.
        boolean hasAny = false;
        boolean hasNormal = false;
        boolean hasAlwaysOnTop = false;
        for (var a : annotations) {
            hasAny = true;
            if (a.isAlwaysOnTop()) {
                hasAlwaysOnTop = true;
            } else {
                hasNormal = true;
            }
            if (hasNormal && hasAlwaysOnTop) break;
        }
        if (!hasAny) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            if (hasNormal) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_GREATER);
                GL11.glDepthMask(false);
                drawAll(annotations, lightDarkMode, /* occluded */ true, /* pass2 */ false);

                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(true);
                drawAll(annotations, lightDarkMode, /* occluded */ false, /* pass2 */ false);
            }

            // Pass 2b: alwaysOnTop.
            if (hasAlwaysOnTop) {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                drawAll(annotations, lightDarkMode, /* occluded */ false, /* pass2 */ true);
            }
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glLineWidth(1f);
            GL11.glPopAttrib();
        }
    }

    public static void drawAll(Iterable<InWorldAnnotation> annotations, LightDarkMode mode, boolean occluded,
        boolean pass2) {
        for (var a : annotations) {
            if (a.isAlwaysOnTop() != pass2) continue;
            if (occluded && a.isAlwaysOnTop()) continue;
            if (a instanceof InWorldBoxAnnotation box) {
                int color = resolve(box.color(), mode, a.isHovered(), occluded);
                drawBoxEdges(box.min(), box.max(), color, box.thickness());
            } else if (a instanceof InWorldBoxFaceOverlayAnnotation overlay) {
                if (!occluded) {
                    int color = resolve(overlay.color(), mode, a.isHovered(), false);
                    drawBoxFaceOverlay(overlay, color);
                }
            } else if (a instanceof InWorldLineAnnotation line) {
                int color = resolve(line.color(), mode, a.isHovered(), occluded);
                drawLineAnnotation(line, mode, color, occluded);
            } else if (a instanceof SceneFloorGridAnnotation grid) {
                if (!occluded) {
                    drawFloorGrid(grid);
                }
            }
        }
    }

    private static void drawLineAnnotation(InWorldLineAnnotation line, LightDarkMode mode, int color,
        boolean occluded) {
        var points = line.points();
        for (int i = 0; i + 1 < points.size(); i++) {
            Vector3f from = points.get(i);
            Vector3f to = points.get(i + 1);
            if (line.arrow() == InWorldLineAnnotation.Arrow.START && i == 0) {
                from = clipLineStartForArrow(from, to, line.thickness());
            }
            if (line.arrow() == InWorldLineAnnotation.Arrow.END && i + 2 == points.size()) {
                to = clipLineEndForArrow(from, to, line.thickness());
            }
            drawLine(from, to, color, line.thickness());
        }
        if (line.arrow() == InWorldLineAnnotation.Arrow.START) {
            drawArrowHead(points.get(0), points.get(1), color, line.thickness());
        } else if (line.arrow() == InWorldLineAnnotation.Arrow.END) {
            drawArrowHead(points.getLast(), points.get(points.size() - 2), color, line.thickness());
        }
        drawLinePoints(line, mode, occluded);
    }

    private static void drawLinePoints(InWorldLineAnnotation line, LightDarkMode mode, boolean occluded) {
        var points = line.points();
        for (int i = 0; i < points.size(); i++) {
            InWorldLineAnnotation.PointStyle style = line.pointStyleFor(i);
            boolean show = style != null && style.show() != null ? style.show() : line.showPoints();
            if (!show) {
                continue;
            }
            ColorValue colorValue = style != null && style.color() != null ? style.color() : line.pointColor();
            float size = style != null && style.size() != null ? style.size() : line.pointSize();
            int color = resolve(colorValue, mode, line.isHovered(), occluded);
            drawPointCube(points.get(i), color, size);
        }
    }

    public static int resolve(ColorValue cv, LightDarkMode mode, boolean hovered, boolean occluded) {
        int argb = cv.resolve(mode);
        if (hovered) argb = lighter(argb, 50);
        if (occluded) {
            argb = darker(argb, 50);
            int a = (argb >>> 24) & 0xFF;
            argb = ((a / 2) << 24) | (argb & 0x00FFFFFF);
        }
        return argb;
    }

    public static int lighter(int argb, int percent) {
        int adj = percent * 255 / 100;
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + adj);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + adj);
        int b = Math.min(255, (argb & 0xFF) + adj);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darker(int argb, int percent) {
        int adj = percent * 255 / 100;
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - adj);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - adj);
        int b = Math.max(0, (argb & 0xFF) - adj);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void applyColor(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
    }

    static int shadeFaceColor(int argb, float nx, float ny, float nz) {
        return multiplyRgb(argb, faceShade(nx, ny, nz));
    }

    public static float faceShade(float nx, float ny, float nz) {
        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);
        if (ay >= ax && ay >= az) {
            return ny >= 0f ? 1f : 0.5f;
        }
        return ax >= az ? 0.6f : 0.8f;
    }

    public static int multiplyRgb(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, Math.round(((argb >>> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >>> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawBoxEdges(Vector3f min, Vector3f max, int argb, float thickness) {
        // Render each edge as an extruded cuboid in world space. The cuboid is half-thickness on
        // each side perpendicular to the edge axis, and extends an extra half-thickness past both
        // corners along the edge axis. The three cuboids meeting at every box corner therefore
        // overlap into a solid square corner instead of producing the concave notch that
        // GL_LINES with cap stroking leaves at thick widths.
        float t = Math.max(thickness / 32f, 1f / 256f) * 0.5f;
        GL11.glBegin(GL11.GL_QUADS);
        // 4 edges along X
        fillCuboid(min.x - t, min.y - t, min.z - t, max.x + t, min.y + t, min.z + t, argb);
        fillCuboid(min.x - t, min.y - t, max.z - t, max.x + t, min.y + t, max.z + t, argb);
        fillCuboid(min.x - t, max.y - t, min.z - t, max.x + t, max.y + t, min.z + t, argb);
        fillCuboid(min.x - t, max.y - t, max.z - t, max.x + t, max.y + t, max.z + t, argb);
        // 4 edges along Y
        fillCuboid(min.x - t, min.y - t, min.z - t, min.x + t, max.y + t, min.z + t, argb);
        fillCuboid(max.x - t, min.y - t, min.z - t, max.x + t, max.y + t, min.z + t, argb);
        fillCuboid(min.x - t, min.y - t, max.z - t, min.x + t, max.y + t, max.z + t, argb);
        fillCuboid(max.x - t, min.y - t, max.z - t, max.x + t, max.y + t, max.z + t, argb);
        // 4 edges along Z
        fillCuboid(min.x - t, min.y - t, min.z - t, min.x + t, min.y + t, max.z + t, argb);
        fillCuboid(max.x - t, min.y - t, min.z - t, max.x + t, min.y + t, max.z + t, argb);
        fillCuboid(min.x - t, max.y - t, min.z - t, min.x + t, max.y + t, max.z + t, argb);
        fillCuboid(max.x - t, max.y - t, min.z - t, max.x + t, max.y + t, max.z + t, argb);
        GL11.glEnd();
    }

    public static void drawPointCube(Vector3f center, int argb, float size) {
        float half = Math.max(size / 32f, 1f / 256f) * 0.5f;
        GL11.glBegin(GL11.GL_QUADS);
        fillCuboid(
            center.x - half,
            center.y - half,
            center.z - half,
            center.x + half,
            center.y + half,
            center.z + half,
            argb);
        GL11.glEnd();
    }

    private static Vector3f clipLineStartForArrow(Vector3f tip, Vector3f next, float thickness) {
        float dx = next.x - tip.x;
        float dy = next.y - tip.y;
        float dz = next.z - tip.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) {
            return tip;
        }
        float clipDistance = Math.min(computeArrowBaseOffset(thickness) + computeLineHalfThickness(thickness), len);
        float invLen = 1f / len;
        return new Vector3f(
            tip.x + dx * invLen * clipDistance,
            tip.y + dy * invLen * clipDistance,
            tip.z + dz * invLen * clipDistance);
    }

    private static Vector3f clipLineEndForArrow(Vector3f previous, Vector3f tip, float thickness) {
        float dx = tip.x - previous.x;
        float dy = tip.y - previous.y;
        float dz = tip.z - previous.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) {
            return tip;
        }
        float clipDistance = Math.min(computeArrowBaseOffset(thickness) + computeLineHalfThickness(thickness), len);
        float invLen = 1f / len;
        return new Vector3f(
            tip.x - dx * invLen * clipDistance,
            tip.y - dy * invLen * clipDistance,
            tip.z - dz * invLen * clipDistance);
    }

    private static float computeLineHalfThickness(float thickness) {
        return Math.max(thickness / 32f, 1f / 256f) * 0.5f;
    }

    private static float computeArrowBaseRadius(float thickness) {
        return Math.max(computeLineHalfThickness(thickness) * 1.35f, 0.028f);
    }

    private static float computeArrowBaseOffset(float thickness) {
        return Math.max(computeArrowBaseRadius(thickness) * 3.5f, 0.14f);
    }

    public static void drawArrowHead(Vector3f tip, Vector3f lineInterior, int argb, float thickness) {
        float dx = tip.x - lineInterior.x;
        float dy = tip.y - lineInterior.y;
        float dz = tip.z - lineInterior.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        float ix = dx / len, iy = dy / len, iz = dz / len;
        float arrowLength = computeArrowBaseOffset(thickness);
        float arrowRadius = computeArrowBaseRadius(thickness);

        float ux, uy, uz;
        if (Math.abs(iy) < 0.9f) {
            ux = 0f;
            uy = 1f;
        } else {
            ux = 1f;
            uy = 0f;
        }
        uz = 0f;

        float n1x = iy * uz - iz * uy;
        float n1y = iz * ux - ix * uz;
        float n1z = ix * uy - iy * ux;
        float n1l = (float) Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
        n1x /= n1l;
        n1y /= n1l;
        n1z /= n1l;
        float n2x = iy * n1z - iz * n1y;
        float n2y = iz * n1x - ix * n1z;
        float n2z = ix * n1y - iy * n1x;

        float bx = tip.x - ix * arrowLength;
        float by = tip.y - iy * arrowLength;
        float bz = tip.z - iz * arrowLength;
        float p1x = bx + n1x * arrowRadius;
        float p1y = by + n1y * arrowRadius;
        float p1z = bz + n1z * arrowRadius;
        float p2x = bx + n2x * arrowRadius;
        float p2y = by + n2y * arrowRadius;
        float p2z = bz + n2z * arrowRadius;
        float p3x = bx - n1x * arrowRadius;
        float p3y = by - n1y * arrowRadius;
        float p3z = bz - n1z * arrowRadius;
        float p4x = bx - n2x * arrowRadius;
        float p4y = by - n2y * arrowRadius;
        float p4z = bz - n2z * arrowRadius;

        GL11.glBegin(GL11.GL_TRIANGLES);
        triangle(
            argb,
            n1x + n2x + ix,
            n1y + n2y + iy,
            n1z + n2z + iz,
            tip.x,
            tip.y,
            tip.z,
            p1x,
            p1y,
            p1z,
            p2x,
            p2y,
            p2z);
        triangle(
            argb,
            -n1x + n2x + ix,
            -n1y + n2y + iy,
            -n1z + n2z + iz,
            tip.x,
            tip.y,
            tip.z,
            p2x,
            p2y,
            p2z,
            p3x,
            p3y,
            p3z);
        triangle(
            argb,
            -n1x - n2x + ix,
            -n1y - n2y + iy,
            -n1z - n2z + iz,
            tip.x,
            tip.y,
            tip.z,
            p3x,
            p3y,
            p3z,
            p4x,
            p4y,
            p4z);
        triangle(
            argb,
            n1x - n2x + ix,
            n1y - n2y + iy,
            n1z - n2z + iz,
            tip.x,
            tip.y,
            tip.z,
            p4x,
            p4y,
            p4z,
            p1x,
            p1y,
            p1z);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUADS);
        quad(argb, -ix, -iy, -iz, p1x, p1y, p1z, p4x, p4y, p4z, p3x, p3y, p3z, p2x, p2y, p2z);
        GL11.glEnd();
    }

    public static void drawLine(Vector3f from, Vector3f to, int argb, float thickness) {
        // Extrude the segment into a 4-sided square prism with two end caps. Both ends are pushed
        // outward by half-thickness along the segment direction so when several lines meet they
        // overlap into a clean corner.
        float t = computeLineHalfThickness(thickness);
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float dz = to.z - from.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        float ix = dx / len, iy = dy / len, iz = dz / len;
        // Pick a stable up vector that is not parallel to the line direction.
        float ux, uy, uz;
        if (Math.abs(iy) < 0.9f) {
            ux = 0f;
            uy = 1f;
        } else {
            ux = 1f;
            uy = 0f;
        }
        uz = 0f;
        // n1 = normalize(cross(i, up))
        float n1x = iy * uz - iz * uy;
        float n1y = iz * ux - ix * uz;
        float n1z = ix * uy - iy * ux;
        float n1l = (float) Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
        n1x /= n1l;
        n1y /= n1l;
        n1z /= n1l;
        // n2 = cross(i, n1)
        float n2x = iy * n1z - iz * n1y;
        float n2y = iz * n1x - ix * n1z;
        float n2z = ix * n1y - iy * n1x;

        float ax = from.x - ix * t;
        float ay = from.y - iy * t;
        float az = from.z - iz * t;
        float bx = to.x + ix * t;
        float by = to.y + iy * t;
        float bz = to.z + iz * t;

        float p1x = n1x * t, p1y = n1y * t, p1z = n1z * t;
        float p2x = n2x * t, p2y = n2y * t, p2z = n2z * t;

        GL11.glBegin(GL11.GL_QUADS);
        // Side faces.
        sideQuad(argb, ax, ay, az, bx, by, bz, -p1x - p2x, -p1y - p2y, -p1z - p2z, p1x - p2x, p1y - p2y, p1z - p2z);
        sideQuad(argb, ax, ay, az, bx, by, bz, p1x - p2x, p1y - p2y, p1z - p2z, p1x + p2x, p1y + p2y, p1z + p2z);
        sideQuad(argb, ax, ay, az, bx, by, bz, p1x + p2x, p1y + p2y, p1z + p2z, -p1x + p2x, -p1y + p2y, -p1z + p2z);
        sideQuad(argb, ax, ay, az, bx, by, bz, -p1x + p2x, -p1y + p2y, -p1z + p2z, -p1x - p2x, -p1y - p2y, -p1z - p2z);
        // End caps.
        quad(
            argb,
            -ix,
            -iy,
            -iz,
            ax - p1x - p2x,
            ay - p1y - p2y,
            az - p1z - p2z,
            ax + p1x - p2x,
            ay + p1y - p2y,
            az + p1z - p2z,
            ax + p1x + p2x,
            ay + p1y + p2y,
            az + p1z + p2z,
            ax - p1x + p2x,
            ay - p1y + p2y,
            az - p1z + p2z);
        quad(
            argb,
            ix,
            iy,
            iz,
            bx - p1x - p2x,
            by - p1y - p2y,
            bz - p1z - p2z,
            bx - p1x + p2x,
            by - p1y + p2y,
            bz - p1z + p2z,
            bx + p1x + p2x,
            by + p1y + p2y,
            bz + p1z + p2z,
            bx + p1x - p2x,
            by + p1y - p2y,
            bz + p1z - p2z);
        GL11.glEnd();
    }

    public static void sideQuad(int argb, float ax, float ay, float az, float bx, float by, float bz, float o1x,
        float o1y, float o1z, float o2x, float o2y, float o2z) {
        quad(
            argb,
            o1x + o2x,
            o1y + o2y,
            o1z + o2z,
            ax + o1x,
            ay + o1y,
            az + o1z,
            bx + o1x,
            by + o1y,
            bz + o1z,
            bx + o2x,
            by + o2y,
            bz + o2z,
            ax + o2x,
            ay + o2y,
            az + o2z);
    }

    public static void fillCuboid(float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        // -X
        quad(argb, -1f, 0f, 0f, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        // +X
        quad(argb, 1f, 0f, 0f, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        // -Y
        quad(argb, 0f, -1f, 0f, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        // +Y
        quad(argb, 0f, 1f, 0f, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        // -Z
        quad(argb, 0f, 0f, -1f, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        // +Z
        quad(argb, 0f, 0f, 1f, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
    }

    public static void drawBoxFaceOverlay(InWorldBoxFaceOverlayAnnotation overlay, int argb) {
        Vector3f min = overlay.min();
        Vector3f max = overlay.max();
        float x0 = min.x;
        float y0 = min.y;
        float z0 = min.z;
        float x1 = max.x;
        float y1 = max.y;
        float z1 = max.z;
        float eps = 0.002f;

        GL11.glBegin(GL11.GL_QUADS);
        if (overlay.shouldDrawNegativeYFace()) {
            quad(argb, 0f, -1f, 0f, x0, y0 - eps, z0, x1, y0 - eps, z0, x1, y0 - eps, z1, x0, y0 - eps, z1);
        }
        if (overlay.shouldDrawPositiveYFace()) {
            quad(argb, 0f, 1f, 0f, x0, y1 + eps, z0, x0, y1 + eps, z1, x1, y1 + eps, z1, x1, y1 + eps, z0);
        }
        if (overlay.shouldDrawNegativeZFace()) {
            quad(argb, 0f, 0f, -1f, x0, y0, z0 - eps, x0, y1, z0 - eps, x1, y1, z0 - eps, x1, y0, z0 - eps);
        }
        if (overlay.shouldDrawPositiveZFace()) {
            quad(argb, 0f, 0f, 1f, x0, y0, z1 + eps, x1, y0, z1 + eps, x1, y1, z1 + eps, x0, y1, z1 + eps);
        }
        if (overlay.shouldDrawNegativeXFace()) {
            quad(argb, -1f, 0f, 0f, x0 - eps, y0, z0, x0 - eps, y0, z1, x0 - eps, y1, z1, x0 - eps, y1, z0);
        }
        if (overlay.shouldDrawPositiveXFace()) {
            quad(argb, 1f, 0f, 0f, x1 + eps, y0, z0, x1 + eps, y1, z0, x1 + eps, y1, z1, x1 + eps, y0, z1);
        }
        GL11.glEnd();
    }

    public static void drawBlockFaceOverlay(InWorldBlockFaceOverlayAnnotation overlay, int argb) {
        drawBoxFaceOverlay(overlay, argb);
    }

    public static void quad(int argb, float nx, float ny, float nz, float x1, float y1, float z1, float x2, float y2,
        float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        applyColor(shadeFaceColor(argb, nx, ny, nz));
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x3, y3, z3);
        GL11.glVertex3f(x4, y4, z4);
    }

    public static void triangle(int argb, float nx, float ny, float nz, float x1, float y1, float z1, float x2,
        float y2, float z2, float x3, float y3, float z3) {
        applyColor(shadeFaceColor(argb, nx, ny, nz));
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x3, y3, z3);
    }

    /**
     * Draws a flat floor grid annotation as thin quads lying on a horizontal plane.
     *
     * <p>
     * Lines are drawn at every integer X and Z coordinate within the annotation's bounds,
     * covering the structure's XZ footprint plus one block outward in each direction.
     */
    public static void drawFloorGrid(SceneFloorGridAnnotation grid) {
        float half = 0.02f;
        float planeY = grid.getY() + 0.002f;
        int x0 = grid.getMinX();
        int z0 = grid.getMinZ();
        int x1 = grid.getMaxX();
        int z1 = grid.getMaxZ();
        int color = 0x55FFFFFF;
        applyColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        for (int ix = x0; ix <= x1; ix++) {
            GL11.glVertex3f(ix - half, planeY, z0);
            GL11.glVertex3f(ix + half, planeY, z0);
            GL11.glVertex3f(ix + half, planeY, z1);
            GL11.glVertex3f(ix - half, planeY, z1);
        }
        for (int iz = z0; iz <= z1; iz++) {
            GL11.glVertex3f(x0, planeY, iz - half);
            GL11.glVertex3f(x1, planeY, iz - half);
            GL11.glVertex3f(x1, planeY, iz + half);
            GL11.glVertex3f(x0, planeY, iz + half);
        }
        GL11.glEnd();

        if (grid.isShowDebugLabels()) {
            drawFloorGridLabels(grid);
        }
    }

    /**
     * Renders X/Z coordinate numbers and N/S/E/W direction initials in 3D world space.
     *
     * <p>
     * Labels are rendered as vertical signs standing just outside each grid edge, facing the viewer
     * from outside the grid, matching the Ponder editor overlay style. Text scale is computed so
     * the widest label on each axis fits within one block width, ensuring negative numbers and
     * multi-digit values never overflow their cell.
     */
    private static void drawFloorGridLabels(SceneFloorGridAnnotation grid) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

        int x0 = grid.getMinX();
        int z0 = grid.getMinZ();
        int x1 = grid.getMaxX();
        int z1 = grid.getMaxZ();
        float baseY = grid.getY();

        // FontRenderer height in pixels. One label should span at most 0.8 blocks tall
        // (leaving breathing room) and the widest label at most 1.0 block wide.
        // Compute per-axis scales so the widest label fits within 1 block.
        int maxXLabelPx = 1;
        for (int ix = x0; ix <= x1; ix++) {
            maxXLabelPx = Math.max(maxXLabelPx, fr.getStringWidth(Integer.toString(ix)));
        }
        int maxZLabelPx = 1;
        for (int iz = z0; iz <= z1; iz++) {
            maxZLabelPx = Math.max(maxZLabelPx, fr.getStringWidth(Integer.toString(iz)));
        }

        // Scale so that widest label <= 0.85 blocks; height capped to 0.85 blocks too.
        float limitBlocks = 0.85f;
        float scaleX = Math.min(limitBlocks / maxXLabelPx, limitBlocks / fr.FONT_HEIGHT);
        float scaleZ = Math.min(limitBlocks / maxZLabelPx, limitBlocks / fr.FONT_HEIGHT);
        // Direction initials are always single letters; 0.75-block fixed size looks best.
        float scaleDir = Math.min(0.75f / fr.getStringWidth("W"), 0.75f / fr.FONT_HEIGHT);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        // X-axis numbers: vertical signs standing at the north (-Z) edge, one per column.
        // Sign faces outward toward -Z, so rotateY = 0.
        for (int ix = x0; ix <= x1; ix++) {
            String label = Integer.toString(ix);
            int labelW = fr.getStringWidth(label);
            drawVerticalLabel(fr, label, ix + 0.5f, baseY, z0 - 0.1f, scaleX, labelW, 0f);
        }

        // Z-axis numbers: vertical signs standing at the west (-X) edge, one per row.
        // Sign faces outward toward -X, so rotateY = 90.
        for (int iz = z0; iz <= z1; iz++) {
            String label = Integer.toString(iz);
            int labelW = fr.getStringWidth(label);
            drawVerticalLabel(fr, label, x0 - 0.1f, baseY, iz + 0.5f, scaleZ, labelW, 90f);
        }

        // Cardinal direction labels: flat on the ground at midpoints of each grid edge,
        // with Ponder-style fade trail.
        final float dirOffset = 3.0f;
        float midX = (x0 + x1) * 0.5f;
        float midZ = (z0 + z1) * 0.5f;
        drawFloorGridDirLabel(fr, "N", midX, baseY, z0 - dirOffset, scaleDir, 180f);
        drawFloorGridDirLabel(fr, "S", midX, baseY, z1 + dirOffset, scaleDir, 0f);
        drawFloorGridDirLabel(fr, "E", x1 + dirOffset, baseY, midZ, scaleDir, 90f);
        drawFloorGridDirLabel(fr, "W", x0 - dirOffset, baseY, midZ, scaleDir, -90f);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Draws a vertically-standing label sign at the given world position.
     *
     * <p>
     * The sign is upright (not flat on the ground). {@code rotateY} spins it to face outward from
     * its grid edge: 0 degrees faces -Z, 90 degrees faces -X, 180 degrees faces +Z, -90 degrees faces +X.
     */
    private static void drawVerticalLabel(FontRenderer fr, String text, float wx, float wy, float wz, float scale,
        int textWidthPx, float rotateYDeg) {
        GL11.glPushMatrix();
        GL11.glTranslatef(wx, wy, wz);
        if (rotateYDeg != 0f) {
            GL11.glRotatef(rotateYDeg, 0f, 1f, 0f);
        }
        // Scale from pixel space to block space; negate X so text is not mirrored.
        GL11.glScalef(-scale, -scale, scale);
        // Center the label horizontally within the cell.
        GL11.glTranslatef(-textWidthPx / 2f, 0f, 0f);
        fr.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
        GL11.glPopMatrix();
    }

    /**
     * Draws a cardinal direction label lying flat on the ground plane outside a grid edge,
     * with a Ponder-style fading bar-and-dot trail. The label is rotated around Y to face
     * outward from the correct edge, then tipped 90 degrees around X to lie on the XZ plane.
     */
    private static void drawFloorGridDirLabel(FontRenderer fr, String text, float wx, float wy, float wz, float scale,
        float rotateYDeg) {
        int labelW = fr.getStringWidth(text);
        GL11.glPushMatrix();
        GL11.glTranslatef(wx, wy, wz);
        if (rotateYDeg != 0f) {
            GL11.glRotatef(rotateYDeg, 0f, 1f, 0f);
        }
        // Tip text flat onto the XZ ground plane.
        GL11.glRotatef(90f, 1f, 0f, 0f);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glTranslatef(-labelW / 2f, 0f, 0f);
        fr.drawStringWithShadow(text, 0, 0, 0x66FFFFFF);
        // Ponder-style fade: bar then dot below the initial.
        int barX = labelW / 2 - fr.getStringWidth("|") / 2;
        int dotX = labelW / 2 - fr.getStringWidth(".") / 2;
        fr.drawStringWithShadow("|", barX, fr.FONT_HEIGHT - 1, 0x44FFFFFF);
        fr.drawStringWithShadow(".", dotX, fr.FONT_HEIGHT * 2 - 2, 0x22FFFFFF);
        GL11.glPopMatrix();
    }
}
