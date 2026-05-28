package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.ResponsiveVisualSizing;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.internal.ui.GuideSliderRenderer;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBlockFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.PonderInputAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneFloorGridAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheEntry;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityLoader;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityStateSupport;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookTileEntityLoader;
import com.hfstudio.guidenh.guide.scene.ponder.PonderEntityAnimationRuntimeSupport;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframe;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeBlockChange;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeCameraState;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeEntityAction;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeEntityAnimation;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeParticle;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeTileNbtOperation;
import com.hfstudio.guidenh.guide.scene.ponder.PonderNbtPath;
import com.hfstudio.guidenh.guide.scene.ponder.PonderSceneData;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityRayPicker;
import com.hfstudio.guidenh.guide.scene.support.GuideGregTechTileSupport;
import com.hfstudio.guidenh.guide.sound.GuideSoundPlayback;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.Ae2Helpers;
import com.hfstudio.guidenh.integration.ae2.Ae2PonderSupport;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibTooltipContentBuilder;

import lombok.Getter;

public class LytGuidebookScene extends LytBlock {

    public static class PonderWeatherColumnReservation {

        private final int startTick;
        private final int endTickExclusive;

        public PonderWeatherColumnReservation(int startTick, int endTickExclusive) {
            this.startTick = startTick;
            this.endTickExclusive = endTickExclusive;
        }

        public boolean overlaps(int otherStartTick, int otherEndTickExclusive) {
            return startTick < otherEndTickExclusive && otherStartTick < endTickExclusive;
        }
    }

    public static final float DRAG_ROTATE_SENSITIVITY = 0.5f;
    public static final float WHEEL_ZOOM_STEP = 1.1f;
    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 10f;
    private static final int MIN_RESPONSIVE_SCENE_SIZE = 16;
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
    public static final ResolvedTextStyle BLOCK_STATS_TEXT_STYLE = DefaultStyles.BODY_TEXT
        .mergeWith(DefaultStyles.BASE_STYLE);
    public static final ResolvedTextStyle BLOCK_STATS_COUNT_TEXT_STYLE = DefaultStyles.BODY_TEXT.toBuilder()
        .color(ConstantColor.WHITE)
        .dropShadow(true)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    public static final int BLOCK_STATS_BACKGROUND_COLOR = 0xAA111922;
    public static final int BLOCK_STATS_BORDER_COLOR = 0x66FFFFFF;
    public static final int BLOCK_STATS_PADDING_X = 5;
    public static final int BLOCK_STATS_PADDING_Y = 4;
    public static final int BLOCK_STATS_GAP = 4;
    public static final int BLOCK_STATS_ROW_GAP = 2;
    public static final int BLOCK_STATS_ITEM_SIZE = 16;
    public static final int BLOCK_STATS_MIN_WIDTH = 32;
    public static final int BLOCK_STATS_MIN_HEIGHT = 20;
    public static final int BLOCK_STATS_DEFAULT_MAX_WIDTH = 224;
    public static final int BLOCK_STATS_DEFAULT_MAX_HEIGHT = 96;
    public static final int BLOCK_STATS_SCROLLBAR_SIZE = 6;
    public static final int BLOCK_STATS_SCROLLBAR_MIN_THUMB = 12;
    public static final int BLOCK_STATS_WHEEL_STEP = 18;
    public static final int BLOCK_STATS_DOCK_GAP = 4;
    public static final int BLOCK_STATS_SELECTED_ROW_COLOR = 0x6656C8FF;
    public static final int BLOCK_STATS_HIGHLIGHT_COLOR = 0x6600F5FF;
    private static final int MAX_PONDER_PARTICLE_POOL_SIZE = 1024;

    private int dragButton = -1;
    private int dragLastX;
    private int dragLastY;
    private boolean draggingVisibleLayerSlider;
    private boolean draggingStructureLibTierSlider;
    @Nullable
    private String draggingStructureLibChannelId;
    private boolean draggingPonderBar;
    private final List<SceneSoundCue> soundCues = new ArrayList<>();

    @Nullable
    private PonderSceneData ponderSceneData;
    private List<List<SceneAnnotation>> ponderKeyframeAnnotationSets = new ArrayList<>();
    private List<List<GuideSoundSpec>> ponderKeyframeSoundSets = new ArrayList<>();
    private int ponderCurrentTick = 0;
    private boolean ponderPaused = false;
    private boolean ponderFinished = false;
    private boolean ponderExportLayerOverrideEnabled;
    private float ponderCamZoom = 1f;
    private float ponderCamRotX = 0f;
    private float ponderCamRotY = 0f;
    private float ponderCamRotZ = 0f;
    private float ponderCamOffX = 0f;
    private float ponderCamOffY = 0f;
    private final List<SceneAnnotation> ponderActiveAnnotations = new ArrayList<>();
    private final List<SceneAnnotation> ponderOutgoingAnnotations = new ArrayList<>();
    private int ponderOutgoingFadeTick = 0;
    private final ArrayList<GuidebookSceneParticle> staticSceneParticles = new ArrayList<>();
    private final ArrayList<GuidebookSceneParticle> ponderSceneParticles = new ArrayList<>();
    private final ArrayList<GuidebookSceneParticle> renderSceneParticlesScratch = new ArrayList<>();
    private final ArrayList<GuidebookSceneWeatherEffect> staticWeatherEffects = new ArrayList<>();
    private final ArrayList<GuidebookSceneWeatherEffect> ponderWeatherEffects = new ArrayList<>();
    private final ArrayList<GuidebookSceneWeatherEffect> renderWeatherEffectsScratch = new ArrayList<>();
    private final ArrayList<GuidebookSceneWeatherEffect> resolvedWeatherEffectsScratch = new ArrayList<>();
    private final ArrayDeque<GuidebookSceneParticle> ponderParticlePool = new ArrayDeque<>();
    private final Random ponderParticleRng = new Random();
    private int sceneAnimationTick = 0;
    private int ponderLastKeyframeIdx = -2;
    private int ponderAnnotationFadeTick = 5;
    private final Set<Integer> triggeredPonderSoundKeyframes = new HashSet<>();
    private final Map<Long, PonderBlockInfo> ponderBlockSnapshot = new LinkedHashMap<>();
    private final Map<String, PonderEntityRuntime> ponderEntityRefs = new LinkedHashMap<>();
    private final Map<String, PonderEntityRuntime> ponderEntityRuntimesBySceneEntityId = new HashMap<>();
    private final Map<String, LinkedHashSet<String>> ponderSceneEntityRefs = new HashMap<>();
    private final List<PonderEntityAnimationRuntimeSupport.TimedAnimation> ponderTimedEntityAnimations = new ArrayList<>();
    private final Map<String, PonderEntityAnimationRuntimeSupport.Baseline> ponderEntityAnimationBaselines = new LinkedHashMap<>();
    private final Map<Long, PonderWeatherColumnReservation> ponderWeatherColumnReservations = new LinkedHashMap<>();
    private boolean ponderTimelineBaselineReady;
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
    public static final GuideIconButton.Role[] PONDER_BUTTON_ROLES = { GuideIconButton.Role.PONDER_PREV_KEYFRAME,
        GuideIconButton.Role.PONDER_PLAY_PAUSE, GuideIconButton.Role.PONDER_RESTART };

    public static final int PONDER_BTN_TOTAL_WIDTH = SCENE_SLIDER_AREA_HEIGHT * 3;
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
    private boolean showBackground = true;
    @Nullable
    private LytSize cameraViewportOverride;
    private final List<SceneAnnotation> annotations = new ArrayList<>();

    // Reuse annotation partitions instead of allocating new lists every frame.
    private final List<InWorldAnnotation> inWorldScratch = new ArrayList<>();
    private final List<OverlayAnnotation> overlayScratch = new ArrayList<>();
    private final List<GuideBlockStatsStackResolver.ResolvedStack> blockStatsStackScratch = new ArrayList<>(4);
    private final List<InWorldBlockFaceOverlayAnnotation> structureLibHatchOverlayAnnotations = new ArrayList<>();
    @Nullable
    private SceneFloorGridAnnotation cachedFloorGridAnnotation;
    private int cachedFloorGridMinX = Integer.MIN_VALUE;
    private int cachedFloorGridMinZ = Integer.MIN_VALUE;
    private int cachedFloorGridMaxX = Integer.MIN_VALUE;
    private int cachedFloorGridMaxZ = Integer.MIN_VALUE;

    // Reuse hovered-block overlay objects across frames.
    private final Vector3f hoverBoxMin = new Vector3f();
    private final Vector3f hoverBoxMax = new Vector3f();
    private final Vector3f projectedCornerScratch = new Vector3f();
    private final Vector3f projectedLineFromScratch = new Vector3f();
    private final Vector3f projectedLineToScratch = new Vector3f();
    private final float[] pickRayScratch = new float[6];
    private final float[] diggingParticleColorScratch = new float[3];
    private final float[] diggingParticleVelocityScratch = new float[3];
    private final ConstantColor hoverBoxColor = new ConstantColor(0xFFFFFFFF);
    private final ConstantColor blockStatsHighlightColor = new ConstantColor(BLOCK_STATS_HIGHLIGHT_COLOR);

    private final ConstantColor originXAxisColor = new ConstantColor(ORIGIN_X_AXIS_COLOR);
    private final ConstantColor originYAxisColor = new ConstantColor(ORIGIN_Y_AXIS_COLOR);
    private final ConstantColor originZAxisColor = new ConstantColor(ORIGIN_Z_AXIS_COLOR);
    private final InWorldBoxAnnotation hoverBoxAnnotation = new InWorldBoxAnnotation(
        hoverBoxMin,
        hoverBoxMax,
        hoverBoxColor,
        1f);
    private final List<InWorldBoxFaceOverlayAnnotation> blockStatsHighlightAnnotations = new ArrayList<>();
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
    private boolean cachedSceneButtonRolesDirty = true;
    private boolean cachedSceneButtonsVisible = true;
    private boolean cachedSceneHasAnnotations = true;
    private boolean cachedSceneHasStructureLibHatches;
    private boolean cachedGridButtonEnabled = true;
    private boolean cachedBlockStatsButtonEnabled = true;
    private GuideIconButton.Role[] cachedSceneButtonRoles = SCENE_BUTTONS_SHOWN;

    private boolean annotationsVisible = true;
    @Nullable
    private Integer visibleLayerOverride;
    @Nullable
    private StructureLibSceneMetadata structureLibSceneMetadata;
    private final LinkedHashMap<String, StructureLibSceneBinding> structureLibBindings = new LinkedHashMap<>();
    private final List<StructureLibSceneMetadata.ChannelData> selectableStructureLibChannels = new ArrayList<>();
    @Getter
    private int structureLibCurrentTier = 1;
    private final LinkedHashMap<String, Integer> structureLibChannelOverrides = new LinkedHashMap<>();
    @Getter
    private boolean structureLibHatchHighlightEnabled;
    @Nullable
    private Consumer<StructureLibPreviewSelection> structureLibSelectionChangeListener;
    @Nullable
    private String structureLibPrimaryBindingKey;
    @Nullable
    private StructureLibPreviewSelection pendingStructureLibPreviewSelection;
    private final LinkedHashMap<String, StructureLibPreviewSelection> seededStructureLibPreviewSelections = new LinkedHashMap<>();
    private boolean initialStateCaptured;
    private boolean initialAnnotationsVisible = true;
    @Nullable
    private Integer initialVisibleLayerOverride;
    private int initialStructureLibCurrentTier = 1;
    private final LinkedHashMap<String, Integer> initialStructureLibChannelOverrides = new LinkedHashMap<>();
    private final LinkedHashMap<String, StructureLibPreviewSelection> initialStructureLibSelectionsByBinding = new LinkedHashMap<>();
    private final LinkedHashMap<String, StructureLibPreviewSelection> initialPendingStructureLibSelectionsByBinding = new LinkedHashMap<>();
    private boolean initialStructureLibHatchHighlightEnabled;
    @Nullable
    private GuideSceneStructureCacheEntry initialStructureState;
    private boolean gridButtonEnabled = true;
    private boolean gridVisible = false;
    private boolean initialGridVisible = false;
    private boolean blockStatsEnabled = false;
    @Getter
    private boolean blockStatsVisible = false;
    private boolean initialBlockStatsVisible = false;
    private boolean blockStatsButtonEnabled = false;
    private BlockStatsMode blockStatsMode = BlockStatsMode.AUTO;
    private BlockStatsCorner blockStatsCorner = BlockStatsCorner.TOP_RIGHT;
    private BlockStatsDock blockStatsDock = BlockStatsDock.INSIDE;
    private boolean blockStatsShowNames;
    private BlockStatsFilterMode blockStatsFilterMode = BlockStatsFilterMode.BLACKLIST;
    private final Set<String> blockStatsFilterKeys = new HashSet<>();
    private final List<SceneBlockStatsEntry> manualBlockStatsEntries = new ArrayList<>();
    private final List<SceneBlockStatsEntry> cachedBlockStatsEntries = new ArrayList<>();
    private boolean blockStatsDirty = true;
    private boolean blockStatsWidthsDirty = true;
    private int cachedBlockStatsItemWidth = BLOCK_STATS_ITEM_SIZE;
    private int cachedBlockStatsContentWidth;
    private int blockStatsMaxWidth = BLOCK_STATS_DEFAULT_MAX_WIDTH;
    private int blockStatsMaxHeight = BLOCK_STATS_DEFAULT_MAX_HEIGHT;
    private boolean blockStatsMaxWidthExplicit;
    private boolean blockStatsMaxHeightExplicit;
    private int blockStatsScrollX;
    private int blockStatsScrollY;
    private int blockStatsMaxScrollX;
    private int blockStatsMaxScrollY;
    private int blockStatsViewportWidth;
    private int blockStatsViewportHeight;
    private int blockStatsContentWidth;
    private int blockStatsContentHeight;
    @Nullable
    private LytRect cachedBlockStatsBoxRect;
    @Nullable
    private LytRect cachedBlockStatsViewportRect;
    @Nullable
    private LytRect cachedBlockStatsHorizontalTrackRect;
    @Nullable
    private LytRect cachedBlockStatsHorizontalThumbRect;
    @Nullable
    private LytRect cachedBlockStatsVerticalTrackRect;
    @Nullable
    private LytRect cachedBlockStatsVerticalThumbRect;
    private boolean draggingBlockStatsHorizontalScrollbar;
    private boolean draggingBlockStatsVerticalScrollbar;
    private int blockStatsScrollbarGrabOffset;
    @Nullable
    private String selectedBlockStatsKey;
    private final List<BlockStatsHitRegion> cachedBlockStatsHitRegions = new ArrayList<>();
    private int cachedBlockStatsHitRegionCount;

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
    private int currentMouseAbsX = -1;
    private int currentMouseAbsY = -1;
    private boolean currentMousePositionAvailable;

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

    public static class PonderTimelineKeyframe {

        @Getter
        private final int time;
        @Nullable
        private final String label;

        public PonderTimelineKeyframe(int time, @Nullable String label) {
            this.time = Math.max(0, time);
            this.label = label;
        }

        @Nullable
        public String getLabel() {
            return label;
        }
    }

    public LytGuidebookScene() {
        camera.setPerspectivePreset(PerspectivePreset.ISOMETRIC_NORTH_EAST);
        snapshotInitialCamera();
    }

    public void setInteractive(boolean interactive) {
        if (this.interactive == interactive) {
            return;
        }
        this.interactive = interactive;
        if (!interactive) {
            endDrag();
        }
        invalidateDocumentLayout();
    }

    public void setLevel(GuidebookLevel level) {
        this.level = level != null ? level : new GuidebookLevel();
        if (!this.level.isEmpty()) {
            var c = this.level.getCenter();
            camera.setRotationCenter(c[0], c[1], c[2]);
        }
        snapshotInitialCamera();
        clearLayerDrivenHoverState();
        markBlockStatsDirty();
    }

    public void addSoundCue(SceneSoundCue cue) {
        if (cue != null) {
            soundCues.add(cue);
        }
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
        initialStructureLibSelectionsByBinding.clear();
        initialPendingStructureLibSelectionsByBinding.clear();
        for (Map.Entry<String, StructureLibSceneBinding> entry : structureLibBindings.entrySet()) {
            StructureLibSceneBinding binding = entry.getValue();
            initialStructureLibSelectionsByBinding.put(entry.getKey(), binding.getPreviewSelection());
            StructureLibPreviewSelection pendingSelection = binding.getPendingSelection();
            if (pendingSelection != null) {
                initialPendingStructureLibSelectionsByBinding.put(entry.getKey(), pendingSelection);
            }
        }
        initialStructureLibHatchHighlightEnabled = structureLibHatchHighlightEnabled;
        initialGridVisible = gridVisible;
        initialBlockStatsVisible = blockStatsVisible;
    }

