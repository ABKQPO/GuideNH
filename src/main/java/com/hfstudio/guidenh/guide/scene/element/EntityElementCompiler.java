package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class EntityElementCompiler implements SceneElementTagCompiler {

    private static final Logger LOG = LoggerFactory.getLogger(EntityElementCompiler.class);

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Entity");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        String id = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (id == null || id.isEmpty()) {
            errorSink.appendError(compiler, "<Entity> missing id attribute", el);
            return;
        }
        LOG.debug("Scene <Entity id={}> parsed but entity rendering is not yet implemented", id);
    }
}
