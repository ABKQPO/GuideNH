package com.hfstudio.guidenh.guide.scene;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureFingerprintResolver;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistParent;

import cpw.mods.fml.common.FMLLog;

public class SceneTagCompiler extends BlockTagCompiler {

    private static final String[] SCENE_ROOT_TAG_NAMES = { "GameScene", "Scene" };
    private static final String[] SCENE_HEAVY_TAG_NAMES = { "ImportStructure", "ImportStructureLib", "PlaceBlock",
        "RemoveBlocks", "RemoveEntity", "ReplaceBlock", "Block", "Entity" };
    private static final int SCENE_HEAVY_ELEMENT_THRESHOLD = 8;

    private Map<String, SceneElementTagCompiler> elementCompilers = Collections.emptyMap();
    private final GuideSceneStructureFingerprintResolver structureFingerprintResolver = new GuideSceneStructureFingerprintResolver();

    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 180;

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
        // Width and height
        String rawWidth = MdxAttrs.getString(compiler, parent, el, "width", null);
        String rawHeight = MdxAttrs.getString(compiler, parent, el, "height", null);
        boolean explicitWidth = rawWidth != null;
        boolean explicitHeight = rawHeight != null;
        int w = MdxAttrs.getInt(compiler, parent, el, "width", DEFAULT_WIDTH);
        int h = MdxAttrs.getInt(compiler, parent, el, "height", DEFAULT_HEIGHT);

        // Zoom
        float zoom = MdxAttrs.getFloat(compiler, parent, el, "zoom", Float.NaN);
        boolean explicitZoom = !Float.isNaN(zoom);

        // Camera preset (yaw/pitch/roll)
        String perspective = MdxAttrs.getString(compiler, parent, el, "perspective", null);

        // Rotation overrides
        float rx = MdxAttrs.getFloat(compiler, parent, el, "rotateX", Float.NaN);
        float ry = MdxAttrs.getFloat(compiler, parent, el, "rotateY", Float.NaN);
        float rz = MdxAttrs.getFloat(compiler, parent, el, "rotateZ", Float.NaN);

        // Pan offsets (screen-space)
        float offX = MdxAttrs.getFloat(compiler, parent, el, "offsetX", Float.NaN);
        float offY = MdxAttrs.getFloat(compiler, parent, el, "offsetY", Float.NaN);
        boolean explicitOffX = !Float.isNaN(offX);
        boolean explicitOffY = !Float.isNaN(offY);

        // World-space rotation center
        float centerX = MdxAttrs.getFloat(compiler, parent, el, "centerX", Float.NaN);
        float centerY = MdxAttrs.getFloat(compiler, parent, el, "centerY", Float.NaN);
        float centerZ = MdxAttrs.getFloat(compiler, parent, el, "centerZ", Float.NaN);
        boolean explicitCenter = !Float.isNaN(centerX) || !Float.isNaN(centerY) || !Float.isNaN(centerZ);

        // Scene flags
        boolean interactive = MdxAttrs.getBoolean(compiler, parent, el, "interactive", true);
        boolean showBackground = resolveShowBackground(compiler, parent, el);
        boolean allowLayerSlider = MdxAttrs
            .getBoolean(compiler, parent, el, "allowLayerSlider", ModConfig.ui.sceneLayerSliderEnabled);
        boolean gridButtonEnabled = MdxAttrs.getBoolean(compiler, parent, el, "gridButtonEnabled", true);
        boolean showGrid = MdxAttrs.getBoolean(compiler, parent, el, "showGrid", false);

        // Raw source text of children (preserves BlockStats and all scene element markup)
        String childrenSource = compiler.getBlockTagChildrenSource(el);

        // Pre-parse children source at compile time (pure function -- no I/O, no registry).
        // SceneScript uses the pre-parsed AST instead of re-running MdAst.fromMarkdown().
        MdAstRoot preParsedAst = null;
        if (childrenSource != null && !childrenSource.isEmpty()) {
            try {
                preParsedAst = MdAst.fromMarkdown(childrenSource, GuideMarkdownOptions.runtime());
                MdAstToMdxConverter.convert(preParsedAst, Collections.emptyMap());
            } catch (RuntimeException e) {
                FMLLog.getLogger().warn("[GuideNH] [SceneTagCompiler] Failed to pre-parse scene children", e);
            }
        }

