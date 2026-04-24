package com.hfstudio.guidenh.guide.scene.annotation;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

public abstract class OverlayAnnotation extends SceneAnnotation {

    public abstract LytRect getBoundingRect(CameraSettings camera, LytRect viewport);

    public abstract void render(CameraSettings camera, RenderContext context, LytRect viewport);
}
