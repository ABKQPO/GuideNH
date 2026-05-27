package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
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
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }
        String id = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (id == null || id.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "<Entity> missing id attribute", el);
            return;
        }

        String data = MdxAttrs.getString(compiler, errorSink, el, "data", null);
        String playerName = MdxAttrs.getString(compiler, errorSink, el, "name", null);
        String playerUuid = MdxAttrs.getString(compiler, errorSink, el, "uuid", null);
        String sceneEntityId = GuidebookSceneEntityLoader
            .trimToNull(MdxAttrs.getString(compiler, errorSink, el, "sceneEntityId", null));
        String mountTargetSceneEntityId = GuidebookSceneEntityLoader
            .trimToNull(MdxAttrs.getString(compiler, errorSink, el, "mount", null));
        Boolean unmount = getOptionalBoolean(compiler, errorSink, el, "unmount");
        Boolean showName = getOptionalBoolean(compiler, errorSink, el, "showName");
        Boolean showCape = getOptionalBoolean(compiler, errorSink, el, "showCape");
        Boolean baby = getOptionalBoolean(compiler, errorSink, el, "baby");
        Vector3f headRotation = getOptionalVector3(compiler, errorSink, el, "headRotation");
        Vector3f leftArmRotation = getOptionalVector3(compiler, errorSink, el, "leftArmRotation");
        Vector3f rightArmRotation = getOptionalVector3(compiler, errorSink, el, "rightArmRotation");
        Vector3f leftLegRotation = getOptionalVector3(compiler, errorSink, el, "leftLegRotation");
        Vector3f rightLegRotation = getOptionalVector3(compiler, errorSink, el, "rightLegRotation");
        Vector3f capeRotation = getOptionalVector3(compiler, errorSink, el, "capeRotation");
        World world = null;
        try {
            world = level.getOrCreateFakeWorld();
        } catch (IllegalStateException ignored) {
            // Scene parsing can happen before a client world exists. In that case we still create
            // the entity and bind it to the preview fake world on first render.
        }

        Entity entity;
        try {
            entity = GuidebookSceneEntityLoader.load(world, id, data, playerName, playerUuid);
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
        GuidebookSceneEntityImportSupport.applyRotation(entity, rotationY, rotationX, rotationY, rotationY);
        GuidebookPreviewPlayerPose pose = GuidebookSceneEntityStateSupport.createPreviewPlayerPose(
            headRotation,
            leftArmRotation,
            rightArmRotation,
            leftLegRotation,
            rightLegRotation,
            capeRotation);
        GuidebookSceneEntityStateSupport.applyVisualState(entity, id, showName, showCape, baby, pose, true);
        level.addEntity(entity, sceneEntityId);
        if (MdxAttrs.getBoolean(unmount, false)) {
            level.clearSceneEntityMount(sceneEntityId);
        } else if (mountTargetSceneEntityId != null) {
            level.setSceneEntityMount(sceneEntityId, mountTargetSceneEntityId);
        }
    }

    public static Boolean getOptionalBoolean(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name) {
        try {
            return MdxAttrs.getOptionalBoolean(el, name);
        } catch (MdxAttrs.AttributeException exception) {
            errorSink.appendError(compiler, exception.getMessage(), el);
            return null;
        }
    }

    public static Vector3f getOptionalVector3(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name) {
        if (el.getAttribute(name) == null) {
            return null;
        }

        String raw = MdxAttrs.getString(compiler, errorSink, el, name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, name + " expects 3 space-separated floats", el);
            return null;
        }

        Vector3f parsed = GuidebookSceneEntityStateSupport.parseOptionalVector3(raw);
        if (parsed == null) {
            errorSink.appendError(compiler, name + " expects 3 space-separated floats, got: '" + raw + "'", el);
            return null;
        }
        return parsed;
    }
}
