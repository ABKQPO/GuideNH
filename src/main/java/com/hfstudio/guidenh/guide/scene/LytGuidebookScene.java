package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.compat.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.compat.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.compat.structurelib.StructureLibTooltipContentBuilder;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.internal.ui.GuideSliderRenderer;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBlockFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookTileEntityLoader;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframe;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeBlockChange;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeCameraState;
import com.hfstudio.guidenh.guide.scene.ponder.PonderSceneData;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityRayPicker;
import com.hfstudio.guidenh.guide.scene.support.GuideGregTechTileSupport;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LytGuidebookScene extends LytBlock {

    public static final float DRAG_ROTATE_SENSITIVITY = 0.5f;
    public static final float WHEEL_ZOOM_STEP = 1.1f;
    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 10f;
    public static final int SCENE_SLIDER_AREA_HEIGHT = 14;
    public static final int SCENE_SLIDER_SIDE_PADDING = 8;
    public static final float ORIGIN_AXIS_LENGTH = 1.5f;
    public static final float ORIGIN_AXIS_THICKNESS = 2.0f;
    public static final int ORIGIN_X_AXIS_COLOR = 0xFFFF5A5A;
    public static final int ORIGIN_Y_AXIS_COLOR = 0xFF67E26C;
    public static final int ORIGIN_Z_AXIS_COLOR = 0xFF64A8FF;
    public static final ResolvedTextStyle VISIBLE_LAYER_SLIDER_TEXT_STYLE = DefaultStyles.BODY_TEXT
        .mergeWith(DefaultStyles.BASE_STYLE);
    public static final ResolvedTextStyle STRUCTURELIB_TIER_SLIDER_TEXT_STYLE = DefaultStyles.BODY_TEXT
        .mergeWith(DefaultStyles.BASE_STYLE);
    public static final ResolvedTextStyle STRUCTURELIB_CHANNEL_SLIDER_TEXT_STYLE = DefaultStyles.BODY_TEXT
        .mergeWith(DefaultStyles.BASE_STYLE);

    private int dragButton = -1;
    private int dragLastX;
    private int dragLastY;
    private boolean draggingVisibleLayerSlider;
    private boolean draggingStructureLibTierSlider;
    @Nullable
    private String draggingStructureLibChannelId;
    private boolean draggingPonderBar;

    @Nullable
    private PonderSceneData ponderSceneData;
    private List<List<SceneAnnotation>> ponderKeyframeAnnotationSets = new ArrayList<>();
    private int ponderCurrentTick = 0;
    private boolean ponderPaused = false;
    private boolean ponderFinished = false;
    private float ponderCamZoom = 1f;
    private float ponderCamRotX = 0f;
    private float ponderCamRotY = 0f;
    private float ponderCamRotZ = 0f;
    private float ponderCamOffX = 0f;
    private float ponderCamOffY = 0f;
    private final List<SceneAnnotation> ponderActiveAnnotations = new ArrayList<>();
    private final List<SceneAnnotation> ponderOutgoingAnnotations = new ArrayList<>();
    private int ponderOutgoingFadeTick = 0;
    private final List<GuidebookSceneParticle> ponderSceneParticles = new ArrayList<>();
    private final Random ponderParticleRng = new Random();
    private int ponderLastKeyframeIdx = -2;
    private int ponderAnnotationFadeTick = 5;
    private final Map<Long, PonderBlockInfo> ponderBlockSnapshot = new LinkedHashMap<>();
    @Nullable
    private LytRect cachedPonderBarTrackRect;
    @Nullable
    private LytRect cachedPonderBarHitRect;
    private int cachedPonderBtnAbsX;
    private int cachedPonderBtnAbsY;

    private boolean interactive = true;
    private boolean sceneButtonsVisible = true;
    private boolean bottomControlsVisible = true;
    private boolean reserveBottomControlArea = true;
    private boolean visibleLayerSliderEnabled;
    private boolean forceOriginAxesVisible;
    private boolean forceHideOriginAxes;

    public static int SCENE_BG_COLOR = 0xFF0A0A10;
    public static int SCENE_BORDER_COLOR = 0xFF303040;

    public static final ResourceLocation BUTTONS_TEXTURE = new ResourceLocation(
        "guidenh",
        "textures/guide/buttons.png");

    public static final GuideIconButton.Role[] SCENE_BUTTONS_SHOWN = { GuideIconButton.Role.HIDE_ANNOTATIONS,
        GuideIconButton.Role.ZOOM_IN, GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };
    public static final GuideIconButton.Role[] SCENE_BUTTONS_HIDDEN = { GuideIconButton.Role.SHOW_ANNOTATIONS,
        GuideIconButton.Role.ZOOM_IN, GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };
    // Shown when the scene has no annotations at all: drop the annotation toggle entirely.
    public static final GuideIconButton.Role[] SCENE_BUTTONS_NO_ANNOTATIONS = { GuideIconButton.Role.ZOOM_IN,
        GuideIconButton.Role.ZOOM_OUT, GuideIconButton.Role.RESET_VIEW };

    public static final int PONDER_BTN_TOTAL_WIDTH = SCENE_SLIDER_AREA_HEIGHT * 3;
    public static final int PONDER_PROGRESS_TRACK_COLOR = 0x40FFFFFF;
    public static final int PONDER_PROGRESS_FILL_COLOR = 0xAA1CB4E9;
    public static final int PONDER_KEYFRAME_NODE_COLOR = 0xC0AAAADD;
    public static final int PONDER_KEYFRAME_NODE_HOVER_COLOR = 0xFFC0C0FF;

    public static final int DEFAULT_WIDTH = 256;
    public static final int DEFAULT_HEIGHT = 192;

    private GuidebookLevel level = new GuidebookLevel();
    private CameraSettings camera = new CameraSettings();
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int sceneBackgroundColor = SCENE_BG_COLOR;
    private int sceneBorderColor = SCENE_BORDER_COLOR;
    @Nullable
    private LytSize cameraViewportOverride;
    private final List<SceneAnnotation> annotations = new ArrayList<>();

    // Reuse annotation partitions instead of allocating new lists every frame.
    private final List<InWorldAnnotation> inWorldScratch = new ArrayList<>();
    private final List<OverlayAnnotation> overlayScratch = new ArrayList<>();

    // Reuse hovered-block overlay objects across frames.
    private final Vector3f hoverBoxMin = new Vector3f();
    private final Vector3f hoverBoxMax = new Vector3f();
    private final Vector3f projectedCornerScratch = new Vector3f();
    private final Vector3f projectedLineFromScratch = new Vector3f();
    private final Vector3f projectedLineToScratch = new Vector3f();
    private final float[] pickRayScratch = new float[6];
    private final ConstantColor hoverBoxColor = new ConstantColor(0xFFFFFFFF);

    private final ConstantColor originXAxisColor = new ConstantColor(ORIGIN_X_AXIS_COLOR);
    private final ConstantColor originYAxisColor = new ConstantColor(ORIGIN_Y_AXIS_COLOR);
    private final ConstantColor originZAxisColor = new ConstantColor(ORIGIN_Z_AXIS_COLOR);
    private final InWorldBoxAnnotation hoverBoxAnnotation = new InWorldBoxAnnotation(
        hoverBoxMin,
        hoverBoxMax,
        hoverBoxColor,
        1f);
    private final InWorldLineAnnotation originXAxisAnnotation = createOriginAxisAnnotation(
        new Vector3f(ORIGIN_AXIS_LENGTH, 0.0f, 0.0f),
        originXAxisColor);
    private final InWorldLineAnnotation originYAxisAnnotation = createOriginAxisAnnotation(
        new Vector3f(0.0f, ORIGIN_AXIS_LENGTH, 0.0f),
        originYAxisColor);
    private final InWorldLineAnnotation originZAxisAnnotation = createOriginAxisAnnotation(
        new Vector3f(0.0f, 0.0f, ORIGIN_AXIS_LENGTH),
        originZAxisColor);
    private LytRect cachedOverlayViewport;
    private LytRect cachedScreenRect;
    private LytRect cachedSceneRect;
    private LytRect cachedVisibleLayerSliderRect;
    private LytRect cachedVisibleLayerSliderHitRect;
    private LytRect cachedTierSliderRect;
    private LytRect cachedTierSliderHitRect;
    private final Map<String, LytRect> cachedChannelSliderRects = new LinkedHashMap<>();
    private final Map<String, LytRect> cachedChannelSliderHitRects = new LinkedHashMap<>();
    private int sceneButtonsAbsX;
    private int sceneButtonsAbsY;
    private boolean cachedSceneButtonsVisible = true;
    private boolean cachedSceneHasAnnotations = true;
    private boolean cachedSceneHasStructureLibHatches;
    private GuideIconButton.Role[] cachedSceneButtonRoles = SCENE_BUTTONS_SHOWN;

    private boolean annotationsVisible = true;
    @Nullable
    private Integer visibleLayerOverride;
    @Nullable
    private StructureLibSceneMetadata structureLibSceneMetadata;
    private int structureLibCurrentTier = 1;
    private final LinkedHashMap<String, Integer> structureLibChannelOverrides = new LinkedHashMap<>();
    private boolean structureLibHatchHighlightEnabled;
    @Nullable
    private Consumer<StructureLibPreviewSelection> structureLibSelectionChangeListener;
    @Nullable
    private StructureLibPreviewSelection pendingStructureLibPreviewSelection;
    private boolean initialStateCaptured;
    private boolean initialAnnotationsVisible = true;
    @Nullable
    private Integer initialVisibleLayerOverride;
    private int initialStructureLibCurrentTier = 1;
    private final LinkedHashMap<String, Integer> initialStructureLibChannelOverrides = new LinkedHashMap<>();
    private boolean initialStructureLibHatchHighlightEnabled;

    private float[] initialCam = new float[] { 1f, 0f, 0f, 0f, 0f, 0f };

    private int @Nullable [] hoveredBlock;
    @Nullable
    private AxisAlignedBB hoveredBlockBounds;
    @Nullable
    private MovingObjectPosition hoveredBlockHitResult;
    @Nullable
    private Entity hoveredEntity;
    @Nullable
    private AxisAlignedBB hoveredEntityBounds;
    @Nullable
    private MovingObjectPosition hoveredEntityHitResult;
    private int @Nullable [] hoveredStructureLibHatch;

    public static class PickRay {

        private final Vec3 start;
        private final Vec3 end;

        private PickRay(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }
    }

    public static class BlockPickResult {

        private final int[] pos;
        private final AxisAlignedBB bounds;
        private final MovingObjectPosition hitResult;
        private final double distanceSq;

        private BlockPickResult(int[] pos, @Nullable AxisAlignedBB bounds, @Nullable MovingObjectPosition hitResult,
            double distanceSq) {
            this.pos = pos;
            this.bounds = bounds;
            this.hitResult = hitResult;
            this.distanceSq = distanceSq;
        }
    }

    public LytGuidebookScene() {
        camera.setPerspectivePreset(PerspectivePreset.ISOMETRIC_NORTH_EAST);
        snapshotInitialCamera();
    }

    public GuidebookLevel getLevel() {
        return level;
    }

    public void setLevel(GuidebookLevel level) {
        this.level = level != null ? level : new GuidebookLevel();
        if (!this.level.isEmpty()) {
            var c = this.level.getCenter();
            camera.setRotationCenter(c[0], c[1], c[2]);
        }
        snapshotInitialCamera();
        clearLayerDrivenHoverState();
    }

    public CameraSettings getCamera() {
        return camera;
    }

    public void setCamera(CameraSettings camera) {
        this.camera = camera != null ? camera : new CameraSettings();
        snapshotInitialCamera();
    }

    public void snapshotInitialCamera() {
        initialCam[0] = camera.getZoom();
        initialCam[1] = camera.getRotationX();
        initialCam[2] = camera.getRotationY();
        initialCam[3] = camera.getRotationZ();
        initialCam[4] = camera.getOffsetX();
        initialCam[5] = camera.getOffsetY();
    }

    public void captureInitialInteractiveState() {
        initialStateCaptured = true;
        initialAnnotationsVisible = annotationsVisible;
        initialVisibleLayerOverride = visibleLayerOverride;
        initialStructureLibCurrentTier = structureLibCurrentTier;
        initialStructureLibChannelOverrides.clear();
        initialStructureLibChannelOverrides.putAll(structureLibChannelOverrides);
        initialStructureLibHatchHighlightEnabled = structureLibHatchHighlightEnabled;
    }

    public void resetInteractiveState() {
        dragButton = -1;
        draggingVisibleLayerSlider = false;
        draggingStructureLibTierSlider = false;
        draggingStructureLibChannelId = null;
        draggingPonderBar = false;
        hoveredBlock = null;
        hoveredBlockBounds = null;
        hoveredBlockHitResult = null;
        hoveredEntity = null;
        hoveredEntityBounds = null;
        hoveredEntityHitResult = null;
        hoveredStructureLibHatch = null;
        clearAnnotationHover();
        if (initialStateCaptured) {
            annotationsVisible = initialAnnotationsVisible;
            visibleLayerOverride = initialVisibleLayerOverride;
            structureLibCurrentTier = initialStructureLibCurrentTier;
            structureLibChannelOverrides.clear();
            structureLibChannelOverrides.putAll(initialStructureLibChannelOverrides);
            structureLibHatchHighlightEnabled = initialStructureLibHatchHighlightEnabled;
        }
        if (ponderSceneData != null) {
            ponderCurrentTick = 0;
            ponderPaused = true;
            ponderFinished = false;
            ponderAnnotationFadeTick = 5;
            updatePonderState();
        }
        pendingStructureLibPreviewSelection = null;
        clearCachedVisibleLayerSliderRects();
        clearCachedTierSliderRects();
        clearCachedChannelSliderRects();
        resetViewToInitialCamera();
    }

    public int getSceneWidth() {
        return width;
    }

    public int getSceneHeight() {
        return height;
    }

    public void setSceneSize(int width, int height) {
        this.width = Math.max(16, width);
        this.height = Math.max(16, height);
    }

    public int getSceneBackgroundColor() {
        return sceneBackgroundColor;
    }

    public void setSceneBackgroundColor(int sceneBackgroundColor) {
        this.sceneBackgroundColor = sceneBackgroundColor;
    }

    public int getSceneBorderColor() {
        return sceneBorderColor;
    }

    public void setSceneBorderColor(int sceneBorderColor) {
        this.sceneBorderColor = sceneBorderColor;
    }

    public void setCameraViewportOverride(int width, int height) {
        this.cameraViewportOverride = new LytSize(Math.max(16, width), Math.max(16, height));
    }

    public void clearCameraViewportOverride() {
        this.cameraViewportOverride = null;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public boolean isSceneButtonsVisible() {
        return sceneButtonsVisible;
    }

    public void setSceneButtonsVisible(boolean sceneButtonsVisible) {
        this.sceneButtonsVisible = sceneButtonsVisible;
    }

    public boolean isBottomControlsVisible() {
        return bottomControlsVisible;
    }

    public void setBottomControlsVisible(boolean bottomControlsVisible) {
        this.bottomControlsVisible = bottomControlsVisible;
    }

    public boolean isReserveBottomControlArea() {
        return reserveBottomControlArea;
    }

    public void setReserveBottomControlArea(boolean reserveBottomControlArea) {
        this.reserveBottomControlArea = reserveBottomControlArea;
    }

    public boolean isVisibleLayerSliderEnabled() {
        return visibleLayerSliderEnabled;
    }

    public void setVisibleLayerSliderEnabled(boolean visibleLayerSliderEnabled) {
        this.visibleLayerSliderEnabled = visibleLayerSliderEnabled;
    }

    public boolean isForceOriginAxesVisible() {
        return forceOriginAxesVisible;
    }

    public void setForceOriginAxesVisible(boolean forceOriginAxesVisible) {
        this.forceOriginAxesVisible = forceOriginAxesVisible;
    }

    public boolean isForceHideOriginAxes() {
        return forceHideOriginAxes;
    }

    public void setForceHideOriginAxes(boolean forceHideOriginAxes) {
        this.forceHideOriginAxes = forceHideOriginAxes;
    }

    public boolean hasVisibleLayerData() {
        return getVisibleLayerCount() > 0;
    }

    public int getCurrentVisibleLayer() {
        return resolveCurrentVisibleLayer();
    }

    public void setVisibleLayer(int layer) {
        setVisibleLayerInternal(layer, true);
    }

    public void setVisibleLayerSilently(int layer) {
        setVisibleLayerInternal(layer, false);
    }

    private void setVisibleLayerInternal(int layer, boolean preserveHoverState) {
        visibleLayerOverride = Math.max(0, layer);
        clearLayerDrivenHoverState();
    }

    private int getVisibleLayerCount() {
        if (level == null || level.isEmpty()) {
            return 0;
        }
        int[] bounds = level.getBounds();
        return Math.max(1, bounds[4] - bounds[1] + 1);
    }

    private int getVisibleLayerMinY() {
        if (level == null || level.isEmpty()) {
            return 0;
        }
        return level.getBounds()[1];
    }

    private int resolveCurrentVisibleLayer() {
        int layerCount = getVisibleLayerCount();
        if (layerCount <= 0) {
            return 0;
        }
        int maxLayer = layerCount;
        if (visibleLayerOverride == null) {
            return 0;
        }
        int requestedLayer = visibleLayerOverride;
        if (requestedLayer < 0) {
            return 0;
        }
        return Math.min(requestedLayer, maxLayer);
    }

    @Nullable
    private Integer resolveVisibleLayerY() {
        if (ponderSceneData != null && !ponderPaused && !ponderFinished) {
            // During active playback the keyframe layer takes precedence.
            int activeIdx = ponderSceneData.resolveActiveKeyframeIndex(ponderCurrentTick);
            if (activeIdx >= 0) {
                PonderKeyframe kf = ponderSceneData.getKeyframe(activeIdx);
                if (kf != null) {
                    Integer kfLayer = kf.getLayer();
                    if (kfLayer == null) return null;
                    if (!hasVisibleLayerData()) return null;
                    return getVisibleLayerMinY() + Math.max(0, kfLayer);
                }
            }
            return null;
        }
        // Paused, finished, or no ponder: respect the user-controlled layer slider.
        if (!hasVisibleLayerData()) {
            return null;
        }
        int currentLayer = resolveCurrentVisibleLayer();
        if (currentLayer <= 0) {
            return null;
        }
        return getVisibleLayerMinY() + currentLayer - 1;
    }

    private boolean isBlockVisibleForCurrentLayer(int y) {
        Integer visibleLayerY = resolveVisibleLayerY();
        return visibleLayerY == null || y == visibleLayerY;
    }

    private boolean isAnnotationVisibleForCurrentLayer(SceneAnnotation annotation) {
        Integer visibleLayerY = resolveVisibleLayerY();
        if (visibleLayerY == null || annotation == null) {
            return true;
        }
        if (annotation instanceof DiamondAnnotation diamondAnnotation) {
            return isPointWithinVisibleLayer(diamondAnnotation.getPos().y, visibleLayerY);
        }
        if (annotation instanceof InWorldBoxAnnotation boxAnnotation) {
            return intersectsVisibleLayer(boxAnnotation.min().y, boxAnnotation.max().y, visibleLayerY);
        }
        if (annotation instanceof InWorldLineAnnotation lineAnnotation) {
            return intersectsVisibleLayer(lineAnnotation.from().y, lineAnnotation.to().y, visibleLayerY);
        }
        if (annotation instanceof InWorldBlockFaceOverlayAnnotation overlayAnnotation) {
            return overlayAnnotation.getBlockY() == visibleLayerY;
        }
        return true;
    }

    public static boolean isPointWithinVisibleLayer(float y, int visibleLayerY) {
        return y >= visibleLayerY && y < visibleLayerY + 1f;
    }

    public static boolean intersectsVisibleLayer(float fromY, float toY, int visibleLayerY) {
        float minY = Math.min(fromY, toY);
        float maxY = Math.max(fromY, toY);
        return maxY >= visibleLayerY && minY < visibleLayerY + 1f;
    }

    private void clearLayerDrivenHoverState() {
        hoveredBlock = null;
        hoveredBlockBounds = null;
        hoveredBlockHitResult = null;
        hoveredEntity = null;
        hoveredEntityBounds = null;
        hoveredEntityHitResult = null;
        hoveredStructureLibHatch = null;
        clearAnnotationHover();
    }

    public void setStructureLibSceneMetadata(@Nullable StructureLibSceneMetadata structureLibSceneMetadata) {
        this.structureLibSceneMetadata = structureLibSceneMetadata;
        this.structureLibChannelOverrides.clear();
        if (structureLibSceneMetadata == null) {
            this.structureLibCurrentTier = 1;
            this.structureLibHatchHighlightEnabled = false;
            this.hoveredStructureLibHatch = null;
            return;
        }
        StructureLibSceneMetadata.TierData tierData = structureLibSceneMetadata.getTierData();
        this.structureLibCurrentTier = tierData != null ? tierData.getCurrentValue() : 1;
        for (StructureLibSceneMetadata.ChannelData channelData : structureLibSceneMetadata.getChannelDataList()) {
            if (channelData != null && channelData.getCurrentValue() > 0) {
                this.structureLibChannelOverrides.put(channelData.getChannelId(), channelData.getCurrentValue());
            }
        }
        if (!structureLibSceneMetadata.hasHatchTooltipData()) {
            this.structureLibHatchHighlightEnabled = false;
            this.hoveredStructureLibHatch = null;
        }
    }

    @Nullable
    public StructureLibSceneMetadata getStructureLibSceneMetadata() {
        return structureLibSceneMetadata;
    }

    public boolean hasStructureLibSceneMetadata() {
        return structureLibSceneMetadata != null;
    }

    public int getStructureLibCurrentTier() {
        return structureLibCurrentTier;
    }

    public void setStructureLibCurrentTier(int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureLibCurrentTier, true);
    }

    public void setStructureLibCurrentTierSilently(int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureLibCurrentTier, false);
    }

    public void setStructureLibSelectionChangeListener(
        @Nullable Consumer<StructureLibPreviewSelection> structureLibSelectionChangeListener) {
        this.structureLibSelectionChangeListener = structureLibSelectionChangeListener;
    }

    private void setStructureLibCurrentTierInternal(int structureLibCurrentTier, boolean notifyListener) {
        StructureLibSceneMetadata.TierData tierData = getStructureLibTierData();
        int previousValue = this.structureLibCurrentTier;
        if (tierData == null) {
            this.structureLibCurrentTier = Math.max(1, structureLibCurrentTier);
        } else {
            this.structureLibCurrentTier = clampChannelValue(
                structureLibCurrentTier,
                tierData.getMinValue(),
                tierData.getMaxValue());
        }
        if (notifyListener && previousValue != this.structureLibCurrentTier) {
            notifyStructureLibSelectionChanged();
        }
    }

    public int getStructureLibChannelValue(String channelId) {
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        if (normalized == null) {
            return 0;
        }
        Integer value = structureLibChannelOverrides.get(normalized);
        return value != null ? value : 0;
    }

    public void setStructureLibChannelValue(String channelId, int value) {
        setStructureLibChannelValueInternal(channelId, value, true);
    }

    public void setStructureLibChannelValueSilently(String channelId, int value) {
        setStructureLibChannelValueInternal(channelId, value, false);
    }

    private void setStructureLibChannelValueInternal(String channelId, int value, boolean notifyListener) {
        StructureLibSceneMetadata.ChannelData channelData = getStructureLibChannelData(channelId);
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        if (normalized == null) {
            return;
        }
        int previousValue = getStructureLibChannelValue(normalized);
        int nextValue;
        if (channelData == null) {
            nextValue = Math.max(0, value);
        } else {
            nextValue = clampChannelValue(value, channelData.getMinValue(), channelData.getMaxValue());
        }
        if (nextValue > 0) {
            structureLibChannelOverrides.put(normalized, nextValue);
        } else {
            structureLibChannelOverrides.remove(normalized);
        }
        if (notifyListener && previousValue != nextValue) {
            notifyStructureLibSelectionChanged();
        }
    }

    public StructureLibPreviewSelection getStructureLibPreviewSelection() {
        return new StructureLibPreviewSelection(structureLibCurrentTier, structureLibChannelOverrides);
    }

    @Nullable
    public StructureLibPreviewSelection getPendingStructureLibPreviewSelection() {
        return pendingStructureLibPreviewSelection;
    }

    public void setPendingStructureLibPreviewSelection(@Nullable StructureLibPreviewSelection pendingSelection) {
        this.pendingStructureLibPreviewSelection = pendingSelection;
    }

    private void notifyStructureLibSelectionChanged() {
        if (structureLibSelectionChangeListener != null) {
            structureLibSelectionChangeListener.accept(getStructureLibPreviewSelection());
        }
    }

    public boolean hasStructureLibTierData() {
        StructureLibSceneMetadata.TierData tierData = getStructureLibTierData();
        return tierData != null && tierData.isSelectable();
    }

    public boolean hasStructureLibChannelData() {
        if (structureLibSceneMetadata == null) {
            return false;
        }
        for (StructureLibSceneMetadata.ChannelData channelData : structureLibSceneMetadata.getChannelDataList()) {
            if (channelData.isSelectable()) {
                return true;
            }
        }
        return false;
    }

    public int getBottomControlAreaHeight() {
        if (!bottomControlsVisible) {
            return 0;
        }
        return ponderControlAreaHeight() + visibleLayerSliderAreaHeight()
            + structureLibTierSliderAreaHeight()
            + structureLibChannelSliderAreaHeight();
    }

    public boolean hasStructureLibHatchData() {
        return structureLibSceneMetadata != null && structureLibSceneMetadata.hasHatchTooltipData();
    }

    public boolean isStructureLibHatchHighlightEnabled() {
        return structureLibHatchHighlightEnabled;
    }

    @Nullable
    public ContentTooltip createStructureLibTooltipForHoveredBlock(String blockName, boolean shiftDown) {
        if (structureLibSceneMetadata == null) {
            return null;
        }
        int[] tooltipPos = hoveredStructureLibHatch != null ? hoveredStructureLibHatch : hoveredBlock;
        if (tooltipPos == null) {
            return null;
        }
        if (!isBlockVisibleForCurrentLayer(tooltipPos[1])) {
            return null;
        }
        StructureLibSceneMetadata.BlockTooltipData tooltipData = structureLibSceneMetadata
            .getBlockTooltipData(tooltipPos[0], tooltipPos[1], tooltipPos[2]);
        if (tooltipData == null || !tooltipData.hasAdditionalTooltipContent()) {
            return null;
        }
        return StructureLibTooltipContentBuilder.build(
            blockName,
            tooltipData.getStructureLibDescription(),
            shiftDown,
            tooltipData.getBlockCandidates(),
            tooltipData.getHatchDescriptionLines(),
            tooltipData.getHatchCandidates());
    }

    public void setStructureLibHatchHighlightEnabled(boolean structureLibHatchHighlightEnabled) {
        this.structureLibHatchHighlightEnabled = structureLibHatchHighlightEnabled && hasStructureLibHatchData();
        if (!this.structureLibHatchHighlightEnabled) {
            this.hoveredStructureLibHatch = null;
        }
    }

    public void addAnnotation(SceneAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    public List<SceneAnnotation> getAnnotations() {
        return annotations;
    }

    @Nullable
    public SceneAnnotation updateAnnotationHover(int mouseX, int mouseY) {
        if (!annotationsVisible) {
            clearAnnotationHover();
            return null;
        }
        SceneAnnotation hit = null;
        LytRect viewport = cachedScreenRect = updateCachedRect(cachedScreenRect, lastAbsX, lastAbsY, lastW, lastH);
        // Iterate top-down: overlays sit on top of in-world geometry.
        for (int i = annotations.size() - 1; i >= 0; i--) {
            var a = annotations.get(i);
            boolean hovered = false;
            if (hit == null && isAnnotationVisibleForCurrentLayer(a)) {
                if (a instanceof OverlayAnnotation ov) {
                    hovered = ov.getBoundingRect(camera, viewport)
                        .contains(mouseX, mouseY);
                } else if (a instanceof InWorldBoxAnnotation box) {
                    hovered = boxScreenRectContains(box, viewport, mouseX, mouseY);
                } else if (a instanceof InWorldLineAnnotation line) {
                    hovered = lineScreenContains(line, viewport, mouseX, mouseY);
                }
            }
            a.setHovered(hovered);
            if (hovered) hit = a;
        }
        return hit;
    }

    public static final int LINE_HOVER_TOLERANCE_PX = 4;
    public static final int LINE_HOVER_TOLERANCE_PX_SQUARED = LINE_HOVER_TOLERANCE_PX * LINE_HOVER_TOLERANCE_PX;

    private boolean boxScreenRectContains(InWorldBoxAnnotation box, LytRect viewport, int mouseX, int mouseY) {
        var min = box.min();
        var max = box.max();
        int cx = viewport.x() + viewport.width() / 2;
        int cy = viewport.y() + viewport.height() / 2;
        int minSx = Integer.MAX_VALUE, minSy = Integer.MAX_VALUE;
        int maxSx = Integer.MIN_VALUE, maxSy = Integer.MIN_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            float x = ((corner & 1) == 0) ? min.x : max.x;
            float y = ((corner & 2) == 0) ? min.y : max.y;
            float z = ((corner & 4) == 0) ? min.z : max.z;
            var projected = camera.worldToScreen(x, y, z, projectedCornerScratch);
            int sx = cx + Math.round(projected.x);
            int sy = cy + Math.round(projected.y);
            if (sx < minSx) minSx = sx;
            if (sy < minSy) minSy = sy;
            if (sx > maxSx) maxSx = sx;
            if (sy > maxSy) maxSy = sy;
        }
        return mouseX >= minSx && mouseX <= maxSx && mouseY >= minSy && mouseY <= maxSy;
    }

    private boolean lineScreenContains(InWorldLineAnnotation line, LytRect viewport, int mouseX, int mouseY) {
        int cx = viewport.x() + viewport.width() / 2;
        int cy = viewport.y() + viewport.height() / 2;
        var a = camera.worldToScreen(line.from().x, line.from().y, line.from().z, projectedLineFromScratch);
        var b = camera.worldToScreen(line.to().x, line.to().y, line.to().z, projectedLineToScratch);
        float ax = cx + a.x, ay = cy + a.y;
        float bx = cx + b.x, by = cy + b.y;
        float dx = bx - ax, dy = by - ay;
        float lenSq = dx * dx + dy * dy;
        float t = lenSq < 1e-4f ? 0f : Math.max(0f, Math.min(1f, ((mouseX - ax) * dx + (mouseY - ay) * dy) / lenSq));
        float px = ax + t * dx, py = ay + t * dy;
        float ex = mouseX - px, ey = mouseY - py;
        return ex * ex + ey * ey <= LINE_HOVER_TOLERANCE_PX_SQUARED;
    }

    public void clearAnnotationHover() {
        for (var a : annotations) a.setHovered(false);
    }

    public boolean isAnnotationsVisible() {
        return annotationsVisible;
    }

    public void setAnnotationsVisible(boolean visible) {
        this.annotationsVisible = visible;
        if (!visible) clearAnnotationHover();
    }

    // LytBlock

    /** Horizontal space the floating button column steals from the row when interactive. */
    private int buttonColumnReserve() {
        return interactive && sceneButtonsVisible ? (BTN_OUTSIDE_GAP + BTN_SIZE) : 0;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int reserve = buttonColumnReserve();
        int totalDesired = width + reserve;
        int w = Math.min(totalDesired, Math.max(reserve + 16, availableWidth));
        int sceneW = Math.max(16, w - reserve);
        int buttonCount = interactive && sceneButtonsVisible ? cachedSceneButtonRoles().length : 0;
        int buttonsTotalH = interactive && sceneButtonsVisible
            ? (BTN_SIZE * buttonCount + BTN_GAP * Math.max(0, buttonCount - 1))
            : 0;
        int sceneH = Math.max(height, buttonsTotalH);
        int h = sceneH + (reserveBottomControlArea ? getBottomControlAreaHeight() : 0);
        this.layoutSceneWidth = sceneW;
        this.layoutSceneHeight = sceneH;
        return new LytRect(x, y, w, h);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        int sceneW = layoutSceneWidth > 0 ? layoutSceneWidth : getBounds().width() - buttonColumnReserve();
        if (sceneW < 16) sceneW = Math.max(16, getBounds().width() - buttonColumnReserve());
        int sliderAreaHeight = getBottomControlAreaHeight();
        int sceneH = layoutSceneHeight > 0 ? layoutSceneHeight : Math.max(16, getBounds().height() - sliderAreaHeight);
        int totalH = reserveBottomControlArea ? Math.max(sceneH + sliderAreaHeight, getBounds().height())
            : Math.max(sceneH, getBounds().height());
        LytRect outerRect = new LytRect(getBounds().x(), getBounds().y(), sceneW, totalH);
        LytRect sceneRect = cachedSceneRect = updateCachedRect(
            cachedSceneRect,
            outerRect.x(),
            outerRect.y(),
            outerRect.width(),
            sceneH);
        // Resolve document zoom and compute screen-space absolute coords for both render paths.
        float docZoom = context instanceof VanillaRenderContext mrc ? mrc.getZoom() : 1.0f;
        int absX = sceneRect.x();
        int absY = sceneRect.y();
        int outerAbsX = outerRect.x();
        int outerAbsY = outerRect.y();
        int clipX = outerAbsX, clipY = outerAbsY, clipW = outerRect.width(), clipH = outerRect.height();
        if (context instanceof VanillaRenderContext mrc) {
            absX = mrc.getDocumentOriginX() + Math.round(getBounds().x() * docZoom);
            absY = mrc.getDocumentOriginY() + Math.round((getBounds().y() - mrc.getScrollOffsetY()) * docZoom);
            outerAbsX = absX;
            outerAbsY = absY;
            LytRect vp = mrc.viewport();
            clipX = mrc.getDocumentOriginX();
            clipY = mrc.getDocumentOriginY();
            clipW = vp.width();
            clipH = vp.height();
        }
        // Scale layout dimensions to screen pixels when document zoom != 1.
        int w = Math.round(sceneRect.width() * docZoom);
        int h = Math.round(sceneRect.height() * docZoom);
        int outerW = Math.round(outerRect.width() * docZoom);
        int outerH = Math.round(outerRect.height() * docZoom);

        if (level.isEmpty() && annotations.isEmpty() && !shouldRenderOriginAxes()) {
            this.lastAbsX = absX;
            this.lastAbsY = absY;
            this.lastW = w;
            this.lastH = h;
            this.lastOuterAbsX = outerAbsX;
            this.lastOuterAbsY = outerAbsY;
            this.lastOuterW = outerW;
            this.lastOuterH = outerH;
            this.cachedScreenRect = updateCachedRect(cachedScreenRect, absX, absY, w, h);
            context.fillRect(sceneRect, sceneBackgroundColor);
            drawBottomControls(context, outerRect);
            context.drawBorder(sceneRect, sceneBorderColor, 1);
            return;
        }

        context.fillRect(sceneRect, sceneBackgroundColor);
        this.renderedContentClip = updateCachedRect(this.renderedContentClip, clipX, clipY, clipW, clipH);

        if (cameraViewportOverride != null) {
            camera.setViewportSize(cameraViewportOverride);
        } else {
            camera.setViewportSize(w, h);
        }
        this.lastAbsX = absX;
        this.lastAbsY = absY;
        this.lastW = w;
        this.lastH = h;
        this.lastOuterAbsX = outerAbsX;
        this.lastOuterAbsY = outerAbsY;
        this.lastOuterW = outerW;
        this.lastOuterH = outerH;
        this.cachedScreenRect = updateCachedRect(cachedScreenRect, absX, absY, w, h);

        List<InWorldAnnotation> inWorld = inWorldScratch;
        List<OverlayAnnotation> overlays = overlayScratch;
        inWorld.clear();
        overlays.clear();
        if (annotationsVisible) {
            for (var a : annotations) {
                if (!isAnnotationVisibleForCurrentLayer(a)) {
                    continue;
                }
                if (a instanceof InWorldAnnotation iw) inWorld.add(iw);
                else if (a instanceof OverlayAnnotation ov) overlays.add(ov);
            }
        }
        appendStructureLibHatchOverlays(inWorld);
        appendOriginAxesAnnotations(inWorld);
        for (SceneAnnotation pa : ponderOutgoingAnnotations) {
            if (pa instanceof OverlayAnnotation ov) {
                ov.setFade(Math.min(1f, ponderOutgoingFadeTick / 5f));
                overlays.add(ov);
            }
        }
        for (SceneAnnotation pa : ponderActiveAnnotations) {
            if (pa instanceof InWorldAnnotation iw) inWorld.add(iw);
            else if (pa instanceof OverlayAnnotation ov) {
                ov.setFade(Math.min(1f, ponderAnnotationFadeTick / 5f));
                overlays.add(ov);
            }
        }

        if (hoveredEntity != null && hoveredEntityBounds != null && isEntityVisibleForCurrentLayer(hoveredEntity)) {
            float eps = 0.002f;
            hoverBoxMin.set(
                (float) hoveredEntityBounds.minX - eps,
                (float) hoveredEntityBounds.minY - eps,
                (float) hoveredEntityBounds.minZ - eps);
            hoverBoxMax.set(
                (float) hoveredEntityBounds.maxX + eps,
                (float) hoveredEntityBounds.maxY + eps,
                (float) hoveredEntityBounds.maxZ + eps);
            inWorld.add(hoverBoxAnnotation);
        } else if (hoveredBlock != null && isBlockVisibleForCurrentLayer(hoveredBlock[1])) {
            int bx = hoveredBlock[0], by = hoveredBlock[1], bz = hoveredBlock[2];
            double minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
            if (hoveredBlockBounds != null) {
                minX = hoveredBlockBounds.minX - bx;
                minY = hoveredBlockBounds.minY - by;
                minZ = hoveredBlockBounds.minZ - bz;
                maxX = hoveredBlockBounds.maxX - bx;
                maxY = hoveredBlockBounds.maxY - by;
                maxZ = hoveredBlockBounds.maxZ - bz;
            } else {
                Block block = level.getBlock(bx, by, bz);
                if (block != null && block != Blocks.air) {
                    try {
                        AxisAlignedBB blockBounds = GuideBlockBoundsResolver.resolveSelectedBounds(level, bx, by, bz);
                        if (blockBounds != null) {
                            minX = blockBounds.minX - bx;
                            minY = blockBounds.minY - by;
                            minZ = blockBounds.minZ - bz;
                            maxX = blockBounds.maxX - bx;
                            maxY = blockBounds.maxY - by;
                            maxZ = blockBounds.maxZ - bz;
                        }
                        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
                            minX = minY = minZ = 0;
                            maxX = maxY = maxZ = 1;
                        }
                    } catch (Throwable t) {
                        minX = minY = minZ = 0;
                        maxX = maxY = maxZ = 1;
                    }
                }
            }
            if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
                minX = minY = minZ = 0;
                maxX = maxY = maxZ = 1;
            }
            float EPS = 0.002f;
            hoverBoxMin.set((float) (bx + minX) - EPS, (float) (by + minY) - EPS, (float) (bz + minZ) - EPS);
            hoverBoxMax.set((float) (bx + maxX) + EPS, (float) (by + maxY) + EPS, (float) (bz + maxZ) + EPS);
            inWorld.add(hoverBoxAnnotation);
        }
        Integer visibleLayerY = resolveVisibleLayerY();

        boolean ponderCameraApplied = false;
        float savedZoom = 0, savedRotX = 0, savedRotY = 0, savedRotZ = 0, savedOffX = 0, savedOffY = 0;
        if (ponderSceneData != null) {
            savedZoom = camera.getZoom();
            savedRotX = camera.getRotationX();
            savedRotY = camera.getRotationY();
            savedRotZ = camera.getRotationZ();
            savedOffX = camera.getOffsetX();
            savedOffY = camera.getOffsetY();
            camera.setZoom(ponderCamZoom);
            camera.setRotationX(ponderCamRotX);
            camera.setRotationY(ponderCamRotY);
            camera.setRotationZ(ponderCamRotZ);
            camera.setOffsetX(ponderCamOffX);
            camera.setOffsetY(ponderCamOffY);
            ponderCameraApplied = true;
        }

        GuidebookLevelRenderer.getInstance()
            .render(
                level,
                camera,
                absX,
                absY,
                w,
                h,
                clipX,
                clipY,
                clipW,
                clipH,
                0f,
                inWorld,
                context.lightDarkMode(),
                visibleLayerY,
                ponderSceneParticles);

        if (!overlays.isEmpty()) {
            LytRect viewport = cachedOverlayViewport = updateCachedRect(cachedOverlayViewport, absX, absY, w, h);
            // Scissor overlays to the scene rect so diamond icons etc. cannot escape.
            // NOTE: pushScissor expects SCREEN coords (same space as GuideScreen.cachedScissorRect),
            // not document-local coords. Passing sceneRect (doc-local) caused the scissor to clip
            // overlays out of sight, making diamond annotations disappear.
            context.pushScissor(viewport);
            try {
                for (var o : overlays) {
                    o.render(camera, context, viewport);
                }
            } finally {
                context.popScissor();
            }
        }

        if (ponderCameraApplied) {
            camera.setZoom(savedZoom);
            camera.setRotationX(savedRotX);
            camera.setRotationY(savedRotY);
            camera.setRotationZ(savedRotZ);
            camera.setOffsetX(savedOffX);
            camera.setOffsetY(savedOffY);
        }

        context.restoreExternalRenderState();
        drawBottomControls(context, outerRect);

        // Draw border AFTER the 3D content so border pixels always sit on top.
        context.drawBorder(sceneRect, sceneBorderColor, 1);

        if (interactive && sceneButtonsVisible) {
            drawSceneButtons(sceneRect.x(), sceneRect.y(), w, h, absX, absY, docZoom);
        }
    }

    public static final int BTN_SIZE = 16;
    public static final int BTN_GAP = 2;
    public static final int BTN_OUTSIDE_GAP = 3;

    private int lastAbsX, lastAbsY, lastW, lastH;
    private float lastDocZoom = 1.0f;
    private int lastOuterAbsX, lastOuterAbsY, lastOuterW, lastOuterH;
    /** Width reserved for the inner 3D scene (bounds.width minus the button column). */
    private int layoutSceneWidth;
    /** Height reserved for the inner 3D scene (bounds.height minus the bottom slider band). */
    private int layoutSceneHeight;

    @Nullable
    private LytRect renderedContentClip;

    // Reuse rect records when geometry is unchanged.
    public static LytRect updateCachedRect(@Nullable LytRect current, int x, int y, int w, int h) {
        if (current != null && current.x() == x && current.y() == y && current.width() == w && current.height() == h) {
            return current;
        }
        return new LytRect(x, y, w, h);
    }

    private void appendOriginAxesAnnotations(List<InWorldAnnotation> inWorld) {
        if (!shouldRenderOriginAxes()) {
            return;
        }
        appendOriginAxisAnnotation(inWorld, originXAxisAnnotation);
        appendOriginAxisAnnotation(inWorld, originYAxisAnnotation);
        appendOriginAxisAnnotation(inWorld, originZAxisAnnotation);
    }

    private void appendOriginAxisAnnotation(List<InWorldAnnotation> inWorld, InWorldLineAnnotation annotation) {
        if (isAnnotationVisibleForCurrentLayer(annotation)) {
            inWorld.add(annotation);
        }
    }

    private boolean shouldRenderOriginAxes() {
        return !forceHideOriginAxes && (forceOriginAxesVisible || ModConfig.debug.enableDebugMode);
    }

    private static InWorldLineAnnotation createOriginAxisAnnotation(Vector3f to, ConstantColor color) {
        InWorldLineAnnotation annotation = new InWorldLineAnnotation(new Vector3f(), to, color, ORIGIN_AXIS_THICKNESS);
        return annotation;
    }

    private void drawSceneButtons(int drawX, int drawY, int screenW, int screenH, int absX, int absY, float docZoom) {
        this.lastAbsX = absX;
        this.lastAbsY = absY;
        this.lastW = screenW;
        this.lastH = screenH;
        this.lastDocZoom = docZoom;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        // GL drawing uses layout coords; convert screen pixels back to layout units for bx.
        int layoutW = Math.round(screenW / docZoom);
        int bx = drawX + layoutW + BTN_OUTSIDE_GAP;
        int by = drawY;
        int btnScreenSize = Math.round(BTN_SIZE * docZoom);
        int btnScreenStep = Math.round((BTN_SIZE + BTN_GAP) * docZoom);
        int absBx = absX + screenW + Math.round(BTN_OUTSIDE_GAP * docZoom);
        int absBy = absY;
        sceneButtonsAbsX = absBx;
        sceneButtonsAbsY = absBy;
        cachedScreenRect = updateCachedRect(cachedScreenRect, absX, absY, screenW, screenH);
        int mx, my;
        try {
            var mc = Minecraft.getMinecraft();
            int sw = DisplayScale.scaledWidth(), sh = DisplayScale.scaledHeight();
            mx = Mouse.getX() * sw / mc.displayWidth;
            my = sh - Mouse.getY() * sh / mc.displayHeight - 1;
        } catch (Throwable t) {
            mx = -1;
            my = -1;
        }
        GuideIconButton.Role[] roles = cachedSceneButtonRoles();
        for (var role : roles) {
            boolean hover = mx >= absBx && my >= absBy && mx < absBx + btnScreenSize && my < absBy + btnScreenSize;
            drawOneSceneButton(bx, by, role, hover);
            by += BTN_SIZE + BTN_GAP;
            absBy += btnScreenStep;
        }
    }

    private GuideIconButton.Role[] sceneButtonRoles() {
        GuideIconButton.Role[] base;
        if (annotations.isEmpty()) {
            base = SCENE_BUTTONS_NO_ANNOTATIONS;
        } else {
            base = annotationsVisible ? SCENE_BUTTONS_SHOWN : SCENE_BUTTONS_HIDDEN;
        }
        if (!hasStructureLibHatchData()) {
            return base;
        }

        GuideIconButton.Role[] roles = new GuideIconButton.Role[base.length + 1];
        System.arraycopy(base, 0, roles, 0, base.length);
        roles[base.length] = GuideIconButton.Role.HIGHLIGHT_STRUCTURELIB_HATCHES;
        return roles;
    }

    private GuideIconButton.Role[] cachedSceneButtonRoles() {
        boolean hasAnnotations = !annotations.isEmpty();
        boolean hasStructureLibHatches = hasStructureLibHatchData();
        if (cachedSceneButtonsVisible != annotationsVisible || cachedSceneHasAnnotations != hasAnnotations
            || cachedSceneHasStructureLibHatches != hasStructureLibHatches) {
            cachedSceneButtonsVisible = annotationsVisible;
            cachedSceneHasAnnotations = hasAnnotations;
            cachedSceneHasStructureLibHatches = hasStructureLibHatches;
            cachedSceneButtonRoles = sceneButtonRoles();
        }
        return cachedSceneButtonRoles;
    }

    private void drawOneSceneButton(int x, int y, GuideIconButton.Role role, boolean hovered) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(BUTTONS_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int color = GuideIconButton.resolveIconColor(true, hovered, isSceneButtonActive(role));
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);
        float texSize = GuideIconButton.TEXTURE_SIZE;
        float u0 = role.iconSrcX() / texSize;
        float v0 = role.iconSrcY() / texSize;
        float u1 = (role.iconSrcX() + 16) / texSize;
        float v1 = (role.iconSrcY() + 16) / texSize;
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + BTN_SIZE, 0, u0, v1);
        tess.addVertexWithUV(x + BTN_SIZE, y + BTN_SIZE, 0, u1, v1);
        tess.addVertexWithUV(x + BTN_SIZE, y, 0, u1, v0);
        tess.addVertexWithUV(x, y, 0, u0, v0);
        tess.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private boolean isSceneButtonActive(GuideIconButton.Role role) {
        return role == GuideIconButton.Role.HIGHLIGHT_STRUCTURELIB_HATCHES && structureLibHatchHighlightEnabled;
    }

    GuideIconButton.Role[] getVisibleSceneButtonRolesForTesting() {
        return cachedSceneButtonRoles();
    }

    @Nullable
    public GuideIconButton.Role sceneButtonAt(int mouseX, int mouseY) {
        if (ponderSceneData != null && lastOuterH > 0) {
            int btnSize = SCENE_SLIDER_AREA_HEIGHT;
            if (mouseY >= cachedPonderBtnAbsY && mouseY < cachedPonderBtnAbsY + btnSize) {
                GuideIconButton.Role[] pRoles = { GuideIconButton.Role.PONDER_PREV_KEYFRAME,
                    GuideIconButton.Role.PONDER_PLAY_PAUSE, GuideIconButton.Role.PONDER_RESTART };
                for (int i = 0; i < pRoles.length; i++) {
                    int bx = cachedPonderBtnAbsX + i * btnSize;
                    if (mouseX >= bx && mouseX < bx + btnSize) return pRoles[i];
                }
            }
        }
        if (!sceneButtonsVisible) return null;
        if (lastW <= 0 || lastH <= 0) return null;
        if (renderedContentClip != null) {
            int cx0 = renderedContentClip.x();
            int cy0 = renderedContentClip.y();
            int cx1 = cx0 + renderedContentClip.width();
            int cy1 = cy0 + renderedContentClip.height();
            if (lastAbsX + lastW <= cx0 || lastAbsX >= cx1) return null;
            if (lastAbsY + lastH <= cy0 || lastAbsY >= cy1) return null;
            // Also reject when the mouse is outside the visible content viewport entirely.
            if (mouseX < cx0 || mouseX >= cx1 || mouseY < cy0 || mouseY >= cy1) return null;
        }
        // Vertically, the mouse must be within this scene's own rendered band; otherwise a scene
        // below the viewport could return a false-positive hit purely on X coincidence because
        // its stashed sceneButtonsAbsY still falls inside the button rect math.
        if (mouseY < lastAbsY || mouseY >= lastAbsY + lastH) return null;
        int bx = sceneButtonsAbsX;
        int by = sceneButtonsAbsY;
        int btnScreenSize = Math.round(BTN_SIZE * lastDocZoom);
        int btnScreenStep = Math.round((BTN_SIZE + BTN_GAP) * lastDocZoom);
        // Early-out on X: the whole button column lives at [sceneButtonsAbsX, sceneButtonsAbsX + btnScreenSize).
        if (mouseX < bx || mouseX >= bx + btnScreenSize) return null;
        var roles = cachedSceneButtonRoles();
        for (var role : roles) {
            boolean visible = renderedContentClip == null || (bx + btnScreenSize > renderedContentClip.x()
                && bx < renderedContentClip.x() + renderedContentClip.width()
                && by + btnScreenSize > renderedContentClip.y()
                && by < renderedContentClip.y() + renderedContentClip.height());
            if (visible && mouseX >= bx && mouseX < bx + btnScreenSize && mouseY >= by && mouseY < by + btnScreenSize) {
                return role;
            }
            by += btnScreenStep;
        }
        return null;
    }

    public void setHoveredBlock(int @Nullable [] xyz) {
        this.hoveredBlock = xyz;
        if (xyz == null) {
            this.hoveredBlockBounds = null;
            this.hoveredBlockHitResult = null;
        }
    }

    public void setHoveredEntity(@Nullable Entity entity) {
        this.hoveredEntity = entity;
        if (entity == null) {
            this.hoveredEntityBounds = null;
            this.hoveredEntityHitResult = null;
        }
    }

    public int @Nullable [] getHoveredBlock() {
        return hoveredBlock;
    }

    @Nullable
    public MovingObjectPosition getHoveredBlockHitResult() {
        return hoveredBlockHitResult;
    }

    @Nullable
    public Entity getHoveredEntity() {
        return hoveredEntity;
    }

    @Nullable
    public MovingObjectPosition getHoveredEntityHitResult() {
        return hoveredEntityHitResult;
    }

    public void setHoveredStructureLibHatch(int @Nullable [] xyz) {
        this.hoveredStructureLibHatch = xyz;
    }

    public int @Nullable [] getHoveredStructureLibHatch() {
        return hoveredStructureLibHatch;
    }

    public void updateHoveredSceneTarget(int mouseAbsX, int mouseAbsY) {
        PickRay pickRay = resolvePickRay(mouseAbsX, mouseAbsY);
        if (pickRay == null) {
            setHoveredEntity(null);
            setHoveredBlock(null);
            return;
        }

        GuideEntityRayPicker.Hit entityHit = pickEntityHit(pickRay);
        BlockPickResult blockHit = pickBlockHit(pickRay);
        if (entityHit != null && (blockHit == null || entityHit.getDistanceSq() < blockHit.distanceSq)) {
            hoveredEntity = entityHit.getEntity();
            hoveredEntityBounds = entityHit.getBounds();
            hoveredEntityHitResult = entityHit.getHitResult();
            setHoveredBlock(null);
            return;
        }

        setHoveredEntity(null);
        if (blockHit == null) {
            setHoveredBlock(null);
            return;
        }

        hoveredBlock = blockHit.pos;
        hoveredBlockBounds = blockHit.bounds;
        hoveredBlockHitResult = blockHit.hitResult;
    }

    public int @Nullable [] pickStructureLibHatch(int mouseAbsX, int mouseAbsY) {
        if (!structureLibHatchHighlightEnabled || structureLibSceneMetadata == null || lastW <= 0 || lastH <= 0) {
            return null;
        }
        List<StructureLibSceneMetadata.BlockTooltipEntry> hatchEntries = structureLibSceneMetadata
            .getHatchTooltipEntries();
        if (hatchEntries.isEmpty()) {
            return null;
        }

        PickRay pickRay = resolvePickRay(mouseAbsX, mouseAbsY);
        if (pickRay == null) {
            return null;
        }
        Vec3 rayStart = pickRay.start;
        Vec3 rayEnd = pickRay.end;

        int[] best = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (StructureLibSceneMetadata.BlockTooltipEntry entry : hatchEntries) {
            if (!isBlockVisibleForCurrentLayer(entry.getY())) {
                continue;
            }
            AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(
                entry.getX(),
                entry.getY(),
                entry.getZ(),
                entry.getX() + 1,
                entry.getY() + 1,
                entry.getZ() + 1);
            MovingObjectPosition hit = bounds.calculateIntercept(rayStart, rayEnd);
            if (hit == null || hit.hitVec == null) {
                continue;
            }

            double distanceSq = hit.hitVec.squareDistanceTo(rayStart);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = new int[] { entry.getX(), entry.getY(), entry.getZ() };
            }
        }
        return best;
    }

    public LytRect getScreenRect() {
        return cachedScreenRect = updateCachedRect(cachedScreenRect, lastAbsX, lastAbsY, lastW, lastH);
    }

    public boolean containsSceneViewport(int mouseX, int mouseY) {
        if (lastW <= 0 || lastH <= 0) return false;

        int x0 = lastAbsX;
        int y0 = lastAbsY;
        int x1 = x0 + lastW;
        int y1 = y0 + lastH;

        if (renderedContentClip != null) {
            int cx0 = renderedContentClip.x();
            int cy0 = renderedContentClip.y();
            int cx1 = cx0 + renderedContentClip.width();
            int cy1 = cy0 + renderedContentClip.height();
            if (x1 <= cx0 || x0 >= cx1 || y1 <= cy0 || y0 >= cy1) return false;
            x0 = Math.max(x0, cx0);
            y0 = Math.max(y0, cy0);
            x1 = Math.min(x1, cx1);
            y1 = Math.min(y1, cy1);
        }

        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private String getVisibleLayerSliderLabel() {
        int currentLayer = getCurrentVisibleLayer();
        String value = currentLayer <= 0 ? GuidebookText.SceneAll.text() : Integer.toString(currentLayer);
        return GuidebookText.SceneSliderLabelFormat.text(GuidebookText.SceneVisibleLayerLabel.text(), value);
    }

    private String getStructureLibTierSliderLabel() {
        return GuidebookText.SceneSliderLabelFormat
            .text(GuidebookText.SceneStructureLibTierLabel.text(), Integer.toString(structureLibCurrentTier));
    }

    private String getStructureLibChannelSliderLabel(StructureLibSceneMetadata.ChannelData channelData) {
        String value = getStructureLibChannelValue(channelData.getChannelId()) <= 0 ? GuidebookText.SceneNotSet.text()
            : Integer.toString(getStructureLibChannelValue(channelData.getChannelId()));
        return GuidebookText.SceneSliderLabelFormat.text(channelData.getLabel(), value);
    }

    public boolean containsStructureLibChannelSlider(int mouseX, int mouseY) {
        if (!bottomControlsVisible || !hasStructureLibChannelData()) {
            return false;
        }
        for (LytRect hitRect : resolveStructureLibChannelSliderHitRects().values()) {
            if (!hitRect.isEmpty() && hitRect.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsStructureLibTierSlider(int mouseX, int mouseY) {
        if (!bottomControlsVisible || !hasStructureLibTierData()) {
            return false;
        }
        LytRect hitRect = resolveStructureLibTierSliderHitRect();
        return !hitRect.isEmpty() && hitRect.contains(mouseX, mouseY);
    }

    public boolean containsVisibleLayerSlider(int mouseX, int mouseY) {
        if (!bottomControlsVisible || !hasVisibleLayerSlider()) {
            return false;
        }
        LytRect hitRect = resolveVisibleLayerSliderHitRect();
        return !hitRect.isEmpty() && hitRect.contains(mouseX, mouseY);
    }

    public boolean containsPonderButtons(int mouseX, int mouseY) {
        if (ponderSceneData == null) {
            return false;
        }
        int btnW = SCENE_SLIDER_AREA_HEIGHT;
        int totalW = PONDER_BTN_TOTAL_WIDTH;
        return mouseX >= cachedPonderBtnAbsX && mouseX < cachedPonderBtnAbsX + totalW
            && mouseY >= cachedPonderBtnAbsY
            && mouseY < cachedPonderBtnAbsY + btnW;
    }

    @Nullable
    public GuideIconButton.Role ponderButtonAt(int mouseX, int mouseY) {
        if (!containsPonderButtons(mouseX, mouseY)) {
            return null;
        }
        int relX = mouseX - cachedPonderBtnAbsX;
        int btnW = SCENE_SLIDER_AREA_HEIGHT;
        int idx = relX / btnW;
        return switch (idx) {
            case 0 -> GuideIconButton.Role.PONDER_PREV_KEYFRAME;
            case 1 -> GuideIconButton.Role.PONDER_PLAY_PAUSE;
            case 2 -> GuideIconButton.Role.PONDER_RESTART;
            default -> null;
        };
    }

    public boolean containsBottomControlSlider(int mouseX, int mouseY) {
        return containsPonderBar(mouseX, mouseY) || containsPonderButtons(mouseX, mouseY)
            || containsStructureLibTierSlider(mouseX, mouseY)
            || containsVisibleLayerSlider(mouseX, mouseY)
            || containsStructureLibChannelSlider(mouseX, mouseY);
    }

    public boolean containsSceneInteractiveTarget(int mouseX, int mouseY) {
        return containsSceneViewport(mouseX, mouseY) || containsBottomControlSlider(mouseX, mouseY);
    }

    public int @Nullable [] pickBlock(int mouseAbsX, int mouseAbsY) {
        PickRay pickRay = resolvePickRay(mouseAbsX, mouseAbsY);
        if (pickRay == null) {
            hoveredBlockBounds = null;
            hoveredBlockHitResult = null;
            return null;
        }

        BlockPickResult hit = pickBlockHit(pickRay);
        hoveredBlockBounds = hit != null ? hit.bounds : null;
        hoveredBlockHitResult = hit != null ? hit.hitResult : null;
        return hit != null ? hit.pos : null;
    }

    @Nullable
    public Entity pickEntity(int mouseAbsX, int mouseAbsY) {
        PickRay pickRay = resolvePickRay(mouseAbsX, mouseAbsY);
        if (pickRay == null) {
            hoveredEntityBounds = null;
            hoveredEntityHitResult = null;
            return null;
        }

        GuideEntityRayPicker.Hit hit = pickEntityHit(pickRay);
        hoveredEntityBounds = hit != null ? hit.getBounds() : null;
        hoveredEntityHitResult = hit != null ? hit.getHitResult() : null;
        return hit != null ? hit.getEntity() : null;
    }

    @Nullable
    private PickRay resolvePickRay(int mouseAbsX, int mouseAbsY) {
        if (level.isEmpty() || lastW <= 0 || lastH <= 0) {
            return null;
        }

        float relX = (mouseAbsX) - (lastAbsX + lastW * 0.5f);
        float relY = (mouseAbsY) - (lastAbsY + lastH * 0.5f);

        // Apply ponder camera so the raycast matches the rendered view.
        boolean ponderApplied = false;
        float pSavedZoom = 0, pSavedRX = 0, pSavedRY = 0, pSavedRZ = 0, pSavedOX = 0, pSavedOY = 0;
        if (ponderSceneData != null) {
            pSavedZoom = camera.getZoom();
            pSavedRX = camera.getRotationX();
            pSavedRY = camera.getRotationY();
            pSavedRZ = camera.getRotationZ();
            pSavedOX = camera.getOffsetX();
            pSavedOY = camera.getOffsetY();
            camera.setZoom(ponderCamZoom);
            camera.setRotationX(ponderCamRotX);
            camera.setRotationY(ponderCamRotY);
            camera.setRotationZ(ponderCamRotZ);
            camera.setOffsetX(ponderCamOffX);
            camera.setOffsetY(ponderCamOffY);
            ponderApplied = true;
        }

        camera.setViewportSize(lastW, lastH);
        float[] ray = camera.screenToWorldRay(relX, relY, pickRayScratch);

        if (ponderApplied) {
            camera.setZoom(pSavedZoom);
            camera.setRotationX(pSavedRX);
            camera.setRotationY(pSavedRY);
            camera.setRotationZ(pSavedRZ);
            camera.setOffsetX(pSavedOX);
            camera.setOffsetY(pSavedOY);
        }

        float ox = ray[0], oy = ray[1], oz = ray[2];
        float dx = ray[3], dy = ray[4], dz = ray[5];
        int[] sceneBounds = level.getBounds();
        float entryDistance = rayAabb(
            ox,
            oy,
            oz,
            dx,
            dy,
            dz,
            sceneBounds[0],
            sceneBounds[1],
            sceneBounds[2],
            sceneBounds[3] + 1f,
            sceneBounds[4] + 1f,
            sceneBounds[5] + 1f);
        if (Float.isNaN(entryDistance)) {
            return null;
        }

        float spanX = sceneBounds[3] - sceneBounds[0] + 1f;
        float spanY = sceneBounds[4] - sceneBounds[1] + 1f;
        float spanZ = sceneBounds[5] - sceneBounds[2] + 1f;
        float sceneSpan = (float) Math.sqrt(spanX * spanX + spanY * spanY + spanZ * spanZ) + 8f;
        float rayReach = Math.max(64f, Math.max(0f, entryDistance) + sceneSpan);
        return new PickRay(
            Vec3.createVectorHelper(ox, oy, oz),
            Vec3.createVectorHelper(ox + dx * rayReach, oy + dy * rayReach, oz + dz * rayReach));
    }

    @Nullable
    private BlockPickResult pickBlockHit(PickRay pickRay) {
        int[] best = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        AxisAlignedBB bestBounds = null;
        MovingObjectPosition bestHitResult = null;
        var fakeWorld = level.getOrCreateFakeWorld();
        for (int[] b : level.getFilledBlocks()) {
            if (!isBlockVisibleForCurrentLayer(b[1])) {
                continue;
            }
            Block block = level.getBlock(b[0], b[1], b[2]);
            if (block == null || block == Blocks.air) {
                continue;
            }

            MovingObjectPosition hit = null;
            AxisAlignedBB fallbackBounds = null;
            try {
                hit = block.collisionRayTrace(fakeWorld, b[0], b[1], b[2], pickRay.start, pickRay.end);
            } catch (Throwable ignored) {}

            if (hit == null || hit.hitVec == null) {
                fallbackBounds = GuideBlockBoundsResolver.resolveWorldBounds(level, b[0], b[1], b[2]);
                if (fallbackBounds != null) {
                    MovingObjectPosition fallbackHit = fallbackBounds.calculateIntercept(pickRay.start, pickRay.end);
                    hit = withBlockCoordinates(fallbackHit, b[0], b[1], b[2]);
                }
            }

            if (hit == null || hit.hitVec == null) {
                continue;
            }

            double distanceSq = hit.hitVec.squareDistanceTo(pickRay.start);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = b;
                bestHitResult = withBlockCoordinates(hit, b[0], b[1], b[2]);
                bestBounds = fallbackBounds != null ? fallbackBounds
                    : resolveHoveredBounds(level, b[0], b[1], b[2], pickRay.start, pickRay.end);
            }
        }
        return best != null ? new BlockPickResult(best, bestBounds, bestHitResult, bestDistanceSq) : null;
    }

    @Nullable
    private GuideEntityRayPicker.Hit pickEntityHit(PickRay pickRay) {
        return GuideEntityRayPicker.pick(level.getEntities(), pickRay.start, pickRay.end, resolveVisibleLayerY());
    }

    @Nullable
    private AxisAlignedBB resolveHoveredBounds(GuidebookLevel level, int x, int y, int z, Vec3 rayStart, Vec3 rayEnd) {
        AxisAlignedBB hitBounds = GuideBlockBoundsResolver.resolveRayHitBounds(level, x, y, z, rayStart, rayEnd);
        return hitBounds != null ? hitBounds : GuideBlockBoundsResolver.resolveSelectedBounds(level, x, y, z);
    }

    private boolean isEntityVisibleForCurrentLayer(@Nullable Entity entity) {
        Integer visibleLayerY = resolveVisibleLayerY();
        AxisAlignedBB bounds = entity != null ? entity.boundingBox : null;
        return visibleLayerY == null || bounds == null
            || (bounds.maxY > visibleLayerY && bounds.minY < visibleLayerY + 1.0D);
    }

    @Nullable
    public static MovingObjectPosition withBlockCoordinates(@Nullable MovingObjectPosition hit, int x, int y, int z) {
        if (hit == null || hit.hitVec == null) {
            return hit;
        }
        if (hit.blockX == x && hit.blockY == y && hit.blockZ == z) {
            return hit;
        }
        return new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
    }

    public static float rayAabb(float ox, float oy, float oz, float dx, float dy, float dz, float minX, float minY,
        float minZ, float maxX, float maxY, float maxZ) {
        float tmin = Float.NEGATIVE_INFINITY;
        float tmax = Float.POSITIVE_INFINITY;
        // X
        if (Math.abs(dx) < 1e-6f) {
            if (ox < minX || ox > maxX) return Float.NaN;
        } else {
            float t1 = (minX - ox) / dx;
            float t2 = (maxX - ox) / dx;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        // Y
        if (Math.abs(dy) < 1e-6f) {
            if (oy < minY || oy > maxY) return Float.NaN;
        } else {
            float t1 = (minY - oy) / dy;
            float t2 = (maxY - oy) / dy;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        // Z
        if (Math.abs(dz) < 1e-6f) {
            if (oz < minZ || oz > maxZ) return Float.NaN;
        } else {
            float t1 = (minZ - oz) / dz;
            float t2 = (maxZ - oz) / dz;
            if (t1 > t2) {
                float tt = t1;
                t1 = t2;
                t2 = tt;
            }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return Float.NaN;
        }
        return tmin;
    }

    public void activateSceneButton(GuideIconButton.Role role) {
        switch (role) {
            case HIDE_ANNOTATIONS, SHOW_ANNOTATIONS -> setAnnotationsVisible(!annotationsVisible);
            case HIGHLIGHT_STRUCTURELIB_HATCHES -> setStructureLibHatchHighlightEnabled(
                !structureLibHatchHighlightEnabled);
            case ZOOM_IN -> {
                if (ponderSceneData != null) {
                    ponderCamZoom = Math.min(MAX_ZOOM, ponderCamZoom * 1.25f);
                } else {
                    camera.setZoom(Math.min(MAX_ZOOM, camera.getZoom() * 1.25f));
                }
            }
            case ZOOM_OUT -> {
                if (ponderSceneData != null) {
                    ponderCamZoom = Math.max(MIN_ZOOM, ponderCamZoom / 1.25f);
                } else {
                    camera.setZoom(Math.max(MIN_ZOOM, camera.getZoom() / 1.25f));
                }
            }
            case RESET_VIEW -> resetViewToInitialCamera();
            case PONDER_PREV_KEYFRAME -> ponderPrevKeyframe();
            case PONDER_PLAY_PAUSE -> ponderTogglePlay();
            case PONDER_RESTART -> ponderRestart();
            default -> {}
        }
    }

    private void resetViewToInitialCamera() {
        if (ponderSceneData != null) {
            // In ponder mode the camera is driven by ponderCam* fields.
            // Re-running updatePonderState restores them to the scripted values
            // for the current tick, discarding any user pan/rotate offsets.
            updatePonderState();
        } else {
            camera.setZoom(initialCam[0]);
            camera.setRotationX(initialCam[1]);
            camera.setRotationY(initialCam[2]);
            camera.setRotationZ(initialCam[3]);
            camera.setOffsetX(initialCam[4]);
            camera.setOffsetY(initialCam[5]);
        }
    }

    public void startDrag(int mouseX, int mouseY, int button) {
        if (!interactive) return;
        if (button == 0 && containsPonderBar(mouseX, mouseY)) {
            draggingPonderBar = true;
            applyPonderBarAt(mouseX);
            return;
        }
        if (button == 0 && containsStructureLibTierSlider(mouseX, mouseY)) {
            draggingStructureLibTierSlider = true;
            applyStructureLibTierSliderAt(mouseX);
            return;
        }
        if (button == 0 && containsVisibleLayerSlider(mouseX, mouseY)) {
            draggingVisibleLayerSlider = true;
            applyVisibleLayerSliderAt(mouseX);
            return;
        }
        String hoveredChannelId = resolveHoveredStructureLibChannelId(mouseX, mouseY);
        if (button == 0 && hoveredChannelId != null) {
            draggingStructureLibChannelId = hoveredChannelId;
            applyStructureLibChannelSliderAt(hoveredChannelId, mouseX);
            return;
        }
        if (isPonderPlaying()) return;
        this.dragButton = button;
        this.dragLastX = mouseX;
        this.dragLastY = mouseY;
    }

    public static boolean isRotateButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 0 : button == 1;
    }

    public static boolean isPanButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 1 : button == 0;
    }

    public void drag(int mouseX, int mouseY) {
        if (draggingPonderBar) {
            applyPonderBarAt(mouseX);
            return;
        }
        if (draggingStructureLibTierSlider) {
            applyStructureLibTierSliderAt(mouseX);
            return;
        }
        if (draggingVisibleLayerSlider) {
            applyVisibleLayerSliderAt(mouseX);
            return;
        }
        if (draggingStructureLibChannelId != null) {
            applyStructureLibChannelSliderAt(draggingStructureLibChannelId, mouseX);
            return;
        }
        if (dragButton < 0) return;
        if (isPonderPlaying()) return;
        int dx = mouseX - dragLastX;
        int dy = mouseY - dragLastY;
        dragLastX = mouseX;
        dragLastY = mouseY;

        // Route to ponderCam* so changes are visible when ponder is active (paused or playing).
        if (ponderSceneData != null) {
            if (isPanButton(dragButton)) {
                ponderCamOffX += dx;
                ponderCamOffY -= dy;
            } else if (isRotateButton(dragButton)) {
                ponderCamRotY += dx * DRAG_ROTATE_SENSITIVITY;
                ponderCamRotX += dy * DRAG_ROTATE_SENSITIVITY;
            }
        } else {
            if (isPanButton(dragButton)) {
                camera.setOffsetX(camera.getOffsetX() + dx);
                camera.setOffsetY(camera.getOffsetY() - dy);
            } else if (isRotateButton(dragButton)) {
                camera.setRotationY(camera.getRotationY() + dx * DRAG_ROTATE_SENSITIVITY);
                camera.setRotationX(camera.getRotationX() + dy * DRAG_ROTATE_SENSITIVITY);
            }
        }
    }

    public void endDrag() {
        this.dragButton = -1;
        this.draggingVisibleLayerSlider = false;
        this.draggingStructureLibTierSlider = false;
        this.draggingStructureLibChannelId = null;
        this.draggingPonderBar = false;
    }

    public boolean isDragging() {
        return dragButton >= 0 || draggingVisibleLayerSlider
            || draggingStructureLibTierSlider
            || draggingStructureLibChannelId != null
            || draggingPonderBar;
    }

    public void scroll(int mouseX, int mouseY, int dwheel) {
        if (!interactive) return;
        if (dwheel == 0) return;
        if (containsStructureLibTierSlider(mouseX, mouseY) && hasStructureLibTierData()) {
            nudgeStructureLibTier(dwheel);
            return;
        }
        if (containsVisibleLayerSlider(mouseX, mouseY) && hasVisibleLayerSlider()) {
            nudgeVisibleLayer(dwheel);
            return;
        }
        String hoveredChannelId = resolveHoveredStructureLibChannelId(mouseX, mouseY);
        if (hoveredChannelId != null) {
            nudgeStructureLibChannel(hoveredChannelId, dwheel);
            return;
        }
        if (isPonderPlaying()) return;
        if (ponderSceneData != null) {
            float z = ponderCamZoom;
            if (dwheel > 0) z *= WHEEL_ZOOM_STEP;
            else z /= WHEEL_ZOOM_STEP;
            ponderCamZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
            return;
        }
        float z = camera.getZoom();
        if (dwheel > 0) z *= WHEEL_ZOOM_STEP;
        else z /= WHEEL_ZOOM_STEP;
        if (z < MIN_ZOOM) z = MIN_ZOOM;
        if (z > MAX_ZOOM) z = MAX_ZOOM;
        camera.setZoom(z);
    }

    public void scroll(int dwheel) {
        if (!interactive) return;
        if (dwheel == 0) return;
        if (isPonderPlaying()) return;
        if (ponderSceneData != null) {
            float z = ponderCamZoom;
            if (dwheel > 0) z *= WHEEL_ZOOM_STEP;
            else z /= WHEEL_ZOOM_STEP;
            ponderCamZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
            return;
        }
        float z = camera.getZoom();
        if (dwheel > 0) z *= WHEEL_ZOOM_STEP;
        else z /= WHEEL_ZOOM_STEP;
        if (z < MIN_ZOOM) z = MIN_ZOOM;
        if (z > MAX_ZOOM) z = MAX_ZOOM;
        camera.setZoom(z);
    }

    public LytRect getVisibleLayerSliderRectForTesting() {
        return resolveVisibleLayerSliderTrackRect();
    }

    public int getVisibleLayerSliderAreaHeightForTesting() {
        return visibleLayerSliderAreaHeight();
    }

    public int getCurrentVisibleLayerForTesting() {
        return getCurrentVisibleLayer();
    }

    public void setVisibleLayerSilentlyForTesting(int layer) {
        setVisibleLayerSilently(layer);
    }

    public List<Class<?>> getVisibleAnnotationTypesForTesting() {
        if (annotations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Class<?>> visibleTypes = new ArrayList<>();
        for (SceneAnnotation annotation : annotations) {
            if (isAnnotationVisibleForCurrentLayer(annotation)) {
                visibleTypes.add(annotation.getClass());
            }
        }
        return visibleTypes;
    }

    public LytRect getStructureLibChannelSliderRectForTesting() {
        List<StructureLibSceneMetadata.ChannelData> channels = getSelectableStructureLibChannels();
        return channels.isEmpty() ? LytRect.empty()
            : resolveStructureLibChannelSliderTrackRect(
                channels.get(0)
                    .getChannelId());
    }

    public int getStructureLibChannelSliderAreaHeightForTesting() {
        return structureLibChannelSliderAreaHeight();
    }

    private void appendStructureLibHatchOverlays(List<InWorldAnnotation> inWorld) {
        if (!structureLibHatchHighlightEnabled || structureLibSceneMetadata == null) {
            return;
        }
        List<StructureLibSceneMetadata.BlockTooltipEntry> hatchEntries = structureLibSceneMetadata
            .getHatchTooltipEntries();
        if (hatchEntries.isEmpty()) {
            return;
        }

        for (StructureLibSceneMetadata.BlockTooltipEntry entry : hatchEntries) {
            if (!isBlockVisibleForCurrentLayer(entry.getY())) {
                continue;
            }
            InWorldBlockFaceOverlayAnnotation overlay = new InWorldBlockFaceOverlayAnnotation(
                entry.getX(),
                entry.getY(),
                entry.getZ(),
                new ConstantColor(StructureLibTooltipContentBuilder.resolveHatchOverlayArgb(entry.getTooltipData())),
                structureLibSceneMetadata.getHatchTooltipPositions());
            overlay.setAlwaysOnTop(true);
            overlay.setHovered(
                hoveredStructureLibHatch != null && hoveredStructureLibHatch[0] == entry.getX()
                    && hoveredStructureLibHatch[1] == entry.getY()
                    && hoveredStructureLibHatch[2] == entry.getZ());
            inWorld.add(overlay);
        }
    }

    @Nullable
    private StructureLibSceneMetadata.TierData getStructureLibTierData() {
        return structureLibSceneMetadata != null ? structureLibSceneMetadata.getTierData() : null;
    }

    @Nullable
    private StructureLibSceneMetadata.ChannelData getStructureLibChannelData(String channelId) {
        return structureLibSceneMetadata != null ? structureLibSceneMetadata.getChannelData(channelId) : null;
    }

    private boolean hasVisibleLayerSlider() {
        return visibleLayerSliderEnabled && hasVisibleLayerData();
    }

    private int visibleLayerSliderAreaHeight() {
        return hasVisibleLayerSlider() ? SCENE_SLIDER_AREA_HEIGHT : 0;
    }

    private int ponderControlAreaHeight() {
        return ponderSceneData != null ? SCENE_SLIDER_AREA_HEIGHT : 0;
    }

    public boolean hasPonderData() {
        return ponderSceneData != null;
    }

    public boolean isPonderPlaying() {
        return ponderSceneData != null && !ponderPaused && !ponderFinished;
    }

    private int structureLibTierSliderAreaHeight() {
        return hasStructureLibTierData() ? SCENE_SLIDER_AREA_HEIGHT : 0;
    }

    private int structureLibChannelSliderAreaHeight() {
        return getSelectableStructureLibChannels().size() * SCENE_SLIDER_AREA_HEIGHT;
    }

    private List<StructureLibSceneMetadata.ChannelData> getSelectableStructureLibChannels() {
        if (structureLibSceneMetadata == null) {
            return Collections.emptyList();
        }
        List<StructureLibSceneMetadata.ChannelData> selectable = new ArrayList<>();
        for (StructureLibSceneMetadata.ChannelData channelData : structureLibSceneMetadata.getChannelDataList()) {
            if (channelData != null && channelData.isSelectable()) {
                selectable.add(channelData);
            }
        }
        return selectable.isEmpty() ? Collections.emptyList() : selectable;
    }

    public void attachPonderData(PonderSceneData data, List<List<SceneAnnotation>> annotationsByKeyframe) {
        clearPonderBlockChanges();
        this.ponderSceneData = data;
        this.ponderKeyframeAnnotationSets = annotationsByKeyframe != null ? new ArrayList<>(annotationsByKeyframe)
            : new ArrayList<>();
        this.ponderCurrentTick = 0;
        this.ponderPaused = true;
        this.ponderFinished = false;
        this.draggingPonderBar = false;
        this.ponderLastKeyframeIdx = -2;
        this.ponderAnnotationFadeTick = 5;
        this.ponderOutgoingAnnotations.clear();
        this.ponderOutgoingFadeTick = 0;
        this.ponderSceneParticles.clear();
        snapshotPonderBlocks();
        updatePonderState();
    }

    public void ponderTick() {
        if (ponderSceneData == null || ponderPaused || ponderFinished) return;
        ponderCurrentTick++;
        if (ponderCurrentTick >= ponderSceneData.getTotalTime()) {
            ponderCurrentTick = ponderSceneData.getTotalTime();
            ponderFinished = true;
        }
        if (ponderAnnotationFadeTick < 5) {
            ponderAnnotationFadeTick++;
        }
        if (ponderOutgoingFadeTick > 0) {
            ponderOutgoingFadeTick--;
            if (ponderOutgoingFadeTick == 0) {
                ponderOutgoingAnnotations.clear();
            }
        }
        ponderSceneParticles.removeIf(p -> {
            p.tick();
            return p.isDead();
        });
        updatePonderState();
    }

    public void ponderTogglePlay() {
        if (ponderSceneData == null) return;
        if (ponderFinished) {
            ponderCurrentTick = 0;
            ponderFinished = false;
            ponderPaused = false;
            updatePonderState();
        } else {
            ponderPaused = !ponderPaused;
        }
    }

    public void ponderRestart() {
        if (ponderSceneData == null) return;
        ponderCurrentTick = 0;
        ponderPaused = false;
        ponderFinished = false;
        draggingPonderBar = false;
        ponderLastKeyframeIdx = -2;
        ponderAnnotationFadeTick = 0;
        ponderActiveAnnotations.clear();
        ponderOutgoingAnnotations.clear();
        ponderOutgoingFadeTick = 0;
        ponderSceneParticles.clear();
        updatePonderState();
    }

    public void ponderPrevKeyframe() {
        if (ponderSceneData == null) return;
        int targetTick = 0;
        for (PonderKeyframe kf : ponderSceneData.getKeyframes()) {
            if (kf.getTime() < ponderCurrentTick) {
                targetTick = kf.getTime();
            }
        }
        seekToTick(targetTick);
    }

    public void seekToTick(int tick) {
        if (ponderSceneData == null) return;
        ponderCurrentTick = Math.max(0, Math.min(ponderSceneData.getTotalTime(), tick));
        ponderPaused = true;
        ponderFinished = ponderCurrentTick >= ponderSceneData.getTotalTime();
        draggingPonderBar = false;
        ponderAnnotationFadeTick = 5;
        updatePonderState();
    }

    private void applyPonderBarAt(int mouseAbsX) {
        if (ponderSceneData == null || cachedPonderBarTrackRect == null) return;
        int barLeft = cachedPonderBarTrackRect.x();
        int barWidth = cachedPonderBarTrackRect.width();
        if (barWidth <= 0) return;
        float fraction = (mouseAbsX - barLeft) / (float) barWidth;
        fraction = Math.max(0f, Math.min(1f, fraction));
        int tick = Math.round(fraction * ponderSceneData.getTotalTime());
        ponderCurrentTick = Math.max(0, Math.min(ponderSceneData.getTotalTime(), tick));
        ponderPaused = true;
        ponderFinished = ponderCurrentTick >= ponderSceneData.getTotalTime();
        updatePonderState();
    }

    public boolean containsPonderBar(int mouseX, int mouseY) {
        if (ponderSceneData == null || cachedPonderBarHitRect == null || cachedPonderBarHitRect.isEmpty()) {
            return false;
        }
        return cachedPonderBarHitRect.contains(mouseX, mouseY);
    }

    private void updatePonderState() {
        if (ponderSceneData == null) return;
        int activeIdx = ponderSceneData.resolveActiveKeyframeIndex(ponderCurrentTick);

        if (activeIdx != ponderLastKeyframeIdx) {
            boolean wasAtValidKeyframe = ponderLastKeyframeIdx >= 0;
            ponderLastKeyframeIdx = activeIdx;

            // Move current overlay annotations to outgoing so they fade out smoothly.
            if (!ponderPaused && !ponderActiveAnnotations.isEmpty()) {
                ponderOutgoingAnnotations.clear();
                for (SceneAnnotation s : ponderActiveAnnotations) {
                    if (s instanceof OverlayAnnotation) {
                        ponderOutgoingAnnotations.add(s);
                    }
                }
                ponderOutgoingFadeTick = ponderAnnotationFadeTick;
                // If the fade budget is already zero, discard immediately instead of
                // accumulating stale annotations that would never be cleaned up.
                if (ponderOutgoingFadeTick == 0) {
                    ponderOutgoingAnnotations.clear();
                }
            } else {
                ponderOutgoingAnnotations.clear();
                ponderOutgoingFadeTick = 0;
            }

            ponderAnnotationFadeTick = ponderPaused ? 5 : 0;
            if (!ponderBlockSnapshot.isEmpty()) {
                // Trigger particle effects only during forward playback, not on seek or initial load.
                boolean triggerParticles = !ponderPaused && wasAtValidKeyframe;
                applyPonderBlockChanges(activeIdx, triggerParticles);
            }
        }

        ponderActiveAnnotations.clear();
        if (activeIdx >= 0 && activeIdx < ponderKeyframeAnnotationSets.size()) {
            List<SceneAnnotation> anns = ponderKeyframeAnnotationSets.get(activeIdx);
            if (anns != null) ponderActiveAnnotations.addAll(anns);
        }

        float[] activeCam = resolveFullCameraAt(activeIdx);

        if (activeIdx >= 0 && activeIdx < ponderSceneData.getKeyframeCount() - 1) {
            PonderKeyframe kfA = ponderSceneData.getKeyframe(activeIdx);
            PonderKeyframe kfB = ponderSceneData.getKeyframe(activeIdx + 1);
            int segDur = kfB.getTime() - kfA.getTime();
            if (segDur > 0) {
                Integer easeTicks = kfB.getCameraEaseTicks();
                float t;
                if (easeTicks != null && easeTicks == 0) {
                    t = 1.0f;
                } else {
                    int elapsed = ponderCurrentTick - kfA.getTime();
                    int useDur = (easeTicks != null && easeTicks > 0) ? Math.min(easeTicks, segDur) : segDur;
                    t = elapsed >= useDur ? 1.0f : easeInOut(elapsed / (float) useDur);
                }
                float[] nextCam = resolveFullCameraAt(activeIdx + 1);
                activeCam[0] = lerp(activeCam[0], nextCam[0], t);
                activeCam[1] = lerp(activeCam[1], nextCam[1], t);
                activeCam[2] = lerp(activeCam[2], nextCam[2], t);
                activeCam[3] = lerp(activeCam[3], nextCam[3], t);
                activeCam[4] = lerp(activeCam[4], nextCam[4], t);
                activeCam[5] = lerp(activeCam[5], nextCam[5], t);
            }
        }

        ponderCamZoom = activeCam[0];
        ponderCamRotX = activeCam[1];
        ponderCamRotY = activeCam[2];
        ponderCamRotZ = activeCam[3];
        ponderCamOffX = activeCam[4];
        ponderCamOffY = activeCam[5];
    }

    private float[] resolveFullCameraAt(int kfIndex) {
        float[] result = new float[] { camera.getZoom(), camera.getRotationX(), camera.getRotationY(),
            camera.getRotationZ(), camera.getOffsetX(), camera.getOffsetY() };
        boolean[] resolved = new boolean[6];
        int upper = Math.min(kfIndex, ponderSceneData.getKeyframeCount() - 1);
        for (int i = upper; i >= 0; i--) {
            PonderKeyframe kf = ponderSceneData.getKeyframe(i);
            if (kf == null) continue;
            PonderKeyframeCameraState cs = kf.getCamera();
            if (cs == null) continue;
            if (!resolved[0] && cs.getZoom() != null) {
                result[0] = cs.getZoom();
                resolved[0] = true;
            }
            if (!resolved[1] && cs.getRotX() != null) {
                result[1] = cs.getRotX();
                resolved[1] = true;
            }
            if (!resolved[2] && cs.getRotY() != null) {
                result[2] = cs.getRotY();
                resolved[2] = true;
            }
            if (!resolved[3] && cs.getRotZ() != null) {
                result[3] = cs.getRotZ();
                resolved[3] = true;
            }
            if (!resolved[4] && cs.getOffX() != null) {
                result[4] = cs.getOffX();
                resolved[4] = true;
            }
            if (!resolved[5] && cs.getOffY() != null) {
                result[5] = cs.getOffY();
                resolved[5] = true;
            }
            if (resolved[0] && resolved[1] && resolved[2] && resolved[3] && resolved[4] && resolved[5]) break;
        }
        return result;
    }

    private static float easeInOut(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float inv = -2f * t + 2f;
        return t < 0.5f ? 2f * t * t : 1f - inv * inv / 2f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Nullable
    private static Block resolveBlock(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        return (Block) Block.blockRegistry.getObject(name);
    }

    private void snapshotPonderBlocks() {
        ponderBlockSnapshot.clear();
        if (ponderSceneData == null) return;
        for (PonderKeyframe kf : ponderSceneData.getKeyframes()) {
            for (PonderKeyframeBlockChange bc : kf.getBlockChanges()) {
                long key = GuidebookLevel.packPos(bc.getX(), bc.getY(), bc.getZ());
                if (!ponderBlockSnapshot.containsKey(key)) {
                    int bx = bc.getX(), by = bc.getY(), bz = bc.getZ();
                    Block existing = level.getBlock(bx, by, bz);
                    int meta = level.getBlockMetadata(bx, by, bz);
                    ponderBlockSnapshot
                        .put(key, new PonderBlockInfo(bx, by, bz, existing == Blocks.air ? null : existing, meta));
                }
            }
        }
    }

    private void restoreFromPonderSnapshot() {
        for (PonderBlockInfo info : ponderBlockSnapshot.values()) {
            level.setBlock(info.x, info.y, info.z, info.initialBlock, info.initialMeta);
        }
    }

    private void clearPonderBlockChanges() {
        restoreFromPonderSnapshot();
        ponderBlockSnapshot.clear();
    }

    private void applyPonderBlockChanges(int upToKeyframeIdx, boolean triggerParticles) {
        restoreFromPonderSnapshot();
        if (upToKeyframeIdx < 0 || ponderSceneData == null) return;
        for (int i = 0; i <= upToKeyframeIdx && i < ponderSceneData.getKeyframeCount(); i++) {
            PonderKeyframe kf = ponderSceneData.getKeyframe(i);
            if (kf == null) continue;
            boolean isTarget = i == upToKeyframeIdx;
            for (PonderKeyframeBlockChange bc : kf.getBlockChanges()) {
                Block oldBlock = level.getBlock(bc.getX(), bc.getY(), bc.getZ());
                int oldMeta = level.getBlockMetadata(bc.getX(), bc.getY(), bc.getZ());
                Block newBlock = resolveBlock(bc.getBlock());
                TileEntity te = resolvePonderBlockTileEntity(bc, newBlock);
                level.setBlock(bc.getX(), bc.getY(), bc.getZ(), newBlock, bc.getMeta(), te);
                if (triggerParticles && isTarget && bc.shouldSpawnParticles()) {
                    boolean removing = newBlock == null || newBlock == Blocks.air;
                    Block particleBlock = removing ? oldBlock : newBlock;
                    int particleMeta = removing ? oldMeta : bc.getMeta();
                    if (particleBlock != null && particleBlock != Blocks.air) {
                        spawnBlockParticles(bc.getX(), bc.getY(), bc.getZ(), particleBlock, particleMeta);
                    }
                }
            }
        }
    }

    private void spawnBlockParticles(int bx, int by, int bz, Block block, int meta) {
        IIcon icon = null;
        try {
            icon = block.getIcon(1, meta);
        } catch (Exception ignored) {}
        if (icon == null) return;

        float iconW = icon.getMaxU() - icon.getMinU();
        float iconH = icon.getMaxV() - icon.getMinV();
        if (iconW <= 0 || iconH <= 0) return;

        Random rng = ponderParticleRng;
        int count = 4 + rng.nextInt(5);
        for (int i = 0; i < count; i++) {
            float px = bx + 0.1f + rng.nextFloat() * 0.8f;
            float py = by + 0.1f + rng.nextFloat() * 0.8f;
            float pz = bz + 0.1f + rng.nextFloat() * 0.8f;

            float angle = rng.nextFloat() * (float) (Math.PI * 2.0);
            float hSpeed = 0.05f + rng.nextFloat() * 0.12f;
            float vx = (float) Math.cos(angle) * hSpeed;
            float vy = 0.1f + rng.nextFloat() * 0.15f;
            float vz = (float) Math.sin(angle) * hSpeed;

            float uFrac = rng.nextFloat() * 0.75f;
            float vFrac = rng.nextFloat() * 0.75f;
            float u0 = icon.getMinU() + uFrac * iconW;
            float v0 = icon.getMinV() + vFrac * iconH;
            float u1 = u0 + iconW * 0.25f;
            float v1 = v0 + iconH * 0.25f;

            int maxAge = 8 + rng.nextInt(8);
            float size = 0.045f + rng.nextFloat() * 0.03f;
            ponderSceneParticles
                .add(new GuidebookSceneParticle(px, py, pz, vx, vy, vz, u0, v0, u1, v1, 1f, 1f, 1f, maxAge, size));
        }
    }

    @Nullable
    private TileEntity resolvePonderBlockTileEntity(PonderKeyframeBlockChange bc, @Nullable Block block) {
        if (block == null) return null;
        String nbtStr = bc.getNbt();
        NBTTagCompound tag = null;
        if (nbtStr != null && !nbtStr.isEmpty()) {
            try {
                tag = GuideTextNbtCodec.readTextSafeCompound(nbtStr);
            } catch (Exception ignored) {}
        }
        if (tag == null && !block.hasTileEntity(bc.getMeta())) return null;
        return GuidebookTileEntityLoader
            .load(level.getOrCreateFakeWorld(), block, bc.getMeta(), bc.getX(), bc.getY(), bc.getZ(), tag);
    }

    /** Compact holder for the initial block state at a ponder-changed position. */
    private static final class PonderBlockInfo {

        final int x;
        final int y;
        final int z;
        @Nullable
        final Block initialBlock;
        final int initialMeta;

        PonderBlockInfo(int x, int y, int z, @Nullable Block initialBlock, int initialMeta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.initialBlock = initialBlock;
            this.initialMeta = initialMeta;
        }
    }

    private void drawPonderControls(RenderContext context, LytRect outerRect) {
        int totalControlH = getBottomControlAreaHeight();
        int renderRowY = outerRect.bottom() - totalControlH;
        int absRowY = lastOuterAbsY + lastOuterH - totalControlH;

        int rowH = SCENE_SLIDER_AREA_HEIGHT;
        int btnSize = SCENE_SLIDER_AREA_HEIGHT;

        int renderBtnX = outerRect.x() + SCENE_SLIDER_SIDE_PADDING;
        int absBtnX = lastOuterAbsX + SCENE_SLIDER_SIDE_PADDING;

        cachedPonderBtnAbsX = absBtnX;
        cachedPonderBtnAbsY = absRowY;

        int[] mousePos = resolveCurrentMousePosition();
        int mx = mousePos != null ? mousePos[0] : -1;
        int my = mousePos != null ? mousePos[1] : -1;

        GuideIconButton.Role[] btnRoles = { GuideIconButton.Role.PONDER_PREV_KEYFRAME,
            GuideIconButton.Role.PONDER_PLAY_PAUSE, GuideIconButton.Role.PONDER_RESTART };

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        for (int i = 0; i < btnRoles.length; i++) {
            int absX = absBtnX + i * btnSize;
            boolean hover = mx >= absX && mx < absX + btnSize && my >= absRowY && my < absRowY + btnSize;
            boolean active = btnRoles[i] == GuideIconButton.Role.PONDER_PLAY_PAUSE && !ponderPaused && !ponderFinished;
            int color = GuideIconButton.resolveIconColor(true, hover, active);
            int drawX = renderBtnX + i * btnSize;
            GuideIconButton.drawIcon(mc, btnRoles[i], drawX, renderRowY, btnSize, btnSize, color);
        }

        int renderBarLeft = renderBtnX + PONDER_BTN_TOTAL_WIDTH + 2;
        int renderBarRight = outerRect.right() - SCENE_SLIDER_SIDE_PADDING;
        int renderBarW = renderBarRight - renderBarLeft;
        if (renderBarW < 4) return;
        int renderBarY = renderRowY + (rowH - GuideSliderRenderer.TRACK_HEIGHT) / 2;

        int absBarLeft = absBtnX + PONDER_BTN_TOTAL_WIDTH + 2;
        int absBarRight = lastOuterAbsX + lastOuterW - SCENE_SLIDER_SIDE_PADDING;
        int absBarW = absBarRight - absBarLeft;
        int absBarY = absRowY + (rowH - GuideSliderRenderer.TRACK_HEIGHT) / 2;

        float progress = ponderSceneData.getProgress(ponderCurrentTick);
        GuideSliderRenderer.SliderGeometry renderGeom = GuideSliderRenderer
            .layout(renderBarLeft, renderBarY, renderBarW, progress);
        GuideSliderRenderer.SliderGeometry absGeom = GuideSliderRenderer.layout(absBarLeft, absBarY, absBarW, progress);

        cachedPonderBarTrackRect = absGeom.trackRect();
        cachedPonderBarHitRect = absGeom.hitRect();

        int hitPad = GuideSliderRenderer.HIT_PADDING_Y;
        boolean barHighlighted = draggingPonderBar || (mx >= absBarLeft && mx < absBarRight
            && my >= absBarY - hitPad
            && my < absBarY + GuideSliderRenderer.TRACK_HEIGHT + hitPad);
        GuideSliderRenderer.render(Gui::drawRect, renderGeom, barHighlighted);

        drawPonderKeyframeNodes(renderBarLeft, renderBarW, renderBarY, rowH, absBarLeft, absBarW, absBarY, mx, my);
    }

    private void drawPonderKeyframeNodes(int renderBarLeft, int renderBarW, int renderBarY, int rowH, int absBarLeft,
        int absBarW, int absBarY, int mx, int my) {
        if (ponderSceneData == null || renderBarW <= 0) return;
        int totalTime = ponderSceneData.getTotalTime();
        int nodeW = 2;
        int nodeH = rowH - 2;
        int nodeRenderY = renderBarY - (nodeH - GuideSliderRenderer.TRACK_HEIGHT) / 2;
        int hitPad = GuideSliderRenderer.HIT_PADDING_Y;

        for (PonderKeyframe kf : ponderSceneData.getKeyframes()) {
            float frac = totalTime > 0 ? kf.getTime() / (float) totalTime : 0f;
            int renderNx = renderBarLeft + Math.round(frac * (renderBarW - nodeW));
            int absNx = absBarLeft + Math.round(frac * (absBarW - nodeW));
            boolean hovered = mx >= absNx - 2 && mx < absNx + nodeW + 4
                && my >= absBarY - hitPad
                && my < absBarY + GuideSliderRenderer.TRACK_HEIGHT + hitPad;
            int nodeColor = hovered ? PONDER_KEYFRAME_NODE_HOVER_COLOR : PONDER_KEYFRAME_NODE_COLOR;
            int drawH = hovered ? nodeH + 2 : nodeH;
            int drawY = hovered ? nodeRenderY - 1 : nodeRenderY;
            Gui.drawRect(renderNx, drawY, renderNx + nodeW, drawY + drawH, nodeColor);

            if (hovered) {
                String label = kf.getLabel();
                if (label != null && !label.isEmpty()) {
                    Minecraft mc = Minecraft.getMinecraft();
                    boolean isAhead = ponderCurrentTick < kf.getTime();
                    int textX = isAhead ? renderNx - mc.fontRenderer.getStringWidth(label) - 4 : renderNx + nodeW + 4;
                    mc.fontRenderer.drawStringWithShadow(label, textX, nodeRenderY, PONDER_KEYFRAME_NODE_HOVER_COLOR);
                }
            }
        }
    }

    private void drawBottomControls(RenderContext context, LytRect outerRect) {
        clearCachedVisibleLayerSliderRects();
        clearCachedTierSliderRects();
        clearCachedChannelSliderRects();
        cachedPonderBarTrackRect = null;
        cachedPonderBarHitRect = null;
        logBottomControlState("evaluate", outerRect);
        if (getBottomControlAreaHeight() <= 0) {
            logBottomControlState("skip-empty", outerRect);
            return;
        }
        if (ponderSceneData != null) {
            drawPonderControls(context, outerRect);
        }
        if (!isPonderPlaying()) {
            if (hasStructureLibTierData()) {
                drawStructureLibTierSlider(context, outerRect);
            }
            if (hasVisibleLayerSlider()) {
                drawVisibleLayerSlider(context, outerRect);
            }
            for (StructureLibSceneMetadata.ChannelData channelData : getSelectableStructureLibChannels()) {
                drawStructureLibChannelSlider(context, outerRect, channelData);
            }
        }
    }

    private int getBottomControlRowCount() {
        return (ponderSceneData != null ? 1 : 0) + (hasStructureLibTierData() ? 1 : 0)
            + (hasVisibleLayerSlider() ? 1 : 0)
            + getSelectableStructureLibChannels().size();
    }

    private int @Nullable [] resolveCurrentMousePosition() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            int scaledWidth = DisplayScale.scaledWidth();
            int scaledHeight = DisplayScale.scaledHeight();
            return new int[] { Mouse.getX() * scaledWidth / mc.displayWidth,
                scaledHeight - Mouse.getY() * scaledHeight / mc.displayHeight - 1 };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int bottomControlAreaTop(int outerBottom) {
        return outerBottom - getBottomControlAreaHeight();
    }

    private int resolveVisibleLayerRowIndex() {
        if (!hasVisibleLayerSlider()) {
            return -1;
        }
        return (ponderSceneData != null ? 1 : 0) + (hasStructureLibTierData() ? 1 : 0);
    }

    private int resolveStructureLibTierRowIndex() {
        return hasStructureLibTierData() ? (ponderSceneData != null ? 1 : 0) : -1;
    }

    private int resolveStructureLibChannelRowIndex(String channelId) {
        List<StructureLibSceneMetadata.ChannelData> channels = getSelectableStructureLibChannels();
        int ponderOffset = ponderSceneData != null ? 1 : 0;
        int index = 0;
        for (StructureLibSceneMetadata.ChannelData channelData : channels) {
            if (channelData.getChannelId()
                .equals(StructureLibPreviewSelection.normalizeChannelId(channelId))) {
                return ponderOffset + (hasStructureLibTierData() ? 1 : 0) + (hasVisibleLayerSlider() ? 1 : 0) + index;
            }
            index++;
        }
        return -1;
    }

    private LytRect resolveSliderTrackRect(int rowIndex) {
        int originX = lastOuterW > 0 ? lastOuterAbsX : getBounds().x();
        int originY = lastOuterH > 0 ? lastOuterAbsY : getBounds().y();
        int outerWidth = lastOuterW > 0 ? lastOuterW
            : Math.max(16, layoutSceneWidth > 0 ? layoutSceneWidth : this.width);
        int outerHeight = lastOuterH > 0 ? lastOuterH
            : Math.max(getBounds().height(), this.height + getBottomControlAreaHeight());
        return resolveSliderTrackRect(originX, originY, outerWidth, outerHeight, rowIndex);
    }

    private LytRect resolveSliderTrackRect(int originX, int originY, int outerWidth, int outerHeight, int rowIndex) {
        if (rowIndex < 0 || outerWidth <= SCENE_SLIDER_SIDE_PADDING * 2 || getBottomControlRowCount() <= 0) {
            return LytRect.empty();
        }
        int totalControlHeight = getBottomControlAreaHeight();
        if (outerHeight < totalControlHeight) {
            return LytRect.empty();
        }
        int rowTop = originY + outerHeight - totalControlHeight + rowIndex * SCENE_SLIDER_AREA_HEIGHT;
        int sliderX = originX + SCENE_SLIDER_SIDE_PADDING;
        int sliderWidth = Math.max(24, outerWidth - SCENE_SLIDER_SIDE_PADDING * 2);
        int sliderY = rowTop + (SCENE_SLIDER_AREA_HEIGHT - GuideSliderRenderer.TRACK_HEIGHT) / 2;
        return new LytRect(sliderX, sliderY, sliderWidth, GuideSliderRenderer.TRACK_HEIGHT);
    }

    private void clearCachedVisibleLayerSliderRects() {
        cachedVisibleLayerSliderRect = LytRect.empty();
        cachedVisibleLayerSliderHitRect = LytRect.empty();
    }

    private void clearCachedTierSliderRects() {
        cachedTierSliderRect = LytRect.empty();
        cachedTierSliderHitRect = LytRect.empty();
    }

    private void clearCachedChannelSliderRects() {
        cachedChannelSliderRects.clear();
        cachedChannelSliderHitRects.clear();
    }

    private LytRect resolveVisibleLayerSliderTrackRect() {
        return resolveSliderTrackRect(resolveVisibleLayerRowIndex());
    }

    private LytRect resolveVisibleLayerSliderHitRect() {
        if (cachedVisibleLayerSliderHitRect != null && !cachedVisibleLayerSliderHitRect.isEmpty()) {
            return cachedVisibleLayerSliderHitRect;
        }
        LytRect trackRect = resolveVisibleLayerSliderTrackRect();
        if (trackRect.isEmpty()) {
            return LytRect.empty();
        }
        return GuideSliderRenderer.layout(trackRect.x(), trackRect.y(), trackRect.width(), getVisibleLayerFraction())
            .hitRect();
    }

    private void drawVisibleLayerSlider(RenderContext context, LytRect outerRect) {
        int rowIndex = resolveVisibleLayerRowIndex();
        LytRect screenTrackRect = resolveVisibleLayerSliderTrackRect();
        LytRect renderTrackRect = resolveSliderTrackRect(
            outerRect.x(),
            outerRect.y(),
            outerRect.width(),
            outerRect.height(),
            rowIndex);
        if (screenTrackRect.isEmpty() || renderTrackRect.isEmpty()) {
            logSliderSkipped("visible-layer", resolveVisibleLayerRowIndex(), outerRect);
            return;
        }
        float fraction = getVisibleLayerFraction();
        GuideSliderRenderer.SliderGeometry screenGeometry = GuideSliderRenderer
            .layout(screenTrackRect.x(), screenTrackRect.y(), screenTrackRect.width(), fraction);
        GuideSliderRenderer.SliderGeometry renderGeometry = GuideSliderRenderer
            .layout(renderTrackRect.x(), renderTrackRect.y(), renderTrackRect.width(), fraction);
        cachedVisibleLayerSliderRect = screenGeometry.trackRect();
        cachedVisibleLayerSliderHitRect = screenGeometry.hitRect();
        boolean highlighted = draggingVisibleLayerSlider;
        if (!highlighted) {
            int[] mouse = resolveCurrentMousePosition();
            highlighted = mouse != null && screenGeometry.hitRect()
                .contains(mouse[0], mouse[1]);
        }
        logSliderGeometry("visible-layer", screenGeometry, rowIndex, getVisibleLayerSliderLabel(), outerRect);
        drawSlider(
            context,
            renderGeometry,
            highlighted,
            outerRect,
            rowIndex,
            getVisibleLayerSliderLabel(),
            VISIBLE_LAYER_SLIDER_TEXT_STYLE);
    }

    private void applyVisibleLayerSliderAt(int mouseX) {
        int visibleLayerCount = getVisibleLayerCount();
        LytRect sliderTrackRect = resolveVisibleLayerSliderTrackRect();
        if (visibleLayerCount <= 0 || sliderTrackRect.isEmpty()) {
            return;
        }
        float fraction = GuideSliderRenderer.fractionFromMouse(mouseX, sliderTrackRect.x(), sliderTrackRect.width());
        int targetLayer = Math.round(fraction * visibleLayerCount);
        setVisibleLayer(targetLayer);
    }

    private float getVisibleLayerFraction() {
        int visibleLayerCount = getVisibleLayerCount();
        if (visibleLayerCount <= 0) {
            return 0f;
        }
        return resolveCurrentVisibleLayer() / (float) visibleLayerCount;
    }

    private void nudgeVisibleLayer(int dwheel) {
        setVisibleLayer(resolveCurrentVisibleLayer() + Integer.signum(dwheel));
    }

    private LytRect resolveStructureLibTierSliderTrackRect() {
        return resolveSliderTrackRect(resolveStructureLibTierRowIndex());
    }

    private LytRect resolveStructureLibTierSliderHitRect() {
        if (cachedTierSliderHitRect != null && !cachedTierSliderHitRect.isEmpty()) {
            return cachedTierSliderHitRect;
        }
        LytRect trackRect = resolveStructureLibTierSliderTrackRect();
        if (trackRect.isEmpty()) {
            return LytRect.empty();
        }
        return GuideSliderRenderer
            .layout(trackRect.x(), trackRect.y(), trackRect.width(), getStructureLibTierFraction())
            .hitRect();
    }

    private void drawStructureLibTierSlider(RenderContext context, LytRect outerRect) {
        int rowIndex = resolveStructureLibTierRowIndex();
        LytRect screenTrackRect = resolveStructureLibTierSliderTrackRect();
        LytRect renderTrackRect = resolveSliderTrackRect(
            outerRect.x(),
            outerRect.y(),
            outerRect.width(),
            outerRect.height(),
            rowIndex);
        if (screenTrackRect.isEmpty() || renderTrackRect.isEmpty()) {
            logSliderSkipped("structurelib-tier", resolveStructureLibTierRowIndex(), outerRect);
            return;
        }
        float fraction = getStructureLibTierFraction();
        GuideSliderRenderer.SliderGeometry screenGeometry = GuideSliderRenderer
            .layout(screenTrackRect.x(), screenTrackRect.y(), screenTrackRect.width(), fraction);
        GuideSliderRenderer.SliderGeometry renderGeometry = GuideSliderRenderer
            .layout(renderTrackRect.x(), renderTrackRect.y(), renderTrackRect.width(), fraction);
        cachedTierSliderRect = screenGeometry.trackRect();
        cachedTierSliderHitRect = screenGeometry.hitRect();
        boolean highlighted = draggingStructureLibTierSlider;
        if (!highlighted) {
            int[] mouse = resolveCurrentMousePosition();
            highlighted = mouse != null && screenGeometry.hitRect()
                .contains(mouse[0], mouse[1]);
        }
        logSliderGeometry("structurelib-tier", screenGeometry, rowIndex, getStructureLibTierSliderLabel(), outerRect);
        drawSlider(
            context,
            renderGeometry,
            highlighted,
            outerRect,
            rowIndex,
            getStructureLibTierSliderLabel(),
            STRUCTURELIB_TIER_SLIDER_TEXT_STYLE);
    }

    private void applyStructureLibTierSliderAt(int mouseX) {
        StructureLibSceneMetadata.TierData tierData = getStructureLibTierData();
        LytRect sliderTrackRect = resolveStructureLibTierSliderTrackRect();
        if (tierData == null || sliderTrackRect.isEmpty()) {
            return;
        }
        int minValue = tierData.getMinValue();
        int maxValue = tierData.getMaxValue();
        if (maxValue <= minValue) {
            setStructureLibCurrentTier(minValue);
            return;
        }
        float fraction = GuideSliderRenderer.fractionFromMouse(mouseX, sliderTrackRect.x(), sliderTrackRect.width());
        int nextValue = minValue + Math.round(Math.max(0f, Math.min(1f, fraction)) * (maxValue - minValue));
        setStructureLibCurrentTier(clampChannelValue(nextValue, minValue, maxValue));
    }

    private float getStructureLibTierFraction() {
        StructureLibSceneMetadata.TierData tierData = getStructureLibTierData();
        if (tierData == null || tierData.getMaxValue() <= tierData.getMinValue()) {
            return 0f;
        }
        return (structureLibCurrentTier - tierData.getMinValue())
            / (float) (tierData.getMaxValue() - tierData.getMinValue());
    }

    private void nudgeStructureLibTier(int dwheel) {
        setStructureLibCurrentTier(structureLibCurrentTier + Integer.signum(dwheel));
    }

    private Map<String, LytRect> resolveStructureLibChannelSliderHitRects() {
        if (!cachedChannelSliderHitRects.isEmpty()) {
            return cachedChannelSliderHitRects;
        }
        for (StructureLibSceneMetadata.ChannelData channelData : getSelectableStructureLibChannels()) {
            LytRect trackRect = resolveStructureLibChannelSliderTrackRect(channelData.getChannelId());
            if (trackRect.isEmpty()) {
                continue;
            }
            cachedChannelSliderHitRects.put(
                channelData.getChannelId(),
                GuideSliderRenderer
                    .layout(
                        trackRect.x(),
                        trackRect.y(),
                        trackRect.width(),
                        getStructureLibChannelFraction(channelData))
                    .hitRect());
        }
        return cachedChannelSliderHitRects;
    }

    @Nullable
    private String resolveHoveredStructureLibChannelId(int mouseX, int mouseY) {
        for (Map.Entry<String, LytRect> entry : resolveStructureLibChannelSliderHitRects().entrySet()) {
            if (!entry.getValue()
                .isEmpty() && entry.getValue()
                    .contains(mouseX, mouseY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private LytRect resolveStructureLibChannelSliderTrackRect(String channelId) {
        return resolveSliderTrackRect(resolveStructureLibChannelRowIndex(channelId));
    }

    private void drawStructureLibChannelSlider(RenderContext context, LytRect outerRect,
        StructureLibSceneMetadata.ChannelData channelData) {
        int rowIndex = resolveStructureLibChannelRowIndex(channelData.getChannelId());
        LytRect screenTrackRect = resolveStructureLibChannelSliderTrackRect(channelData.getChannelId());
        LytRect renderTrackRect = resolveSliderTrackRect(
            outerRect.x(),
            outerRect.y(),
            outerRect.width(),
            outerRect.height(),
            rowIndex);
        if (screenTrackRect.isEmpty() || renderTrackRect.isEmpty()) {
            logSliderSkipped(
                "structurelib-channel:" + channelData.getChannelId(),
                resolveStructureLibChannelRowIndex(channelData.getChannelId()),
                outerRect);
            return;
        }
        float fraction = getStructureLibChannelFraction(channelData);
        GuideSliderRenderer.SliderGeometry screenGeometry = GuideSliderRenderer
            .layout(screenTrackRect.x(), screenTrackRect.y(), screenTrackRect.width(), fraction);
        GuideSliderRenderer.SliderGeometry renderGeometry = GuideSliderRenderer
            .layout(renderTrackRect.x(), renderTrackRect.y(), renderTrackRect.width(), fraction);
        cachedChannelSliderRects.put(channelData.getChannelId(), screenGeometry.trackRect());
        cachedChannelSliderHitRects.put(channelData.getChannelId(), screenGeometry.hitRect());
        boolean highlighted = channelData.getChannelId()
            .equals(draggingStructureLibChannelId);
        if (!highlighted) {
            int[] mouse = resolveCurrentMousePosition();
            highlighted = mouse != null && screenGeometry.hitRect()
                .contains(mouse[0], mouse[1]);
        }
        logSliderGeometry(
            "structurelib-channel:" + channelData.getChannelId(),
            screenGeometry,
            rowIndex,
            getStructureLibChannelSliderLabel(channelData),
            outerRect);
        drawSlider(
            context,
            renderGeometry,
            highlighted,
            outerRect,
            rowIndex,
            getStructureLibChannelSliderLabel(channelData),
            STRUCTURELIB_CHANNEL_SLIDER_TEXT_STYLE);
    }

    private void applyStructureLibChannelSliderAt(String channelId, int mouseX) {
        StructureLibSceneMetadata.ChannelData channelData = getStructureLibChannelData(channelId);
        LytRect sliderTrackRect = resolveStructureLibChannelSliderTrackRect(channelId);
        if (channelData == null || sliderTrackRect.isEmpty()) {
            return;
        }
        applyStructureLibChannelFraction(
            channelData,
            GuideSliderRenderer.fractionFromMouse(mouseX, sliderTrackRect.x(), sliderTrackRect.width()));
    }

    private void applyStructureLibChannelFraction(StructureLibSceneMetadata.ChannelData channelData, float fraction) {
        if (channelData == null) {
            return;
        }
        int minValue = channelData.getMinValue();
        int maxValue = channelData.getMaxValue();
        if (maxValue <= minValue) {
            setStructureLibChannelValue(channelData.getChannelId(), minValue);
            return;
        }
        int nextValue = minValue + Math.round(Math.max(0f, Math.min(1f, fraction)) * (maxValue - minValue));
        setStructureLibChannelValue(channelData.getChannelId(), clampChannelValue(nextValue, minValue, maxValue));
    }

    private float getStructureLibChannelFraction(StructureLibSceneMetadata.ChannelData channelData) {
        if (channelData == null || channelData.getMaxValue() <= channelData.getMinValue()) {
            return 0f;
        }
        return (getStructureLibChannelValue(channelData.getChannelId()) - channelData.getMinValue())
            / (float) (channelData.getMaxValue() - channelData.getMinValue());
    }

    private void nudgeStructureLibChannel(String channelId, int dwheel) {
        StructureLibSceneMetadata.ChannelData channelData = getStructureLibChannelData(channelId);
        if (channelData == null) {
            return;
        }
        setStructureLibChannelValue(channelId, getStructureLibChannelValue(channelId) + Integer.signum(dwheel));
    }

    private void drawSlider(RenderContext context, GuideSliderRenderer.SliderGeometry geometry, boolean highlighted,
        LytRect outerRect, int rowIndex, String label, ResolvedTextStyle style) {
        GuideSliderRenderer.render(Gui::drawRect, geometry, highlighted);
        int textWidth = context.getStringWidth(label, style);
        int textHeight = context.getLineHeight(style);
        int textX = outerRect.x() + (outerRect.width() - textWidth) / 2;
        int rowTop = bottomControlAreaTop(outerRect.bottom()) + rowIndex * SCENE_SLIDER_AREA_HEIGHT;
        int textY = rowTop + (SCENE_SLIDER_AREA_HEIGHT - textHeight) / 2;
        context.drawText(label, textX, textY, style);
    }

    private void logBottomControlState(String phase, LytRect outerRect) {
        // Skip the per-frame string construction (key + describeRect + varargs boxing) entirely
        // when debug logging is disabled. logInfoOnce itself also re-checks, but only after we
        // have already paid for the formatting work.
        if (!GuideDebugLog.isEnabled()) {
            return;
        }
        int selectableChannels = getSelectableStructureLibChannels().size();
        GuideGregTechTileSupport.logInfoOnce(
            "scene-bottom-controls:" + Integer.toHexString(System.identityHashCode(this))
                + ":"
                + phase
                + ":"
                + getBottomControlAreaHeight()
                + ":"
                + getBottomControlRowCount()
                + ":"
                + selectableChannels,
            "Scene bottom controls {}: outerRect={} bottomVisible={} visibleLayerEnabled={} visibleLayerCount={} hasVisibleLayerSlider={} hasTierSlider={} selectableChannels={} bottomAreaHeight={} rowCount={}",
            phase,
            describeRect(outerRect),
            bottomControlsVisible,
            visibleLayerSliderEnabled,
            getVisibleLayerCount(),
            hasVisibleLayerSlider(),
            hasStructureLibTierData(),
            selectableChannels,
            getBottomControlAreaHeight(),
            getBottomControlRowCount());
    }

    private void logSliderSkipped(String sliderId, int rowIndex, LytRect outerRect) {
        if (!GuideDebugLog.isEnabled()) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "scene-slider-skip:" + Integer.toHexString(
                System.identityHashCode(this)) + ":" + sliderId + ":" + rowIndex + ":" + getBottomControlAreaHeight(),
            "Scene slider skipped {}: rowIndex={} outerRect={} bottomAreaHeight={} rowCount={} lastOuter={}x{}",
            sliderId,
            rowIndex,
            describeRect(outerRect),
            getBottomControlAreaHeight(),
            getBottomControlRowCount(),
            lastOuterW,
            lastOuterH);
    }

    private void logSliderGeometry(String sliderId, GuideSliderRenderer.SliderGeometry geometry, int rowIndex,
        String label, LytRect outerRect) {
        if (!GuideDebugLog.isEnabled()) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "scene-slider-geometry:" + Integer.toHexString(System.identityHashCode(this))
                + ":"
                + sliderId
                + ":"
                + rowIndex
                + ":"
                + describeRect(geometry.trackRect()),
            "Scene slider geometry {}: rowIndex={} outerRect={} track={} fill={} thumb={} hit={} label={}",
            sliderId,
            rowIndex,
            describeRect(outerRect),
            describeRect(geometry.trackRect()),
            describeRect(geometry.fillRect()),
            describeRect(geometry.thumbRect()),
            describeRect(geometry.hitRect()),
            label);
    }

    public static String describeRect(@Nullable LytRect rect) {
        if (rect == null) {
            return "null-rect";
        }
        if (rect.isEmpty()) {
            return "empty-rect";
        }
        return "(" + rect.x() + "," + rect.y() + " " + rect.width() + "x" + rect.height() + ")";
    }

    public static int clampChannelValue(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return Math.min(value, maxValue);
    }
}
