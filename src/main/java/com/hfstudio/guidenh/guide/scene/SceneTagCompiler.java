package com.hfstudio.guidenh.guide.scene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCache;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheEntry;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheKey;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureFingerprintResolver;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistParent;

public class SceneTagCompiler extends BlockTagCompiler {

    public static final LytErrorSink NOOP_ERROR_SINK = (compiler, text, node) -> {};
    private static final String[] SCENE_ROOT_TAG_NAMES = { "GameScene", "Scene" };
    private static final String[] SCENE_HEAVY_TAG_NAMES = { "ImportStructure", "ImportStructureLib", "PlaceBlock",
        "RemoveBlocks", "RemoveEntity", "ReplaceBlock", "Block", "Entity" };
    private static final int SCENE_HEAVY_ELEMENT_THRESHOLD = 8;

    private Map<String, SceneElementTagCompiler> elementCompilers = Map.of();
    private final GuideSceneStructureFingerprintResolver structureFingerprintResolver = new GuideSceneStructureFingerprintResolver();

    public static boolean likelyHasHeavySceneWork(@Nullable ParsedGuidePage parsedPage) {
        return parsedPage != null && likelyHasHeavySceneWork(parsedPage.getSource());
    }

    public static boolean likelyHasHeavySceneWork(@Nullable String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return false;
        }

        int sceneRootCount = countTags(sourceText, SCENE_ROOT_TAG_NAMES);
        if (sceneRootCount <= 0) {
            return false;
        }
        if (sceneRootCount > 1) {
            return true;
        }

