package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
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
        String playerName = MdxAttrs.getString(compiler, errorSink, el, "name", null);
        String playerUuid = MdxAttrs.getString(compiler, errorSink, el, "uuid", null);
        Boolean showName = getOptionalBoolean(compiler, errorSink, el, "showName");
        Boolean showCape = getOptionalBoolean(compiler, errorSink, el, "showCape");
        Vector3f headRotation = getOptionalVector3(compiler, errorSink, el, "headRotation");
        Vector3f leftArmRotation = getOptionalVector3(compiler, errorSink, el, "leftArmRotation");
        Vector3f rightArmRotation = getOptionalVector3(compiler, errorSink, el, "rightArmRotation");
        Vector3f leftLegRotation = getOptionalVector3(compiler, errorSink, el, "leftLegRotation");
        Vector3f rightLegRotation = getOptionalVector3(compiler, errorSink, el, "rightLegRotation");
        Vector3f capeRotation = getOptionalVector3(compiler, errorSink, el, "capeRotation");
        net.minecraft.world.World world = null;
        try {
            world = level.getOrCreateFakeWorld();
        } catch (IllegalStateException ignored) {
            // Scene parsing can happen before a client world exists. In that case we still create
            // the entity and bind it to the preview fake world on first render.
        }

        net.minecraft.entity.Entity entity;
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
        entity.prevRotationYaw = rotationY;
        entity.prevRotationPitch = rotationX;

        if (entity instanceof EntityLivingBase living) {
            living.rotationYawHead = rotationY;
            living.prevRotationYawHead = rotationY;
            living.renderYawOffset = rotationY;
            living.prevRenderYawOffset = rotationY;
        }

        if (entity instanceof GuidebookNameplateControllable nameplateControllable) {
            boolean defaultVisible = GuidebookSceneEntityLoader.isPreviewPlayerId(id);
            nameplateControllable
                .setGuidebookNameplateVisible(showName != null ? showName.booleanValue() : defaultVisible);
        } else if (showName != null && entity instanceof EntityLiving living) {
            living.setAlwaysRenderNameTag(showName.booleanValue());
        }

        if (entity instanceof GuidebookCapeControllable capeControllable) {
            boolean defaultVisible = GuidebookSceneEntityLoader.isPreviewPlayerId(id);
            capeControllable.setGuidebookCapeVisible(showCape != null ? showCape.booleanValue() : defaultVisible);
        }

        if (entity instanceof GuidebookPlayerPoseControllable poseControllable) {
            poseControllable.setGuidebookPreviewPlayerPose(
                new GuidebookPreviewPlayerPose(
                    headRotation,
                    leftArmRotation,
                    rightArmRotation,
                    leftLegRotation,
                    rightLegRotation,
                    capeRotation));
        }

        level.addEntity(entity);
    }

    public static Boolean getOptionalBoolean(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name) {
        var attribute = el.getAttribute(name);
        if (attribute == null) {
            return null;
        }
        if (attribute.hasExpressionValue()) {
            String expressionValue = attribute.getExpressionValue();
            if ("true".equals(expressionValue)) {
                return Boolean.TRUE;
            }
            if ("false".equals(expressionValue)) {
                return Boolean.FALSE;
            }
        }
        errorSink.appendError(compiler, name + " should be {true} or {false}", el);
        return null;
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

        String[] parts = raw.trim()
            .split("\\s+");
        if (parts.length != 3) {
            errorSink.appendError(compiler, name + " expects 3 space-separated floats, got: '" + raw + "'", el);
            return null;
        }

        try {
            return new Vector3f(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed vector3 for " + name + ": '" + raw + "'", el);
            return null;
        }
    }
}
