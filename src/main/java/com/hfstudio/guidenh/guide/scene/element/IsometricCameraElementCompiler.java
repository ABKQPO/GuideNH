package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code guideme.scene.element.IsometricCameraElementCompiler}。
 */
public class IsometricCameraElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("IsometricCamera");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        float yaw = MdxAttrs.getFloat(compiler, errorSink, el, "yaw", 0.0f);
        float pitch = MdxAttrs.getFloat(compiler, errorSink, el, "pitch", 0.0f);
        float roll = MdxAttrs.getFloat(compiler, errorSink, el, "roll", 0.0f);
        camera.setIsometricYawPitchRoll(yaw, pitch, roll);
    }
}
