package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class RemoveEntityElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("RemoveEntity");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }

        String sceneEntityId = GuidebookSceneEntityLoader
            .trimToNull(MdxAttrs.getString(compiler, errorSink, el, "sceneEntityId", null));
        if (sceneEntityId == null) {
            errorSink.appendError(compiler, "<RemoveEntity> missing sceneEntityId attribute", el);
            return;
        }

        Boolean unmount = EntityElementCompiler.getOptionalBoolean(compiler, errorSink, el, "unmount");
        if (MdxAttrs.getBoolean(unmount, false)) {
            level.clearSceneEntityMount(sceneEntityId);
        }
        level.removeEntitiesBySceneEntityId(sceneEntityId);
    }
}
