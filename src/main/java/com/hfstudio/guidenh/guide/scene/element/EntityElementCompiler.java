package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import net.minecraft.entity.EntityLivingBase;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class EntityElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Entity");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        String id = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (id == null || id.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "<Entity> missing id attribute", el);
            return;
        }

        String data = MdxAttrs.getString(compiler, errorSink, el, "data", null);
        net.minecraft.world.World world = null;
        try {
            world = level.getOrCreateFakeWorld();
        } catch (IllegalStateException ignored) {
            // Scene parsing can happen before a client world exists. In that case we still create
            // the entity and bind it to the preview fake world on first render.
        }

        net.minecraft.entity.Entity entity;
        try {
            entity = GuidebookSceneEntityLoader.load(world, id, data);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return;
        }

        if (entity == null) {
            errorSink.appendError(compiler, "Failed to load entity '" + id + "'", el);
            return;
        }

        float x = MdxAttrs.getFloat(compiler, errorSink, el, "x", 0.5f);
        float y = MdxAttrs.getFloat(compiler, errorSink, el, "y", 0.0f);
        float z = MdxAttrs.getFloat(compiler, errorSink, el, "z", 0.5f);
        float rotationY = MdxAttrs.getFloat(compiler, errorSink, el, "rotationY", -45.0f);
        float rotationX = MdxAttrs.getFloat(compiler, errorSink, el, "rotationX", 0.0f);

        entity.setLocationAndAngles(x, y, z, rotationY, rotationX);
        entity.prevRotationYaw = rotationY;
        entity.prevRotationPitch = rotationX;

        if (entity instanceof EntityLivingBase living) {
            living.rotationYawHead = rotationY;
            living.prevRotationYawHead = rotationY;
            living.renderYawOffset = rotationY;
            living.prevRenderYawOffset = rotationY;
        }

        level.addEntity(entity);
    }
}
