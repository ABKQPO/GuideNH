package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;

public class InWorldAnnotationRenderer {

    private InWorldAnnotationRenderer() {}

    public static void render(Iterable<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        boolean hasAlwaysOnTop = false;
        for (var a : annotations) {
            if (a.isAlwaysOnTop()) {
                hasAlwaysOnTop = true;
                break;
            }
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_GREATER);
            GL11.glDepthMask(false);
            drawAll(annotations, lightDarkMode, /* occluded */ true, /* pass2 */ false);

            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            drawAll(annotations, lightDarkMode, /* occluded */ false, /* pass2 */ false);

            // —— Pass 2b: alwaysOnTop ——
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
            } else if (a instanceof InWorldBlockFaceOverlayAnnotation overlay) {
                if (!occluded) {
                    int color = resolve(overlay.color(), mode, a.isHovered(), false);
                    drawBlockFaceOverlay(overlay, color);
                }
            } else if (a instanceof InWorldLineAnnotation line) {
                int color = resolve(line.color(), mode, a.isHovered(), occluded);
                drawLine(line.from(), line.to(), color, line.thickness());
            }
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
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + percent * 255 / 100);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + percent * 255 / 100);
        int b = Math.min(255, (argb & 0xFF) + percent * 255 / 100);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darker(int argb, int percent) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - percent * 255 / 100);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - percent * 255 / 100);
        int b = Math.max(0, (argb & 0xFF) - percent * 255 / 100);
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

    public static void drawLine(Vector3f from, Vector3f to, int argb, float thickness) {
        // Extrude the segment into a 4-sided square prism with two end caps. Both ends are pushed
        // outward by half-thickness along the segment direction so when several lines meet they
        // overlap into a clean corner.
        float t = Math.max(thickness / 32f, 1f / 256f) * 0.5f;
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

    public static void drawBlockFaceOverlay(InWorldBlockFaceOverlayAnnotation overlay, int argb) {
        float x0 = overlay.getBlockX();
        float y0 = overlay.getBlockY();
        float z0 = overlay.getBlockZ();
        float x1 = x0 + 1f;
        float y1 = y0 + 1f;
        float z1 = z0 + 1f;
        float eps = 0.002f;

        GL11.glBegin(GL11.GL_QUADS);
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX(), overlay.getBlockY() - 1, overlay.getBlockZ())) {
            quad(argb, 0f, -1f, 0f, x0, y0 - eps, z0, x1, y0 - eps, z0, x1, y0 - eps, z1, x0, y0 - eps, z1);
        }
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX(), overlay.getBlockY() + 1, overlay.getBlockZ())) {
            quad(argb, 0f, 1f, 0f, x0, y1 + eps, z0, x0, y1 + eps, z1, x1, y1 + eps, z1, x1, y1 + eps, z0);
        }
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX(), overlay.getBlockY(), overlay.getBlockZ() - 1)) {
            quad(argb, 0f, 0f, -1f, x0, y0, z0 - eps, x0, y1, z0 - eps, x1, y1, z0 - eps, x1, y0, z0 - eps);
        }
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX(), overlay.getBlockY(), overlay.getBlockZ() + 1)) {
            quad(argb, 0f, 0f, 1f, x0, y0, z1 + eps, x1, y0, z1 + eps, x1, y1, z1 + eps, x0, y1, z1 + eps);
        }
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX() - 1, overlay.getBlockY(), overlay.getBlockZ())) {
            quad(argb, -1f, 0f, 0f, x0 - eps, y0, z0, x0 - eps, y0, z1, x0 - eps, y1, z1, x0 - eps, y1, z0);
        }
        if (!overlay.hasGroupedNeighbor(overlay.getBlockX() + 1, overlay.getBlockY(), overlay.getBlockZ())) {
            quad(argb, 1f, 0f, 0f, x1 + eps, y0, z0, x1 + eps, y1, z0, x1 + eps, y1, z1, x1 + eps, y0, z1);
        }
        GL11.glEnd();
    }

    public static void quad(int argb, float nx, float ny, float nz, float x1, float y1, float z1, float x2, float y2,
        float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        applyColor(shadeFaceColor(argb, nx, ny, nz));
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x3, y3, z3);
        GL11.glVertex3f(x4, y4, z4);
    }
}