    public void captureInitialStructureStateIfAbsent() {
        if (initialStructureState == null && !level.isEmpty()) {
            initialStructureState = GuideSceneStructureCacheEntry.capture(this);
        }
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
            float[] capturedInitialCam = initialCam.clone();
            if (initialStructureState != null) {
                initialStructureState.restoreInto(this);
                initialCam = capturedInitialCam;
            }
            annotationsVisible = initialAnnotationsVisible;
            visibleLayerOverride = initialVisibleLayerOverride;
            structureLibCurrentTier = initialStructureLibCurrentTier;
            structureLibChannelOverrides.clear();
            structureLibChannelOverrides.putAll(initialStructureLibChannelOverrides);
            for (Map.Entry<String, StructureLibSceneBinding> entry : structureLibBindings.entrySet()) {
                StructureLibSceneBinding binding = entry.getValue();
                StructureLibPreviewSelection selection = initialStructureLibSelectionsByBinding.get(entry.getKey());
                if (selection != null) {
                    binding.applyPreviewSelection(selection);
                }
                binding.setPendingSelection(initialPendingStructureLibSelectionsByBinding.get(entry.getKey()));
            }
            bindPrimaryStructureLibState(getPrimaryStructureLibBinding());
            structureLibHatchHighlightEnabled = initialStructureLibHatchHighlightEnabled;
            gridVisible = initialGridVisible;
            resetBlockStatsInteractiveState();
        }
        if (ponderSceneData != null) {
            ponderCurrentTick = 0;
            ponderPaused = true;
            ponderFinished = false;
            ponderAnnotationFadeTick = 5;
            updatePonderState();
        }
        clearCachedVisibleLayerSliderRects();
        clearCachedTierSliderRects();
        clearCachedChannelSliderRects();
        resetViewToInitialCamera();
    }

    private void resetBlockStatsInteractiveState() {
        boolean restoredVisible = blockStatsEnabled && initialBlockStatsVisible;
        boolean visibilityChanged = blockStatsVisible != restoredVisible;
        boolean scrollChanged = blockStatsScrollX != 0 || blockStatsScrollY != 0;
        boolean selectionChanged = selectedBlockStatsKey != null;
        blockStatsVisible = restoredVisible;
        blockStatsScrollX = 0;
        blockStatsScrollY = 0;
        selectedBlockStatsKey = null;
        if (visibilityChanged || scrollChanged || selectionChanged) {
            clearBlockStatsGeometry();
            invalidateDocumentLayout();
        }
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

    public void setCameraViewportOverride(int width, int height) {
        this.cameraViewportOverride = new LytSize(Math.max(16, width), Math.max(16, height));
    }

    public void clearCameraViewportOverride() {
        this.cameraViewportOverride = null;
    }

    public boolean isGridButtonEnabled() {
        return gridButtonEnabled || ModConfig.debug.enableDebugMode;
    }

    public void setGridButtonEnabled(boolean gridButtonEnabled) {
        this.gridButtonEnabled = gridButtonEnabled;
        this.cachedSceneButtonRolesDirty = true;
    }

    public void setBlockStatsEnabled(boolean blockStatsEnabled) {
        this.blockStatsEnabled = blockStatsEnabled;
        if (!blockStatsEnabled) {
            this.blockStatsVisible = false;
            this.blockStatsButtonEnabled = false;
        }
        this.cachedSceneButtonRolesDirty = true;
        clearBlockStatsGeometry();
    }

    public void resetBlockStatsConfiguration() {
        blockStatsEnabled = false;
        blockStatsVisible = false;
        blockStatsButtonEnabled = false;
        blockStatsMode = BlockStatsMode.AUTO;
        blockStatsCorner = BlockStatsCorner.TOP_RIGHT;
        blockStatsDock = BlockStatsDock.INSIDE;
        blockStatsShowNames = false;
        blockStatsFilterMode = BlockStatsFilterMode.BLACKLIST;
        blockStatsFilterKeys.clear();
        manualBlockStatsEntries.clear();
        blockStatsMaxWidth = BLOCK_STATS_DEFAULT_MAX_WIDTH;
        blockStatsMaxHeight = BLOCK_STATS_DEFAULT_MAX_HEIGHT;
        blockStatsMaxWidthExplicit = false;
        blockStatsMaxHeightExplicit = false;
        selectedBlockStatsKey = null;
        cachedSceneButtonRolesDirty = true;
        markBlockStatsDirty();
    }

    public void setBlockStatsVisible(boolean blockStatsVisible) {
        boolean newVisible = blockStatsEnabled && blockStatsVisible;
        if (this.blockStatsVisible != newVisible) {
            this.blockStatsVisible = newVisible;
            if (!newVisible) {
                selectedBlockStatsKey = null;
            }
            clearBlockStatsGeometry();
            invalidateDocumentLayout();
        }
    }

    public void setBlockStatsButtonEnabled(boolean blockStatsButtonEnabled) {
        this.blockStatsButtonEnabled = blockStatsEnabled && blockStatsButtonEnabled;
        this.cachedSceneButtonRolesDirty = true;
    }

    public void setBlockStatsMode(BlockStatsMode blockStatsMode) {
        this.blockStatsMode = blockStatsMode != null ? blockStatsMode : BlockStatsMode.AUTO;
        if (this.blockStatsMode == BlockStatsMode.MANUAL) {
            selectedBlockStatsKey = null;
        }
        markBlockStatsDirty();
    }

    public void setBlockStatsCorner(BlockStatsCorner blockStatsCorner) {
        this.blockStatsCorner = blockStatsCorner != null ? blockStatsCorner : BlockStatsCorner.TOP_RIGHT;
        clearBlockStatsGeometry();
    }

    public void setBlockStatsDock(BlockStatsDock blockStatsDock) {
        this.blockStatsDock = blockStatsDock != null ? blockStatsDock : BlockStatsDock.INSIDE;
        selectedBlockStatsKey = null;
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    public void setBlockStatsShowNames(boolean blockStatsShowNames) {
        this.blockStatsShowNames = blockStatsShowNames;
        blockStatsWidthsDirty = true;
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    public void setBlockStatsMaxSize(int maxWidth, int maxHeight) {
        setBlockStatsMaxWidth(maxWidth);
        setBlockStatsMaxHeight(maxHeight);
    }

    public void setBlockStatsMaxWidth(int maxWidth) {
        blockStatsMaxWidthExplicit = true;
        this.blockStatsMaxWidth = Math.max(BLOCK_STATS_MIN_WIDTH, maxWidth);
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    public void setBlockStatsMaxHeight(int maxHeight) {
        blockStatsMaxHeightExplicit = true;
        this.blockStatsMaxHeight = Math.max(BLOCK_STATS_MIN_HEIGHT, maxHeight);
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    public void applyDefaultBlockStatsMaxSizeFromScene() {
        if (blockStatsMaxWidthExplicit && blockStatsMaxHeightExplicit) {
            return;
        }
        int maxWidth = Math
            .max(BLOCK_STATS_DEFAULT_MAX_WIDTH, Math.max(BLOCK_STATS_MIN_WIDTH, Math.round(width * 0.4f)));
        int maxHeight = Math
            .max(BLOCK_STATS_DEFAULT_MAX_HEIGHT, Math.max(BLOCK_STATS_MIN_HEIGHT, Math.round(height * 0.4f)));
        boolean changed = false;
        if (!blockStatsMaxWidthExplicit) {
            changed |= blockStatsMaxWidth != maxWidth;
            blockStatsMaxWidth = maxWidth;
        }
        if (!blockStatsMaxHeightExplicit) {
            changed |= blockStatsMaxHeight != maxHeight;
            blockStatsMaxHeight = maxHeight;
        }
        if (!changed) {
            return;
        }
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    public void setBlockStatsFilterMode(BlockStatsFilterMode blockStatsFilterMode) {
        this.blockStatsFilterMode = blockStatsFilterMode != null ? blockStatsFilterMode
            : BlockStatsFilterMode.BLACKLIST;
        markBlockStatsDirty();
    }

    public void setBlockStatsFilterKeys(Set<String> filterKeys) {
        this.blockStatsFilterKeys.clear();
        if (filterKeys != null) {
            for (String filterKey : filterKeys) {
                String normalized = normalizeBlockStatsKey(filterKey);
                if (normalized != null) {
                    this.blockStatsFilterKeys.add(normalized);
                }
            }
        }
        markBlockStatsDirty();
    }

    public void addManualBlockStatsEntry(ItemStack stack, int count) {
        if (stack == null || stack.getItem() == null || count <= 0) {
            return;
        }
        String key = blockStatsKey(stack);
        if (key == null) {
            return;
        }
        ItemStack displayStack = stack.copy();
        displayStack.stackSize = displayCount(count);
        manualBlockStatsEntries.add(new SceneBlockStatsEntry(key, displayStack, safeDisplayName(displayStack), count));
        markBlockStatsDirty();
    }

    public void clearManualBlockStatsEntries() {
        manualBlockStatsEntries.clear();
        markBlockStatsDirty();
    }

    public List<SceneBlockStatsEntry> getBlockStatsEntriesForExport() {
        if (!blockStatsEnabled) {
            return List.of();
        }
        if (blockStatsDirty) {
            rebuildBlockStatsEntries();
        }
        return List.copyOf(cachedBlockStatsEntries);
    }

    public boolean shouldExportBlockStats() {
        return blockStatsEnabled && !getBlockStatsEntriesForExport().isEmpty();
    }

    public BlockStatsLayoutState getBlockStatsLayoutStateForExport() {
        return new BlockStatsLayoutState(
            blockStatsVisible,
            blockStatsMode,
            blockStatsCorner,
            blockStatsDock,
            blockStatsShowNames,
            blockStatsMaxWidth,
            blockStatsMaxHeight,
            width,
            height,
            buttonColumnReserve());
    }

    private void invalidateDocumentLayout() {
        var document = getDocument();
        if (document != null) {
            document.invalidateLayout();
        }
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
        if (ponderSceneData != null && ((!ponderPaused && !ponderFinished) || ponderExportLayerOverrideEnabled)) {
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

    @Nullable
    public Integer getVisibleLayerYForExport() {
        return resolveVisibleLayerY();
    }

    private boolean isBlockVisibleForCurrentLayer(int y) {
        Integer visibleLayerY = resolveVisibleLayerY();
        return visibleLayerY == null || y == visibleLayerY;
    }

    private boolean isAnnotationVisibleForCurrentLayer(SceneAnnotation annotation) {
        if (!isStructureLibConditionSatisfied(annotation != null ? annotation.getStructureLibCondition() : null)) {
            return false;
        }
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
            return lineIntersectsVisibleLayer(lineAnnotation, visibleLayerY);
        }
        if (annotation instanceof InWorldBoxFaceOverlayAnnotation overlayAnnotation) {
            return intersectsVisibleLayer(overlayAnnotation.min().y, overlayAnnotation.max().y, visibleLayerY);
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

    private boolean isAnnotationVisibleForLayerSelection(InWorldAnnotation annotation,
        GuidebookSceneLayerSelection layerSelection) {
        if (annotation == null || layerSelection == null
            || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL) {
            return true;
        }
        if (annotation instanceof InWorldBoxAnnotation boxAnnotation) {
            return intersectsLayerSelection(boxAnnotation.min().y, boxAnnotation.max().y, layerSelection);
        }
        if (annotation instanceof InWorldLineAnnotation lineAnnotation) {
            return lineIntersectsLayerSelection(lineAnnotation, layerSelection);
        }
        if (annotation instanceof InWorldBoxFaceOverlayAnnotation overlayAnnotation) {
            return intersectsLayerSelection(overlayAnnotation.min().y, overlayAnnotation.max().y, layerSelection);
        }
        return true;
    }

    private boolean isOverlayAnnotationVisibleForLayerSelection(OverlayAnnotation annotation,
        GuidebookSceneLayerSelection layerSelection) {
        if (annotation == null || layerSelection == null
            || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL) {
            return true;
        }
        if (annotation instanceof DiamondAnnotation diamondAnnotation) {
            return layerSelection.isLayerVisible((int) Math.floor(diamondAnnotation.getPos().y));
        }
        if (annotation instanceof TextAnnotation textAnnotation) {
            return isTextAnnotationVisibleForLayerSelection(textAnnotation, layerSelection);
        }
        if (annotation instanceof PonderInputAnnotation inputAnnotation) {
            return layerSelection.isLayerVisible((int) Math.floor(inputAnnotation.getWorldPos().y));
        }
        return true;
    }

    private boolean isTextAnnotationVisibleForLayerSelection(TextAnnotation annotation,
        GuidebookSceneLayerSelection layerSelection) {
        if (annotation.isIndependent()) {
            return true;
        }
        return layerSelection.isLayerVisible((int) Math.floor(annotation.getWorldPos().y));
    }

    private boolean intersectsLayerSelection(float fromY, float toY, GuidebookSceneLayerSelection layerSelection) {
        float minY = Math.min(fromY, toY);
        float maxY = Math.max(fromY, toY);
        int minLayer = (int) Math.floor(minY);
        int maxLayer = (int) Math.floor(maxY);
        for (int layer = minLayer; layer <= maxLayer; layer++) {
            if (layerSelection.isLayerVisible(layer)) {
                return true;
            }
        }
        return false;
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

    public StructureLibSceneBinding registerStructureLibBinding(@Nullable String name) {
        String normalizedName = StructureLibSceneCondition.normalizeStructureName(name);
        String bindingKey = normalizedName != null ? normalizedName : "structurelib#" + structureLibBindings.size();
        StructureLibSceneBinding binding = structureLibBindings.get(bindingKey);
        if (binding != null) {
            StructureLibPreviewSelection seededSelection = seededStructureLibPreviewSelections.get(bindingKey);
            if (seededSelection != null) {
                binding.setPendingSelection(seededSelection);
            }
            return binding;
        }
        StructureLibSceneBinding created = new StructureLibSceneBinding(normalizedName, bindingKey);
        StructureLibPreviewSelection seededSelection = seededStructureLibPreviewSelections.get(bindingKey);
        if (seededSelection != null) {
            created.setPendingSelection(seededSelection);
        }
        created.setSelectionChangeListener(structureLibSelectionChangeListener);
        structureLibBindings.put(bindingKey, created);
        if (structureLibPrimaryBindingKey == null) {
            structureLibPrimaryBindingKey = bindingKey;
        }
        return created;
    }

    @Nullable
    public StructureLibSceneBinding resolveStructureLibBinding(@Nullable String name) {
        String normalizedName = StructureLibSceneCondition.normalizeStructureName(name);
        if (normalizedName == null) {
            return structureLibBindings.size() == 1 ? structureLibBindings.values()
                .iterator()
                .next() : getPrimaryStructureLibBinding();
        }
        for (StructureLibSceneBinding binding : structureLibBindings.values()) {
            if (normalizedName.equals(binding.getName())) {
                return binding;
            }
        }
        return null;
    }

    public int getStructureLibBindingCount() {
        return structureLibBindings.size();
    }

    public List<StructureLibSceneBinding> getStructureLibBindings() {
        return List.copyOf(structureLibBindings.values());
    }

    public Map<String, StructureLibPreviewSelection> getStructureLibPreviewSelectionsByBinding() {
        LinkedHashMap<String, StructureLibPreviewSelection> selections = new LinkedHashMap<>(
            structureLibBindings.size());
        for (Map.Entry<String, StructureLibSceneBinding> entry : structureLibBindings.entrySet()) {
            selections.put(
                entry.getKey(),
                entry.getValue()
                    .getPreviewSelection());
        }
        return selections;
    }

    @Nullable
    private StructureLibSceneBinding getPrimaryStructureLibBinding() {
        return structureLibPrimaryBindingKey != null ? structureLibBindings.get(structureLibPrimaryBindingKey) : null;
    }

    public boolean isStructureLibConditionSatisfied(@Nullable StructureLibSceneCondition condition) {
        if (condition == null || !condition.hasAnyConstraint()) {
            return true;
        }
        StructureLibSceneBinding binding = resolveStructureLibBinding(condition.getStructureName());
        if (binding == null || binding.getMetadata() == null) {
            return false;
        }
        if (condition.getTierCondition() != null && !condition.getTierCondition()
            .matches(binding.getCurrentTier())) {
            return false;
        }
        for (Map.Entry<String, StructureLibValueCondition> entry : condition.getChannelConditions()
            .entrySet()) {
            if (!entry.getValue()
                .matches(binding.getChannelValue(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private void bindPrimaryStructureLibState(@Nullable StructureLibSceneBinding binding) {
        this.structureLibSceneMetadata = binding != null ? binding.getMetadata() : null;
        this.structureLibChannelOverrides.clear();
        this.selectableStructureLibChannels.clear();
        this.structureLibHatchOverlayAnnotations.clear();
        if (binding == null || binding.getMetadata() == null) {
            this.structureLibCurrentTier = 1;
            this.structureLibHatchHighlightEnabled = false;
            this.hoveredStructureLibHatch = null;
            this.pendingStructureLibPreviewSelection = null;
            return;
        }
        this.structureLibCurrentTier = binding.getCurrentTier();
        this.structureLibChannelOverrides.putAll(binding.getChannelOverrides());
        StructureLibSceneMetadata metadata = binding.getMetadata();
        for (StructureLibSceneMetadata.ChannelData channelData : metadata.getChannelDataList()) {
            if (channelData != null && channelData.isSelectable()) {
                this.selectableStructureLibChannels.add(channelData);
            }
        }
        for (StructureLibSceneMetadata.BlockTooltipEntry entry : metadata.getHatchTooltipEntries()) {
            InWorldBlockFaceOverlayAnnotation overlay = new InWorldBlockFaceOverlayAnnotation(
                entry.getX(),
                entry.getY(),
                entry.getZ(),
                new ConstantColor(StructureLibTooltipContentBuilder.resolveHatchOverlayArgb(entry.getTooltipData())),
                metadata.getHatchTooltipPositions());
            overlay.setAlwaysOnTop(true);
            this.structureLibHatchOverlayAnnotations.add(overlay);
        }
        if (!metadata.hasHatchTooltipData()) {
            this.structureLibHatchHighlightEnabled = false;
            this.hoveredStructureLibHatch = null;
        }
        binding.setSelectionChangeListener(structureLibSelectionChangeListener);
        pendingStructureLibPreviewSelection = binding.getPendingSelection();
    }

    public void setStructureLibSceneMetadata(@Nullable StructureLibSceneMetadata structureLibSceneMetadata) {
        structureLibBindings.clear();
        structureLibPrimaryBindingKey = null;
        if (structureLibSceneMetadata == null) {
            bindPrimaryStructureLibState(null);
            return;
        }
        StructureLibSceneBinding binding = registerStructureLibBinding(null);
        binding.setMetadata(structureLibSceneMetadata);
        bindPrimaryStructureLibState(binding);
    }

    public void setStructureLibSceneMetadata(@Nullable String name,
        @Nullable StructureLibSceneMetadata structureLibSceneMetadata) {
        StructureLibSceneBinding binding = registerStructureLibBinding(name);
        binding.setMetadata(structureLibSceneMetadata);
        if (binding == getPrimaryStructureLibBinding()) {
            bindPrimaryStructureLibState(binding);
        }
    }

    @Nullable
    public StructureLibSceneMetadata getStructureLibSceneMetadata() {
        return structureLibSceneMetadata;
    }

    @Nullable
    public StructureLibSceneMetadata getStructureLibSceneMetadata(@Nullable String name) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(name);
        return binding != null ? binding.getMetadata() : null;
    }

    public boolean hasStructureLibSceneMetadata() {
        return structureLibSceneMetadata != null;
    }

    public boolean hasStructureLibSceneMetadata(@Nullable String name) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(name);
        return binding != null && binding.getMetadata() != null;
    }

    public int getStructureLibCurrentTier(@Nullable String name) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(name);
        return binding != null ? binding.getCurrentTier() : StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
    }

    public void setStructureLibCurrentTier(int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureLibCurrentTier, true);
    }

    public void setStructureLibCurrentTierSilently(int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureLibCurrentTier, false);
    }

    public void setStructureLibCurrentTier(@Nullable String structureName, int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureName, structureLibCurrentTier, true);
    }

    public void setStructureLibCurrentTierSilently(@Nullable String structureName, int structureLibCurrentTier) {
        setStructureLibCurrentTierInternal(structureName, structureLibCurrentTier, false);
    }

    public void setStructureLibSelectionChangeListener(
        @Nullable Consumer<StructureLibPreviewSelection> structureLibSelectionChangeListener) {
        this.structureLibSelectionChangeListener = structureLibSelectionChangeListener;
        StructureLibSceneBinding binding = getPrimaryStructureLibBinding();
        if (binding != null) {
            binding.setSelectionChangeListener(structureLibSelectionChangeListener);
        }
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
        StructureLibSceneBinding binding = getPrimaryStructureLibBinding();
        if (binding != null) {
            binding.setCurrentTier(this.structureLibCurrentTier);
        }
        if (notifyListener && previousValue != this.structureLibCurrentTier) {
            notifyStructureLibSelectionChanged();
        }
    }

    private void setStructureLibCurrentTierInternal(@Nullable String structureName, int structureLibCurrentTier,
        boolean notifyListener) {
        StructureLibSceneBinding binding = structureName != null ? registerStructureLibBinding(structureName)
            : getPrimaryStructureLibBinding();
        if (binding == null || binding == getPrimaryStructureLibBinding()) {
            setStructureLibCurrentTierInternal(structureLibCurrentTier, notifyListener);
            return;
        }
        int previousValue = binding.getCurrentTier();
        binding.setCurrentTier(structureLibCurrentTier);
        if (notifyListener && previousValue != binding.getCurrentTier()) {
            notifyStructureLibSelectionChanged(structureName);
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

    public int getStructureLibChannelValue(@Nullable String structureName, String channelId) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(structureName);
        return binding != null ? binding.getChannelValue(channelId) : 0;
    }

    public void setStructureLibChannelValue(String channelId, int value) {
        setStructureLibChannelValueInternal(channelId, value, true);
    }

    public void setStructureLibChannelValueSilently(String channelId, int value) {
        setStructureLibChannelValueInternal(channelId, value, false);
    }

    public void setStructureLibChannelValue(@Nullable String structureName, String channelId, int value) {
        setStructureLibChannelValueInternal(structureName, channelId, value, true);
    }

    public void setStructureLibChannelValueSilently(@Nullable String structureName, String channelId, int value) {
        setStructureLibChannelValueInternal(structureName, channelId, value, false);
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
        StructureLibSceneBinding binding = getPrimaryStructureLibBinding();
        if (binding != null) {
            binding.setChannelValue(normalized, nextValue);
        }
        if (notifyListener && previousValue != nextValue) {
            notifyStructureLibSelectionChanged();
        }
    }

    private void setStructureLibChannelValueInternal(@Nullable String structureName, String channelId, int value,
        boolean notifyListener) {
        StructureLibSceneBinding binding = structureName != null ? registerStructureLibBinding(structureName)
            : getPrimaryStructureLibBinding();
        if (binding == null || binding == getPrimaryStructureLibBinding()) {
            setStructureLibChannelValueInternal(channelId, value, notifyListener);
            return;
        }
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        if (normalized == null) {
            return;
        }
        int previousValue = binding.getChannelValue(normalized);
        binding.setChannelValue(normalized, value);
        if (notifyListener && previousValue != binding.getChannelValue(normalized)) {
            notifyStructureLibSelectionChanged(structureName);
        }
    }

    public StructureLibPreviewSelection getStructureLibPreviewSelection() {
        return new StructureLibPreviewSelection(structureLibCurrentTier, structureLibChannelOverrides);
    }

    public StructureLibPreviewSelection getStructureLibPreviewSelection(@Nullable String structureName) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(structureName);
        return binding != null ? binding.getPreviewSelection() : StructureLibPreviewSelection.defaultSelection();
    }

    @Nullable
    public StructureLibPreviewSelection getPendingStructureLibPreviewSelection() {
        return pendingStructureLibPreviewSelection;
    }

    @Nullable
    public StructureLibPreviewSelection getPendingStructureLibPreviewSelection(@Nullable String structureName) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(structureName);
        return binding != null ? binding.getPendingSelection() : null;
    }

    public void setPendingStructureLibPreviewSelection(@Nullable StructureLibPreviewSelection pendingSelection) {
        this.pendingStructureLibPreviewSelection = pendingSelection;
        StructureLibSceneBinding binding = getPrimaryStructureLibBinding();
        if (binding != null) {
            binding.setPendingSelection(pendingSelection);
        }
    }

    public void setPendingStructureLibPreviewSelection(@Nullable String structureName,
        @Nullable StructureLibPreviewSelection pendingSelection) {
        StructureLibSceneBinding binding = pendingSelection != null ? registerStructureLibBinding(structureName)
            : resolveStructureLibBinding(structureName);
        if (binding != null) {
            binding.setPendingSelection(pendingSelection);
        }
        if (binding == getPrimaryStructureLibBinding()) {
            this.pendingStructureLibPreviewSelection = pendingSelection;
        }
    }

    public boolean applyStructureLibPreviewSelection(@Nullable String structureName,
        @Nullable StructureLibPreviewSelection previewSelection, boolean notifyListener) {
        if (previewSelection == null) {
            return false;
        }
        StructureLibSceneBinding binding = structureName != null ? registerStructureLibBinding(structureName)
            : getPrimaryStructureLibBinding();
        if (binding == null) {
            return false;
        }

        StructureLibPreviewSelection previousSelection = binding.getPreviewSelection();
        binding.applyPreviewSelection(previewSelection);
        if (binding == getPrimaryStructureLibBinding()) {
            if (binding.getMetadata() != null) {
                bindPrimaryStructureLibState(binding);
            } else {
                structureLibCurrentTier = previewSelection.getMasterTier();
                structureLibChannelOverrides.clear();
                structureLibChannelOverrides.putAll(previewSelection.getChannelOverrides());
            }
        }

        boolean changed = !previousSelection.equals(binding.getPreviewSelection());
        if (changed && notifyListener) {
            notifyStructureLibSelectionChanged(structureName);
        }
        return changed;
    }

    public void seedStructureLibPreviewSelections(@Nullable Map<String, StructureLibPreviewSelection> selections) {
        seededStructureLibPreviewSelections.clear();
        if (selections == null || selections.isEmpty()) {
            return;
        }
        seededStructureLibPreviewSelections.putAll(selections);
    }

    public void clearSeededStructureLibPreviewSelections() {
        seededStructureLibPreviewSelections.clear();
    }

    public List<SceneSoundCue> getSoundCues() {
        return List.copyOf(soundCues);
    }

    public void clearSoundCues() {
        soundCues.clear();
    }

    public boolean isInteractive() {
        return interactive;
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
        clearCachedVisibleLayerSliderRects();
        clearCachedTierSliderRects();
        clearCachedChannelSliderRects();
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
        clearCachedVisibleLayerSliderRects();
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

    public GuidebookLevel getLevel() {
        return level;
    }

    public CameraSettings getCamera() {
        return camera;
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

    public boolean isShowBackground() {
        return showBackground;
    }

    public void setShowBackground(boolean showBackground) {
        this.showBackground = showBackground;
    }

    public List<SceneAnnotation> getAnnotations() {
        return annotations;
    }

    public boolean isAnnotationsVisible() {
        return annotationsVisible;
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        cachedSceneButtonRolesDirty = true;
    }

    public void setHoveredStructureLibHatch(int @Nullable [] hoveredStructureLibHatch) {
        this.hoveredStructureLibHatch = hoveredStructureLibHatch;
    }

    private void notifyStructureLibSelectionChanged() {
        if (structureLibSelectionChangeListener != null) {
            structureLibSelectionChangeListener.accept(getStructureLibPreviewSelection());
        }
    }

    public void notifyStructureLibSelectionChanged(@Nullable String structureName) {
        StructureLibSceneBinding binding = resolveStructureLibBinding(structureName);
        if (binding == null) {
            notifyStructureLibSelectionChanged();
            return;
        }
        if (binding == getPrimaryStructureLibBinding()) {
            notifyStructureLibSelectionChanged();
            return;
        }
        Consumer<StructureLibPreviewSelection> listener = binding.getSelectionChangeListener();
        if (listener != null) {
            listener.accept(binding.getPreviewSelection());
        }
    }

    public boolean hasStructureLibTierData() {
        StructureLibSceneMetadata.TierData tierData = getStructureLibTierData();
        return tierData != null && tierData.isSelectable();
    }

    public boolean hasStructureLibChannelData() {
        return !selectableStructureLibChannels.isEmpty();
    }

    public int getBottomControlAreaHeight() {
        if (!hasBottomControls()) {
            return 0;
        }
        return ponderControlAreaHeight() + visibleLayerSliderAreaHeight()
            + structureLibTierSliderAreaHeight()
            + structureLibChannelSliderAreaHeight();
    }

    public boolean hasStructureLibHatchData() {
        return structureLibSceneMetadata != null && structureLibSceneMetadata.hasHatchTooltipData();
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
            cachedSceneButtonRolesDirty = true;
        }
    }

    public void addStaticParticle(GuidebookSceneParticle particle) {
        if (particle != null) {
            staticSceneParticles.add(particle);
        }
    }

    public void addStaticWeatherEffect(GuidebookSceneWeatherEffect effect) {
        if (effect != null) {
            staticWeatherEffects.add(effect);
        }
    }

    private List<GuidebookSceneParticle> resolveRenderableSceneParticles() {
        if (staticSceneParticles.isEmpty()) {
            return ponderSceneParticles;
        }
        if (ponderSceneParticles.isEmpty()) {
            return staticSceneParticles;
        }
        renderSceneParticlesScratch.clear();
        renderSceneParticlesScratch.ensureCapacity(staticSceneParticles.size() + ponderSceneParticles.size());
        renderSceneParticlesScratch.addAll(staticSceneParticles);
        renderSceneParticlesScratch.addAll(ponderSceneParticles);
        return renderSceneParticlesScratch;
    }

    public List<GuidebookSceneParticle> getRenderableParticlesForExport() {
        return resolveRenderableSceneParticles();
    }

    private List<GuidebookSceneWeatherEffect> resolveRenderableWeatherEffects() {
        if (staticWeatherEffects.isEmpty()) {
            return ponderWeatherEffects;
        }
        if (ponderWeatherEffects.isEmpty()) {
            return staticWeatherEffects;
        }
        renderWeatherEffectsScratch.clear();
        renderWeatherEffectsScratch.ensureCapacity(staticWeatherEffects.size() + ponderWeatherEffects.size());
        renderWeatherEffectsScratch.addAll(staticWeatherEffects);
        renderWeatherEffectsScratch.addAll(ponderWeatherEffects);
        return renderWeatherEffectsScratch;
    }

    public List<GuidebookSceneWeatherEffect> getRenderableWeatherEffectsForExport() {
        return resolveRenderableWeatherEffectsForLayerSelection(
            GuidebookSceneLayerSelection.fromVisibleLayer(null),
            (int) Math.floor(resolveWeatherAnimationTick()));
    }

    public int getSceneAnimationTickForRender() {
        return (int) Math.floor(resolveWeatherAnimationTick());
    }

    private List<GuidebookSceneWeatherEffect> resolveRenderableWeatherEffectsForCurrentView(
        GuidebookSceneLayerSelection layerSelection) {
        return resolveRenderableWeatherEffectsForLayerSelection(
            layerSelection,
            (int) Math.floor(resolveWeatherAnimationTick()));
    }

    private float resolveWeatherAnimationTick() {
        return ponderSceneData != null ? ponderCurrentTick : sceneAnimationTick;
    }

    private List<GuidebookSceneWeatherEffect> resolveRenderableWeatherEffectsForLayerSelection(
        @Nullable GuidebookSceneLayerSelection layerSelection, @Nullable Integer activeTick) {
        List<GuidebookSceneWeatherEffect> effects = resolveRenderableWeatherEffects();
        if (effects.isEmpty()) {
            return effects;
        }
        int[] bounds = copyLevelBounds();
        List<GuidebookSceneWeatherEffect> resolved = GuidebookSceneWeatherSupport
            .resolveRenderableEffects(effects, bounds, activeTick, layerSelection, level);
        if (resolved.isEmpty()) {
            return List.of();
        }
        if (resolved == effects || resolved == staticWeatherEffects
            || resolved == ponderWeatherEffects
            || resolved == renderWeatherEffectsScratch) {
            return resolved;
        }
        resolvedWeatherEffectsScratch.clear();
        resolvedWeatherEffectsScratch.ensureCapacity(resolved.size());
        resolvedWeatherEffectsScratch.addAll(resolved);
        return resolvedWeatherEffectsScratch;
    }

    public List<InWorldAnnotation> collectInWorldAnnotationsForExport(boolean includeSceneAnnotations,
        boolean includeGrid, GuidebookSceneLayerSelection layerSelection) {
        ArrayList<InWorldAnnotation> inWorld = new ArrayList<>();
        GuidebookSceneLayerSelection effectiveLayers = layerSelection != null ? layerSelection
            : GuidebookSceneLayerSelection.all();
        if (includeSceneAnnotations) {
            for (SceneAnnotation annotation : annotations) {
                if (annotation instanceof InWorldAnnotation inWorldAnnotation
                    && isAnnotationVisibleForLayerSelection(inWorldAnnotation, effectiveLayers)
                    && isStructureLibConditionSatisfied(annotation.getStructureLibCondition())) {
                    inWorld.add(inWorldAnnotation);
                }
            }
            appendStructureLibHatchOverlays(inWorld);
            appendOriginAxesAnnotations(inWorld);
            appendBlockStatsHighlightAnnotations(inWorld);
            for (SceneAnnotation annotation : ponderActiveAnnotations) {
                if (annotation instanceof InWorldAnnotation inWorldAnnotation
                    && isAnnotationVisibleForLayerSelection(inWorldAnnotation, effectiveLayers)
                    && isStructureLibConditionSatisfied(annotation.getStructureLibCondition())) {
                    inWorld.add(inWorldAnnotation);
                }
            }
        }
        if (includeGrid && !level.isEmpty()) {
            int[] bounds = level.getBounds();
            SceneFloorGridAnnotation grid = getOrCreateFloorGridAnnotation(bounds);
            grid.setShowDebugLabels(ModConfig.debug.enableDebugMode);
            inWorld.add(grid);
        }
        return inWorld;
    }

    public List<OverlayAnnotation> collectOverlayAnnotationsForExport(GuidebookSceneLayerSelection layerSelection) {
        ArrayList<OverlayAnnotation> overlays = new ArrayList<>();
        GuidebookSceneLayerSelection effectiveLayers = layerSelection != null ? layerSelection
            : GuidebookSceneLayerSelection.all();
        for (SceneAnnotation annotation : annotations) {
            if (annotation instanceof OverlayAnnotation overlay
                && isOverlayAnnotationVisibleForLayerSelection(overlay, effectiveLayers)
                && isStructureLibConditionSatisfied(annotation.getStructureLibCondition())) {
                overlays.add(overlay);
            }
        }
        for (SceneAnnotation annotation : ponderOutgoingAnnotations) {
            if (annotation instanceof OverlayAnnotation overlay
                && isOverlayAnnotationVisibleForLayerSelection(overlay, effectiveLayers)
                && isStructureLibConditionSatisfied(annotation.getStructureLibCondition())) {
                overlay.setFade(Math.min(1f, ponderOutgoingFadeTick / 5f));
                overlays.add(overlay);
            }
        }
        for (SceneAnnotation annotation : ponderActiveAnnotations) {
            if (annotation instanceof OverlayAnnotation overlay
                && isOverlayAnnotationVisibleForLayerSelection(overlay, effectiveLayers)
                && isStructureLibConditionSatisfied(annotation.getStructureLibCondition())) {
                overlay.setFade(Math.min(1f, ponderAnnotationFadeTick / 5f));
                overlays.add(overlay);
            }
        }
        return overlays;
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
        var points = line.points();
        for (int i = 0; i + 1 < points.size(); i++) {
            var from = points.get(i);
            var to = points.get(i + 1);
            var a = camera.worldToScreen(from.x, from.y, from.z, projectedLineFromScratch);
            var b = camera.worldToScreen(to.x, to.y, to.z, projectedLineToScratch);
            float ax = cx + a.x, ay = cy + a.y;
            float bx = cx + b.x, by = cy + b.y;
            float dx = bx - ax, dy = by - ay;
            float lenSq = dx * dx + dy * dy;
            float t = lenSq < 1e-4f ? 0f
                : Math.max(0f, Math.min(1f, ((mouseX - ax) * dx + (mouseY - ay) * dy) / lenSq));
            float px = ax + t * dx, py = ay + t * dy;
            float ex = mouseX - px, ey = mouseY - py;
            if (ex * ex + ey * ey <= LINE_HOVER_TOLERANCE_PX_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private boolean lineIntersectsVisibleLayer(InWorldLineAnnotation line, int visibleLayerY) {
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (var point : line.points()) {
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        return intersectsVisibleLayer(minY, maxY, visibleLayerY);
    }

    private boolean lineIntersectsLayerSelection(InWorldLineAnnotation line,
        GuidebookSceneLayerSelection layerSelection) {
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (var point : line.points()) {
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        return intersectsLayerSelection(minY, maxY, layerSelection);
    }

    public void clearAnnotationHover() {
        for (var a : annotations) a.setHovered(false);
    }

    public void setAnnotationsVisible(boolean visible) {
        this.annotationsVisible = visible;
        if (!visible) clearAnnotationHover();
        cachedSceneButtonRolesDirty = true;
    }

    // LytBlock

    /** Horizontal space the floating button column steals from the row when interactive. */
    private int buttonColumnReserve() {
        return interactive && sceneButtonsVisible ? (BTN_OUTSIDE_GAP + BTN_SIZE) : 0;
    }

    public int getSceneButtonColumnReserveForExport() {
        return buttonColumnReserve();
    }

    private int blockStatsDockLengthForLayout(boolean horizontal) {
        if (!blockStatsEnabled || !blockStatsVisible
            || blockStatsMode == BlockStatsMode.MANUAL
            || !blockStatsDock.isOutside()) {
            return 0;
        }
        int count = blockStatsLayoutEntryCount();
        if (count <= 0) {
            return 0;
        }
        int rowHeight = blockStatsRowHeightForLayout();
        int maxMain = Math.max(rowHeight, horizontal ? width : height);
        int rowsOrColumns = Math.max(1, maxMain / rowHeight);
        int lines = (count + rowsOrColumns - 1) / rowsOrColumns;
        int cross = lines * blockStatsDockEntryWidthForLayout() + BLOCK_STATS_PADDING_X * 2;
        return Math.max(BLOCK_STATS_MIN_WIDTH, Math.min(blockStatsMaxWidth, cross));
    }

    private int blockStatsDockHeightForLayout() {
        if (!blockStatsEnabled || !blockStatsVisible
            || blockStatsMode == BlockStatsMode.MANUAL
            || !blockStatsDock.isOutside()) {
            return 0;
        }
        int count = blockStatsLayoutEntryCount();
        if (count <= 0) {
            return 0;
        }
        int rowHeight = blockStatsRowHeightForLayout();
        if (blockStatsDock == BlockStatsDock.TOP || blockStatsDock == BlockStatsDock.BOTTOM) {
            int entryWidth = blockStatsDockEntryWidthForLayout();
            int maxMain = Math.max(entryWidth, width);
            int columns = Math.max(1, maxMain / entryWidth);
            int rows = (count + columns - 1) / columns;
            return Math.max(
                BLOCK_STATS_MIN_HEIGHT,
                Math.min(blockStatsMaxHeight, rows * rowHeight + BLOCK_STATS_PADDING_Y * 2));
        }
        int rows = Math.max(1, Math.min(count, Math.max(1, height / rowHeight)));
        return Math
            .max(BLOCK_STATS_MIN_HEIGHT, Math.min(blockStatsMaxHeight, rows * rowHeight + BLOCK_STATS_PADDING_Y * 2));
    }

    private int blockStatsDockEntryWidthForLayout() {
        int itemWidth = blockStatsItemWidthForLayout();
        return blockStatsShowNames ? Math.max(96, Math.min(blockStatsMaxWidth, itemWidth + BLOCK_STATS_GAP + 72))
            : itemWidth;
    }

    private int blockStatsItemWidthForLayout() {
        int width = BLOCK_STATS_ITEM_SIZE;
        if (blockStatsDirty) {
            rebuildBlockStatsEntries();
        }
        for (SceneBlockStatsEntry entry : cachedBlockStatsEntries) {
            if (entry.getCount() > 1) {
                width = Math.max(width, estimateBlockStatsCountWidth(formatBlockStatsDisplayCount(entry.getCount())));
            }
        }
        return width;
    }

    private static int estimateBlockStatsCountWidth(String countText) {
        return Math.max(BLOCK_STATS_ITEM_SIZE, (countText != null ? countText.length() : 0) * 6 + 4);
    }

    private int blockStatsRowHeightForLayout() {
        return BLOCK_STATS_ITEM_SIZE + BLOCK_STATS_ROW_GAP;
    }

    private int blockStatsLayoutEntryCount() {
        if (blockStatsMode == BlockStatsMode.MANUAL) {
            return manualBlockStatsEntries.size();
        }
        if (level == null || level.isEmpty()) {
            return 0;
        }
        if (blockStatsDirty) {
            rebuildBlockStatsEntries();
        }
        return cachedBlockStatsEntries.size();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int targetSceneWidth = ResponsiveVisualSizing
            .scaleWidth(width, context.getVisualScale(), MIN_RESPONSIVE_SCENE_SIZE);
        int reserve = buttonColumnReserve();
        int leftDock = blockStatsDock == BlockStatsDock.LEFT
            ? blockStatsDockLengthForLayout(false) + BLOCK_STATS_DOCK_GAP
            : 0;
        int rightDock = blockStatsDock == BlockStatsDock.RIGHT
            ? blockStatsDockLengthForLayout(false) + BLOCK_STATS_DOCK_GAP
            : 0;
        int topDock = blockStatsDock == BlockStatsDock.TOP ? blockStatsDockHeightForLayout() + BLOCK_STATS_DOCK_GAP : 0;
        int bottomDock = blockStatsDock == BlockStatsDock.BOTTOM
            ? blockStatsDockHeightForLayout() + BLOCK_STATS_DOCK_GAP
            : 0;
        int totalDesired = targetSceneWidth + reserve + leftDock + rightDock;
        int w = Math.min(totalDesired, Math.max(reserve + MIN_RESPONSIVE_SCENE_SIZE, availableWidth));
        int availableForDocks = Math.max(0, w - reserve - targetSceneWidth);
        if (leftDock + rightDock > availableForDocks) {
            if (leftDock > 0 && rightDock > 0) {
                leftDock = Math.min(leftDock, availableForDocks / 2);
                rightDock = Math.min(rightDock, availableForDocks - leftDock);
            } else if (leftDock > 0) {
                leftDock = Math.min(leftDock, availableForDocks);
            } else {
                rightDock = Math.min(rightDock, availableForDocks);
            }
        }
        int minDockSpace = BLOCK_STATS_MIN_WIDTH + BLOCK_STATS_DOCK_GAP;
        if (leftDock > 0 && leftDock < minDockSpace) {
            leftDock = 0;
        }
        if (rightDock > 0 && rightDock < minDockSpace) {
            rightDock = 0;
        }
        int sceneW = Math.max(MIN_RESPONSIVE_SCENE_SIZE, w - reserve - leftDock - rightDock);
        int buttonCount = interactive && sceneButtonsVisible ? cachedSceneButtonRoles().length : 0;
        int buttonsTotalH = interactive && sceneButtonsVisible
            ? (BTN_SIZE * buttonCount + BTN_GAP * Math.max(0, buttonCount - 1))
            : 0;
        int sceneH = computeResponsiveSceneHeight(sceneW, buttonsTotalH);
        int h = topDock + sceneH + (reserveBottomControlArea ? getBottomControlAreaHeight() : 0) + bottomDock;
        this.layoutSceneWidth = sceneW;
        this.layoutSceneHeight = sceneH;
        this.layoutSceneOffsetX = leftDock;
        this.layoutSceneOffsetY = topDock;
        return new LytRect(x, y, w, h);
    }

    private int computeResponsiveSceneHeight(int sceneWidth, int buttonsTotalHeight) {
        int baseWidth = Math.max(1, width);
        int baseHeight = Math.max(1, height);
        if (sceneWidth >= baseWidth) {
            return Math.max(baseHeight, buttonsTotalHeight);
        }
        float scale = sceneWidth / (float) baseWidth;
        int scaledHeight = Math.max(MIN_RESPONSIVE_SCENE_SIZE, Math.round(baseHeight * scale));
        return Math.max(scaledHeight, buttonsTotalHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        int sceneW = layoutSceneWidth > 0 ? layoutSceneWidth
            : getBounds().width() - buttonColumnReserve() - layoutSceneOffsetX;
        if (sceneW < 16) sceneW = 16;
        int sliderAreaHeight = getBottomControlAreaHeight();
        int sceneH = layoutSceneHeight > 0 ? layoutSceneHeight : Math.max(16, getBounds().height() - sliderAreaHeight);
        int totalH = reserveBottomControlArea ? Math.max(sceneH + sliderAreaHeight, getBounds().height())
            : Math.max(sceneH, getBounds().height());
        LytRect outerRect = new LytRect(
            getBounds().x() + layoutSceneOffsetX,
            getBounds().y() + layoutSceneOffsetY,
            sceneW,
            sceneH + (reserveBottomControlArea ? sliderAreaHeight : 0));
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
            absX = mrc.getDocumentOriginX() + Math.round(sceneRect.x() * docZoom);
            absY = mrc.getDocumentOriginY() + Math.round((sceneRect.y() - mrc.getScrollOffsetY()) * docZoom);
            outerAbsX = absX;
            outerAbsY = absY;
        }
        // Scale layout dimensions to screen pixels when document zoom != 1.
        int w = Math.round(sceneRect.width() * docZoom);
        int h = Math.round(sceneRect.height() * docZoom);
        int outerW = Math.round(outerRect.width() * docZoom);
        int outerH = Math.round(outerRect.height() * docZoom);
        int sceneRight = absX + w;
        int sceneBottom = absY + h;
        LytRect activeScissor = context.currentScissor();
        if (activeScissor != null) {
            clipX = Math.max(absX, activeScissor.x());
            clipY = Math.max(absY, activeScissor.y());
            int clipRight = Math.min(sceneRight, activeScissor.right());
            int clipBottom = Math.min(sceneBottom, activeScissor.bottom());
            clipW = Math.max(0, clipRight - clipX);
            clipH = Math.max(0, clipBottom - clipY);
        } else {
            clipX = absX;
            clipY = absY;
            clipW = w;
            clipH = h;
        }
        playEnterSoundsIfNeeded();

        if (level.isEmpty() && annotations.isEmpty() && !shouldRenderOriginAxes()) {
            this.lastAbsX = absX;
            this.lastAbsY = absY;
            this.lastW = w;
            this.lastH = h;
            this.lastDocZoom = docZoom;
            this.lastOuterAbsX = outerAbsX;
            this.lastOuterAbsY = outerAbsY;
            this.lastOuterW = outerW;
            this.lastOuterH = outerH;
            this.cachedScreenRect = updateCachedRect(cachedScreenRect, absX, absY, w, h);
            renderSceneBackground(context, sceneRect);
            drawBottomControls(context, outerRect);
            drawBlockStatsOverlay(context, sceneRect, outerRect);
            renderSceneBorder(context, sceneRect);
            if (interactive && sceneButtonsVisible) {
                drawSceneButtons(sceneRect.x(), sceneRect.y(), w, h, absX, absY, docZoom);
            }
            return;
        }

        renderSceneBackground(context, sceneRect);
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
        this.lastDocZoom = docZoom;
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
        if (gridVisible && !level.isEmpty()) {
            int[] bounds = level.getBounds();
            SceneFloorGridAnnotation grid = getOrCreateFloorGridAnnotation(bounds);
            grid.setShowDebugLabels(ModConfig.debug.enableDebugMode);
            inWorld.add(grid);
        }
        appendBlockStatsHighlightAnnotations(inWorld);
        for (SceneAnnotation pa : ponderOutgoingAnnotations) {
            if (pa instanceof OverlayAnnotation ov && isStructureLibConditionSatisfied(pa.getStructureLibCondition())) {
                ov.setFade(Math.min(1f, ponderOutgoingFadeTick / 5f));
                overlays.add(ov);
            }
        }
        for (SceneAnnotation pa : ponderActiveAnnotations) {
            if (!isStructureLibConditionSatisfied(pa.getStructureLibCondition())) {
                continue;
            }
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
        GuidebookSceneLayerSelection weatherLayerSelection = GuidebookSceneLayerSelection
            .fromVisibleLayer(visibleLayerY);
        List<GuidebookSceneWeatherEffect> weatherEffects = resolveRenderableWeatherEffectsForCurrentView(
            weatherLayerSelection);

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
                weatherLayerSelection,
                resolveRenderableSceneParticles(),
                weatherEffects,
                resolveWeatherAnimationTick());

        context.restoreExternalRenderState();
        drawBlockStatsOverlay(context, sceneRect, outerRect);
        context.restoreExternalRenderState();

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

        drawBottomControls(context, outerRect);

        // Draw border AFTER the 3D content so border pixels always sit on top.
        renderSceneBorder(context, sceneRect);

        if (interactive && sceneButtonsVisible) {
            drawSceneButtons(sceneRect.x(), sceneRect.y(), w, h, absX, absY, docZoom);
        }
    }

    private void renderSceneBackground(RenderContext context, LytRect sceneRect) {
        if (!showBackground) {
            return;
        }
        context.fillRect(sceneRect, sceneBackgroundColor);
    }

    private void renderSceneBorder(RenderContext context, LytRect sceneRect) {
        if (!showBackground) {
            return;
        }
        context.drawBorder(sceneRect, sceneBorderColor, 1);
    }

    public static final int BTN_SIZE = 16;
    public static final int BTN_GAP = 2;
    public static final int BTN_OUTSIDE_GAP = 3;

    private SceneFloorGridAnnotation getOrCreateFloorGridAnnotation(int[] bounds) {
        int minX = bounds[0] - 1;
        int minZ = bounds[2] - 1;
        int maxX = bounds[3] + 2;
        int maxZ = bounds[5] + 2;
        if (cachedFloorGridAnnotation == null || cachedFloorGridMinX != minX
            || cachedFloorGridMinZ != minZ
            || cachedFloorGridMaxX != maxX
            || cachedFloorGridMaxZ != maxZ) {
            cachedFloorGridAnnotation = new SceneFloorGridAnnotation(minX, minZ, maxX, maxZ, 0f);
            cachedFloorGridMinX = minX;
            cachedFloorGridMinZ = minZ;
            cachedFloorGridMaxX = maxX;
            cachedFloorGridMaxZ = maxZ;
        }
        return cachedFloorGridAnnotation;
    }

    private int lastAbsX, lastAbsY, lastW, lastH;
    private float lastDocZoom = 1.0f;
    private int lastOuterAbsX, lastOuterAbsY, lastOuterW, lastOuterH;
    /** Width reserved for the inner 3D scene (bounds.width minus the button column). */
    private int layoutSceneWidth;
    /** Height reserved for the inner 3D scene (bounds.height minus the bottom slider band). */
    private int layoutSceneHeight;
    private int layoutSceneOffsetX;
    private int layoutSceneOffsetY;

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

    private void appendBlockStatsHighlightAnnotations(List<InWorldAnnotation> inWorld) {
        int used = 0;
        if (selectedBlockStatsKey == null || blockStatsMode == BlockStatsMode.MANUAL || level.isEmpty()) {
            return;
        }
        if (blockStatsDirty) {
            rebuildBlockStatsEntries();
        }
        SceneBlockStatsEntry selected = findBlockStatsEntry(selectedBlockStatsKey);
        if (selected == null) {
            selectedBlockStatsKey = null;
            return;
        }
        for (SceneBlockStatsEntry.BlockStatsPlacement placement : selected.getPlacements()) {
            if (!isBlockVisibleForCurrentLayer(placement.y())) {
                continue;
            }
            AxisAlignedBB bounds = placement.bounds();
            if (bounds == null) {
                bounds = AxisAlignedBB.getBoundingBox(
                    placement.x(),
                    placement.y(),
                    placement.z(),
                    placement.x() + 1,
                    placement.y() + 1,
                    placement.z() + 1);
            }
            InWorldBoxFaceOverlayAnnotation annotation = getOrCreateBlockStatsHighlightAnnotation(used);
            annotation.setBounds(
                (float) bounds.minX,
                (float) bounds.minY,
                (float) bounds.minZ,
                (float) bounds.maxX,
                (float) bounds.maxY,
                (float) bounds.maxZ);
            inWorld.add(annotation);
            used++;
        }
    }

    private InWorldBoxFaceOverlayAnnotation getOrCreateBlockStatsHighlightAnnotation(int index) {
        while (blockStatsHighlightAnnotations.size() <= index) {
            InWorldBoxFaceOverlayAnnotation annotation = new InWorldBoxFaceOverlayAnnotation(
                new Vector3f(),
                new Vector3f(),
                blockStatsHighlightColor);
            annotation.setAlwaysOnTop(true);
            blockStatsHighlightAnnotations.add(annotation);
        }
        return blockStatsHighlightAnnotations.get(index);
    }

    private List<SceneBlockStatsEntry> getCachedBlockStatsEntries(RenderContext context) {
        if (blockStatsDirty) {
            rebuildBlockStatsEntries();
        }
        if (blockStatsWidthsDirty) {
            cachedBlockStatsItemWidth = BLOCK_STATS_ITEM_SIZE;
            cachedBlockStatsContentWidth = 0;
            for (SceneBlockStatsEntry entry : cachedBlockStatsEntries) {
                if (entry.getCount() > 1) {
                    String countText = formatBlockStatsDisplayCount(entry.getCount());
                    int countWidth = context.getStringWidth(countText, BLOCK_STATS_COUNT_TEXT_STYLE);
                    cachedBlockStatsItemWidth = Math.max(cachedBlockStatsItemWidth, countWidth + 4);
                }
                String text = blockStatsEntryText(entry);
                int width = context.getStringWidth(text, BLOCK_STATS_TEXT_STYLE);
                entry.setCachedTextWidth(width);
                if (width > cachedBlockStatsContentWidth) {
                    cachedBlockStatsContentWidth = width;
                }
            }
            blockStatsWidthsDirty = false;
        }
        return cachedBlockStatsEntries;
    }

    private void rebuildBlockStatsEntries() {
        cachedBlockStatsEntries.clear();
        if (blockStatsMode == BlockStatsMode.MANUAL) {
            for (SceneBlockStatsEntry entry : manualBlockStatsEntries) {
                if (entry != null && isBlockStatsKeyAllowed(entry.getKey())) {
                    ItemStack stack = entry.getStack() != null ? entry.getStack()
                        .copy() : null;
                    if (stack != null) {
                        stack.stackSize = displayCount(entry.getCount());
                        cachedBlockStatsEntries
                            .add(new SceneBlockStatsEntry(entry.getKey(), stack, entry.getLabel(), entry.getCount()));
                    }
                }
            }
        } else {
            LinkedHashMap<String, SceneBlockStatsEntry> byKey = new LinkedHashMap<>();
            for (int[] pos : level.getFilledBlocks()) {
                if (pos == null || pos.length < 3) {
                    continue;
                }
                GuideBlockStatsStackResolver.resolveEntriesInto(level, pos[0], pos[1], pos[2], blockStatsStackScratch);
                for (GuideBlockStatsStackResolver.ResolvedStack entry : blockStatsStackScratch) {
                    addBlockStatsStack(byKey, entry.stack(), pos[0], pos[1], pos[2], entry.bounds());
                }
            }
            cachedBlockStatsEntries.addAll(byKey.values());
        }
        cachedBlockStatsEntries.sort(
            Comparator.comparingInt(SceneBlockStatsEntry::getCount)
                .reversed()
                .thenComparing(SceneBlockStatsEntry::getLabel)
                .thenComparing(SceneBlockStatsEntry::getKey));
        blockStatsDirty = false;
        blockStatsWidthsDirty = true;
        if (selectedBlockStatsKey != null && findBlockStatsEntry(selectedBlockStatsKey) == null) {
            selectedBlockStatsKey = null;
        }
    }

    private void addBlockStatsStack(LinkedHashMap<String, SceneBlockStatsEntry> byKey, ItemStack stack, int x, int y,
        int z, @Nullable AxisAlignedBB bounds) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        String key = blockStatsKey(stack);
        if (key == null || !isBlockStatsKeyAllowed(key)) {
            return;
        }
        int count = Math.max(1, stack.stackSize);
        SceneBlockStatsEntry entry = byKey.get(key);
        if (entry == null) {
            ItemStack displayStack = stack.copy();
            displayStack.stackSize = displayCount(count);
            entry = new SceneBlockStatsEntry(key, displayStack, safeDisplayName(displayStack), count);
            byKey.put(key, entry);
        } else {
            entry.addCount(count);
        }
        entry.addPlacement(x, y, z, bounds, count);
    }

    private static int displayCount(int count) {
        return Math.max(1, count);
    }

    private String blockStatsEntryText(SceneBlockStatsEntry entry) {
        if (!blockStatsShowNames) {
            return "";
        }
        return entry.getLabel() + " x" + entry.getCount();
    }

    private static String formatBlockStatsDisplayCount(int count) {
        int value = Math.max(1, count);
        if (value < 1000) {
            return Integer.toString(value);
        }
        if (value < 10000) {
            int tenths = value / 100;
            if (tenths % 10 == 0) {
                return tenths / 10 + "k";
            }
            return tenths / 10 + "." + tenths % 10 + "k";
        }
        if (value < 1000000) {
            return value / 1000 + "k";
        }
        int millions = value / 1000000;
        return millions + "m";
    }

    @Nullable
    private SceneBlockStatsEntry findBlockStatsEntry(String key) {
        for (SceneBlockStatsEntry entry : cachedBlockStatsEntries) {
            if (entry.getKey()
                .equals(key)) {
                return entry;
            }
        }
        return null;
    }

    private boolean isBlockStatsKeyAllowed(String key) {
        String normalized = normalizeBlockStatsKey(key);
        if (normalized == null) {
            return false;
        }
        boolean matched = blockStatsFilterKeys.contains(normalized)
            || blockStatsFilterKeys.contains(stripBlockStatsMeta(normalized));
        return (blockStatsFilterMode == BlockStatsFilterMode.WHITELIST) == matched;
    }

    @Nullable
    private static String blockStatsKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        Object rawName = Item.itemRegistry.getNameForObject(stack.getItem());
        if (rawName == null) {
            return null;
        }
        int damage = Math.max(0, stack.getItemDamage());
        return normalizeBlockStatsKey(rawName + ":" + damage);
    }

    @Nullable
    public static String normalizeBlockStatsKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static String stripBlockStatsMeta(String key) {
        if (key == null) {
            return null;
        }
        int lastColon = key.lastIndexOf(':');
        int firstColon = key.indexOf(':');
        if (lastColon <= firstColon) {
            return key;
        }
        String metaPart = key.substring(lastColon + 1);
        for (int i = 0; i < metaPart.length(); i++) {
            char c = metaPart.charAt(i);
            if (c < '0' || c > '9') {
                return key;
            }
        }
        return key.substring(0, lastColon);
    }

    private static String safeDisplayName(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        try {
            String displayName = stack.getDisplayName();
            if (displayName != null && !displayName.trim()
                .isEmpty()) {
                return displayName;
            }
        } catch (Throwable ignored) {}
        Object rawName = stack.getItem() != null ? Item.itemRegistry.getNameForObject(stack.getItem()) : null;
        return rawName != null ? rawName.toString() : "";
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void markBlockStatsDirty() {
        blockStatsDirty = true;
        blockStatsWidthsDirty = true;
        blockStatsScrollX = 0;
        blockStatsScrollY = 0;
        clearBlockStatsGeometry();
        invalidateDocumentLayout();
    }

    private void clearBlockStatsGeometry() {
        cachedBlockStatsBoxRect = null;
        cachedBlockStatsViewportRect = null;
        cachedBlockStatsHorizontalTrackRect = null;
        cachedBlockStatsHorizontalThumbRect = null;
        cachedBlockStatsVerticalTrackRect = null;
        cachedBlockStatsVerticalThumbRect = null;
        blockStatsMaxScrollX = 0;
        blockStatsMaxScrollY = 0;
        blockStatsViewportWidth = 0;
        blockStatsViewportHeight = 0;
        blockStatsContentWidth = 0;
        blockStatsContentHeight = 0;
        cachedBlockStatsHitRegionCount = 0;
    }

    private boolean shouldRenderOriginAxes() {
        return !forceHideOriginAxes && (forceOriginAxesVisible || ModConfig.debug.enableDebugMode);
    }

    private void drawBlockStatsOverlay(RenderContext context, LytRect sceneRect, LytRect outerRect) {
        cachedBlockStatsHitRegionCount = 0;
        if (!blockStatsEnabled || !blockStatsVisible || sceneRect == null || sceneRect.isEmpty()) {
            clearBlockStatsGeometry();
            return;
        }
        List<SceneBlockStatsEntry> entries = getCachedBlockStatsEntries(context);
        if (entries.isEmpty()) {
            clearBlockStatsGeometry();
            return;
        }
        if (blockStatsDock.isOutside() && blockStatsMode != BlockStatsMode.MANUAL) {
            drawDockedBlockStatsOverlay(context, sceneRect, outerRect, entries);
            return;
        }
        int lineHeight = context.getLineHeight(BLOCK_STATS_TEXT_STYLE);
        int rowHeight = Math.max(BLOCK_STATS_ITEM_SIZE, lineHeight);
        int rowStride = rowHeight + BLOCK_STATS_ROW_GAP;
        int rowContentWidth = blockStatsEntryWidth();
        blockStatsContentWidth = rowContentWidth;
        blockStatsContentHeight = entries.size() * rowStride - BLOCK_STATS_ROW_GAP;
        int maxWidth = Math
            .min(resolveInsideBlockStatsMaxWidth(sceneRect, entries.size(), rowContentWidth), sceneRect.width());
        int maxHeight = Math
            .min(resolveInsideBlockStatsMaxHeight(sceneRect, entries.size(), rowHeight), sceneRect.height());
        if (maxWidth < BLOCK_STATS_MIN_WIDTH || maxHeight < BLOCK_STATS_MIN_HEIGHT
            || rowContentWidth <= 0
            || blockStatsContentHeight <= 0) {
            clearBlockStatsGeometry();
            return;
        }
        boolean reserveVerticalScrollbar = blockStatsContentHeight + BLOCK_STATS_PADDING_Y * 2 > maxHeight;
        int preferredWidth = rowContentWidth + BLOCK_STATS_PADDING_X * 2
            + (reserveVerticalScrollbar ? BLOCK_STATS_SCROLLBAR_SIZE : 0);
        int boxWidth = Math.min(maxWidth, Math.max(BLOCK_STATS_MIN_WIDTH, preferredWidth));
        int boxHeight = Math
            .min(maxHeight, Math.max(BLOCK_STATS_MIN_HEIGHT, blockStatsContentHeight + BLOCK_STATS_PADDING_Y * 2));

        int innerWidth = Math.max(1, boxWidth - BLOCK_STATS_PADDING_X * 2);
        int innerHeight = Math.max(1, boxHeight - BLOCK_STATS_PADDING_Y * 2);
        boolean verticalNeeded = blockStatsContentHeight > innerHeight;
        if (verticalNeeded) {
            innerWidth = Math.max(1, innerWidth - BLOCK_STATS_SCROLLBAR_SIZE);
        }
        boolean horizontalNeeded = rowContentWidth > innerWidth;
        if (horizontalNeeded) {
            innerHeight = Math.max(1, innerHeight - BLOCK_STATS_SCROLLBAR_SIZE);
        }
        if (!verticalNeeded && blockStatsContentHeight > innerHeight) {
            verticalNeeded = true;
            innerWidth = Math.max(1, innerWidth - BLOCK_STATS_SCROLLBAR_SIZE);
            horizontalNeeded = rowContentWidth > innerWidth;
        }
        blockStatsViewportWidth = innerWidth;
        blockStatsViewportHeight = innerHeight;
        blockStatsMaxScrollX = Math.max(0, rowContentWidth - innerWidth);
        blockStatsMaxScrollY = Math.max(0, blockStatsContentHeight - innerHeight);
        clampBlockStatsScroll();

        int x = switch (blockStatsCorner) {
            case TOP_LEFT, BOTTOM_LEFT -> sceneRect.x() + BLOCK_STATS_GAP;
            case TOP_RIGHT, BOTTOM_RIGHT -> sceneRect.right() - boxWidth - BLOCK_STATS_GAP;
        };
        int y = switch (blockStatsCorner) {
            case TOP_LEFT, TOP_RIGHT -> sceneRect.y() + BLOCK_STATS_GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sceneRect.bottom() - boxHeight - BLOCK_STATS_GAP;
        };
        x = clampInt(x, sceneRect.x(), sceneRect.right() - boxWidth);
        y = clampInt(y, sceneRect.y(), sceneRect.bottom() - boxHeight);
        LytRect box = cachedBlockStatsBoxRect = updateCachedRect(cachedBlockStatsBoxRect, x, y, boxWidth, boxHeight);
        LytRect viewport = cachedBlockStatsViewportRect = updateCachedRect(
            cachedBlockStatsViewportRect,
            x + BLOCK_STATS_PADDING_X,
            y + BLOCK_STATS_PADDING_Y,
            innerWidth,
            innerHeight);
        layoutBlockStatsScrollbars(box, viewport, horizontalNeeded, verticalNeeded);

        context.fillRect(box, BLOCK_STATS_BACKGROUND_COLOR);
        context.drawBorder(box, BLOCK_STATS_BORDER_COLOR, 1);
        context.pushLocalScissor(viewport);
        try {
            drawLinearBlockStatsRows(context, entries, viewport, rowHeight, rowStride, lineHeight);
        } finally {
            context.popScissor();
        }
        drawBlockStatsScrollbars(context);
    }

    private int resolveInsideBlockStatsMaxWidth(LytRect sceneRect, int entryCount, int rowContentWidth) {
        int maxWidth = blockStatsMaxWidth;
        if (entryCount == 1) {
            maxWidth = Math.max(maxWidth, rowContentWidth + BLOCK_STATS_PADDING_X * 2);
        }
        return Math.min(maxWidth, sceneRect.width());
    }

    private int resolveInsideBlockStatsMaxHeight(LytRect sceneRect, int entryCount, int rowHeight) {
        int maxHeight = blockStatsMaxHeight;
        if (entryCount == 1) {
            maxHeight = Math.max(maxHeight, rowHeight + BLOCK_STATS_PADDING_Y * 2);
        }
        return Math.min(maxHeight, sceneRect.height());
    }

    private void drawDockedBlockStatsOverlay(RenderContext context, LytRect sceneRect, LytRect outerRect,
        List<SceneBlockStatsEntry> entries) {
        int lineHeight = context.getLineHeight(BLOCK_STATS_TEXT_STYLE);
        int rowHeight = Math.max(BLOCK_STATS_ITEM_SIZE, lineHeight);
        int rowStride = rowHeight + BLOCK_STATS_ROW_GAP;
        int entryWidth = blockStatsEntryWidth();
        boolean verticalDock = blockStatsDock == BlockStatsDock.LEFT || blockStatsDock == BlockStatsDock.RIGHT;
        int maxWidth = verticalDock ? blockStatsMaxWidth : Math.min(blockStatsMaxWidth, sceneRect.width());
        int maxHeight = verticalDock ? Math.min(blockStatsMaxHeight, sceneRect.height()) : blockStatsMaxHeight;
        if (blockStatsDock == BlockStatsDock.LEFT) {
            maxWidth = Math.min(maxWidth, Math.max(0, sceneRect.x() - getBounds().x() - BLOCK_STATS_DOCK_GAP));
        } else if (blockStatsDock == BlockStatsDock.RIGHT) {
            int rightStart = outerRect.right() + buttonColumnReserve() + BLOCK_STATS_DOCK_GAP;
            maxWidth = Math.min(maxWidth, Math.max(0, getBounds().right() - rightStart));
        } else if (blockStatsDock == BlockStatsDock.TOP) {
            maxHeight = Math.min(maxHeight, Math.max(0, sceneRect.y() - getBounds().y() - BLOCK_STATS_DOCK_GAP));
        } else if (blockStatsDock == BlockStatsDock.BOTTOM) {
            maxHeight = Math
                .min(maxHeight, Math.max(0, getBounds().bottom() - outerRect.bottom() - BLOCK_STATS_DOCK_GAP));
        }
        if (maxWidth < BLOCK_STATS_MIN_WIDTH || maxHeight < BLOCK_STATS_MIN_HEIGHT) {
            clearBlockStatsGeometry();
            return;
        }
        int maxAlong = Math.max(rowStride, verticalDock ? sceneRect.height() : sceneRect.width());
        int alongUnit = Math.max(1, verticalDock ? rowStride : entryWidth);
        int alongSlots = Math.max(1, maxAlong / alongUnit);
        int maxContentWidth = Math.max(entryWidth, maxWidth - BLOCK_STATS_PADDING_X * 2 - BLOCK_STATS_SCROLLBAR_SIZE);
        int maxColumnsToFit = Math.max(1, maxContentWidth / Math.max(1, entryWidth));
        if (verticalDock) {
            int minRowsToFitWidth = Math.max(1, (entries.size() + maxColumnsToFit - 1) / maxColumnsToFit);
            alongSlots = Math.min(entries.size(), Math.max(alongSlots, minRowsToFitWidth));
        } else {
            alongSlots = Math.min(alongSlots, maxColumnsToFit);
        }
        int wrappedLines = Math.max(1, (entries.size() + alongSlots - 1) / alongSlots);
        int usedAlongSlots = Math.min(entries.size(), alongSlots);
        blockStatsContentWidth = verticalDock ? wrappedLines * entryWidth : usedAlongSlots * entryWidth;
        blockStatsContentHeight = verticalDock ? usedAlongSlots * rowStride - BLOCK_STATS_ROW_GAP
            : wrappedLines * rowStride - BLOCK_STATS_ROW_GAP;
        maxHeight = Math.min(maxHeight, Math.max(BLOCK_STATS_MIN_HEIGHT, blockStatsContentHeight));
        boolean reserveVerticalScrollbar = blockStatsContentHeight + BLOCK_STATS_PADDING_Y * 2 > maxHeight;
        int preferredWidth = blockStatsContentWidth + BLOCK_STATS_PADDING_X * 2
            + (reserveVerticalScrollbar ? BLOCK_STATS_SCROLLBAR_SIZE : 0);
        int boxWidth = Math.max(BLOCK_STATS_MIN_WIDTH, Math.min(maxWidth, preferredWidth));
        int boxHeight = Math
            .max(BLOCK_STATS_MIN_HEIGHT, Math.min(maxHeight, blockStatsContentHeight + BLOCK_STATS_PADDING_Y * 2));
        int innerWidth = Math.max(1, boxWidth - BLOCK_STATS_PADDING_X * 2);
        int innerHeight = Math.max(1, boxHeight - BLOCK_STATS_PADDING_Y * 2);
        boolean verticalNeeded = blockStatsContentHeight > innerHeight;
        if (verticalNeeded) {
            innerWidth = Math.max(1, innerWidth - BLOCK_STATS_SCROLLBAR_SIZE);
        }
        boolean horizontalNeeded = blockStatsContentWidth > innerWidth;
        if (horizontalNeeded) {
            innerHeight = Math.max(1, innerHeight - BLOCK_STATS_SCROLLBAR_SIZE);
        }
        if (!verticalNeeded && blockStatsContentHeight > innerHeight) {
            verticalNeeded = true;
            innerWidth = Math.max(1, innerWidth - BLOCK_STATS_SCROLLBAR_SIZE);
            horizontalNeeded = blockStatsContentWidth > innerWidth;
        }
        blockStatsViewportWidth = innerWidth;
        blockStatsViewportHeight = innerHeight;
        blockStatsMaxScrollX = Math.max(0, blockStatsContentWidth - innerWidth);
        blockStatsMaxScrollY = Math.max(0, blockStatsContentHeight - innerHeight);
        clampBlockStatsScroll();

        int x = switch (blockStatsDock) {
            case LEFT -> sceneRect.x() - BLOCK_STATS_DOCK_GAP - boxWidth;
            case RIGHT -> outerRect.right() + buttonColumnReserve() + BLOCK_STATS_DOCK_GAP;
            case TOP -> sceneRect.x();
            case BOTTOM -> sceneRect.x();
            case INSIDE -> sceneRect.x();
        };
        int y = switch (blockStatsDock) {
            case LEFT, RIGHT -> sceneRect.y();
            case TOP -> sceneRect.y() - BLOCK_STATS_DOCK_GAP - boxHeight;
            case BOTTOM -> outerRect.bottom() + BLOCK_STATS_DOCK_GAP;
            case INSIDE -> sceneRect.y();
        };
        LytRect box = cachedBlockStatsBoxRect = updateCachedRect(cachedBlockStatsBoxRect, x, y, boxWidth, boxHeight);
        LytRect viewport = cachedBlockStatsViewportRect = updateCachedRect(
            cachedBlockStatsViewportRect,
            x + BLOCK_STATS_PADDING_X,
            y + BLOCK_STATS_PADDING_Y,
            innerWidth,
            innerHeight);
        layoutBlockStatsScrollbars(box, viewport, horizontalNeeded, verticalNeeded);
        context.fillRect(box, BLOCK_STATS_BACKGROUND_COLOR);
        context.drawBorder(box, BLOCK_STATS_BORDER_COLOR, 1);
        context.pushLocalScissor(viewport);
        try {
            drawWrappedBlockStatsRows(
                context,
                entries,
                viewport,
                rowHeight,
                rowStride,
                lineHeight,
                entryWidth,
                alongSlots,
                verticalDock);
        } finally {
            context.popScissor();
        }
        drawBlockStatsScrollbars(context);
    }

    private int blockStatsEntryWidth() {
        if (!blockStatsShowNames) {
            return blockStatsItemWidth();
        }
        return blockStatsItemWidth() + BLOCK_STATS_GAP + cachedBlockStatsContentWidth;
    }

    private int blockStatsItemWidth() {
        return Math.max(BLOCK_STATS_ITEM_SIZE, cachedBlockStatsItemWidth);
    }

    private void drawLinearBlockStatsRows(RenderContext context, List<SceneBlockStatsEntry> entries, LytRect viewport,
        int rowHeight, int rowStride, int lineHeight) {
        int firstRow = Math.max(0, blockStatsScrollY / Math.max(1, rowStride));
        int rowY = viewport.y() - blockStatsScrollY + firstRow * rowStride;
        int itemX = viewport.x() - blockStatsScrollX;
        int textX = itemX + blockStatsItemWidth() + BLOCK_STATS_GAP;
        int textOffsetY = Math.max(0, (rowHeight - lineHeight) / 2);
        int itemOffsetY = Math.max(0, (rowHeight - BLOCK_STATS_ITEM_SIZE) / 2);
        for (int i = firstRow; i < entries.size() && rowY < viewport.bottom(); i++) {
            drawBlockStatsEntry(
                context,
                entries.get(i),
                viewport,
                itemX,
                rowY,
                textX,
                rowHeight,
                textOffsetY,
                itemOffsetY);
            rowY += rowStride;
        }
    }

    private void drawWrappedBlockStatsRows(RenderContext context, List<SceneBlockStatsEntry> entries, LytRect viewport,
        int rowHeight, int rowStride, int lineHeight, int entryWidth, int alongSlots, boolean verticalDock) {
        int textOffsetY = Math.max(0, (rowHeight - lineHeight) / 2);
        int itemOffsetY = Math.max(0, (rowHeight - BLOCK_STATS_ITEM_SIZE) / 2);
        int baseX = viewport.x() - blockStatsScrollX;
        int baseY = viewport.y() - blockStatsScrollY;
        for (int i = 0; i < entries.size(); i++) {
            int along = i % alongSlots;
            int cross = i / alongSlots;
            int itemX = verticalDock ? baseX + cross * entryWidth : baseX + along * entryWidth;
            int rowY = verticalDock ? baseY + along * rowStride : baseY + cross * rowStride;
            if (rowY + rowHeight < viewport.y() || rowY >= viewport.bottom()
                || itemX + entryWidth < viewport.x()
                || itemX >= viewport.right()) {
                continue;
            }
            drawBlockStatsEntry(
                context,
                entries.get(i),
                viewport,
                itemX,
                rowY,
                itemX + blockStatsItemWidth() + BLOCK_STATS_GAP,
                rowHeight,
                textOffsetY,
                itemOffsetY);
        }
    }

    private void drawBlockStatsEntry(RenderContext context, SceneBlockStatsEntry entry, LytRect viewport, int itemX,
        int rowY, int textX, int rowHeight, int textOffsetY, int itemOffsetY) {
        boolean selected = selectedBlockStatsKey != null && selectedBlockStatsKey.equals(entry.getKey());
        int rowWidth = blockStatsShowNames ? blockStatsEntryWidth() : blockStatsItemWidth();
        if (selected) {
            context.fillRect(itemX - 1, rowY, rowWidth + 2, rowHeight, BLOCK_STATS_SELECTED_ROW_COLOR);
        }
        int itemY = rowY + itemOffsetY;
        context.renderItemIcon(entry.getDisplayStack(), itemX, itemY);
        drawBlockStatsItemCount(context, entry, itemX, itemY);
        addBlockStatsHitRegion(entry, itemX, rowY, rowWidth, rowHeight, viewport);
        if (blockStatsShowNames) {
            String text = blockStatsEntryText(entry);
            context.drawText(text, textX, rowY + textOffsetY, BLOCK_STATS_TEXT_STYLE);
        }
    }

    private void drawBlockStatsItemCount(RenderContext context, SceneBlockStatsEntry entry, int itemX, int itemY) {
        if (entry.getCount() <= 1) {
            return;
        }
        String countText = formatBlockStatsDisplayCount(entry.getCount());
        int textWidth = context.getStringWidth(countText, BLOCK_STATS_COUNT_TEXT_STYLE);
        int textHeight = context.getLineHeight(BLOCK_STATS_COUNT_TEXT_STYLE);
        int countX = itemX + blockStatsItemWidth() - textWidth;
        int countY = itemY + BLOCK_STATS_ITEM_SIZE - textHeight + 1;
        context.drawText(countText, countX, countY, BLOCK_STATS_COUNT_TEXT_STYLE);
    }

    private void addBlockStatsHitRegion(SceneBlockStatsEntry entry, int x, int y, int width, int height,
        LytRect viewport) {
        int x0 = Math.max(x, viewport.x());
        int y0 = Math.max(y, viewport.y());
        int x1 = Math.min(x + width, viewport.right());
        int y1 = Math.min(y + height, viewport.bottom());
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        BlockStatsHitRegion region;
        if (cachedBlockStatsHitRegionCount < cachedBlockStatsHitRegions.size()) {
            region = cachedBlockStatsHitRegions.get(cachedBlockStatsHitRegionCount);
            region.update(x0, y0, x1, y1, entry);
        } else {
            region = new BlockStatsHitRegion(x0, y0, x1, y1, entry);
            cachedBlockStatsHitRegions.add(region);
        }
        cachedBlockStatsHitRegionCount++;
    }

    private void layoutBlockStatsScrollbars(LytRect box, LytRect viewport, boolean horizontalNeeded,
        boolean verticalNeeded) {
        cachedBlockStatsHorizontalTrackRect = null;
        cachedBlockStatsHorizontalThumbRect = null;
        cachedBlockStatsVerticalTrackRect = null;
        cachedBlockStatsVerticalThumbRect = null;
        if (horizontalNeeded) {
            int trackY = box.bottom() - BLOCK_STATS_PADDING_Y - BLOCK_STATS_SCROLLBAR_SIZE;
            cachedBlockStatsHorizontalTrackRect = updateCachedRect(
                cachedBlockStatsHorizontalTrackRect,
                viewport.x(),
                trackY,
                viewport.width(),
                BLOCK_STATS_SCROLLBAR_SIZE);
            cachedBlockStatsHorizontalThumbRect = layoutBlockStatsThumb(
                cachedBlockStatsHorizontalThumbRect,
                cachedBlockStatsHorizontalTrackRect,
                viewport.width(),
                blockStatsContentWidth,
                blockStatsScrollX,
                blockStatsMaxScrollX,
                true);
        }
        if (verticalNeeded) {
            int trackX = box.right() - BLOCK_STATS_PADDING_X - BLOCK_STATS_SCROLLBAR_SIZE;
            cachedBlockStatsVerticalTrackRect = updateCachedRect(
                cachedBlockStatsVerticalTrackRect,
                trackX,
                viewport.y(),
                BLOCK_STATS_SCROLLBAR_SIZE,
                viewport.height());
            cachedBlockStatsVerticalThumbRect = layoutBlockStatsThumb(
                cachedBlockStatsVerticalThumbRect,
                cachedBlockStatsVerticalTrackRect,
                viewport.height(),
                blockStatsContentHeight,
                blockStatsScrollY,
                blockStatsMaxScrollY,
                false);
        }
    }

    private LytRect layoutBlockStatsThumb(@Nullable LytRect current, LytRect track, int viewportSize, int contentSize,
        int scroll, int maxScroll, boolean horizontal) {
        int trackSize = horizontal ? track.width() : track.height();
        int thumbSize = Math.max(
            BLOCK_STATS_SCROLLBAR_MIN_THUMB,
            (int) ((long) trackSize * Math.max(1, viewportSize) / Math.max(1, contentSize)));
        thumbSize = Math.min(trackSize, thumbSize);
        int travel = Math.max(0, trackSize - thumbSize);
        int offset = maxScroll > 0 ? (int) ((long) travel * scroll / maxScroll) : 0;
        return horizontal ? updateCachedRect(current, track.x() + offset, track.y(), thumbSize, track.height())
            : updateCachedRect(current, track.x(), track.y() + offset, track.width(), thumbSize);
    }

    private void drawBlockStatsScrollbars(RenderContext context) {
        drawBlockStatsScrollbar(context, cachedBlockStatsHorizontalTrackRect, cachedBlockStatsHorizontalThumbRect);
        drawBlockStatsScrollbar(context, cachedBlockStatsVerticalTrackRect, cachedBlockStatsVerticalThumbRect);
    }

    private static void drawBlockStatsScrollbar(RenderContext context, @Nullable LytRect track,
        @Nullable LytRect thumb) {
        if (track == null || thumb == null || track.isEmpty() || thumb.isEmpty()) {
            return;
        }
        context.fillRect(track, 0x5522262C);
        context.fillRect(thumb, 0xCCEAF6FF);
    }

    public static class BlockStatsHitRegion {

        private int x0;
        private int y0;
        private int x1;
        private int y1;
        private SceneBlockStatsEntry entry;

        public BlockStatsHitRegion(int x0, int y0, int x1, int y1, SceneBlockStatsEntry entry) {
            update(x0, y0, x1, y1, entry);
        }

        public void update(int x0, int y0, int x1, int y1, SceneBlockStatsEntry entry) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.entry = entry;
        }

        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
        }

        public SceneBlockStatsEntry entry() {
            return entry;
        }
    }

    private void clampBlockStatsScroll() {
        blockStatsScrollX = clampInt(blockStatsScrollX, 0, blockStatsMaxScrollX);
        blockStatsScrollY = clampInt(blockStatsScrollY, 0, blockStatsMaxScrollY);
    }

    public boolean containsBlockStatsOverlay(int mouseX, int mouseY) {
        if (!blockStatsEnabled || !blockStatsVisible || cachedBlockStatsBoxRect == null) {
            return false;
        }
        int layoutX = screenToLayoutX(mouseX);
        int layoutY = screenToLayoutY(mouseY);
        if (cachedBlockStatsBoxRect.contains(layoutX, layoutY)) {
            return true;
        }
        for (int i = 0; i < cachedBlockStatsHitRegionCount; i++) {
            BlockStatsHitRegion region = cachedBlockStatsHitRegions.get(i);
            if (region.contains(layoutX, layoutY)) {
                return true;
            }
        }
        return false;
    }

    public boolean startBlockStatsDrag(int mouseX, int mouseY, int button) {
        if (button != 0 || !containsBlockStatsOverlay(mouseX, mouseY)) {
            return false;
        }
        int layoutX = screenToLayoutX(mouseX);
        int layoutY = screenToLayoutY(mouseY);
        if (cachedBlockStatsHorizontalTrackRect != null
            && cachedBlockStatsHorizontalTrackRect.contains(layoutX, layoutY)) {
            draggingBlockStatsHorizontalScrollbar = true;
            blockStatsScrollbarGrabOffset = cachedBlockStatsHorizontalThumbRect != null
                && cachedBlockStatsHorizontalThumbRect.contains(layoutX, layoutY)
                    ? layoutX - cachedBlockStatsHorizontalThumbRect.x()
                    : BLOCK_STATS_SCROLLBAR_MIN_THUMB / 2;
            updateBlockStatsHorizontalScrollFromMouse(layoutX);
            return true;
        }
        if (cachedBlockStatsVerticalTrackRect != null && cachedBlockStatsVerticalTrackRect.contains(layoutX, layoutY)) {
            draggingBlockStatsVerticalScrollbar = true;
            blockStatsScrollbarGrabOffset = cachedBlockStatsVerticalThumbRect != null
                && cachedBlockStatsVerticalThumbRect.contains(layoutX, layoutY)
                    ? layoutY - cachedBlockStatsVerticalThumbRect.y()
                    : BLOCK_STATS_SCROLLBAR_MIN_THUMB / 2;
            updateBlockStatsVerticalScrollFromMouse(layoutY);
            return true;
        }
        BlockStatsHitRegion hitRegion = blockStatsHitRegionAt(layoutX, layoutY);
        if (hitRegion != null && blockStatsMode != BlockStatsMode.MANUAL) {
            toggleSelectedBlockStatsEntry(
                hitRegion.entry()
                    .getKey());
        }
        return true;
    }

    @Nullable
    public GuideTooltip createBlockStatsTooltip(int mouseX, int mouseY) {
        if (!blockStatsEnabled || !blockStatsVisible) {
            return null;
        }
        BlockStatsHitRegion hitRegion = blockStatsHitRegionAt(screenToLayoutX(mouseX), screenToLayoutY(mouseY));
        if (hitRegion == null) {
            return null;
        }
        SceneBlockStatsEntry entry = hitRegion.entry();
        ItemStack stack = entry.copyDisplayStack();
        if (stack == null) {
            return null;
        }
        return new AppendedItemTooltip(
            stack,
            List.of(GuidebookText.SceneBlockStatsTooltipCount.text(Integer.toString(entry.getCount()))));
    }

    @Nullable
    private BlockStatsHitRegion blockStatsHitRegionAt(int mouseX, int mouseY) {
        for (int i = cachedBlockStatsHitRegionCount - 1; i >= 0; i--) {
            BlockStatsHitRegion region = cachedBlockStatsHitRegions.get(i);
            if (region.contains(mouseX, mouseY)) {
                return region;
            }
        }
        return null;
    }

    private int screenToLayoutX(int mouseX) {
        if (lastDocZoom <= 0f) {
            return mouseX;
        }
        return Math.round((mouseX - lastAbsX) / lastDocZoom) + (cachedSceneRect != null ? cachedSceneRect.x() : 0);
    }

    private int screenToLayoutY(int mouseY) {
        if (lastDocZoom <= 0f) {
            return mouseY;
        }
        return Math.round((mouseY - lastAbsY) / lastDocZoom) + (cachedSceneRect != null ? cachedSceneRect.y() : 0);
    }

    private void toggleSelectedBlockStatsEntry(String key) {
        if (key == null || key.isEmpty()) {
            selectedBlockStatsKey = null;
            return;
        }
        selectedBlockStatsKey = key.equals(selectedBlockStatsKey) ? null : key;
    }

    public boolean isDraggingBlockStatsScrollbar() {
        return draggingBlockStatsHorizontalScrollbar || draggingBlockStatsVerticalScrollbar;
    }

    private void updateBlockStatsHorizontalScrollFromMouse(int mouseX) {
        if (cachedBlockStatsHorizontalTrackRect == null || cachedBlockStatsHorizontalThumbRect == null
            || blockStatsMaxScrollX <= 0) {
            return;
        }
        int track = Math
            .max(1, cachedBlockStatsHorizontalTrackRect.width() - cachedBlockStatsHorizontalThumbRect.width());
        int rel = clampInt(mouseX - blockStatsScrollbarGrabOffset - cachedBlockStatsHorizontalTrackRect.x(), 0, track);
        blockStatsScrollX = (int) ((long) rel * blockStatsMaxScrollX / track);
    }

    private void updateBlockStatsVerticalScrollFromMouse(int mouseY) {
        if (cachedBlockStatsVerticalTrackRect == null || cachedBlockStatsVerticalThumbRect == null
            || blockStatsMaxScrollY <= 0) {
            return;
        }
        int track = Math
            .max(1, cachedBlockStatsVerticalTrackRect.height() - cachedBlockStatsVerticalThumbRect.height());
        int rel = clampInt(mouseY - blockStatsScrollbarGrabOffset - cachedBlockStatsVerticalTrackRect.y(), 0, track);
        blockStatsScrollY = (int) ((long) rel * blockStatsMaxScrollY / track);
    }

    private boolean scrollBlockStatsOverlay(int dwheel) {
        if (dwheel == 0) {
            return false;
        }
        int sign = Integer.signum(dwheel);
        if (GuiScreen.isShiftKeyDown() && blockStatsMaxScrollX > 0
            || blockStatsMaxScrollY <= 0 && blockStatsMaxScrollX > 0) {
            blockStatsScrollX -= sign * BLOCK_STATS_WHEEL_STEP;
        } else if (blockStatsMaxScrollY > 0) {
            blockStatsScrollY -= sign * BLOCK_STATS_WHEEL_STEP;
        } else {
            return false;
        }
        clampBlockStatsScroll();
        return true;
    }

    private static InWorldLineAnnotation createOriginAxisAnnotation(Vector3f to, ConstantColor color) {
        return new InWorldLineAnnotation(new Vector3f(), to, color, ORIGIN_AXIS_THICKNESS);
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
        int extra = 0;
        if (hasStructureLibHatchData()) extra++;
        if (gridButtonEnabled) extra++;
        if (blockStatsButtonEnabled) extra++;
        if (extra == 0) {
            return base;
        }
        GuideIconButton.Role[] roles = new GuideIconButton.Role[base.length + extra];
        System.arraycopy(base, 0, roles, 0, base.length);
        int i = base.length;
        if (hasStructureLibHatchData()) roles[i++] = GuideIconButton.Role.HIGHLIGHT_STRUCTURELIB_HATCHES;
        if (gridButtonEnabled) roles[i++] = GuideIconButton.Role.TOGGLE_GRID;
        if (blockStatsButtonEnabled) roles[i] = GuideIconButton.Role.TOGGLE_BLOCK_STATS;
        return roles;
    }

    private GuideIconButton.Role[] cachedSceneButtonRoles() {
        boolean hasAnnotations = !annotations.isEmpty();
        boolean hasStructureLibHatches = hasStructureLibHatchData();
        if (cachedSceneButtonRolesDirty || cachedSceneButtonsVisible != annotationsVisible
            || cachedSceneHasAnnotations != hasAnnotations
            || cachedSceneHasStructureLibHatches != hasStructureLibHatches
            || cachedGridButtonEnabled != gridButtonEnabled
            || cachedBlockStatsButtonEnabled != blockStatsButtonEnabled) {
            cachedSceneButtonRolesDirty = false;
            cachedSceneButtonsVisible = annotationsVisible;
            cachedSceneHasAnnotations = hasAnnotations;
            cachedSceneHasStructureLibHatches = hasStructureLibHatches;
            cachedGridButtonEnabled = gridButtonEnabled;
            cachedBlockStatsButtonEnabled = blockStatsButtonEnabled;
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
        return (role == GuideIconButton.Role.HIGHLIGHT_STRUCTURELIB_HATCHES && structureLibHatchHighlightEnabled)
            || (role == GuideIconButton.Role.TOGGLE_GRID && gridVisible)
            || (role == GuideIconButton.Role.TOGGLE_BLOCK_STATS && blockStatsVisible);
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
        int bx = sceneButtonsAbsX;
        int by = sceneButtonsAbsY;
        int btnScreenSize = Math.round(BTN_SIZE * lastDocZoom);
        int btnScreenStep = Math.round((BTN_SIZE + BTN_GAP) * lastDocZoom);
        int rolesHeight = btnScreenSize + Math.max(0, cachedSceneButtonRoles().length - 1) * btnScreenStep;
        LytRect buttonColumnRect = new LytRect(bx, by, btnScreenSize, rolesHeight);
        if (renderedContentClip != null) {
            int cx0 = renderedContentClip.x();
            int cy0 = renderedContentClip.y();
            int cx1 = cx0 + renderedContentClip.width();
            int cy1 = cy0 + renderedContentClip.height();
            if (lastAbsX + lastW <= cx0 || lastAbsX >= cx1) return null;
            if (lastAbsY + lastH <= cy0 || lastAbsY >= cy1) return null;
            boolean insideVisibleViewport = mouseX >= cx0 && mouseX < cx1 && mouseY >= cy0 && mouseY < cy1;
            if (!insideVisibleViewport && !buttonColumnRect.contains(mouseX, mouseY)) {
                return null;
            }
        }
        if (mouseY < buttonColumnRect.y() || mouseY >= buttonColumnRect.bottom()) return null;
        // Early-out on X: the whole button column lives at [sceneButtonsAbsX, sceneButtonsAbsX + btnScreenSize).
        if (mouseX < bx || mouseX >= bx + btnScreenSize) return null;
        var roles = cachedSceneButtonRoles();
        for (var role : roles) {
            if (mouseX >= bx && mouseX < bx + btnScreenSize && mouseY >= by && mouseY < by + btnScreenSize) {
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
        if (!hasBottomControls() || !hasStructureLibChannelData()) {
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
        if (!hasStructureLibTierSlider()) {
            return false;
        }
        LytRect hitRect = resolveStructureLibTierSliderHitRect();
        return !hitRect.isEmpty() && hitRect.contains(mouseX, mouseY);
    }

    public boolean containsVisibleLayerSlider(int mouseX, int mouseY) {
        if (!hasVisibleLayerSlider()) {
            return false;
        }
        LytRect hitRect = resolveVisibleLayerSliderHitRect();
        return !hitRect.isEmpty() && hitRect.contains(mouseX, mouseY);
    }

    public boolean containsPonderButtons(int mouseX, int mouseY) {
        if (!hasBottomControls() || ponderSceneData == null) {
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
        return containsSceneViewport(mouseX, mouseY) || containsBottomControlSlider(mouseX, mouseY)
            || containsBlockStatsOverlay(mouseX, mouseY);
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
            case TOGGLE_GRID -> setGridVisible(!gridVisible);
            case TOGGLE_BLOCK_STATS -> setBlockStatsVisible(!blockStatsVisible);
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
        if (startBlockStatsDrag(mouseX, mouseY, button)) {
            return;
        }
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
        if (button == 0) {
            playSceneSounds(GuideSoundTrigger.CLICK, mouseX, mouseY);
        }
        if (isPonderPlaying()) return;
        this.dragButton = button;
        this.dragLastX = mouseX;
        this.dragLastY = mouseY;
    }

    public void updateSoundHover(int mouseX, int mouseY) {
        if (soundCues.isEmpty()) {
            return;
        }
        boolean inside = cachedScreenRect != null && cachedScreenRect.contains(mouseX, mouseY);
        for (SceneSoundCue cue : soundCues) {
            if (!cue.matches(GuideSoundTrigger.HOVER)) {
                continue;
            }
            if (!isStructureLibConditionSatisfied(cue.getStructureLibCondition())) {
                cue.setHovered(false);
                continue;
            }
            if (!inside) {
                cue.setHovered(false);
                continue;
            }
            if (!cue.isHovered()) {
                cue.setHovered(true);
                playSceneSound(cue.getSound(), mouseX, mouseY);
            }
        }
    }

    public static boolean isRotateButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 0 : button == 1;
    }

    private void playEnterSoundsIfNeeded() {
        if (soundCues.isEmpty()) {
            return;
        }
        int centerX = cachedScreenRect != null ? cachedScreenRect.x() + cachedScreenRect.width() / 2
            : lastAbsX + lastW / 2;
        int centerY = cachedScreenRect != null ? cachedScreenRect.y() + cachedScreenRect.height() / 2
            : lastAbsY + lastH / 2;
        for (SceneSoundCue cue : soundCues) {
            if (!cue.matches(GuideSoundTrigger.ENTER) || cue.isEntered()
                || !isStructureLibConditionSatisfied(cue.getStructureLibCondition())) {
                continue;
            }
            cue.setEntered(true);
            playSceneSound(cue.getSound(), centerX, centerY);
        }
    }

    private void playSceneSounds(GuideSoundTrigger trigger, int referenceX, int referenceY) {
        if (soundCues.isEmpty()) {
            return;
        }
        for (SceneSoundCue cue : soundCues) {
            if (cue.matches(trigger) && isStructureLibConditionSatisfied(cue.getStructureLibCondition())) {
                playSceneSound(cue.getSound(), referenceX, referenceY);
            }
        }
    }

    private void playSceneSound(GuideSoundSpec sound, int referenceX, int referenceY) {
        if (sound.hasPosition() && cachedScreenRect != null) {
            float[] screenPoint = projectSoundPosition(sound);
            float dx = screenPoint[0] - referenceX;
            float dy = screenPoint[1] - referenceY;
            float fallbackRadius = Math.min(cachedScreenRect.width(), cachedScreenRect.height()) * 0.75f;
            float volume = GuideSoundPlayback.attenuate(sound, (float) Math.sqrt(dx * dx + dy * dy), fallbackRadius);
            GuideSoundPlayback.play(sound, volume);
            return;
        }
        GuideSoundPlayback.play(sound);
    }

    private float[] projectSoundPosition(GuideSoundSpec sound) {
        float[] saved = null;
        if (ponderSceneData != null) {
            saved = applyPonderCameraForProjection();
        }
        try {
            var projected = camera.worldToScreen(sound.x(), sound.y(), sound.z());
            return new float[] { lastAbsX + projected.x, lastAbsY + projected.y };
        } finally {
            if (saved != null) {
                restoreCameraFromProjection(saved);
            }
        }
    }

    private float[] applyPonderCameraForProjection() {
        float[] saved = new float[] { camera.getZoom(), camera.getRotationX(), camera.getRotationY(),
            camera.getRotationZ(), camera.getOffsetX(), camera.getOffsetY() };
        camera.setZoom(ponderCamZoom);
        camera.setRotationX(ponderCamRotX);
        camera.setRotationY(ponderCamRotY);
        camera.setRotationZ(ponderCamRotZ);
        camera.setOffsetX(ponderCamOffX);
        camera.setOffsetY(ponderCamOffY);
        return saved;
    }

    private void restoreCameraFromProjection(float[] saved) {
        camera.setZoom(saved[0]);
        camera.setRotationX(saved[1]);
        camera.setRotationY(saved[2]);
        camera.setRotationZ(saved[3]);
        camera.setOffsetX(saved[4]);
        camera.setOffsetY(saved[5]);
    }

    public static boolean isPanButton(int button) {
        return ModConfig.ui.sceneSwapMouseButtons ? button == 1 : button == 0;
    }

    public void drag(int mouseX, int mouseY) {
        if (draggingBlockStatsHorizontalScrollbar) {
            updateBlockStatsHorizontalScrollFromMouse(screenToLayoutX(mouseX));
            return;
        }
        if (draggingBlockStatsVerticalScrollbar) {
            updateBlockStatsVerticalScrollFromMouse(screenToLayoutY(mouseY));
            return;
        }
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
        applyCameraDrag(dx, dy);
    }

    public void pollDrag() {
        // Camera movement is driven by explicit mouse drag events. Polling raw Mouse.getDX/Y here
        // also consumes tiny deltas left by a plain click, which makes a click rotate the scene.
    }

    private void applyCameraDrag(float dx, float dy) {
        if (dx == 0f && dy == 0f) {
            return;
        }
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
        this.draggingBlockStatsHorizontalScrollbar = false;
        this.draggingBlockStatsVerticalScrollbar = false;
    }

    public boolean isDragging() {
        return dragButton >= 0 || draggingVisibleLayerSlider
            || draggingStructureLibTierSlider
            || draggingStructureLibChannelId != null
            || draggingPonderBar
            || isDraggingBlockStatsScrollbar();
    }

    public void scroll(int mouseX, int mouseY, int dwheel) {
        if (!interactive) return;
        if (dwheel == 0) return;
        if (containsBlockStatsOverlay(mouseX, mouseY) && scrollBlockStatsOverlay(dwheel)) {
            return;
        }
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

    public List<SceneAnnotation> getPonderActiveAnnotationsForTesting() {
        return List.copyOf(ponderActiveAnnotations);
    }

    private void appendStructureLibHatchOverlays(List<InWorldAnnotation> inWorld) {
        if (!structureLibHatchHighlightEnabled || structureLibHatchOverlayAnnotations.isEmpty()) {
            return;
        }
        for (InWorldBlockFaceOverlayAnnotation overlay : structureLibHatchOverlayAnnotations) {
            if (!isBlockVisibleForCurrentLayer(overlay.getBlockY())) {
                continue;
            }
            overlay.setHovered(
                hoveredStructureLibHatch != null && hoveredStructureLibHatch[0] == overlay.getBlockX()
                    && hoveredStructureLibHatch[1] == overlay.getBlockY()
                    && hoveredStructureLibHatch[2] == overlay.getBlockZ());
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
        return hasBottomControls() && visibleLayerSliderEnabled && hasVisibleLayerData();
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

    public int getPonderCurrentTickForExport() {
        return ponderCurrentTick;
    }

    public int getPonderTotalTimeForExport() {
        return ponderSceneData != null ? ponderSceneData.getTotalTime() : 0;
    }

    public List<PonderTimelineKeyframe> getPonderTimelineKeyframesForExport() {
        if (ponderSceneData == null) {
            return List.of();
        }
        ArrayList<PonderTimelineKeyframe> keyframes = new ArrayList<>();
        for (PonderKeyframe keyframe : ponderSceneData.getKeyframes()) {
            if (keyframe != null) {
                keyframes.add(new PonderTimelineKeyframe(keyframe.getTime(), keyframe.getLabel()));
            }
        }
        return List.copyOf(keyframes);
    }

    public boolean isPonderPlaying() {
        return ponderSceneData != null && !ponderPaused && !ponderFinished;
    }

    public boolean isPonderPausedForPreviewRestore() {
        return ponderSceneData != null && ponderPaused;
    }

    public boolean isPonderFinishedForPreviewRestore() {
        return ponderSceneData != null && ponderFinished;
    }

    private int structureLibTierSliderAreaHeight() {
        return hasStructureLibTierSlider() ? SCENE_SLIDER_AREA_HEIGHT : 0;
    }

    private int structureLibChannelSliderAreaHeight() {
        return getBottomControlStructureLibChannels().size() * SCENE_SLIDER_AREA_HEIGHT;
    }

    private List<StructureLibSceneMetadata.ChannelData> getSelectableStructureLibChannels() {
        return selectableStructureLibChannels.isEmpty() ? List.of() : selectableStructureLibChannels;
    }

    private boolean hasBottomControls() {
        return interactive && bottomControlsVisible;
    }

    private boolean hasStructureLibTierSlider() {
        return hasBottomControls() && hasStructureLibTierData();
    }

    private List<StructureLibSceneMetadata.ChannelData> getBottomControlStructureLibChannels() {
        return hasBottomControls() ? getSelectableStructureLibChannels() : List.of();
    }

    public void attachPonderData(PonderSceneData data, List<List<SceneAnnotation>> annotationsByKeyframe) {
        attachPonderData(data, annotationsByKeyframe, List.of());
    }

    public void attachPonderData(PonderSceneData data, List<List<SceneAnnotation>> annotationsByKeyframe,
        List<List<GuideSoundSpec>> soundsByKeyframe) {
        clearPonderBlockChanges();
        this.ponderSceneData = data;
        rebuildPonderTimedEntityAnimations();
        clearPonderEntityAnimationBaselines();
        this.ponderKeyframeAnnotationSets = annotationsByKeyframe != null ? new ArrayList<>(annotationsByKeyframe)
            : new ArrayList<>();
        this.ponderKeyframeSoundSets = soundsByKeyframe != null ? new ArrayList<>(soundsByKeyframe) : new ArrayList<>();
        this.ponderCurrentTick = 0;
        this.ponderPaused = true;
        this.ponderFinished = false;
        this.draggingPonderBar = false;
        this.ponderLastKeyframeIdx = -2;
        this.ponderAnnotationFadeTick = 5;
        this.ponderOutgoingAnnotations.clear();
        this.ponderOutgoingFadeTick = 0;
        clearPonderRuntimeParticles();
        clearPonderRuntimeWeather();
        this.triggeredPonderSoundKeyframes.clear();
        updatePonderState();
    }

    public void clearPonderDataForPreviewRebuild() {
        clearPonderBlockChanges();
        this.ponderSceneData = null;
        this.ponderTimedEntityAnimations.clear();
        clearPonderEntityAnimationBaselines();
        this.ponderKeyframeAnnotationSets = new ArrayList<>();
        this.ponderKeyframeSoundSets = new ArrayList<>();
        this.ponderCurrentTick = 0;
        this.ponderPaused = false;
        this.ponderFinished = false;
        this.ponderExportLayerOverrideEnabled = false;
        this.draggingPonderBar = false;
        this.ponderLastKeyframeIdx = -2;
        this.ponderAnnotationFadeTick = 5;
        this.ponderActiveAnnotations.clear();
        this.ponderOutgoingAnnotations.clear();
        this.ponderOutgoingFadeTick = 0;
        clearPonderRuntimeParticles();
        clearPonderRuntimeWeather();
        this.triggeredPonderSoundKeyframes.clear();
    }

    public void restorePonderPreviewState(int tick, boolean paused, boolean finished) {
        if (ponderSceneData == null) {
            return;
        }
        ponderCurrentTick = Math.max(0, Math.min(ponderSceneData.getTotalTime(), tick));
        ponderPaused = paused;
        ponderFinished = finished || ponderCurrentTick >= ponderSceneData.getTotalTime();
        ponderExportLayerOverrideEnabled = false;
        draggingPonderBar = false;
        ponderAnnotationFadeTick = ponderPaused ? 5 : 0;
        ponderOutgoingAnnotations.clear();
        ponderOutgoingFadeTick = 0;
        clearPonderRuntimeParticles();
        clearPonderRuntimeWeather();
        ponderLastKeyframeIdx = -2;
        triggeredPonderSoundKeyframes.clear();
        rebuildPonderRuntimeParticlesAtCurrentTick();
        updatePonderState();
    }

    private void rebuildPonderTimedEntityAnimations() {
        ponderTimedEntityAnimations.clear();
        if (ponderSceneData == null) {
            return;
        }

        for (PonderKeyframe keyframe : ponderSceneData.getKeyframes()) {
            if (keyframe == null || keyframe.getAnimateEntities()
                .isEmpty()) {
                continue;
            }

            int startTick = Math.max(0, keyframe.getTime());
            for (PonderKeyframeEntityAnimation animation : keyframe.getAnimateEntities()) {
                if (animation == null) {
                    continue;
                }
                String ref = trimToNull(animation.getRef());
                String preset = PonderEntityAnimationRuntimeSupport.normalizePreset(animation.getPreset());
                if (ref == null || preset == null) {
                    continue;
                }
                ponderTimedEntityAnimations.add(
                    new PonderEntityAnimationRuntimeSupport.TimedAnimation(
                        startTick,
                        ref,
                        preset,
                        Math.max(1, PonderEntityAnimationRuntimeSupport.resolveDurationTicks(animation)),
                        PonderEntityAnimationRuntimeSupport.isPersistentPreset(preset),
                        animation));
            }
        }

        ponderTimedEntityAnimations
            .sort(Comparator.comparingInt(PonderEntityAnimationRuntimeSupport.TimedAnimation::startTick));
    }

    private void clearPonderEntityAnimationBaselines() {
        ponderEntityAnimationBaselines.clear();
    }

    public void initializePonderTimelineBaseline() {
        if (ponderSceneData == null) {
            return;
        }
        restoreFromPonderSnapshot();
        clearPonderEntities();
        ponderBlockSnapshot.clear();
        ponderEntityRefs.clear();
        ponderEntityRuntimesBySceneEntityId.clear();
        ponderSceneEntityRefs.clear();
        clearPonderEntityAnimationBaselines();
        snapshotPonderBlocks();
        ponderTimelineBaselineReady = true;
        ponderLastKeyframeIdx = -2;
        updatePonderState();
    }

    public void ponderTick() {
        sceneAnimationTick++;
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
        for (int i = ponderSceneParticles.size() - 1; i >= 0; i--) {
            GuidebookSceneParticle particle = ponderSceneParticles.get(i);
            particle.tick();
            if (particle.isDead()) {
                recyclePonderParticle(ponderSceneParticles.remove(i));
            }
        }
        updatePonderState();
    }

    public void ponderTogglePlay() {
        if (ponderSceneData == null) return;
        if (ponderFinished) {
            ponderCurrentTick = 0;
            ponderFinished = false;
            ponderPaused = false;
            triggeredPonderSoundKeyframes.clear();
            updatePonderState();
        } else {
            boolean wasPaused = ponderPaused;
            ponderPaused = !ponderPaused;
            if (wasPaused && !ponderPaused) {
                playPonderKeyframeSounds(ponderSceneData.resolveActiveKeyframeIndex(ponderCurrentTick));
            }
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
        clearPonderRuntimeParticles();
        clearPonderRuntimeWeather();
        triggeredPonderSoundKeyframes.clear();
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
        ponderExportLayerOverrideEnabled = false;
        draggingPonderBar = false;
        ponderAnnotationFadeTick = 5;
        triggeredPonderSoundKeyframes.clear();
        rebuildPonderRuntimeParticlesAtCurrentTick();
        updatePonderState();
    }

    public void seekToTickForExport(int tick) {
        seekToTick(tick);
        ponderExportLayerOverrideEnabled = true;
    }

    public void restorePonderTickAfterExport(int tick) {
        seekToTick(tick);
        ponderExportLayerOverrideEnabled = false;
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
        triggeredPonderSoundKeyframes.clear();
        rebuildPonderRuntimeParticlesAtCurrentTick();
        updatePonderState();
    }

    public boolean containsPonderBar(int mouseX, int mouseY) {
        if (!hasBottomControls() || ponderSceneData == null
            || cachedPonderBarHitRect == null
            || cachedPonderBarHitRect.isEmpty()) {
            return false;
        }
        return cachedPonderBarHitRect.contains(mouseX, mouseY);
    }

    private void updatePonderState() {
        if (ponderSceneData == null) return;
        int activeIdx = ponderSceneData.resolveActiveKeyframeIndex(ponderCurrentTick);

        if (activeIdx != ponderLastKeyframeIdx) {
            int previousKeyframeIdx = ponderLastKeyframeIdx;
            boolean wasAtValidKeyframe = previousKeyframeIdx >= 0;
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
            if (hasPonderReplayActions()) {
                applyPonderActionsForKeyframeChange(previousKeyframeIdx, activeIdx, wasAtValidKeyframe);
                clearPonderEntityAnimationBaselines();
            }
            if (!ponderPaused && wasAtValidKeyframe) {
                playPonderKeyframeSounds(activeIdx);
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
        applyPonderEntityAnimationsAtTick(ponderCurrentTick);
    }

    private void applyPonderActionsForKeyframeChange(int previousKeyframeIdx, int activeKeyframeIdx,
        boolean wasAtValidKeyframe) {
        boolean forwardPlayback = !ponderPaused && wasAtValidKeyframe && activeKeyframeIdx > previousKeyframeIdx;
        if (forwardPlayback) {
            applyPonderKeyframeRange(previousKeyframeIdx + 1, activeKeyframeIdx, true);
            return;
        }

        // Trigger particle effects only during forward playback, not on seek or initial load.
        boolean triggerParticles = !ponderPaused && wasAtValidKeyframe;
        applyPonderTimelineActions(activeKeyframeIdx, triggerParticles);
    }

    private void applyPonderKeyframeRange(int startKeyframeIdx, int endKeyframeIdx, boolean triggerParticles) {
        if (ponderSceneData == null || endKeyframeIdx < startKeyframeIdx) {
            return;
        }

        boolean blocksChanged = false;
        int lastKeyframeIndex = Math.min(endKeyframeIdx, ponderSceneData.getKeyframeCount() - 1);
        for (int keyframeIndex = Math.max(0, startKeyframeIdx); keyframeIndex <= lastKeyframeIndex; keyframeIndex++) {
            PonderKeyframe keyframe = ponderSceneData.getKeyframe(keyframeIndex);
            if (keyframe == null) {
                continue;
            }
            blocksChanged |= applyPonderKeyframe(keyframe, triggerParticles && keyframeIndex == lastKeyframeIndex);
        }
        if (blocksChanged) {
            markBlockStatsDirty();
        }
    }

    private boolean applyPonderKeyframe(PonderKeyframe keyframe, boolean triggerParticles) {
        boolean blocksChanged = false;
        for (PonderKeyframeBlockChange blockChange : keyframe.getBlockChanges()) {
            Block oldBlock = level.getBlock(blockChange.getX(), blockChange.getY(), blockChange.getZ());
            int oldMeta = level.getBlockMetadata(blockChange.getX(), blockChange.getY(), blockChange.getZ());
            NBTTagCompound targetTag = parsePonderBlockNbt(blockChange);
            boolean hasTargetTileState = targetTag != null;
            if (targetTag != null && Mods.AE2.isModLoaded()) {
                targetTag = Ae2PonderSupport.normalizePonderTileTag(targetTag);
            }
            if (targetTag == null && Mods.AE2.isModLoaded()) {
                targetTag = Ae2PonderSupport.createCableBusTileTag(blockChange.getBlock(), blockChange.getMeta());
                hasTargetTileState = targetTag != null;
            }
            Block newBlock = resolveBlock(blockChange.getBlock(), targetTag, blockChange.getMeta());
            int newMeta = resolvePonderBlockMeta(newBlock, blockChange.getMeta());
            long posKey = GuidebookLevel.packPos(blockChange.getX(), blockChange.getY(), blockChange.getZ());
            Map<String, byte[]> targetPreviewSupplements = readPreviewSupplements(targetTag);
            Map<String, byte[]> previousPreviewSupplements = level.previewAuthorityStore()
                .snapshotAt(posKey);
            placePonderBlockChange(blockChange, newBlock, targetTag);
            targetPreviewSupplements = resolvePonderPreviewSupplements(
                targetPreviewSupplements,
                previousPreviewSupplements,
                hasTargetTileState,
                oldBlock,
                oldMeta,
                blockChange.getX(),
                blockChange.getY(),
                blockChange.getZ(),
                newBlock,
                newMeta);
            blocksChanged = true;
            level.previewAuthorityStore()
                .restoreAt(posKey, targetPreviewSupplements);
            if (triggerParticles && blockChange.shouldSpawnParticles()) {
                boolean removing = newBlock == null || newBlock == Blocks.air;
                Block particleBlock = removing ? oldBlock : newBlock;
                int particleMeta = removing ? oldMeta : newMeta;
                if (particleBlock != null && particleBlock != Blocks.air) {
                    spawnBlockParticles(
                        blockChange.getX(),
                        blockChange.getY(),
                        blockChange.getZ(),
                        particleBlock,
                        particleMeta);
                }
            }
        }
        applyPonderTileNbtChanges(keyframe);
        applyPonderEntityActions(keyframe);
        if (triggerParticles) {
            spawnPonderKeyframeParticles(keyframe);
        }
        return blocksChanged;
    }

    private void playPonderKeyframeSounds(int keyframeIndex) {
        if (keyframeIndex < 0 || ponderSceneData == null || triggeredPonderSoundKeyframes.contains(keyframeIndex)) {
            return;
        }
        if (keyframeIndex >= ponderKeyframeSoundSets.size()) {
            return;
        }
        List<GuideSoundSpec> sounds = ponderKeyframeSoundSets.get(keyframeIndex);
        if (sounds == null || sounds.isEmpty()) {
            return;
        }
        triggeredPonderSoundKeyframes.add(keyframeIndex);
        int centerX = cachedScreenRect != null ? cachedScreenRect.x() + cachedScreenRect.width() / 2
            : lastAbsX + lastW / 2;
        int centerY = cachedScreenRect != null ? cachedScreenRect.y() + cachedScreenRect.height() / 2
            : lastAbsY + lastH / 2;
        for (GuideSoundSpec sound : sounds) {
            playSceneSound(sound, centerX, centerY);
        }
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
    private static Block resolveBlock(@Nullable String name, @Nullable NBTTagCompound tileTag, int meta) {
        if (name == null || name.isEmpty()) return null;
        Block block = (Block) Block.blockRegistry.getObject(name);
        if (block != null && block != Blocks.air) {
            return block;
        }
        Item item = (Item) Item.itemRegistry.getObject(name);
        if (item instanceof ItemBlock itemBlock) {
            block = itemBlock.field_150939_a;
            if (block != null && block != Blocks.air) {
                return block;
            }
        }
        if (Mods.AE2.isModLoaded()) {
            block = Ae2PonderSupport.resolvePonderBlock(name, tileTag, meta);
            if (block != null && block != Blocks.air) {
                return block;
            }
        }
        return null;
    }

    private void snapshotPonderBlocks() {
        ponderBlockSnapshot.clear();
        ponderEntityRefs.clear();
        ponderEntityRuntimesBySceneEntityId.clear();
        ponderSceneEntityRefs.clear();
        if (ponderSceneData == null) return;
        for (PonderKeyframe kf : ponderSceneData.getKeyframes()) {
            for (PonderKeyframeBlockChange bc : kf.getBlockChanges()) {
                snapshotPonderBlockPosition(bc.getX(), bc.getY(), bc.getZ());
            }
            for (PonderKeyframeTileNbtOperation op : kf.getMergeTileNBT()) {
                snapshotPonderBlockPosition(op.getX(), op.getY(), op.getZ());
            }
            for (PonderKeyframeTileNbtOperation op : kf.getModifyTileNBT()) {
                snapshotPonderBlockPosition(op.getX(), op.getY(), op.getZ());
            }
            for (PonderKeyframeTileNbtOperation op : kf.getRemoveTileNBT()) {
                snapshotPonderBlockPosition(op.getX(), op.getY(), op.getZ());
            }
        }
    }

    private void snapshotPonderBlockPosition(int x, int y, int z) {
        long key = GuidebookLevel.packPos(x, y, z);
        if (ponderBlockSnapshot.containsKey(key)) {
            return;
        }
        Block existing = level.getBlock(x, y, z);
        int meta = level.getBlockMetadata(x, y, z);
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        NBTTagCompound tileNbt = null;
        if (tileEntity != null) {
            tileNbt = new NBTTagCompound();
            try {
                tileEntity.writeToNBT(tileNbt);
            } catch (Exception ignored) {
                tileNbt = null;
            }
        }
        ponderBlockSnapshot.put(
            key,
            new PonderBlockInfo(
                x,
                y,
                z,
                existing == Blocks.air ? null : existing,
                meta,
                tileNbt,
                level.previewAuthorityStore()
                    .snapshotAt(key)));
    }

    private boolean restoreFromPonderSnapshot() {
        boolean changed = false;
        for (PonderBlockInfo info : ponderBlockSnapshot.values()) {
            long key = GuidebookLevel.packPos(info.x, info.y, info.z);
            restorePonderBlockInfo(info);
            level.previewAuthorityStore()
                .restoreAt(key, info.initialPreviewSupplements);
            changed = true;
        }
        return changed;
    }

    private void clearPonderBlockChanges() {
        if (restoreFromPonderSnapshot()) {
            markBlockStatsDirty();
        }
        clearPonderEntities();
        ponderBlockSnapshot.clear();
        ponderEntityRefs.clear();
        ponderEntityRuntimesBySceneEntityId.clear();
        ponderSceneEntityRefs.clear();
        ponderTimelineBaselineReady = false;
    }

    private boolean hasPonderReplayActions() {
        if (!ponderTimelineBaselineReady) {
            return false;
        }
        if (!ponderBlockSnapshot.isEmpty() || !ponderEntityRefs.isEmpty()) {
            return true;
        }
        if (!ponderTimedEntityAnimations.isEmpty()) {
            return true;
        }
        if (ponderSceneData == null) {
            return false;
        }
        for (PonderKeyframe kf : ponderSceneData.getKeyframes()) {
            if (!kf.getCreateEntities()
                .isEmpty()
                || !kf.getSetEntityNBT()
                    .isEmpty()
                || !kf.getMergeEntityNBT()
                    .isEmpty()
                || !kf.getModifyEntityNBT()
                    .isEmpty()
                || !kf.getRemoveEntityNBT()
                    .isEmpty()
                || !kf.getRemoveEntities()
                    .isEmpty()
                || !kf.getParticles()
                    .isEmpty()
                || !kf.getAnimateEntities()
                    .isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void applyPonderTimelineActions(int upToKeyframeIdx, boolean triggerParticles) {
        boolean blocksChanged = restoreFromPonderSnapshot();
        clearPonderEntities();
        if (upToKeyframeIdx < 0 || ponderSceneData == null) {
            if (blocksChanged) {
                markBlockStatsDirty();
            }
            return;
        }
        for (int i = 0; i <= upToKeyframeIdx && i < ponderSceneData.getKeyframeCount(); i++) {
            PonderKeyframe kf = ponderSceneData.getKeyframe(i);
            if (kf == null) {
                continue;
            }
            blocksChanged |= applyPonderKeyframe(kf, triggerParticles && i == upToKeyframeIdx);
        }
        if (blocksChanged) {
            markBlockStatsDirty();
        }
    }

    private void spawnPonderKeyframeParticles(PonderKeyframe keyframe) {
        spawnPonderKeyframeParticles(keyframe, 0);
    }

    private void spawnPonderKeyframeParticles(PonderKeyframe keyframe, int elapsedTicks) {
        ponderSceneParticles.ensureCapacity(ponderSceneParticles.size() + estimatePonderParticleBurstSize(keyframe));
        int[] weatherBounds = null;
        for (PonderKeyframeParticle particle : keyframe.getParticles()) {
            if (particle == null) {
                continue;
            }
            if (elapsedTicks >= estimatePonderReplayLifetime(particle)) {
                continue;
            }
            int startIndex = ponderSceneParticles.size();
            if (particle.isExplosionPreset()) {
                int count = particle
                    .getCount(GuidebookSceneParticleFactory.defaultExplosionParticleCount(particle.getPower(2f)));
                GuidebookSceneParticleFactory.appendExplosionPreset(
                    ponderSceneParticles,
                    ponderParticleRng,
                    particle.getX(),
                    particle.getY(),
                    particle.getZ(),
                    particle.getLifetimeTicks(8),
                    particle.getPower(2f),
                    count,
                    this::acquirePonderParticle);
                advancePonderParticles(startIndex, elapsedTicks);
                continue;
            }
            if (particle.isWeatherPreset()) {
                if (weatherBounds == null) {
                    weatherBounds = copyLevelBounds();
                }
                List<GuidebookSceneWeatherArea> weatherAreas = resolveAvailableWeatherAreas(
                    particle,
                    weatherBounds,
                    keyframe.getTime());
                if (weatherAreas.isEmpty()) {
                    continue;
                }
                ponderWeatherEffects.add(
                    new GuidebookSceneWeatherEffect(
                        GuidebookSceneWeatherType.fromSerializedName(particle.getWeatherType()),
                        weatherAreas,
                        keyframe.getTime(),
                        particle.getWeatherDurationTicks(80),
                        particle.getWeatherDensityPerTick(
                            GuidebookSceneWeatherSupport.defaultDensity(
                                GuidebookSceneWeatherType.fromSerializedName(particle.getWeatherType()))),
                        true));
                continue;
            }
            int count = particle.getCount(1);
            int lifetime = particle.getLifetimeTicks(12);
            float size = particle.getSize(0.14f);
            String particleName = particle.getParticleName();
            float x = particle.getX();
            float y = particle.getY();
            float z = particle.getZ();
            float vx = particle.getVelocityX();
            float vy = particle.getVelocityY();
            float vz = particle.getVelocityZ();
            for (int i = 0; i < count; i++) {
                ponderSceneParticles.add(
                    GuidebookSceneParticleFactory.createRuntimeParticle(
                        acquirePonderParticle(),
                        particleName,
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz,
                        lifetime,
                        size,
                        ponderParticleRng));
            }
            advancePonderParticles(startIndex, elapsedTicks);
        }
    }

    private void advancePonderParticles(int startIndex, int elapsedTicks) {
        if (elapsedTicks <= 0 || startIndex >= ponderSceneParticles.size()) {
            return;
        }
        for (int i = startIndex; i < ponderSceneParticles.size(); i++) {
            ponderSceneParticles.get(i)
                .advanceBy(elapsedTicks);
        }
        for (int i = ponderSceneParticles.size() - 1; i >= startIndex; i--) {
            GuidebookSceneParticle particle = ponderSceneParticles.get(i);
            if (!particle.isDead()) {
                continue;
            }
            recyclePonderParticle(ponderSceneParticles.remove(i));
        }
    }

    private void applyPonderTileNbtChanges(PonderKeyframe kf) {
        for (PonderKeyframeTileNbtOperation op : kf.getMergeTileNBT()) {
            applyPonderTileNbtMerge(op);
        }
        for (PonderKeyframeTileNbtOperation op : kf.getModifyTileNBT()) {
            applyPonderTileNbtSet(op);
        }
        for (PonderKeyframeTileNbtOperation op : kf.getRemoveTileNBT()) {
            applyPonderTileNbtRemove(op);
        }
    }

    private void applyPonderTileNbtMerge(PonderKeyframeTileNbtOperation op) {
        String nbt = op.getNbt();
        if (nbt == null || nbt.trim()
            .isEmpty()) {
            return;
        }
        NBTTagCompound current = readPonderTileNbt(op.getX(), op.getY(), op.getZ());
        if (current == null) {
            return;
        }
        try {
            PonderNbtPath.mergeCompound(current, PonderNbtPath.parseCompound(nbt));
            writePonderTileNbt(op.getX(), op.getY(), op.getZ(), current);
        } catch (Exception ignored) {}
    }

    private void applyPonderTileNbtSet(PonderKeyframeTileNbtOperation op) {
        String path = op.getPath();
        String value = op.getValue();
        if (path == null || value == null) {
            return;
        }
        NBTTagCompound current = readPonderTileNbt(op.getX(), op.getY(), op.getZ());
        if (current == null) {
            return;
        }
        try {
            if (PonderNbtPath.set(current, path, PonderNbtPath.parseValue(value))) {
                writePonderTileNbt(op.getX(), op.getY(), op.getZ(), current);
            }
        } catch (Exception ignored) {}
    }

    private void applyPonderTileNbtRemove(PonderKeyframeTileNbtOperation op) {
        String path = op.getPath();
        if (path == null) {
            return;
        }
        NBTTagCompound current = readPonderTileNbt(op.getX(), op.getY(), op.getZ());
        if (current == null) {
            return;
        }
        if (PonderNbtPath.remove(current, path)) {
            writePonderTileNbt(op.getX(), op.getY(), op.getZ(), current);
        }
    }

    @Nullable
    private NBTTagCompound readPonderTileNbt(int x, int y, int z) {
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (tileEntity == null) {
            Block block = level.getBlock(x, y, z);
            int meta = level.getBlockMetadata(x, y, z);
            if (block == null || block == Blocks.air || !block.hasTileEntity(meta)) {
                return null;
            }
            World fakeWorld = level.getOrCreateFakeWorld();
            tileEntity = GuidebookTileEntityLoader.load(fakeWorld, block, meta, x, y, z, null);
            if (tileEntity == null) {
                return null;
            }
            level.setTileEntity(x, y, z, tileEntity);
        }
        NBTTagCompound current = new NBTTagCompound();
        try {
            tileEntity.writeToNBT(current);
            return current;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writePonderTileNbt(int x, int y, int z, NBTTagCompound tag) {
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (tileEntity == null) {
            return;
        }
        GuidebookTileEntityLoader.applyTag(tileEntity, tag, x, y, z);
        level.setTileEntity(x, y, z, tileEntity);
        markBlockStatsDirty();
    }

    private void applyPonderEntityActions(PonderKeyframe kf) {
        for (PonderKeyframeEntityAction action : kf.getCreateEntities()) {
            createPonderEntity(action);
        }
        for (PonderKeyframeEntityAction action : kf.getRemoveEntities()) {
            removePonderEntity(action);
        }
        for (PonderKeyframeEntityAction action : kf.getSetEntityNBT()) {
            setPonderEntityNbt(action);
        }
        for (PonderKeyframeEntityAction action : kf.getMergeEntityNBT()) {
            mergePonderEntityNbt(action);
        }
        for (PonderKeyframeEntityAction action : kf.getModifyEntityNBT()) {
            modifyPonderEntityNbt(action);
        }
        for (PonderKeyframeEntityAction action : kf.getRemoveEntityNBT()) {
            removePonderEntityNbt(action);
        }
    }

    private void createPonderEntity(PonderKeyframeEntityAction action) {
        String ref = trimToNull(action.getRef());
        String entityId = trimToNull(action.getId());
        String sceneEntityId = resolvePonderSceneEntityId(action);
        if (ref == null || entityId == null) {
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        String nbt = action.getNbt();
        if (nbt != null && !nbt.trim()
            .isEmpty()) {
            try {
                tag = PonderNbtPath.parseCompound(nbt);
            } catch (Exception ignored) {
                return;
            }
        }
        applyPonderEntityTransform(tag, action);
        World fakeWorld = level.getOrCreateFakeWorld();
        Entity entity = GuidebookSceneEntityLoader
            .loadFromNbt(fakeWorld, entityId, tag, action.getName(), action.getUuid());
        if (entity == null) {
            return;
        }
        PonderEntityRuntime existing = ponderEntityRefs.remove(ref);
        if (existing != null) {
            unregisterPonderEntityRef(ref, existing);
            ponderEntityAnimationBaselines.remove(ref);
            removePonderRuntimeBySceneEntityId(existing.sceneEntityId);
        }
        level.addEntity(entity, sceneEntityId);
        applyPonderEntityState(entity, entityId, action, true);
        registerPonderEntityRuntime(
            ref,
            new PonderEntityRuntime(sceneEntityId, entityId, action.getName(), action.getUuid()));
    }

    private void removePonderEntity(PonderKeyframeEntityAction action) {
        String ref = trimToNull(action.getRef());
        if (ref == null) {
            return;
        }
        PonderEntityRuntime runtime = ponderEntityRefs.remove(ref);
        if (runtime == null) {
            return;
        }
        unregisterPonderEntityRef(ref, runtime);
        ponderEntityAnimationBaselines.remove(ref);
        removePonderRuntimeBySceneEntityId(runtime.sceneEntityId);
    }

    private void setPonderEntityNbt(PonderKeyframeEntityAction action) {
        Entity entity = resolvePonderEntity(action);
        String nbt = action.getNbt();
        if (entity == null) {
            return;
        }
        if (nbt != null && !nbt.trim()
            .isEmpty()) {
            try {
                NBTTagCompound tag = PonderNbtPath.parseCompound(nbt);
                entity = applyPonderEntityNbt(entity, tag);
            } catch (Exception ignored) {}
        }
        applyPonderEntityState(entity, resolvePonderEntityTypeId(action, entity), action, false);
    }

    private void mergePonderEntityNbt(PonderKeyframeEntityAction action) {
        Entity entity = resolvePonderEntity(action);
        String nbt = action.getNbt();
        if (entity == null) {
            return;
        }
        if (nbt != null && !nbt.trim()
            .isEmpty()) {
            try {
                NBTTagCompound current = readPonderEntityNbt(entity);
                PonderNbtPath.mergeCompound(current, PonderNbtPath.parseCompound(nbt));
                entity = applyPonderEntityNbt(entity, current);
            } catch (Exception ignored) {}
        }
        applyPonderEntityState(entity, resolvePonderEntityTypeId(action, entity), action, false);
    }

    private void modifyPonderEntityNbt(PonderKeyframeEntityAction action) {
        Entity entity = resolvePonderEntity(action);
        String path = action.getPath();
        String value = action.getValue();
        if (entity == null) {
            return;
        }
        if (path != null && value != null) {
            try {
                NBTTagCompound current = readPonderEntityNbt(entity);
                if (PonderNbtPath.set(current, path, PonderNbtPath.parseValue(value))) {
                    entity = applyPonderEntityNbt(entity, current);
                }
            } catch (Exception ignored) {}
        }
        applyPonderEntityState(entity, resolvePonderEntityTypeId(action, entity), action, false);
    }

    private void removePonderEntityNbt(PonderKeyframeEntityAction action) {
        Entity entity = resolvePonderEntity(action);
        String path = action.getPath();
        if (entity == null) {
            return;
        }
        if (path != null) {
            NBTTagCompound current = readPonderEntityNbt(entity);
            if (PonderNbtPath.remove(current, path)) {
                entity = applyPonderEntityNbt(entity, current);
            }
        }
        applyPonderEntityState(entity, resolvePonderEntityTypeId(action, entity), action, false);
    }

    @Nullable
    private Entity resolvePonderEntity(PonderKeyframeEntityAction action) {
        String ref = trimToNull(action.getRef());
        if (ref == null) {
            return null;
        }
        return resolvePonderAnimatedEntity(ref);
    }

    private NBTTagCompound readPonderEntityNbt(Entity entity) {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBT(tag);
        return tag;
    }

    private Entity applyPonderEntityNbt(Entity entity, NBTTagCompound tag) {
        PonderEntityRuntime runtime = findPonderEntityRuntime(entity);
        try {
            entity.readFromNBT(tag);
            level.addEntity(entity, runtime != null ? runtime.sceneEntityId : null);
            return entity;
        } catch (Throwable ignored) {}

        String entityId = runtime != null ? runtime.entityTypeId : null;
        if (entityId == null) {
            return entity;
        }

        World fakeWorld = level.getOrCreateFakeWorld();
        Entity replacement = GuidebookSceneEntityLoader
            .loadFromNbt(fakeWorld, entityId, tag, runtime.playerName, runtime.playerUuid);
        if (replacement != null) {
            level.replaceEntityPreservingSceneId(entity.getEntityId(), replacement);
            return replacement;
        }
        return entity;
    }

    @Nullable
    private PonderEntityRuntime findPonderEntityRuntime(Entity entity) {
        String sceneEntityId = level.getSceneEntityId(entity.getEntityId());
        return sceneEntityId != null ? ponderEntityRuntimesBySceneEntityId.get(sceneEntityId) : null;
    }

    private void applyPonderEntityTransform(NBTTagCompound tag, PonderKeyframeEntityAction action) {
        boolean hasPosition = action.getX() != null || action.getY() != null || action.getZ() != null;
        if (hasPosition || !tag.hasKey("Pos")) {
            double x = action.getX() != null ? action.getX() : 0.0D;
            double y = action.getY() != null ? action.getY() : 0.0D;
            double z = action.getZ() != null ? action.getZ() : 0.0D;
            tag.setTag("Pos", createDoubleList(x, y, z));
        }
        if (!tag.hasKey("Motion")) {
            tag.setTag("Motion", createDoubleList(0.0D, 0.0D, 0.0D));
        }
        boolean hasRotation = action.getYaw() != null || action.getPitch() != null;
        if (hasRotation || !tag.hasKey("Rotation")) {
            float yaw = action.getYaw() != null ? action.getYaw() : 0.0F;
            float pitch = action.getPitch() != null ? action.getPitch() : 0.0F;
            tag.setTag("Rotation", createFloatList(yaw, pitch));
        }
    }

    @Nullable
    private String resolvePonderEntityTypeId(PonderKeyframeEntityAction action, Entity entity) {
        String explicitEntityId = trimToNull(action.getId());
        if (explicitEntityId != null) {
            return explicitEntityId;
        }
        PonderEntityRuntime runtime = findPonderEntityRuntime(entity);
        return runtime != null ? runtime.entityTypeId : null;
    }

    private void applyPonderEntityState(Entity entity, @Nullable String entityId, PonderKeyframeEntityAction action,
        boolean usePreviewDefaults) {
        if (entity == null) {
            return;
        }

        boolean spatialChanged = GuidebookSceneEntityStateSupport
            .applyOptionalPosition(entity, action.getX(), action.getY(), action.getZ());
        GuidebookSceneEntityStateSupport.applyOptionalRotation(
            entity,
            action.getYaw(),
            action.getPitch(),
            action.getBodyYaw(),
            action.getHeadYaw());
        spatialChanged |= GuidebookSceneEntityStateSupport.applyVisualState(
            entity,
            entityId,
            action.getShowName(),
            action.getShowCape(),
            action.getBaby(),
            resolvePonderPreviewPlayerPose(action),
            usePreviewDefaults);
        if (spatialChanged) {
            level.markSpatialDirty();
        }
        String sceneEntityId = resolvePonderSceneEntityId(action);
        if (Boolean.TRUE.equals(action.getUnmount())) {
            level.clearSceneEntityMount(sceneEntityId);
        } else {
            String mountTargetSceneEntityId = trimToNull(action.getMount());
            if (mountTargetSceneEntityId != null) {
                level.setSceneEntityMount(sceneEntityId, mountTargetSceneEntityId);
            }
        }
    }

    @Nullable
    private GuidebookPreviewPlayerPose resolvePonderPreviewPlayerPose(PonderKeyframeEntityAction action) {
        return GuidebookSceneEntityStateSupport.createPreviewPlayerPose(
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getHeadRotation()),
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getLeftArmRotation()),
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getRightArmRotation()),
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getLeftLegRotation()),
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getRightLegRotation()),
            GuidebookSceneEntityStateSupport.parseOptionalVector3(action.getCapeRotation()));
    }

    private NBTTagList createDoubleList(double... values) {
        NBTTagList list = new NBTTagList();
        for (double value : values) {
            list.appendTag(new NBTTagDouble(value));
        }
        return list;
    }

    private NBTTagList createFloatList(float... values) {
        NBTTagList list = new NBTTagList();
        for (float value : values) {
            list.appendTag(new NBTTagFloat(value));
        }
        return list;
    }

    private void clearPonderEntities() {
        Set<String> removedSceneEntityIds = new HashSet<>();
        for (PonderEntityRuntime runtime : ponderEntityRefs.values()) {
            if (removedSceneEntityIds.add(runtime.sceneEntityId)) {
                level.removeEntitiesBySceneEntityId(runtime.sceneEntityId);
            }
        }
        ponderEntityRefs.clear();
        ponderEntityRuntimesBySceneEntityId.clear();
        ponderSceneEntityRefs.clear();
        clearPonderEntityAnimationBaselines();
    }

    private void removePonderRuntimeBySceneEntityId(String sceneEntityId) {
        if (sceneEntityId == null) {
            return;
        }

        level.removeEntitiesBySceneEntityId(sceneEntityId);

        LinkedHashSet<String> refs = ponderSceneEntityRefs.remove(sceneEntityId);
        if (refs == null || refs.isEmpty()) {
            ponderEntityRuntimesBySceneEntityId.remove(sceneEntityId);
            return;
        }
        ponderEntityRuntimesBySceneEntityId.remove(sceneEntityId);
        for (String removedRef : refs) {
            ponderEntityRefs.remove(removedRef);
            ponderEntityAnimationBaselines.remove(removedRef);
        }
    }

    private void registerPonderEntityRuntime(String ref, PonderEntityRuntime runtime) {
        ponderEntityRefs.put(ref, runtime);
        ponderEntityRuntimesBySceneEntityId.put(runtime.sceneEntityId, runtime);
        ponderSceneEntityRefs.computeIfAbsent(runtime.sceneEntityId, ignored -> new LinkedHashSet<>())
            .add(ref);
    }

    private void unregisterPonderEntityRef(String ref, PonderEntityRuntime runtime) {
        LinkedHashSet<String> refs = ponderSceneEntityRefs.get(runtime.sceneEntityId);
        if (refs == null) {
            return;
        }
        refs.remove(ref);
        if (!refs.isEmpty()) {
            return;
        }
        ponderSceneEntityRefs.remove(runtime.sceneEntityId);
        ponderEntityRuntimesBySceneEntityId.remove(runtime.sceneEntityId);
    }

    private GuidebookSceneParticle acquirePonderParticle() {
        GuidebookSceneParticle particle = ponderParticlePool.pollFirst();
        return particle != null ? particle : new GuidebookSceneParticle();
    }

    private void recyclePonderParticle(@Nullable GuidebookSceneParticle particle) {
        if (particle == null || ponderParticlePool.size() >= MAX_PONDER_PARTICLE_POOL_SIZE) {
            return;
        }
        ponderParticlePool.addFirst(particle);
    }

    private void clearPonderRuntimeParticles() {
        for (GuidebookSceneParticle particle : ponderSceneParticles) {
            recyclePonderParticle(particle);
        }
        ponderSceneParticles.clear();
    }

    private void clearPonderRuntimeWeather() {
        ponderWeatherEffects.clear();
        ponderWeatherColumnReservations.clear();
    }

    private void rebuildPonderRuntimeParticlesAtCurrentTick() {
        clearPonderRuntimeParticles();
        clearPonderRuntimeWeather();
        if (ponderSceneData == null) {
            return;
        }
        for (PonderKeyframe keyframe : ponderSceneData.getKeyframes()) {
            if (keyframe == null) {
                continue;
            }
            int elapsedTicks = ponderCurrentTick - keyframe.getTime();
            if (elapsedTicks < 0) {
                continue;
            }
            spawnPonderKeyframeParticles(keyframe, elapsedTicks);
        }
    }

    private void applyPonderEntityAnimationsAtTick(int tick) {
        if (ponderTimedEntityAnimations.isEmpty() || ponderEntityRefs.isEmpty()) {
            return;
        }

        Map<String, Entity> resolvedEntities = new HashMap<>();
        boolean spatialChanged = false;
        for (String ref : ponderEntityAnimationBaselines.keySet()) {
            Entity entity = resolvePonderAnimatedEntity(ref);
            if (entity == null) {
                continue;
            }
            resolvedEntities.put(ref, entity);
            PonderEntityAnimationRuntimeSupport.Baseline baseline = ponderEntityAnimationBaselines.get(ref);
            if (baseline != null) {
                spatialChanged |= PonderEntityAnimationRuntimeSupport.restoreBaseline(entity, baseline);
            }
        }

        for (PonderEntityAnimationRuntimeSupport.TimedAnimation timedAnimation : ponderTimedEntityAnimations) {
            if (timedAnimation.startTick() > tick) {
                break;
            }

            String ref = timedAnimation.ref();
            Entity entity = resolvedEntities.get(ref);
            if (entity == null) {
                entity = resolvePonderAnimatedEntity(ref);
                if (entity != null) {
                    resolvedEntities.put(ref, entity);
                }
            }
            if (entity == null) {
                continue;
            }

            PonderEntityAnimationRuntimeSupport.Baseline frameBaseline = ponderEntityAnimationBaselines.get(ref);
            if (frameBaseline == null) {
                frameBaseline = PonderEntityAnimationRuntimeSupport.captureBaseline(entity);
                ponderEntityAnimationBaselines.put(ref, frameBaseline);
            }

            int elapsedTicks = tick - timedAnimation.startTick();
            if (elapsedTicks < 0) {
                continue;
            }

            if (elapsedTicks >= timedAnimation.durationTicks() && !timedAnimation.persistent()) {
                continue;
            }

            spatialChanged |= PonderEntityAnimationRuntimeSupport.apply(
                entity,
                frameBaseline,
                timedAnimation.animation(),
                timedAnimation.preset(),
                timedAnimation.durationTicks(),
                elapsedTicks);
        }
        if (spatialChanged) {
            level.markSpatialDirty();
        }
    }

    @Nullable
    private Entity resolvePonderAnimatedEntity(String ref) {
        PonderEntityRuntime runtime = ponderEntityRefs.get(ref);
        return runtime == null ? null : level.getFirstEntityBySceneEntityId(runtime.sceneEntityId);
    }

    @Nullable
    private String resolvePonderSceneEntityId(PonderKeyframeEntityAction action) {
        String sceneEntityId = trimToNull(action.getSceneEntityId());
        if (sceneEntityId != null) {
            return sceneEntityId;
        }
        String ref = trimToNull(action.getRef());
        return ref != null ? "__ponder__" + ref : null;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        float[] color = resolveDiggingParticleColor(block, meta, bx, by, bz);
        int grid = 4;
        ponderSceneParticles.ensureCapacity(ponderSceneParticles.size() + grid * grid * grid);
        for (int ix = 0; ix < grid; ix++) {
            for (int iy = 0; iy < grid; iy++) {
                for (int iz = 0; iz < grid; iz++) {
                    float px = bx + (ix + 0.5f) / grid;
                    float py = by + (iy + 0.5f) / grid;
                    float pz = bz + (iz + 0.5f) / grid;

                    float[] velocity = vanillaDiggingParticleVelocity(
                        rng,
                        px - bx - 0.5f,
                        py - by - 0.5f,
                        pz - bz - 0.5f);

                    float textureJitterX = rng.nextFloat() * 3.0f;
                    float textureJitterY = rng.nextFloat() * 3.0f;
                    float u0 = icon.getInterpolatedU(textureJitterX / 4.0f * 16.0f);
                    float v0 = icon.getInterpolatedV(textureJitterY / 4.0f * 16.0f);
                    float u1 = icon.getInterpolatedU((textureJitterX + 1.0f) / 4.0f * 16.0f);
                    float v1 = icon.getInterpolatedV((textureJitterY + 1.0f) / 4.0f * 16.0f);

                    int maxAge = vanillaDiggingParticleMaxAge(rng);
                    float size = vanillaDiggingParticleHalfSize(rng);
                    ponderSceneParticles.add(
                        acquirePonderParticle().reset(
                            px,
                            py,
                            pz,
                            velocity[0],
                            velocity[1],
                            velocity[2],
                            u0,
                            v0,
                            u1,
                            v1,
                            color[0],
                            color[1],
                            color[2],
                            maxAge,
                            size));
                }
            }
        }
    }

    private float[] resolveDiggingParticleColor(Block block, int meta, int x, int y, int z) {
        float red = 0.6f;
        float green = 0.6f;
        float blue = 0.6f;
        try {
            World fakeWorld = level.getOrCreateFakeWorld();
            int color = block.colorMultiplier(fakeWorld, x, y, z);
            red *= (color >> 16 & 255) / 255.0f;
            green *= (color >> 8 & 255) / 255.0f;
            blue *= (color & 255) / 255.0f;
        } catch (Throwable ignored) {
            try {
                int color = block.getRenderColor(meta);
                red *= (color >> 16 & 255) / 255.0f;
                green *= (color >> 8 & 255) / 255.0f;
                blue *= (color & 255) / 255.0f;
            } catch (Throwable ignoredAgain) {}
        }
        diggingParticleColorScratch[0] = red;
        diggingParticleColorScratch[1] = green;
        diggingParticleColorScratch[2] = blue;
        return diggingParticleColorScratch;
    }

    private float[] vanillaDiggingParticleVelocity(Random rng, float baseX, float baseY, float baseZ) {
        float vx = baseX + (rng.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float vy = baseY + (rng.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float vz = baseZ + (rng.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float speed = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= 1.0e-6f) {
            diggingParticleVelocityScratch[0] = 0.0f;
            diggingParticleVelocityScratch[1] = 0.1f;
            diggingParticleVelocityScratch[2] = 0.0f;
            return diggingParticleVelocityScratch;
        }
        float scale = (rng.nextFloat() + rng.nextFloat() + 1.0f) * 0.15f * 0.4f;
        diggingParticleVelocityScratch[0] = vx / speed * scale;
        diggingParticleVelocityScratch[1] = vy / speed * scale + 0.1f;
        diggingParticleVelocityScratch[2] = vz / speed * scale;
        return diggingParticleVelocityScratch;
    }

    private int estimatePonderParticleBurstSize(PonderKeyframe keyframe) {
        int total = 0;
        for (PonderKeyframeParticle particle : keyframe.getParticles()) {
            if (particle == null) {
                continue;
            }
            if (particle.isExplosionPreset()) {
                int count = particle
                    .getCount(GuidebookSceneParticleFactory.defaultExplosionParticleCount(particle.getPower(2f)));
                total += 1 + count * 2;
                continue;
            }
            if (particle.isWeatherPreset()) {
                int density = particle.getWeatherDensityPerTick(
                    GuidebookSceneWeatherSupport
                        .defaultDensity(GuidebookSceneWeatherType.fromSerializedName(particle.getWeatherType())));
                total += Math.max(1, Math.min(128, density * 4));
                continue;
            }
            total += particle.getCount(1);
        }
        return total;
    }

    private int estimatePonderReplayLifetime(PonderKeyframeParticle particle) {
        if (particle.isExplosionPreset()) {
            return particle.getLifetimeTicks(8) + 20;
        }
        if (particle.isWeatherPreset()) {
            return particle.getWeatherDurationTicks(80);
        }
        return particle.getLifetimeTicks(12);
    }

    private int[] copyLevelBounds() {
        int[] bounds = level.getBounds();
        return new int[] { bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5] };
    }

    private List<GuidebookSceneWeatherArea> resolveAvailableWeatherAreas(PonderKeyframeParticle particle,
        int[] weatherBounds, int startTick) {
        List<GuidebookSceneWeatherArea> areas = GuidebookSceneWeatherSupport
            .resolveWeatherAreas(weatherBounds, particle.getWeatherXValues(), particle.getWeatherZValues());
        if (areas.isEmpty()) {
            return List.of();
        }
        int endTickExclusive = startTick + estimatePonderReplayLifetime(particle);
        List<GuidebookSceneWeatherArea> acceptedAreas = new ArrayList<>(areas.size());
        for (GuidebookSceneWeatherArea area : areas) {
            if (area == null) {
                continue;
            }
            List<GuidebookSceneWeatherArea> trimmedAreas = reserveWeatherAreaColumns(area, startTick, endTickExclusive);
            acceptedAreas.addAll(trimmedAreas);
        }
        return acceptedAreas;
    }

    private List<GuidebookSceneWeatherArea> reserveWeatherAreaColumns(GuidebookSceneWeatherArea area, int startTick,
        int endTickExclusive) {
        if (area.getMinX() > area.getMaxX() || area.getMinZ() > area.getMaxZ()) {
            return List.of();
        }
        List<GuidebookSceneWeatherArea> acceptedAreas = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int rowZ = Integer.MIN_VALUE;
        for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
            for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                if (!tryReserveWeatherColumn(x, z, startTick, endTickExclusive)) {
                    if (rowZ != Integer.MIN_VALUE && minX <= maxX) {
                        acceptedAreas.add(new GuidebookSceneWeatherArea(minX, rowZ, maxX, rowZ));
                    }
                    minX = Integer.MAX_VALUE;
                    maxX = Integer.MIN_VALUE;
                    rowZ = Integer.MIN_VALUE;
                    continue;
                }
                if (rowZ == Integer.MIN_VALUE) {
                    rowZ = z;
                    minX = x;
                }
                maxX = x;
            }
            if (rowZ != Integer.MIN_VALUE && minX <= maxX) {
                acceptedAreas.add(new GuidebookSceneWeatherArea(minX, rowZ, maxX, rowZ));
            }
            minX = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            rowZ = Integer.MIN_VALUE;
        }
        return acceptedAreas;
    }

    private boolean tryReserveWeatherColumn(int x, int z, int startTick, int endTickExclusive) {
        long key = GuidebookLevel.packPos(x, 0, z);
        PonderWeatherColumnReservation existing = ponderWeatherColumnReservations.get(key);
        if (existing != null && existing.overlaps(startTick, endTickExclusive)) {
            return false;
        }
        ponderWeatherColumnReservations.put(key, new PonderWeatherColumnReservation(startTick, endTickExclusive));
        return true;
    }

    private static int vanillaDiggingParticleMaxAge(Random rng) {
        return Math.max(1, (int) (4.0f / (rng.nextFloat() * 0.9f + 0.1f)));
    }

    private static float vanillaDiggingParticleHalfSize(Random rng) {
        float baseParticleScale = (rng.nextFloat() * 0.5f + 0.5f) * 2.0f;
        float diggingParticleScale = baseParticleScale / 2.0f;
        return 0.1f * diggingParticleScale;
    }

    @Nullable
    private NBTTagCompound parsePonderBlockNbt(PonderKeyframeBlockChange bc) {
        String nbtStr = bc.getNbt();
        if (nbtStr == null || nbtStr.isEmpty()) {
            return null;
        }
        try {
            return GuideTextNbtCodec.readTextSafeCompound(nbtStr);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void restorePonderBlockInfo(PonderBlockInfo info) {
        if (info.initialBlock == null || info.initialBlock == Blocks.air) {
            level.setBlock(info.x, info.y, info.z, Blocks.air, 0, null);
            return;
        }
        GuidebookPreviewBlockPlacer.place(
            level,
            info.x,
            info.y,
            info.z,
            info.initialBlock,
            info.initialMeta,
            info.initialTileNbt != null ? (NBTTagCompound) info.initialTileNbt.copy() : null,
            GuidebookLevel.resolveBlockId(info.initialBlock));
    }

    private void placePonderBlockChange(PonderKeyframeBlockChange bc, @Nullable Block block,
        @Nullable NBTTagCompound tileTag) {
        if (block == null || block == Blocks.air) {
            level.setBlock(bc.getX(), bc.getY(), bc.getZ(), Blocks.air, 0, null);
            return;
        }
        GuidebookPreviewBlockPlacer.place(
            level,
            bc.getX(),
            bc.getY(),
            bc.getZ(),
            block,
            resolvePonderBlockMeta(block, bc.getMeta()),
            tileTag != null ? (NBTTagCompound) tileTag.copy() : null,
            GuidebookLevel.resolveBlockId(block));
    }

    private static int resolvePonderBlockMeta(@Nullable Block block, int requestedMeta) {
        if (Mods.AE2.isModLoaded()) {
            return Ae2PonderSupport.resolvePonderBlockMeta(block, requestedMeta);
        }
        return requestedMeta;
    }

    private static Map<String, byte[]> readPreviewSupplements(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(ServerPreviewSupplementNbt.TAG_ROOT, 10)) {
            return Map.of();
        }
        NBTTagCompound root = tag.getCompoundTag(ServerPreviewSupplementNbt.TAG_ROOT);
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String supplementId : root.func_150296_c()) {
            byte[] payload = ServerPreviewSupplementNbt.readSupplement(tag, supplementId);
            if (payload != null && payload.length > 0) {
                result.put(supplementId, payload);
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    private Map<String, byte[]> completePonderPreviewSupplements(Map<String, byte[]> explicitSupplements, int x, int y,
        int z, @Nullable Block block, int meta) {
        if (explicitSupplements != null && !explicitSupplements.isEmpty()) {
            return explicitSupplements;
        }
        if (!Mods.AE2.isModLoaded()) {
            return Map.of();
        }
        return Ae2Helpers.capturePonderPreviewSupplements(level, x, y, z, block, meta);
    }

    private Map<String, byte[]> resolvePonderPreviewSupplements(Map<String, byte[]> explicitSupplements,
        Map<String, byte[]> previousSupplements, boolean hasTargetTileState, @Nullable Block oldBlock, int oldMeta,
        int x, int y, int z, @Nullable Block block, int meta) {
        if (explicitSupplements != null && !explicitSupplements.isEmpty()) {
            return explicitSupplements;
        }
        if (!hasTargetTileState
            && shouldReusePonderPreviewSupplements(previousSupplements, oldBlock, oldMeta, block, meta)) {
            return previousSupplements;
        }
        return completePonderPreviewSupplements(explicitSupplements, x, y, z, block, meta);
    }

    private static boolean shouldReusePonderPreviewSupplements(Map<String, byte[]> previousSupplements,
        @Nullable Block oldBlock, int oldMeta, @Nullable Block block, int meta) {
        if (previousSupplements == null || previousSupplements.isEmpty() || oldBlock == null || block == null) {
            return false;
        }
        if (oldBlock == block && oldMeta == meta) {
            return true;
        }
        return Mods.AE2.isModLoaded() && Ae2PonderSupport.isCableBusBlock(oldBlock)
            && Ae2PonderSupport.isCableBusBlock(block);
    }

    /** Compact holder for the initial block state at a ponder-changed position. */
    private static class PonderBlockInfo {

        final int x;
        final int y;
        final int z;
        @Nullable
        final Block initialBlock;
        final int initialMeta;
        @Nullable
        final NBTTagCompound initialTileNbt;
        final Map<String, byte[]> initialPreviewSupplements;

        PonderBlockInfo(int x, int y, int z, @Nullable Block initialBlock, int initialMeta,
            @Nullable NBTTagCompound initialTileNbt, Map<String, byte[]> initialPreviewSupplements) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.initialBlock = initialBlock;
            this.initialMeta = initialMeta;
            this.initialTileNbt = initialTileNbt != null ? (NBTTagCompound) initialTileNbt.copy() : null;
            this.initialPreviewSupplements = copyPreviewSupplements(initialPreviewSupplements);
        }
    }

    private static Map<String, byte[]> copyPreviewSupplements(Map<String, byte[]> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : source.entrySet()) {
            byte[] payload = entry.getValue();
            if (entry.getKey() != null && payload != null && payload.length > 0) {
                result.put(entry.getKey(), payload.clone());
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    private static class PonderEntityRuntime {

        private final String sceneEntityId;
        private final String entityTypeId;
        @Nullable
        private final String playerName;
        @Nullable
        private final String playerUuid;

        private PonderEntityRuntime(String sceneEntityId, String entityTypeId, @Nullable String playerName,
            @Nullable String playerUuid) {
            this.sceneEntityId = sceneEntityId;
            this.entityTypeId = entityTypeId;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
        }
    }

    public static class BlockStatsLayoutState {

        private final boolean visible;
        private final BlockStatsMode mode;
        private final BlockStatsCorner corner;
        private final BlockStatsDock dock;
        private final boolean showNames;
        private final int maxWidth;
        private final int maxHeight;
        private final int sceneWidth;
        private final int sceneHeight;
        private final int buttonColumnReserve;

        public BlockStatsLayoutState(boolean visible, BlockStatsMode mode, BlockStatsCorner corner, BlockStatsDock dock,
            boolean showNames, int maxWidth, int maxHeight, int sceneWidth, int sceneHeight, int buttonColumnReserve) {
            this.visible = visible;
            this.mode = mode != null ? mode : BlockStatsMode.AUTO;
            this.corner = corner != null ? corner : BlockStatsCorner.TOP_RIGHT;
            this.dock = dock != null ? dock : BlockStatsDock.INSIDE;
            this.showNames = showNames;
            this.maxWidth = Math.max(BLOCK_STATS_MIN_WIDTH, maxWidth);
            this.maxHeight = Math.max(BLOCK_STATS_MIN_HEIGHT, maxHeight);
            this.sceneWidth = Math.max(16, sceneWidth);
            this.sceneHeight = Math.max(16, sceneHeight);
            this.buttonColumnReserve = Math.max(0, buttonColumnReserve);
        }

        public boolean visible() {
            return visible;
        }

        public BlockStatsMode mode() {
            return mode;
        }

        public BlockStatsCorner corner() {
            return corner;
        }

        public BlockStatsDock dock() {
            return dock;
        }

        public boolean showNames() {
            return showNames;
        }

        public int maxWidth() {
            return maxWidth;
        }

        public int maxHeight() {
            return maxHeight;
        }

        public int sceneWidth() {
            return sceneWidth;
        }

        public int sceneHeight() {
            return sceneHeight;
        }

        public int buttonColumnReserve() {
            return buttonColumnReserve;
        }
    }

    private void drawPonderControls(RenderContext context, LytRect outerRect, int totalControlH) {
        int renderRowY = outerRect.bottom() - totalControlH;
        int absRowY = lastOuterAbsY + lastOuterH - totalControlH;

        int rowH = SCENE_SLIDER_AREA_HEIGHT;
        int btnSize = SCENE_SLIDER_AREA_HEIGHT;

        int renderBtnX = outerRect.x() + SCENE_SLIDER_SIDE_PADDING;
        int absBtnX = lastOuterAbsX + SCENE_SLIDER_SIDE_PADDING;

        cachedPonderBtnAbsX = absBtnX;
        cachedPonderBtnAbsY = absRowY;

        int mx = currentMousePositionAvailable ? currentMouseAbsX : -1;
        int my = currentMousePositionAvailable ? currentMouseAbsY : -1;

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        for (int i = 0; i < PONDER_BUTTON_ROLES.length; i++) {
            int absX = absBtnX + i * btnSize;
            boolean hover = mx >= absX && mx < absX + btnSize && my >= absRowY && my < absRowY + btnSize;
            boolean active = PONDER_BUTTON_ROLES[i] == GuideIconButton.Role.PONDER_PLAY_PAUSE && !ponderPaused
                && !ponderFinished;
            int color = GuideIconButton.resolveIconColor(true, hover, active);
            int drawX = renderBtnX + i * btnSize;
            GuideIconButton.drawIcon(mc, PONDER_BUTTON_ROLES[i], drawX, renderRowY, btnSize, btnSize, color);
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
        int bottomControlAreaHeight = getBottomControlAreaHeight();
        logBottomControlState("evaluate", outerRect, bottomControlAreaHeight);
        if (bottomControlAreaHeight <= 0) {
            logBottomControlState("skip-empty", outerRect, bottomControlAreaHeight);
            return;
        }
        currentMousePositionAvailable = updateCurrentMousePosition();
        if (ponderSceneData != null) {
            drawPonderControls(context, outerRect, bottomControlAreaHeight);
        }
        if (!isPonderPlaying()) {
            if (hasStructureLibTierSlider()) {
                drawStructureLibTierSlider(context, outerRect);
            }
            if (hasVisibleLayerSlider()) {
                drawVisibleLayerSlider(context, outerRect);
            }
            for (StructureLibSceneMetadata.ChannelData channelData : getBottomControlStructureLibChannels()) {
                drawStructureLibChannelSlider(context, outerRect, channelData);
            }
        }
    }

    private int getBottomControlRowCount() {
        if (!hasBottomControls()) {
            return 0;
        }
        return (ponderSceneData != null ? 1 : 0) + (hasStructureLibTierSlider() ? 1 : 0)
            + (hasVisibleLayerSlider() ? 1 : 0)
            + getBottomControlStructureLibChannels().size();
    }

    private boolean updateCurrentMousePosition() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            int scaledWidth = DisplayScale.scaledWidth();
            int scaledHeight = DisplayScale.scaledHeight();
            currentMouseAbsX = Mouse.getX() * scaledWidth / mc.displayWidth;
            currentMouseAbsY = scaledHeight - Mouse.getY() * scaledHeight / mc.displayHeight - 1;
            currentMousePositionAvailable = true;
            return true;
        } catch (Throwable ignored) {
            currentMouseAbsX = -1;
            currentMouseAbsY = -1;
            currentMousePositionAvailable = false;
            return false;
        }
    }

    private int bottomControlAreaTop(int outerBottom) {
        return outerBottom - getBottomControlAreaHeight();
    }

    private int resolveVisibleLayerRowIndex() {
        if (!hasVisibleLayerSlider()) {
            return -1;
        }
        return (ponderSceneData != null ? 1 : 0) + (hasStructureLibTierSlider() ? 1 : 0);
    }

    private int resolveStructureLibTierRowIndex() {
        return hasStructureLibTierSlider() ? (ponderSceneData != null ? 1 : 0) : -1;
    }

    private int resolveStructureLibChannelRowIndex(String channelId) {
        List<StructureLibSceneMetadata.ChannelData> channels = getBottomControlStructureLibChannels();
        int ponderOffset = ponderSceneData != null ? 1 : 0;
        int index = 0;
        for (StructureLibSceneMetadata.ChannelData channelData : channels) {
            if (channelData.getChannelId()
                .equals(StructureLibPreviewSelection.normalizeChannelId(channelId))) {
                return ponderOffset + (hasStructureLibTierSlider() ? 1 : 0) + (hasVisibleLayerSlider() ? 1 : 0) + index;
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
            highlighted = currentMousePositionAvailable && screenGeometry.hitRect()
                .contains(currentMouseAbsX, currentMouseAbsY);
        }
        String label = getVisibleLayerSliderLabel();
        logSliderGeometry("visible-layer", screenGeometry, rowIndex, label, outerRect);
        drawSlider(context, renderGeometry, highlighted, outerRect, rowIndex, label, VISIBLE_LAYER_SLIDER_TEXT_STYLE);
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
            highlighted = currentMousePositionAvailable && screenGeometry.hitRect()
                .contains(currentMouseAbsX, currentMouseAbsY);
        }
        String label = getStructureLibTierSliderLabel();
        logSliderGeometry("structurelib-tier", screenGeometry, rowIndex, label, outerRect);
        drawSlider(
            context,
            renderGeometry,
            highlighted,
            outerRect,
            rowIndex,
            label,
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
            highlighted = currentMousePositionAvailable && screenGeometry.hitRect()
                .contains(currentMouseAbsX, currentMouseAbsY);
        }
        String label = getStructureLibChannelSliderLabel(channelData);
        logSliderGeometry(
            "structurelib-channel:" + channelData.getChannelId(),
            screenGeometry,
            rowIndex,
            label,
            outerRect);
        drawSlider(
            context,
            renderGeometry,
            highlighted,
            outerRect,
            rowIndex,
            label,
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

    private void logBottomControlState(String phase, LytRect outerRect, int bottomControlAreaHeight) {
        // Skip the per-frame string construction (key + describeRect + varargs boxing) entirely
        // when debug logging is disabled. logInfoOnce itself also re-checks, but only after we
        // have already paid for the formatting work.
        if (!GuideDebugLog.isEnabled()) {
            return;
        }
        int selectableChannels = selectableStructureLibChannels.size();
        int rowCount = (ponderSceneData != null ? 1 : 0) + (hasStructureLibTierData() ? 1 : 0)
            + (hasVisibleLayerSlider() ? 1 : 0)
            + selectableChannels;
        GuideGregTechTileSupport.logInfoOnce(
            "scene-bottom-controls:" + Integer.toHexString(System.identityHashCode(this))
                + ":"
                + phase
                + ":"
                + bottomControlAreaHeight
                + ":"
                + rowCount
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
            bottomControlAreaHeight,
            rowCount);
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
