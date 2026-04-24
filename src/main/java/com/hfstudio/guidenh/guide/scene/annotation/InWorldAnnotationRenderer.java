package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;

public final class InWorldAnnotationRenderer {

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

    private static void drawAll(Iterable<InWorldAnnotation> annotations, LightDarkMode mode, boolean occluded,
        boolean pass2) {
        for (var a : annotations) {
            if (a.isAlwaysOnTop() != pass2) continue;
            if (occluded && a.isAlwaysOnTop()) continue;
            if (a instanceof InWorldBoxAnnotation box) {
                int color = resolve(box.color(), mode, a.isHovered(), occluded);
                drawBoxEdges(box.min(), box.max(), color, Math.max(1f, box.thickness() * 64f));
            } else if (a instanceof InWorldLineAnnotation line) {
                int color = resolve(line.color(), mode, a.isHovered(), occluded);
                drawLine(line.from(), line.to(), color, Math.max(1f, line.thickness() * 64f));
            }
        }
    }

    private static int resolve(ColorValue cv, LightDarkMode mode, boolean hovered, boolean occluded) {
        int argb = cv.resolve(mode);
        if (hovered) argb = lighter(argb, 50);
        if (occluded) {
            argb = darker(argb, 50);
            int a = (argb >>> 24) & 0xFF;
            argb = ((a / 2) << 24) | (argb & 0x00FFFFFF);
        }
        return argb;
    }

    private static int lighter(int argb, int percent) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + percent * 255 / 100);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + percent * 255 / 100);
        int b = Math.min(255, (argb & 0xFF) + percent * 255 / 100);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int darker(int argb, int percent) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - percent * 255 / 100);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - percent * 255 / 100);
        int b = Math.max(0, (argb & 0xFF) - percent * 255 / 100);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void applyColor(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
    }

    private static void drawBoxEdges(Vector3f min, Vector3f max, int argb, float lineWidth) {
        applyColor(argb);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINES);
        edge(min.x, min.y, min.z, max.x, min.y, min.z);
        edge(max.x, min.y, min.z, max.x, min.y, max.z);
        edge(max.x, min.y, max.z, min.x, min.y, max.z);
        edge(min.x, min.y, max.z, min.x, min.y, min.z);
        edge(min.x, max.y, min.z, max.x, max.y, min.z);
        edge(max.x, max.y, min.z, max.x, max.y, max.z);
        edge(max.x, max.y, max.z, min.x, max.y, max.z);
        edge(min.x, max.y, max.z, min.x, max.y, min.z);
        edge(min.x, min.y, min.z, min.x, max.y, min.z);
        edge(max.x, min.y, min.z, max.x, max.y, min.z);
        edge(max.x, min.y, max.z, max.x, max.y, max.z);
        edge(min.x, min.y, max.z, min.x, max.y, max.z);
        GL11.glEnd();
    }

    private static void drawLine(Vector3f from, Vector3f to, int argb, float lineWidth) {
        applyColor(argb);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINES);
        edge(from.x, from.y, from.z, to.x, to.y, to.z);
        GL11.glEnd();
    }

    private static void edge(float ax, float ay, float az, float bx, float by, float bz) {
        GL11.glVertex3f(ax, ay, az);
        GL11.glVertex3f(bx, by, bz);
    }
}