        // Keep this source-only heuristic narrow so warmup can prioritize obvious scene-heavy pages cheaply.
        return countTags(sourceText, SCENE_HEAVY_TAG_NAMES) >= SCENE_HEAVY_ELEMENT_THRESHOLD;
    }

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
        // Detect whether width/height are explicitly set so auto-size can kick in when omitted.
        String rawWidth = MdxAttrs.getString(compiler, parent, el, "width", null);
        String rawHeight = MdxAttrs.getString(compiler, parent, el, "height", null);
        boolean explicitWidth = rawWidth != null;
        boolean explicitHeight = rawHeight != null;
        int w = MdxAttrs.getInt(compiler, parent, el, "width", LytGuidebookScene.DEFAULT_WIDTH);
        int h = MdxAttrs.getInt(compiler, parent, el, "height", LytGuidebookScene.DEFAULT_HEIGHT);
        scene.setSceneSize(w, h);

        float zoom = MdxAttrs.getFloat(compiler, parent, el, "zoom", Float.NaN);
        boolean explicitZoom = !Float.isNaN(zoom);
        if (explicitZoom) {
            scene.getCamera()
                .setZoom(zoom);
        }

        // Camera preset (yaw/pitch/roll), applied before explicit rotateX/Y/Z overrides.
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
        boolean explicitOffX = !Float.isNaN(offX);
        boolean explicitOffY = !Float.isNaN(offY);
        if (explicitOffX) scene.getCamera()
            .setOffsetX(offX);
        if (explicitOffY) scene.getCamera()
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
        scene.setShowBackground(resolveShowBackground(compiler, parent, el));
        boolean allowLayerSlider = MdxAttrs
            .getBoolean(compiler, parent, el, "allowLayerSlider", ModConfig.ui.sceneLayerSliderEnabled);
        scene.setVisibleLayerSliderEnabled(allowLayerSlider);
        boolean gridButtonEnabled = MdxAttrs.getBoolean(compiler, parent, el, "gridButtonEnabled", true);
        scene.setGridButtonEnabled(gridButtonEnabled);
        boolean showGrid = MdxAttrs.getBoolean(compiler, parent, el, "showGrid", false);
        scene.setGridVisible(showGrid);
        scene.resetBlockStatsConfiguration();
        boolean blockStatsDeclared = false;

        if (el instanceof MdxJsxFlowElement flow) {
            blockStatsDeclared = compileSceneChildren(scene, compiler, parent, flow);
            scene.initializePonderTimelineBaseline();
            configureStructureLibSelectionListeners(scene, compiler, flow, explicitCenter);
        }

        if (!scene.getLevel()
            .isEmpty()) {
            CameraSettings cam = scene.getCamera();
            // Set viewport size so worldToScreen() works correctly during auto-computation.
            cam.setViewportSize(w, h);

            // Determine rotation center; fall back to the geometric center of the level bounds
            // when the author has not specified one explicitly.
            float[] center;
            if (!explicitCenter) {
                center = scene.getLevel()
                    .getCenter();
                cam.setRotationCenter(center[0], center[1], center[2]);
            } else {
                center = new float[] { Float.isNaN(centerX) ? 0f : centerX, Float.isNaN(centerY) ? 0f : centerY,
                    Float.isNaN(centerZ) ? 0f : centerZ };
            }

            // Auto-zoom: project all 8 AABB corners at zoom=1, offset=0 to obtain the
            // screen-space extent of the scene under the current isometric rotation. The
            // initial zoom is then chosen so the full scene fits within the viewport with an
            // 85% fill factor (15% breathing room).
            if (!explicitZoom) {
                cam.setZoom(1f);
                cam.setOffsetX(0f);
                cam.setOffsetY(0f);

                SceneViewportMetrics metrics = measureSceneViewport(
                    cam,
                    scene.getLevel()
                        .getBounds());
                float spanX = metrics.spanX();
                float spanY = metrics.spanY();
                float autoZoom = 1f;
                if (spanX > 0.5f || spanY > 0.5f) {
                    float zX = spanX > 0.5f ? (float) w / spanX : Float.MAX_VALUE;
                    float zY = spanY > 0.5f ? (float) h / spanY : Float.MAX_VALUE;
                    autoZoom = Math.min(zX, zY) * 0.85f;
                    autoZoom = Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, autoZoom));
                }
                cam.setZoom(autoZoom);
                // Restore any explicit offsets that were zeroed for the measurement pass.
                if (explicitOffX) cam.setOffsetX(offX);
                if (explicitOffY) cam.setOffsetY(offY);
            }

            // Auto-size: when width or height is not explicitly set by the author, compute the
            // actual pixel extent of the scene content at the final zoom and use it as the
            // viewport size. This eliminates wasted blank space around scenes smaller than the
            // default canvas while preserving the same fill factor as the reference viewport.
            if (!explicitWidth || !explicitHeight) {
                float savedOffX = cam.getOffsetX();
                float savedOffY = cam.getOffsetY();
                cam.setOffsetX(0f);
                cam.setOffsetY(0f);

                SceneViewportMetrics metrics = measureSceneViewport(
                    cam,
                    scene.getLevel()
                        .getBounds());

                // Add a small border so the scene content never touches the viewport edge.
                int autoPadding = 16;
                int autoMinDim = 64;
                int autoMaxDim = 512;
                float szSpanX = metrics.spanX();
                float szSpanY = metrics.spanY();
                if (!explicitWidth && szSpanX > 0.5f) {
                    w = Math.min(autoMaxDim, Math.max(autoMinDim, (int) Math.ceil(szSpanX) + autoPadding));
                }
                if (!explicitHeight && szSpanY > 0.5f) {
                    h = Math.min(autoMaxDim, Math.max(autoMinDim, (int) Math.ceil(szSpanY) + autoPadding));
                }
                scene.setSceneSize(w, h);
                cam.setViewportSize(w, h);

                // Restore offsets zeroed for the measurement pass.
                cam.setOffsetX(savedOffX);
                cam.setOffsetY(savedOffY);
            }

            // Auto-center: shift the projected scene center to the viewport origin.
            // Applied only when neither the rotation center nor the screen offsets are
            // author-specified, so explicit offsetX/Y or centerX/Y/Z always win.
            if (!explicitCenter && !explicitOffX && !explicitOffY) {
                cam.setOffsetX(0f);
                cam.setOffsetY(0f);
                var sc = cam.worldToScreen(center[0], center[1], center[2]);
                cam.setOffsetX(-sc.x);
                cam.setOffsetY(sc.y);
            }
        }

        applyImplicitBlockStats(scene, blockStatsDeclared);
        scene.applyDefaultBlockStatsMaxSizeFromScene();
        scene.snapshotInitialCamera();
        scene.captureInitialInteractiveState();

        parent.append(scene);
    }

    private boolean resolveShowBackground(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        return MdxAttrs.getBoolean(compiler, parent, el, "showBackground", true);
    }

    private SceneViewportMetrics measureSceneViewport(CameraSettings camera, int[] bounds) {
        float lx = bounds[0];
        float ly = bounds[1];
        float lz = bounds[2];
        float hx = bounds[3] + 1f;
        float hy = bounds[4] + 1f;
        float hz = bounds[5] + 1f;
        float minSX = Float.MAX_VALUE;
        float maxSX = -Float.MAX_VALUE;
        float minSY = Float.MAX_VALUE;
        float maxSY = -Float.MAX_VALUE;
        for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            float wx = (cornerIndex & 1) == 0 ? lx : hx;
            float wy = (cornerIndex & 2) == 0 ? ly : hy;
            float wz = (cornerIndex & 4) == 0 ? lz : hz;
            var screenPoint = camera.worldToScreen(wx, wy, wz);
            if (screenPoint.x < minSX) minSX = screenPoint.x;
            if (screenPoint.x > maxSX) maxSX = screenPoint.x;
            if (screenPoint.y < minSY) minSY = screenPoint.y;
            if (screenPoint.y > maxSY) maxSY = screenPoint.y;
        }
        return new SceneViewportMetrics(minSX, maxSX, minSY, maxSY);
    }

    private boolean compileSceneChildren(LytGuidebookScene scene, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxFlowElement flow) {
        return withCurrentAnnotationScene(scene, () -> {
            List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(flow);
            boolean[] result = new boolean[1];
            compiler.withBlockTagChildrenSourceContext(
                flow,
                () -> result[0] = compileSceneChildrenWithCache(scene, compiler, errorSink, children, Map.of()));
            return result[0];
        });
    }

    static <T> T withCurrentAnnotationScene(LytGuidebookScene scene, Supplier<T> action) {
        LytGuidebookScene previousScene = AnnotationTagCompiler.CURRENT_SCENE.get();
        AnnotationTagCompiler.CURRENT_SCENE.set(scene);
        try {
            return action.get();
        } finally {
            if (previousScene != null) {
                AnnotationTagCompiler.CURRENT_SCENE.set(previousScene);
            } else {
                AnnotationTagCompiler.CURRENT_SCENE.remove();
            }
        }
    }

    private boolean compileSceneChildrenWithCache(LytGuidebookScene scene, PageCompiler compiler,
        LytErrorSink errorSink, List<? extends MdAstAnyContent> children,
        Map<String, StructureLibPreviewSelection> structureLibSelections) {
        GuideSceneStructureCacheKey cacheKey = structureFingerprintResolver
            .buildForGameScene(compiler, children, structureLibSelections);
        if (cacheKey == null) {
            return compileSceneChildrenDetailed(scene, compiler, errorSink, children, true).isBlockStatsDeclared();
        }

        GuideSceneStructureCacheEntry cacheEntry = GuideSceneStructureCache.global()
            .restore(cacheKey);
        if (cacheEntry != null) {
            cacheEntry.restoreInto(scene);
            return compileSceneChildren(scene, compiler, errorSink, children, false);
        }

        SceneChildrenCompileResult result = compileSceneChildrenDetailed(scene, compiler, errorSink, children, true);
        if (result.isStructureCacheable()) {
            GuideSceneStructureCacheEntry normalizedStructureState = GuideSceneStructureCacheEntry.capture(scene);
            GuideSceneStructureCache.global()
                .put(cacheKey, normalizedStructureState);
            normalizedStructureState.restoreInto(scene);
        }
        return result.isBlockStatsDeclared();
    }

    private boolean compileSceneChildren(LytGuidebookScene scene, PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children) {
        return compileSceneChildren(scene, compiler, errorSink, children, true);
    }

    private boolean compileSceneChildren(LytGuidebookScene scene, PageCompiler compiler, LytErrorSink errorSink,
        List<? extends MdAstAnyContent> children, boolean structureMutationEnabled) {
        return compileSceneChildrenDetailed(scene, compiler, errorSink, children, structureMutationEnabled)
            .isBlockStatsDeclared();
    }

    private SceneChildrenCompileResult compileSceneChildrenDetailed(LytGuidebookScene scene, PageCompiler compiler,
        LytErrorSink errorSink, List<? extends MdAstAnyContent> children, boolean structureMutationEnabled) {
        CountingErrorSink trackingErrorSink = new CountingErrorSink(errorSink);
        return GuideSceneStructureCompileScope.supply(structureMutationEnabled, () -> {
            boolean blockStatsDeclared = false;
            boolean structureCacheable = true;
            for (var child : children) {
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
                if ("BlockStats".equals(name)) {
                    compileBlockStatsElement(scene, compiler, trackingErrorSink, childEl);
                    blockStatsDeclared = true;
                    continue;
                }
                if (elCompiler == null) {
                    trackingErrorSink.appendError(compiler, "Unknown scene element <" + name + ">", childNode);
                    if (structureMutationEnabled && structureFingerprintResolver.isStructuralSceneElement(name)) {
                        structureCacheable = false;
                    }
                    continue;
                }
                int errorCountBefore = trackingErrorSink.getErrorCount();
                elCompiler.compile(scene.getLevel(), scene.getCamera(), compiler, trackingErrorSink, childEl);
                if (structureMutationEnabled && structureFingerprintResolver.isStructuralSceneElement(name)
                    && trackingErrorSink.getErrorCount() > errorCountBefore) {
                    structureCacheable = false;
                }
            }
            return new SceneChildrenCompileResult(blockStatsDeclared, structureCacheable);
        });
    }

    private static class SceneChildrenCompileResult {

        private final boolean blockStatsDeclared;
        private final boolean structureCacheable;

        private SceneChildrenCompileResult(boolean blockStatsDeclared, boolean structureCacheable) {
            this.blockStatsDeclared = blockStatsDeclared;
            this.structureCacheable = structureCacheable;
        }

        public boolean isBlockStatsDeclared() {
            return blockStatsDeclared;
        }

        public boolean isStructureCacheable() {
            return structureCacheable;
        }
    }

    private static class CountingErrorSink implements LytErrorSink {

        private final LytErrorSink delegate;
        private int errorCount;

        private CountingErrorSink(LytErrorSink delegate) {
            this.delegate = delegate;
        }

        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {
            errorCount++;
            delegate.appendError(compiler, text, node);
        }

        public int getErrorCount() {
            return errorCount;
        }
    }

    private static void applyImplicitBlockStats(LytGuidebookScene scene, boolean blockStatsDeclared) {
        if (blockStatsDeclared || scene.getLevel()
            .isEmpty()) {
            return;
        }
        scene.setBlockStatsEnabled(true);
        scene.setBlockStatsVisible(ModConfig.ui.sceneBlockStatsVisible);
        scene.setBlockStatsButtonEnabled(ModConfig.ui.sceneBlockStatsButtonEnabled);
        scene.setBlockStatsMode(BlockStatsMode.AUTO);
        scene.setBlockStatsCorner(BlockStatsCorner.TOP_RIGHT);
        scene.setBlockStatsDock(BlockStatsDock.INSIDE);
        scene.setBlockStatsShowNames(false);
        scene.setBlockStatsFilterMode(BlockStatsFilterMode.BLACKLIST);
        scene.setBlockStatsFilterKeys(Set.of());
    }

    private static Set<String> parseBlockStatsFilter(String raw) {
        Set<String> filter = new HashSet<>();
        if (raw == null || raw.trim()
            .isEmpty()) {
            return filter;
        }
        int start = -1;
        for (int index = 0, length = raw.length(); index <= length; index++) {
            char current = index < length ? raw.charAt(index) : ',';
            if (isBlockStatsFilterSeparator(current)) {
                if (start >= 0) {
                    String normalized = LytGuidebookScene.normalizeBlockStatsKey(raw.substring(start, index));
                    if (normalized != null) {
                        filter.add(normalized);
                    }
                    start = -1;
                }
            } else if (start < 0) {
                start = index;
            }
        }
        return filter;
    }

    private static boolean isBlockStatsFilterSeparator(char value) {
        return value == ',' || value == ';' || Character.isWhitespace(value);
    }

    private void compileBlockStatsElement(LytGuidebookScene scene, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        scene.setBlockStatsEnabled(true);
        scene.setBlockStatsVisible(
            MdxAttrs.getBoolean(compiler, errorSink, el, "visible", ModConfig.ui.sceneBlockStatsVisible));
        scene.setBlockStatsButtonEnabled(
            MdxAttrs.getBoolean(compiler, errorSink, el, "buttonEnabled", ModConfig.ui.sceneBlockStatsButtonEnabled));
        scene.setBlockStatsMode(
            BlockStatsMode.fromString(MdxAttrs.getString(compiler, errorSink, el, "mode", null), BlockStatsMode.AUTO));
        scene.setBlockStatsCorner(
            BlockStatsCorner
                .fromString(MdxAttrs.getString(compiler, errorSink, el, "corner", null), BlockStatsCorner.TOP_RIGHT));
        scene.setBlockStatsDock(
            BlockStatsDock
                .fromString(MdxAttrs.getString(compiler, errorSink, el, "dock", null), BlockStatsDock.INSIDE));
        scene.setBlockStatsShowNames(MdxAttrs.getBoolean(compiler, errorSink, el, "showNames", false));
        scene.setBlockStatsFilterMode(
            BlockStatsFilterMode.fromString(
                MdxAttrs.getString(compiler, errorSink, el, "filterMode", null),
                BlockStatsFilterMode.BLACKLIST));
        scene.setBlockStatsFilterKeys(
            parseBlockStatsFilter(MdxAttrs.getString(compiler, errorSink, el, "filter", null)));
        if (el.getAttribute("maxWidth") != null) {
            scene.setBlockStatsMaxWidth(
                MdxAttrs.getInt(compiler, errorSink, el, "maxWidth", LytGuidebookScene.BLOCK_STATS_DEFAULT_MAX_WIDTH));
        }
        if (el.getAttribute("maxHeight") != null) {
            scene.setBlockStatsMaxHeight(
                MdxAttrs
                    .getInt(compiler, errorSink, el, "maxHeight", LytGuidebookScene.BLOCK_STATS_DEFAULT_MAX_HEIGHT));
        }
        scene.clearManualBlockStatsEntries();
        boolean manualEntries = false;
        for (MdAstAnyContent child : el.children()) {
            MdxJsxElementFields childEl = unwrapSceneElement(child);
            if (childEl == null) {
                continue;
            }
            String name = childEl.name();
            if (!"BlockStat".equals(name)) {
                errorSink.appendError(compiler, "Unknown BlockStats element <" + name + ">", child);
                continue;
            }
            ItemStack item = getBlockStatItemStack(compiler, errorSink, childEl);
            if (item == null) {
                continue;
            }
            int count = Math.max(0, getBlockStatCount(compiler, errorSink, childEl));
            scene.addManualBlockStatsEntry(item, count);
            manualEntries = true;
        }
        if (manualEntries) {
            scene.setBlockStatsMode(BlockStatsMode.MANUAL);
        }
    }

    private static int getBlockStatCount(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        return Math.round(Math.max(0f, MdxAttrs.getFloat(compiler, errorSink, el, "count", 0f)));
    }

    private static ItemStack getBlockStatItemStack(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (el.getAttribute("item") != null && el.getAttribute("id") == null) {
            String item = MdxAttrs.getString(compiler, errorSink, el, "item", null);
            if (item == null || item.trim()
                .isEmpty()) {
                errorSink.appendError(compiler, "Missing item attribute.", el);
                return null;
            }
            ItemStack stack = IdUtils.resolveItemStack(
                item.trim(),
                compiler.getPageId()
                    .getResourceDomain());
            if (stack == null) {
                errorSink.appendError(compiler, "Missing item: " + item, el);
            }
            return stack;
        }
        var result = MdxAttrs.getRequiredItemStackAndId(compiler, errorSink, el);
        return result != null ? result.getRight() : null;
    }

    private void rebuildSceneForStructureLibSelection(LytGuidebookScene scene, PageCompiler compiler,
        MdxJsxFlowElement flow, boolean explicitCenter, @Nullable String structureName,
        StructureLibPreviewSelection selection) {
        if (scene == null) {
            return;
        }
        Map<String, StructureLibPreviewSelection> bindingSelections = new LinkedHashMap<>(
            scene.getStructureLibPreviewSelectionsByBinding());
        if (structureName != null && !structureName.trim()
            .isEmpty()) {
            bindingSelections.put(structureName.trim(), selection);
        } else if (!bindingSelections.isEmpty()) {
            String primaryBindingKey = bindingSelections.keySet()
                .iterator()
                .next();
            bindingSelections.put(primaryBindingKey, selection);
        }
        SavedCameraSettings savedCamera = scene.getCamera()
            .save();
        boolean annotationsVisible = scene.isAnnotationsVisible();
        boolean hatchHighlightEnabled = scene.isStructureLibHatchHighlightEnabled();
        boolean gridVisible = scene.isGridVisible();
        boolean blockStatsVisible = scene.isBlockStatsVisible();
        scene.captureInitialStructureStateIfAbsent();
        scene.getAnnotations()
            .clear();
        scene.clearSoundCues();
        scene.setHoveredBlock(null);
        scene.setHoveredEntity(null);
        scene.setHoveredStructureLibHatch(null);
        scene.clearAnnotationHover();
        scene.setStructureLibSceneMetadata(null);
        scene.seedStructureLibPreviewSelections(bindingSelections);
        scene.setLevel(new GuidebookLevel());
        scene.resetBlockStatsConfiguration();
        boolean blockStatsDeclared;
        try {
            blockStatsDeclared = withCurrentAnnotationScene(scene, () -> {
                List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(flow);
                boolean[] result = new boolean[1];
                compiler.withBlockTagChildrenSourceContext(
                    flow,
                    () -> result[0] = compileSceneChildrenWithCache(
                        scene,
                        compiler,
                        NOOP_ERROR_SINK,
                        children,
                        bindingSelections));
                return result[0];
            });
            scene.initializePonderTimelineBaseline();
            configureStructureLibSelectionListeners(scene, compiler, flow, explicitCenter);
        } finally {
            scene.clearSeededStructureLibPreviewSelections();
        }
        applyImplicitBlockStats(scene, blockStatsDeclared);
        scene.applyDefaultBlockStatsMaxSizeFromScene();
        scene.setBlockStatsVisible(blockStatsVisible);
        if (!scene.getLevel()
            .isEmpty() && !explicitCenter) {
            var center = scene.getLevel()
                .getCenter();
            scene.getCamera()
                .setRotationCenter(center[0], center[1], center[2]);
        }
        scene.setAnnotationsVisible(annotationsVisible);
        scene.setStructureLibHatchHighlightEnabled(hatchHighlightEnabled);
        scene.setGridVisible(gridVisible);
        scene.getCamera()
            .restore(savedCamera);
    }

    private void configureStructureLibSelectionListeners(LytGuidebookScene scene, PageCompiler compiler,
        MdxJsxFlowElement flow, boolean explicitCenter) {
        scene.setStructureLibSelectionChangeListener(
            selection -> rebuildSceneForStructureLibSelection(scene, compiler, flow, explicitCenter, null, selection));
        for (StructureLibSceneBinding binding : scene.getStructureLibBindings()) {
            StructureLibSceneMetadata metadata = binding.getMetadata();
            if (binding.getName() == null || metadata == null) {
                continue;
            }
            String structureName = binding.getName();
            binding.setSelectionChangeListener(
                selection -> rebuildSceneForStructureLibSelection(
                    scene,
                    compiler,
                    flow,
                    explicitCenter,
                    structureName,
                    selection));
        }
    }

    public static MdxJsxElementFields unwrapSceneElement(UnistNode node) {
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

    public static boolean isIgnorableNode(UnistNode node) {
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

    private static int countTags(String sourceText, String[] tagNames) {
        int count = 0;
        for (String tagName : tagNames) {
            count += countTag(sourceText, tagName);
        }
        return count;
    }

    private static int countTag(String sourceText, String tagName) {
        int count = 0;
        String opening = "<" + tagName;
        for (int searchIndex = 0; searchIndex >= 0 && searchIndex < sourceText.length();) {
            int matchIndex = sourceText.indexOf(opening, searchIndex);
            if (matchIndex < 0) {
                return count;
            }
            int boundaryIndex = matchIndex + opening.length();
            if (boundaryIndex >= sourceText.length() || isTagNameBoundary(sourceText.charAt(boundaryIndex))) {
                count++;
            }
            searchIndex = matchIndex + opening.length();
        }
        return count;
    }

    private static boolean isTagNameBoundary(char value) {
        return Character.isWhitespace(value) || value == '/' || value == '>';
    }
}
