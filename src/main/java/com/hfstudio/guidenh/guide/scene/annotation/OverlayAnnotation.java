package com.hfstudio.guidenh.guide.scene.annotation;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

public abstract class OverlayAnnotation extends SceneAnnotation {

    private float fade = 1f;

    /** Returns the current fade multiplier in the range {@code [0, 1]}. */
    public float getFade() {
        return fade;
    }

    /**
     * Sets the fade multiplier applied to all rendered alpha values.
     * {@code 0} = fully transparent, {@code 1} = fully opaque.
     * Clamped to {@code [0, 1]}.
     */
    public void setFade(float fade) {
        this.fade = Math.max(0f, Math.min(1f, fade));
    }

    /**
     * Multiplies the alpha channel of an ARGB colour by {@code fade}.
     * Useful for subclass {@code render()} implementations to produce a consistent fade effect.
     */
    protected static int applyFade(int argb, float fade) {
        int a = (int) (((argb >>> 24) & 0xFF) * fade);
        return (Math.min(255, a) << 24) | (argb & 0x00FFFFFF);
    }

    public abstract LytRect getBoundingRect(CameraSettings camera, LytRect viewport);

    public abstract void render(CameraSettings camera, RenderContext context, LytRect viewport);
}
