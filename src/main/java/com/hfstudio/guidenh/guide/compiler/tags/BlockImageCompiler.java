package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver.ResolvedBlockReference;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;
import com.hfstudio.guidenh.guide.scene.element.BlockElementCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.ponder.PonderNbtPath;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockImageCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var blockReference = MdxAttrs.getRequiredBlockReference(compiler, parent, el, "id");
        if (blockReference == null) {
            return;
        }

        var scene = new LytGuidebookScene();
        scene.setInteractive(false);
        scene.setSceneButtonsVisible(false);
        scene.setBottomControlsVisible(false);
        scene.setReserveBottomControlArea(false);
        scene.setVisibleLayerSliderEnabled(false);
        scene.setGridButtonEnabled(false);
        scene.setGridVisible(false);
        scene.setAnnotationsVisible(false);
        scene.setShowBackground(false);

        CameraSettings camera = scene.getCamera();
        camera.setZoom(clampZoom(MdxAttrs.getFloat(compiler, parent, el, "scale", 1f)));
        camera.setPerspectivePreset(resolvePerspective(compiler, parent, el));

        int meta = resolveBlockMeta(blockReference);
        NBTTagCompound tileTag = resolveTileTag(compiler, parent, el);
        GuidebookPreviewBlockPlacer.place(
            scene.getLevel(),
            0,
            0,
            0,
            blockReference.block(),
            meta,
            tileTag,
            blockReference.registryId()
                .toString());

        finalizeSceneLayout(scene);
        scene.snapshotInitialCamera();
        scene.captureInitialInteractiveState();
        parent.append(scene);
    }

    private PerspectivePreset resolvePerspective(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields el) {
        String rawPerspective = MdxAttrs.getString(compiler, parent, el, "perspective", null);
        if (rawPerspective == null || rawPerspective.trim()
            .isEmpty()) {
            return PerspectivePreset.ISOMETRIC_NORTH_EAST;
        }
        return PerspectivePreset.fromSerializedName(rawPerspective.trim());
    }

    private int resolveBlockMeta(ResolvedBlockReference blockReference) {
        if (blockReference.hasExplicitMeta() && blockReference.explicitMeta() != OreDictionary.WILDCARD_VALUE) {
            return blockReference.explicitMeta();
        }
        return BlockElementCompiler.defaultMetaFor(blockReference.block(), null);
    }

    @Nullable
    private NBTTagCompound resolveTileTag(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        NBTTagCompound merged = resolveInlineIdNbt(compiler, parent, el);
        String nbtText = MdxAttrs.getString(compiler, parent, el, "nbt", null);
        if (nbtText == null || nbtText.trim()
            .isEmpty()) {
            return merged;
        }

        try {
            NBTTagCompound attrTag = GuideTextNbtCodec.readTextSafeCompound(nbtText.trim());
            if (merged == null) {
                return attrTag;
            }
            PonderNbtPath.mergeCompound(merged, attrTag);
            return merged;
        } catch (Exception e) {
            parent.appendError(compiler, "Bad nbt: " + e.getMessage(), el);
            return merged;
        }
    }

    @Nullable
    private NBTTagCompound resolveInlineIdNbt(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String oreName = MdxAttrs.getString(el, "ore", null);
        if (oreName != null && !oreName.trim()
            .isEmpty()) {
            return null;
        }
        String rawId = MdxAttrs.getString(el, "id", null);
        if (rawId == null || rawId.trim()
            .isEmpty()) {
            return null;
        }
        String trimmedId = rawId.trim();
        if (trimmedId.indexOf('{') < 0) {
            return null;
        }

        try {
            IdUtils.ParsedItemRef parsed = IdUtils.parseItemRef(
                trimmedId,
                compiler.getPageId()
                    .getResourceDomain());
            return parsed == null || parsed.nbt() == null ? null
                : (NBTTagCompound) parsed.nbt()
                    .copy();
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "Malformed id " + trimmedId + ": " + e.getMessage(), el);
            return null;
        }
    }

    private void finalizeSceneLayout(LytGuidebookScene scene) {
        if (scene.getLevel()
            .isEmpty()) {
            return;
        }

        int width = scene.getSceneWidth();
        int height = scene.getSceneHeight();
        CameraSettings camera = scene.getCamera();
        camera.setViewportSize(width, height);

        float[] center = scene.getLevel()
            .getCenter();
        camera.setRotationCenter(center[0], center[1], center[2]);

        int[] bounds = scene.getLevel()
            .getBounds();
        float minX = bounds[0];
        float minY = bounds[1];
        float minZ = bounds[2];
        float maxX = bounds[3] + 1f;
        float maxY = bounds[4] + 1f;
        float maxZ = bounds[5] + 1f;

        float savedOffsetX = camera.getOffsetX();
        float savedOffsetY = camera.getOffsetY();
        camera.setOffsetX(0f);
        camera.setOffsetY(0f);

        float minScreenX = Float.MAX_VALUE;
        float maxScreenX = -Float.MAX_VALUE;
        float minScreenY = Float.MAX_VALUE;
        float maxScreenY = -Float.MAX_VALUE;
        for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            float worldX = (cornerIndex & 1) == 0 ? minX : maxX;
            float worldY = (cornerIndex & 2) == 0 ? minY : maxY;
            float worldZ = (cornerIndex & 4) == 0 ? minZ : maxZ;
            var screenPoint = camera.worldToScreen(worldX, worldY, worldZ);
            if (screenPoint.x < minScreenX) {
                minScreenX = screenPoint.x;
            }
            if (screenPoint.x > maxScreenX) {
                maxScreenX = screenPoint.x;
            }
            if (screenPoint.y < minScreenY) {
                minScreenY = screenPoint.y;
            }
            if (screenPoint.y > maxScreenY) {
                maxScreenY = screenPoint.y;
            }
        }

        int autoWidth = clampSceneDimension((int) Math.ceil(maxScreenX - minScreenX) + 16);
        int autoHeight = clampSceneDimension((int) Math.ceil(maxScreenY - minScreenY) + 16);
        scene.setSceneSize(autoWidth, autoHeight);
        camera.setViewportSize(autoWidth, autoHeight);

        camera.setOffsetX(0f);
        camera.setOffsetY(0f);
        var projectedCenter = camera.worldToScreen(center[0], center[1], center[2]);
        camera.setOffsetX(-projectedCenter.x + savedOffsetX);
        camera.setOffsetY(projectedCenter.y + savedOffsetY);
    }

    private int clampSceneDimension(int dimension) {
        return Math.max(64, Math.min(256, dimension));
    }

    private float clampZoom(float zoom) {
        return Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, zoom));
    }
}
