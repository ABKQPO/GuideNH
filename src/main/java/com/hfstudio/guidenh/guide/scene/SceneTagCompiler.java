package com.hfstudio.guidenh.guide.scene;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistParent;

public class SceneTagCompiler extends BlockTagCompiler {

    private static final LytErrorSink NOOP_ERROR_SINK = (compiler, text, node) -> {};

    private Map<String, SceneElementTagCompiler> elementCompilers = Collections.emptyMap();

    @Override
    public Set<String> getTagNames() {
        var s = new LinkedHashSet<String>();
        s.add("GameScene");
        s.add("Scene");
        return s;
    }

    @Override
    public void onExtensionsBuilt(ExtensionCollection extensions) {
        Map<String, SceneElementTagCompiler> map = new HashMap<>();
        for (var ext : extensions.get(SceneElementTagCompiler.EXTENSION_POINT)) {
            for (String name : ext.getTagNames()) {
                map.put(name, ext);
            }
        }
        this.elementCompilers = map;
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var scene = new LytGuidebookScene();
        int w = MdxAttrs.getInt(compiler, parent, el, "width", 256);
        int h = MdxAttrs.getInt(compiler, parent, el, "height", 192);
        scene.setSceneSize(w, h);

        float zoom = MdxAttrs.getFloat(compiler, parent, el, "zoom", 1.0f);
        if (Math.abs(zoom - 1.0f) > 1e-4f) {
            scene.getCamera()
                .setZoom(zoom);
        }

        // Camera preset (yaw/pitch/roll) — applied before explicit rotateX/Y/Z overrides.
        String perspective = MdxAttrs.getString(compiler, parent, el, "perspective", null);
        if (perspective != null && !perspective.isEmpty()) {
            scene.getCamera()
                .setPerspectivePreset(PerspectivePreset.fromSerializedName(perspective));
        }

        float rx = MdxAttrs.getFloat(compiler, parent, el, "rotateX", Float.NaN);
        float ry = MdxAttrs.getFloat(compiler, parent, el, "rotateY", Float.NaN);
        float rz = MdxAttrs.getFloat(compiler, parent, el, "rotateZ", Float.NaN);
        if (!Float.isNaN(rx)) scene.getCamera()
            .setRotationX(rx);
        if (!Float.isNaN(ry)) scene.getCamera()
            .setRotationY(ry);
        if (!Float.isNaN(rz)) scene.getCamera()
            .setRotationZ(rz);

        // Pan offsets (screen-space), applied on top of the preset / rotations.
        float offX = MdxAttrs.getFloat(compiler, parent, el, "offsetX", Float.NaN);
        float offY = MdxAttrs.getFloat(compiler, parent, el, "offsetY", Float.NaN);
        if (!Float.isNaN(offX)) scene.getCamera()
            .setOffsetX(offX);
        if (!Float.isNaN(offY)) scene.getCamera()
            .setOffsetY(offY);

        // Explicit world-space rotation center. If any of the 3 coords is given, we override the
        // auto-center computed later from level bounds.
        float centerX = MdxAttrs.getFloat(compiler, parent, el, "centerX", Float.NaN);
        float centerY = MdxAttrs.getFloat(compiler, parent, el, "centerY", Float.NaN);
        float centerZ = MdxAttrs.getFloat(compiler, parent, el, "centerZ", Float.NaN);
        boolean explicitCenter = !Float.isNaN(centerX) || !Float.isNaN(centerY) || !Float.isNaN(centerZ);
        if (explicitCenter) {
            scene.getCamera()
                .setRotationCenter(
                    Float.isNaN(centerX) ? 0f : centerX,
                    Float.isNaN(centerY) ? 0f : centerY,
                    Float.isNaN(centerZ) ? 0f : centerZ);
        }

        boolean interactive = MdxAttrs.getBoolean(compiler, parent, el, "interactive", true);
        scene.setInteractive(interactive);
        boolean allowLayerSlider = MdxAttrs
            .getBoolean(compiler, parent, el, "allowLayerSlider", ModConfig.ui.sceneLayerSliderEnabled);
        scene.setVisibleLayerSliderEnabled(allowLayerSlider);

        if (el instanceof MdxJsxFlowElement flow) {
            compileSceneChildren(scene, compiler, parent, flow);
            scene.setStructureLibSelectionChangeListener(
                selection -> rebuildSceneForStructureLibSelection(scene, compiler, flow, explicitCenter, selection));
        }

        if (!scene.getLevel()
            .isEmpty() && !explicitCenter) {
            var c = scene.getLevel()
                .getCenter();
            scene.getCamera()
                .setRotationCenter(c[0], c[1], c[2]);
        }

        scene.snapshotInitialCamera();

        parent.append(scene);
    }

