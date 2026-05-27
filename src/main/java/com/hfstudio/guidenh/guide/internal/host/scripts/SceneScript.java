package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.extensions.DefaultExtensions;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
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

import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

import cpw.mods.fml.common.FMLLog;

public class SceneScript implements LytScript {

    private final Map<String, SceneElementTagCompiler> elementCompilers;

    public SceneScript() {
        Map<String, SceneElementTagCompiler> map = new HashMap<>();
        for (var ext : DefaultExtensions.sceneElementCompilers()) {
            for (String name : ext.getTagNames()) {
                map.put(name, ext);
            }
        }
        this.elementCompilers = map;
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

        if (ph.childrenSource == null || ph.childrenSource.trim().isEmpty()) return;

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

        // Parse children source and compile scene elements
        ExceptionCollector errorSink = new ExceptionCollector();
        PageCollection pc = ctx.getPageCollection();
        PageCompiler runtimeCompiler = new PageCompiler(pc != null ? pc : new StubPageCollection(),
            ExtensionCollection.EMPTY, "", new ResourceLocation(ph.pageDomain, "scene"), "");
        try {
            MdAstRoot ast = MdAst.fromMarkdown(ph.childrenSource, GuideMarkdownOptions.runtime());
            MdAstToMdxConverter.convert(ast, Collections.emptyMap());
            for (UnistNode child : ast.children()) {
                MdxJsxElementFields el = SceneTagCompiler.unwrapSceneElement(child);
                if (el == null) continue;
                SceneElementTagCompiler ec = elementCompilers.get(el.name());
                if (ec != null) {
                    ec.compile(level, camera, runtimeCompiler, errorSink, el);
                }
            }
        } catch (Exception e) {
            FMLLog.getLogger().warn("[GuideNH] [SceneScript] Failed to re-parse scene children", e);
        }

        LytGuidebookScene scene = new LytGuidebookScene();
        scene.setLevel(level);
        scene.setCamera(camera);
        scene.setSceneSize(width, height);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);

        if (!level.isEmpty()) {
            float[] center = level.getCenter();
            if (!ph.explicitCenter) {
                camera.setRotationCenter(center[0], center[1], center[2]);
            }
            // Auto-center the scene in the viewport using the same approach as BlockImageScript
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            var sc = camera.worldToScreen(center[0], center[1], center[2]);
            camera.setOffsetX(-sc.x + (Float.isNaN(ph.offsetX) ? 0 : ph.offsetX));
            camera.setOffsetY(sc.y + (Float.isNaN(ph.offsetY) ? 0 : ph.offsetY));
        }
        scene.snapshotInitialCamera();
        ctx.replace(scene);
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
