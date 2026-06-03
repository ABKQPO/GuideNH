package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.SceneViewportMetrics;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler.ScenePlaceholder;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.unist.UnistNode;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

import cpw.mods.fml.common.FMLLog;

public class SceneScript implements LytScript {

    public SceneScript() {
    }

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "Scene"; }

    @Override
    public boolean isAsync() { return true; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof ScenePlaceholder ph)) return;

        if (ph.childrenSource == null || ph.childrenSource.trim().isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Empty scene: no scene elements"));
            return;
        }

        GuidebookLevel level = new GuidebookLevel();
        CameraSettings camera = new CameraSettings();
        if (ph.perspective != null && !ph.perspective.trim().isEmpty()) {
            camera.setPerspectivePreset(
                PerspectivePreset.fromSerializedName(ph.perspective.trim()));
        }
        if (!Float.isNaN(ph.zoom)) camera.setZoom(ph.zoom);
        if (!Float.isNaN(ph.rotateX)) camera.setRotationX(ph.rotateX);
        if (!Float.isNaN(ph.rotateY)) camera.setRotationY(ph.rotateY);
        if (!Float.isNaN(ph.rotateZ)) camera.setRotationZ(ph.rotateZ);
        if (!Float.isNaN(ph.offsetX)) camera.setOffsetX(ph.offsetX);
        if (!Float.isNaN(ph.offsetY)) camera.setOffsetY(ph.offsetY);
        if (ph.explicitCenter) {
            camera.setRotationCenter(
                Float.isNaN(ph.centerX) ? 0 : ph.centerX,
                Float.isNaN(ph.centerY) ? 0 : ph.centerY,
                Float.isNaN(ph.centerZ) ? 0 : ph.centerZ);
        }

        int width = ph.width > 0 ? ph.width : 320;
        int height = ph.height > 0 ? ph.height : 180;
        camera.setViewportSize(width, height);

        // Parse children source
        ExceptionCollector errorSink = new ExceptionCollector();
        PageCollection pc = ctx.getPageCollection();
        ExtensionCollection extensions = pc instanceof Guide guide
            ? guide.getExtensions() : ExtensionCollection.EMPTY;
        PageCompiler runtimeCompiler = new PageCompiler(pc != null ? pc : new StubPageCollection(),
            extensions, ph.sourcePack, new ResourceLocation(ph.pageDomain, "scene"),
            ph.childrenSource != null ? ph.childrenSource : "");
        MdAstRoot ast;
        try {
            ast = ph.childrenAst != null ? ph.childrenAst
                : MdAst.fromMarkdown(ph.childrenSource, GuideMarkdownOptions.runtime());
            if (ph.childrenAst == null && ast != null) {
                MdAstToMdxConverter.convert(ast, Collections.emptyMap());
            }
        } catch (Exception e) {
            FMLLog.getLogger().warn("[GuideNH] [SceneScript] Failed to re-parse scene children", e);
            ctx.replace(LytParagraph.error("[Scene] Failed to parse scene elements"));
            return;
        }

        // Build element compiler map from placeholder (set at compile time by SceneTagCompiler)
        Map<String, SceneElementTagCompiler> elementCompilers = new HashMap<>();
        if (ph.sceneElementCompilers != null) {
            for (var ec : ph.sceneElementCompilers) {
                for (String name : ec.getTagNames()) {
                    elementCompilers.put(name, ec);
                }
            }
        }

        // Create the scene EARLY so element compilers can access it via CURRENT_SCENE.
        LytGuidebookScene scene = new LytGuidebookScene();
        scene.setLevel(level);
        scene.setCamera(camera);
        scene.setSceneSize(width, height);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);

        // NB: Phase 2 used GuideSceneStructureCache (fingerprint-based) to avoid
        // recompiling complex scenes on every page visit. Phase 3 compiles from scratch
        // each mount. The cache requires StructureFingerprintResolver + compile-time
        // fingerprint computation, which is not practical to restore in a MOUNT-time script.
        // Low priority — scene compilation is usually fast enough that recompilation
        // per mount is acceptable.

        // Compile scene elements with CURRENT_SCENE set so that element compilers
        // (ImportPonderElementCompiler, ImportStructureLibElementCompiler, annotations, etc.)
        // can call scene.attachPonderData(), scene.addAnnotation(), etc.
        var prevScene = AnnotationTagCompiler.CURRENT_SCENE.get();
        AnnotationTagCompiler.CURRENT_SCENE.set(scene);
        final boolean[] blockStatsExplicitlySet = {false};
        try {
            GuideSceneStructureCompileScope.run(true, () -> {
                for (UnistNode child : ast.children()) {
                    MdxJsxElementFields el = SceneTagCompiler.unwrapSceneElement(child);
                    if (el == null) continue;
                    // Handle BlockStats — not a SceneElementTagCompiler, special-cased in Phase 2
                    if ("BlockStats".equals(el.name())) {
                        applyBlockStatsConfig(scene, el);
                        blockStatsExplicitlySet[0] = true;
                        continue;
                    }
                    SceneElementTagCompiler ec = elementCompilers.get(el.name());
                    if (ec != null) {
                        ec.compile(level, camera, runtimeCompiler, errorSink, el);
                    }
                }
            });
        } finally {
            if (prevScene != null) {
                AnnotationTagCompiler.CURRENT_SCENE.set(prevScene);
            } else {
                AnnotationTagCompiler.CURRENT_SCENE.remove();
            }
        }

        // Dispatch MOUNT events into annotation tooltip subtrees (Recipe/Scene placeholders)
        for (var annotation : scene.getAnnotations()) {
            var tooltip = annotation.getTooltip();
            if (tooltip instanceof ContentTooltip ct) {
                var content = ct.getContent();
                if (content instanceof LytNode root) {
                    ctx.dispatchSubtree(root);
                }
            }
        }

        if (level.isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Scene has no supported elements"));
            return;
        }

        // Apply implicit block stats for non-empty scenes without explicit BlockStats
        if (!blockStatsExplicitlySet[0]) {
            scene.setBlockStatsEnabled(true);
            scene.setBlockStatsVisible(ModConfig.ui.sceneBlockStatsVisible);
            scene.setBlockStatsButtonEnabled(ModConfig.ui.sceneBlockStatsButtonEnabled);
        }

        // Finalize scene setup: auto-center, ponder baseline, interactive state capture
        float[] center = level.getCenter();
        if (!ph.explicitCenter) {
            camera.setRotationCenter(center[0], center[1], center[2]);
        }
        // Auto-center the scene in the viewport
        if (!ph.explicitCenter && Float.isNaN(ph.offsetX) && Float.isNaN(ph.offsetY)) {
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            var sc = camera.worldToScreen(center[0], center[1], center[2]);
            camera.setOffsetX(-sc.x);
            camera.setOffsetY(sc.y);
        } else if (!Float.isNaN(ph.offsetX) || !Float.isNaN(ph.offsetY)) {
            if (!Float.isNaN(ph.offsetX)) camera.setOffsetX(ph.offsetX);
            if (!Float.isNaN(ph.offsetY)) camera.setOffsetY(ph.offsetY);
        }

        // Auto-zoom: when zoom is not explicitly set, fit scene to viewport at 85% fill
        if (Float.isNaN(ph.zoom)) {
            camera.setZoom(1f);
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            if (!level.isEmpty()) {
                int[] bounds = level.getBounds();
                SceneViewportMetrics metrics = SceneViewportMetrics.measure(camera, bounds);
                float spanX = metrics.spanX();
                float spanY = metrics.spanY();
                if (spanX > 0.5f || spanY > 0.5f) {
                    float zX = spanX > 0.5f ? (float) width / spanX : Float.MAX_VALUE;
                    float zY = spanY > 0.5f ? (float) height / spanY : Float.MAX_VALUE;
                    float autoZoom = Math.min(zX, zY) * 0.85f;
                    autoZoom = Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, autoZoom));
                    camera.setZoom(autoZoom);
                }
            }
        }
        // Auto-size: when width/height not explicitly set, measure and compute viewport
        if (!ph.explicitWidth || !ph.explicitHeight) {
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            if (!level.isEmpty()) {
                int[] bounds = level.getBounds();
                SceneViewportMetrics metrics = SceneViewportMetrics.measure(camera, bounds);
                if (!ph.explicitWidth && metrics.spanX() > 0.5f) {
                    width = SceneViewportMetrics.clampDimension(metrics.spanX());
                }
                if (!ph.explicitHeight && metrics.spanY() > 0.5f) {
                    height = SceneViewportMetrics.clampDimension(metrics.spanY());
                }
                scene.setSceneSize(width, height);
                camera.setViewportSize(width, height);
            }
        }

        scene.initializePonderTimelineBaseline();
        scene.captureInitialInteractiveState();
        scene.snapshotInitialCamera();
        // NB: Phase 2 called configureStructureLibSelectionListeners() which set up
        // rebuildSceneForStructureLibSelection() callbacks for interactive StructureLib
        // preview variant switching. Phase 3 defers this — scene rebuild would need to
        // re-invoke the full element compilation loop, which is impractical in a script.
        // The interactive variant UI will not respond to selection changes until the
        // page is re-mounted (navigate away and back).
        ctx.replace(scene);
    }

    /**
     * Applies BlockStats element attributes to the scene.
     * <p>
     * NB: Full BlockStats restoration (BlockStat sub-elements, filters, implicit enable)
     * requires the Phase 2 compileBlockStatsElement() logic (~100 lines). This minimal
     * restoration handles the most common attribute-only use case.
     */
    private static void applyBlockStatsConfig(LytGuidebookScene scene, MdxJsxElementFields el) {
        String visibleStr = el.getAttributeString("visible", null);
        if (visibleStr != null) scene.setBlockStatsVisible(Boolean.parseBoolean(visibleStr));
        String enabledStr = el.getAttributeString("buttonEnabled", null);
        if (enabledStr != null) scene.setBlockStatsButtonEnabled(Boolean.parseBoolean(enabledStr));
    }

    private static class ExceptionCollector implements LytErrorSink {
        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {
            FMLLog.getLogger().warn("[GuideNH] [SceneScript] {}", text);
        }
    }

    private static class StubPageCollection implements PageCollection {
        @Override public <T extends PageIndex> T getIndex(Class<T> c) { return null; }
        @Override public Collection<ParsedGuidePage> getPages() {
            return Collections.emptyList();
        }
        @Override public ParsedGuidePage getParsedPage(ResourceLocation id) { return null; }
        @Override public GuidePage getPage(ResourceLocation id) { return null; }
        @Override public byte[] loadAsset(ResourceLocation id) { return null; }
        @Override public NavigationTree getNavigationTree() {
            return new NavigationTree();
        }
        @Override public boolean pageExists(ResourceLocation pageId) { return false; }
    }
}