    private void compileSceneChildren(LytGuidebookScene scene, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxFlowElement flow) {
        AnnotationTagCompiler.CURRENT_SCENE.set(scene);
        try {
            for (var child : flow.children()) {
                UnistNode childNode = child;
                MdxJsxElementFields childEl = unwrapSceneElement(childNode);
                if (childEl == null) {
                    continue;
                }
                String name = childEl.name();
                if (name == null) {
                    continue;
                }
                var elCompiler = elementCompilers.get(name);
                if (elCompiler == null) {
                    errorSink.appendError(compiler, "Unknown scene element <" + name + ">", childNode);
                    continue;
                }
                elCompiler.compile(scene.getLevel(), scene.getCamera(), compiler, errorSink, childEl);
            }
        } finally {
            AnnotationTagCompiler.CURRENT_SCENE.remove();
        }
    }

    private void rebuildSceneForStructureLibSelection(LytGuidebookScene scene, PageCompiler compiler,
        MdxJsxFlowElement flow, boolean explicitCenter, StructureLibPreviewSelection selection) {
        if (scene == null) {
            return;
        }
        SavedCameraSettings savedCamera = scene.getCamera()
            .save();
        boolean annotationsVisible = scene.isAnnotationsVisible();
        boolean hatchHighlightEnabled = scene.isStructureLibHatchHighlightEnabled();
        scene.getAnnotations()
            .clear();
        scene.setHoveredBlock(null);
        scene.setHoveredStructureLibHatch(null);
        scene.clearAnnotationHover();
        scene.setStructureLibSceneMetadata(null);
        scene.setPendingStructureLibPreviewSelection(selection);
        scene.setLevel(new GuidebookLevel());
        try {
            compileSceneChildren(scene, compiler, NOOP_ERROR_SINK, flow);
        } finally {
            scene.setPendingStructureLibPreviewSelection(null);
        }
        if (!scene.getLevel()
            .isEmpty() && !explicitCenter) {
            var center = scene.getLevel()
                .getCenter();
            scene.getCamera()
                .setRotationCenter(center[0], center[1], center[2]);
        }
        scene.setAnnotationsVisible(annotationsVisible);
        scene.setStructureLibHatchHighlightEnabled(hatchHighlightEnabled);
        scene.getCamera()
            .restore(savedCamera);
    }

    private static MdxJsxElementFields unwrapSceneElement(UnistNode node) {
        if (node instanceof MdxJsxElementFields elementFields) {
            return elementFields;
        }
        if (!(node instanceof UnistParent parent)) {
            return null;
        }

        MdxJsxElementFields found = null;
        for (UnistNode child : parent.children()) {
            if (isIgnorableNode(child)) {
                continue;
            }
            MdxJsxElementFields nested = unwrapSceneElement(child);
            if (nested == null) {
                return null;
            }
            if (found != null) {
                return null;
            }
            found = nested;
        }
        return found;
    }

    private static boolean isIgnorableNode(UnistNode node) {
        if (node instanceof MdxJsxElementFields) {
            return false;
        }
        if (node instanceof MdAstNode astNode) {
            return astNode.toText()
                .trim()
                .isEmpty();
        }
        return false;
    }
}