        // Store element compilers from ExtensionCollection in placeholder
        var sceneElementCompilers = compiler.getExtensions().get(SceneElementTagCompiler.EXTENSION_POINT);

        // Create placeholder block that carries all scene config to SceneScript
        String styleClass = "GameScene".equals(el.name()) ? "GameScene" : "Scene";
        ScenePlaceholder placeholder = new ScenePlaceholder(
            w, h, explicitWidth, explicitHeight,
            zoom, explicitZoom,
            perspective,
            rx, ry, rz,
            offX, offY, explicitOffX, explicitOffY,
            centerX, centerY, centerZ, explicitCenter,
            interactive, showBackground,
            allowLayerSlider, gridButtonEnabled, showGrid,
            childrenSource,
            compiler.getPageId().getResourceDomain(),
            compiler.getSourcePack(),
            preParsedAst,
            sceneElementCompilers
        );
        placeholder.setStyleClass(styleClass);
        placeholder.setStyle(LytParagraph.PLACEHOLDER_STYLE);
        placeholder.appendText("[" + styleClass + "]");
        parent.append(placeholder);
    }

    private boolean resolveShowBackground(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        return MdxAttrs.getBoolean(compiler, parent, el, "showBackground", true);
    }

    // ---- Scene data holder ----

    /**
     * Placeholder block that stores all extracted scene configuration for deferred scene creation
     * by {@code SceneScript}. Extends LytParagraph so it lives in the LytNode tree and can receive
     * MOUNT dispatch.
     */
    public static class ScenePlaceholder extends LytParagraph {

        public final int width;
        public final int height;
        public final boolean explicitWidth;
        public final boolean explicitHeight;
        public final float zoom;
        public final boolean explicitZoom;
        @Nullable public final String perspective;
        public final float rotateX;
        public final float rotateY;
        public final float rotateZ;
        public final float offsetX;
        public final float offsetY;
        public final boolean explicitOffsetX;
        public final boolean explicitOffsetY;
        public final float centerX;
        public final float centerY;
        public final float centerZ;
        public final boolean explicitCenter;
        public final boolean interactive;
        public final boolean showBackground;
        public final boolean allowLayerSlider;
        public final boolean gridButtonEnabled;
        public final boolean showGrid;
        @Nullable public final String childrenSource;
        public final String pageDomain;
        public final String sourcePack;
        @Nullable public final MdAstRoot childrenAst;
        @Nullable
        public final List<SceneElementTagCompiler> sceneElementCompilers;

        public ScenePlaceholder(
            int width, int height,
            boolean explicitWidth, boolean explicitHeight,
            float zoom, boolean explicitZoom,
            @Nullable String perspective,
            float rotateX, float rotateY, float rotateZ,
            float offsetX, float offsetY,
            boolean explicitOffsetX, boolean explicitOffsetY,
            float centerX, float centerY, float centerZ,
            boolean explicitCenter,
            boolean interactive, boolean showBackground,
            boolean allowLayerSlider, boolean gridButtonEnabled,
            boolean showGrid,
            @Nullable String childrenSource,
            String pageDomain,
            String sourcePack,
            @Nullable MdAstRoot childrenAst,
            @Nullable List<SceneElementTagCompiler> sceneElementCompilers) {
            this.width = width;
            this.height = height;
            this.explicitWidth = explicitWidth;
            this.explicitHeight = explicitHeight;
            this.zoom = zoom;
            this.explicitZoom = explicitZoom;
            this.perspective = perspective;
            this.rotateX = rotateX;
            this.rotateY = rotateY;
            this.rotateZ = rotateZ;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.explicitOffsetX = explicitOffsetX;
            this.explicitOffsetY = explicitOffsetY;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.explicitCenter = explicitCenter;
            this.interactive = interactive;
            this.showBackground = showBackground;
            this.allowLayerSlider = allowLayerSlider;
            this.gridButtonEnabled = gridButtonEnabled;
            this.showGrid = showGrid;
            this.childrenSource = childrenSource;
            this.pageDomain = pageDomain;
            this.sourcePack = sourcePack;
            this.childrenAst = childrenAst;
            this.sceneElementCompilers = sceneElementCompilers;
        }
    }

    // ---- Utility methods (pure, kept for script use) ----

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
