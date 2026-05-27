package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneParticleFactory;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ParticleElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Particle");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "Particle tag used outside <GameScene>", el);
            return;
        }

        float x = MdxAttrs.getFloat(compiler, errorSink, el, "x", 0.5f);
        float y = MdxAttrs.getFloat(compiler, errorSink, el, "y", 0.5f);
        float z = MdxAttrs.getFloat(compiler, errorSink, el, "z", 0.5f);
        float size = Math.max(0.01f, MdxAttrs.getFloat(compiler, errorSink, el, "size", 0.18f));
        String particleName = resolveParticleName(compiler, errorSink, el);
        scene.addStaticParticle(GuidebookSceneParticleFactory.createStaticSceneParticle(particleName, x, y, z, size));
    }

    private static String resolveParticleName(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String particleName = MdxAttrs.getString(compiler, errorSink, el, "name", null);
        if (particleName == null || particleName.trim()
            .isEmpty()) {
            particleName = MdxAttrs.getString(compiler, errorSink, el, "particle", null);
        }
        if (particleName != null && !particleName.trim()
            .isEmpty() && !GuidebookSceneParticleFactory.isSupportedParticleName(particleName)) {
            errorSink.appendError(compiler, "Unsupported particle name '" + particleName + "'", el);
        }
        return particleName;
    }
}
